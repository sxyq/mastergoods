#!/usr/bin/env python3
"""Capture Android AI chat evidence without faking an acceptance pass.

This script complements capture_ai_home_device_evidence.py. It only marks a
capture as device-chat evidence when the device is unlocked, the app is visible,
and the UI tree contains AI chat anchors plus answer/result/evidence signals.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


DEFAULT_ADB = Path("/Users/sunyiyang/Library/Android/sdk/platform-tools/adb")
DEFAULT_OUTPUT_ROOT = Path("docs/acceptance-evidence/ai-agent")
DEFAULT_PACKAGE = "com.zhihuiji.app"
DEFAULT_ACTIVITY = "com.zhihuiji.app/.MainActivity"
DEFAULT_BACKEND_PORT = 18080
DEFAULT_QUESTION = "客户应收情况"
EXTRA_AGENT_OPEN_CHAT = "com.zhihuiji.app.extra.AGENT_OPEN_CHAT"
EXTRA_AGENT_INITIAL_QUESTION = "com.zhihuiji.app.extra.AGENT_INITIAL_QUESTION"

LOCKSCREEN_TERMS = (
    "手电筒",
    "拍照",
    "电池电量",
    "WLAN 信号",
    "5G信号",
)
CHAT_REQUIRED_ANCHORS = (
    "AI 对话",
    "智慧记 AI",
)
CHAT_SHELL_ANCHORS = (
    "服务端问答与结果块",
    "输入经营问题",
    "服务端查询",
    "模型流",
    "图表结果",
)
ANSWER_ANCHORS = (
    "真实",
    "查询",
    "规则摘要",
    "模型",
    "Markdown",
    "应收",
    "库存",
    "销售",
    "客户",
)
RESULT_BLOCK_ANCHORS = (
    "查询结果",
    "实时结果",
    "依据",
    "调用",
    "范围",
    "截断",
    "上限",
    "图表",
    "表格",
)
TOOL_ANCHORS = (
    "正在查询真实业务数据",
    "工具查询完成",
    "工具查询失败",
    "customer receivable lookup",
    "customer_receivable_lookup",
    "sales overview lookup",
    "sales_overview_lookup",
    "inventory low stock lookup",
    "inventory_low_stock_lookup",
)
SAFETY_BLOCK_ANCHORS = (
    "安全拦截",
    "安全审查未通过",
    "该请求不能执行",
)
STOP_CANCEL_ANCHORS = (
    "停止接收",
    "已停止本机接收",
    "生成已取消",
    "服务端取消",
    "取消未确认",
    "取消失败",
)
CLEAR_CHAT_ANCHORS = (
    "清空对话",
    "开始一次真实 Agent 对话",
    "还没有对话",
)
HOME_ONLY_ANCHORS = (
    "主屏保持干净",
    "开始一次真实 Agent 对话",
)
STATIC_SHELL_TEXTS = {
    "AI",
    "从一个问题开始",
    "发送问题后，服务端会基于当前账号权限选择可用工具，并返回 Markdown、表格或统计图。",
    "服务端查询",
    "模型流",
    "图表结果",
}


@dataclass
class CommandResult:
    args: list[str]
    returncode: int
    stdout: bytes
    stderr: bytes

    @property
    def text(self) -> str:
        return (self.stdout + self.stderr).decode("utf-8", errors="replace")


@dataclass
class UiNode:
    text: str
    content_desc: str
    resource_id: str
    class_name: str
    package: str
    bounds: tuple[int, int, int, int] | None
    clickable: bool
    enabled: bool

    @property
    def combined_text(self) -> str:
        joined = " ".join(part for part in (self.text, self.content_desc) if part).strip()
        return joined or self.resource_id


@dataclass
class UiSnapshot:
    window_state: str
    raw_xml: str
    clean_xml: str
    texts: list[str]
    nodes: list[UiNode]
    package_seen: bool
    device_locked: bool


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def run(args: list[str], *, check: bool = False, timeout: float = 30) -> CommandResult:
    result = subprocess.run(args, capture_output=True, timeout=timeout, check=False)
    wrapped = CommandResult(args=args, returncode=result.returncode, stdout=result.stdout, stderr=result.stderr)
    if check and result.returncode != 0:
        raise RuntimeError(f"command failed ({result.returncode}): {' '.join(args)}\n{wrapped.text}")
    return wrapped


def adb_cmd(adb: Path, serial: str | None, *args: str) -> list[str]:
    cmd = [str(adb)]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(args)
    return cmd


def clean_ui_xml(raw: str) -> str:
    start = raw.find("<?xml")
    end = raw.rfind("</hierarchy>")
    if start >= 0 and end >= 0:
        return raw[start : end + len("</hierarchy>")]
    return raw


def extract_ui_texts(xml_text: str) -> list[str]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return []
    texts: list[str] = []
    for node in root.iter("node"):
        for key in ("text", "content-desc"):
            value = (node.attrib.get(key) or "").strip()
            if value:
                texts.append(value)
    return texts


def parse_bounds(raw_bounds: str) -> tuple[int, int, int, int] | None:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", raw_bounds.strip())
    if not match:
        return None
    left, top, right, bottom = (int(value) for value in match.groups())
    if right <= left or bottom <= top:
        return None
    return left, top, right, bottom


def extract_ui_nodes(xml_text: str) -> list[UiNode]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return []
    nodes: list[UiNode] = []
    for node in root.iter("node"):
        nodes.append(
            UiNode(
                text=(node.attrib.get("text") or "").strip(),
                content_desc=(node.attrib.get("content-desc") or "").strip(),
                resource_id=(node.attrib.get("resource-id") or "").strip(),
                class_name=(node.attrib.get("class") or "").strip(),
                package=(node.attrib.get("package") or "").strip(),
                bounds=parse_bounds(node.attrib.get("bounds") or ""),
                clickable=(node.attrib.get("clickable") or "").lower() == "true",
                enabled=(node.attrib.get("enabled") or "").lower() != "false",
            )
        )
    return nodes


def matching_terms(texts: list[str], terms: tuple[str, ...]) -> list[str]:
    joined = "\n".join(texts)
    lowered = joined.lower()
    return [term for term in terms if term.lower() in lowered]


def text_for_evidence(node: UiNode, input_bounds: list[tuple[int, int, int, int]]) -> list[str]:
    if node.bounds and any(bounds_contains(bounds, node.bounds) for bounds in input_bounds):
        return []
    values = []
    for value in (node.text, node.content_desc):
        stripped = value.strip()
        if not stripped or stripped in STATIC_SHELL_TEXTS:
            continue
        values.append(stripped)
    return values


def node_matches_terms(node: UiNode, terms: tuple[str, ...] | list[str]) -> bool:
    haystack = normalize_visibility_text(" ".join(
        part for part in (node.text, node.content_desc, node.resource_id) if part
    ))
    return any(normalize_visibility_text(term) in haystack for term in terms if term)


def find_node(nodes: list[UiNode], terms: tuple[str, ...] | list[str], *, clickable_only: bool = False) -> UiNode | None:
    for node in nodes:
        if clickable_only and (not node.clickable or not node.enabled):
            continue
        if node_matches_terms(node, terms):
            return node
    return None


def node_center(node: UiNode) -> tuple[int, int] | None:
    if node.bounds is None:
        return None
    left, top, right, bottom = node.bounds
    return (left + right) // 2, (top + bottom) // 2


def bounds_contains(outer: tuple[int, int, int, int] | None, inner: tuple[int, int, int, int] | None) -> bool:
    if outer is None or inner is None:
        return False
    left, top, right, bottom = outer
    inner_left, inner_top, inner_right, inner_bottom = inner
    return left <= inner_left and top <= inner_top and right >= inner_right and bottom >= inner_bottom


def clickable_container_for(nodes: list[UiNode], target: UiNode | None, *, require_enabled: bool = True) -> UiNode | None:
    if target is None or target.bounds is None:
        return None
    candidates = [
        node
        for node in nodes
        if node.clickable and (node.enabled or not require_enabled) and bounds_contains(node.bounds, target.bounds)
    ]
    if not candidates:
        return None
    return min(
        candidates,
        key=lambda node: (node.bounds[2] - node.bounds[0]) * (node.bounds[3] - node.bounds[1]) if node.bounds else 10**12,
    )


def parse_raw_sse_events(raw: str) -> list[dict[str, object]]:
    events: list[dict[str, object]] = []
    current_event: str | None = None
    data_lines: list[str] = []

    def flush() -> None:
        nonlocal current_event, data_lines
        if not data_lines:
            current_event = None
            return
        data_text = "\n".join(data_lines)
        data_lines = []
        try:
            payload = json.loads(data_text)
        except json.JSONDecodeError:
            payload = {"raw_data": data_text}
        if not isinstance(payload, dict):
            payload = {"data": payload}
        event_type = str(payload.get("event_type") or payload.get("eventType") or current_event or "message")
        events.append({"event_type": event_type, "payload": payload})
        current_event = None

    for raw_line in raw.splitlines():
        line = raw_line.rstrip("\r")
        if not line.strip():
            flush()
            continue
        if line.startswith(":"):
            continue
        if line.startswith("event:"):
            current_event = line[6:].strip()
            continue
        if line.startswith("data:"):
            data_lines.append(line[5:].lstrip())
            continue
        flush()
        stripped = line.strip()
        if not stripped or stripped == "[DONE]":
            continue
        try:
            payload = json.loads(stripped)
        except json.JSONDecodeError:
            continue
        if isinstance(payload, dict):
            event_type = str(payload.get("event_type") or payload.get("eventType") or "message")
            events.append({"event_type": event_type, "payload": payload})
    flush()
    return events


def payload_value(payload: dict[str, object], *keys: str) -> object | None:
    for key in keys:
        if key in payload:
            return payload[key]
    data = payload.get("data")
    if isinstance(data, dict):
        for key in keys:
            if key in data:
                return data[key]
    return None


def non_model_delta_texts_from_raw_sse(raw: str) -> list[dict[str, str]]:
    deltas: list[dict[str, str]] = []
    for event in parse_raw_sse_events(raw):
        if str(event.get("event_type") or "") != "answer_delta":
            continue
        payload = event.get("payload")
        if not isinstance(payload, dict):
            continue
        source = payload_value(payload, "delta_source", "deltaSource")
        if source is None or source == "model_stream":
            continue
        delta = payload_value(payload, "delta")
        if isinstance(delta, str) and delta.strip():
            deltas.append({"source": str(source), "text": delta.strip()})
    return deltas


def normalize_visibility_text(text: str) -> str:
    return "".join(char.lower() for char in text if char.isalnum())


def visibility_probe_fragments(text: str, *, min_length: int = 8, window: int = 32) -> list[str]:
    normalized = normalize_visibility_text(text)
    if len(normalized) < min_length:
        return [normalized] if normalized else []
    window_size = min(window, len(normalized))
    middle_start = max(0, len(normalized) // 2 - min_length // 2)
    probes = [
        normalized,
        normalized[:window_size],
        normalized[middle_start : middle_start + window_size],
        normalized[-window_size:],
        normalized[:min_length],
        normalized[middle_start : middle_start + min_length],
        normalized[-min_length:],
    ]
    probes.extend(
        normalized[index : index + min_length]
        for index in range(0, max(1, len(normalized) - min_length + 1), min_length)
    )
    deduped: list[str] = []
    for probe in probes:
        if len(probe) >= min_length and probe not in deduped:
            deduped.append(probe)
    return deduped


def non_model_delta_visibility(
    texts: list[str],
    raw_sse_path: str | None,
) -> tuple[bool | None, list[dict[str, str]]]:
    if not raw_sse_path:
        return None, []
    raw_path = Path(raw_sse_path)
    if not raw_path.exists():
        return None, []
    deltas = non_model_delta_texts_from_raw_sse(raw_path.read_text(encoding="utf-8", errors="replace"))
    if not deltas:
        return False, []
    joined = normalize_visibility_text("\n".join(texts))
    visible = []
    for item in deltas:
        probes = visibility_probe_fragments(item["text"])
        matched = [probe for probe in probes if probe and probe in joined]
        if matched:
            visible.append({**item, "matched_fragment": matched[0]})
    return bool(visible), visible


def scenario_guidance(scenario: str) -> list[str]:
    if scenario == "safety-block":
        return [
            "Send a high-risk or disallowed prompt before capture.",
            "Expected UI: assistant message is an error terminal state, stop affordance is gone, RunTrace contains failed safety result.",
        ]
    if scenario == "stop":
        return [
            "Start a streaming AI run, tap the stop control with content-desc/text `停止接收`, then capture.",
            "Expected UI: local receiving stopped and server cancel result is shown honestly.",
        ]
    if scenario == "clear":
        return [
            "Start a streaming AI run or keep an active conversation, tap `清空对话`, then capture.",
            "Expected UI: chat is cleared only after the active run has been cancelled or cancellation is explicitly unconfirmed.",
        ]
    return [
        "Capture a normal completed or streaming AI chat with answer, result block and real tool anchor.",
    ]


def looks_like_chat_input(node: UiNode) -> bool:
    if not node.enabled:
        return False
    haystack = normalize_visibility_text(" ".join(
        part for part in (node.text, node.content_desc, node.resource_id, node.class_name) if part
    ))
    return (
        "edittext" in haystack
        or "input" in haystack
        or "message" in haystack
        or "question" in haystack
        or "输入" in haystack
        or "提问" in haystack
        or "请输入" in haystack
    )


def looks_like_login_input(node: UiNode, label: str) -> bool:
    if not node.enabled:
        return False
    haystack = normalize_visibility_text(" ".join(
        part for part in (node.text, node.content_desc, node.resource_id, node.class_name) if part
    ))
    return "edittext" in haystack and normalize_visibility_text(label) in haystack


def capture_ui_snapshot(adb: Path, serial: str, package_name: str) -> UiSnapshot:
    window_state = collect_window_state(adb, serial)
    raw_tree = run(adb_cmd(adb, serial, "exec-out", "uiautomator", "dump", "/dev/tty"), check=False, timeout=20)
    raw_xml = raw_tree.text
    clean_xml = clean_ui_xml(raw_xml)
    texts = extract_ui_texts(clean_xml)
    nodes = extract_ui_nodes(clean_xml)
    device_locked = is_device_locked(window_state)
    return UiSnapshot(
        window_state=window_state,
        raw_xml=raw_xml,
        clean_xml=clean_xml,
        texts=texts,
        nodes=nodes,
        package_seen=package_visible(package_name, clean_xml, window_state, device_locked=device_locked),
        device_locked=device_locked,
    )


def evidence_texts_from_nodes(nodes: list[UiNode]) -> list[str]:
    input_bounds = [node.bounds for node in nodes if looks_like_chat_input(node) and node.bounds]
    texts: list[str] = []
    for node in nodes:
        texts.extend(text_for_evidence(node, input_bounds))
    return texts


def classify_ui_state(texts: list[str], hits: dict[str, list[str]]) -> str:
    if hits["lockscreen"]:
        return "locked"
    if hits["home_only"] and not hits["answer"] and not hits["result_block"]:
        return "ai_home"
    if hits["chat_required"] and not hits["answer"] and not hits["result_block"]:
        return "chat_idle"
    if hits["chat_required"] and hits["answer"] and not hits["result_block"]:
        return "chat_answer_visible"
    if hits["chat_required"] and hits["answer"] and hits["result_block"]:
        return "chat_result_visible"
    if texts:
        return "unknown_visible"
    return "unknown_empty"


def tap_screen(adb: Path, serial: str, point: tuple[int, int]) -> None:
    x, y = point
    run(adb_cmd(adb, serial, "shell", "input", "tap", str(x), str(y)), check=False)


def focus_chat_input(adb: Path, serial: str, nodes: list[UiNode]) -> bool:
    input_node = next((node for node in nodes if looks_like_chat_input(node) and node.bounds), None)
    if input_node is None:
        return False
    center = node_center(input_node)
    if center is None:
        return False
    tap_screen(adb, serial, center)
    time.sleep(0.4)
    return True


def fill_text_input(adb: Path, serial: str, node: UiNode, text: str) -> bool:
    center = node_center(node)
    if center is None:
        return False
    tap_screen(adb, serial, center)
    time.sleep(0.3)
    send_text_to_device(adb, serial, text)
    time.sleep(0.6)
    return True


def clear_chat_input(adb: Path, serial: str, nodes: list[UiNode]) -> None:
    input_node = next((node for node in nodes if looks_like_chat_input(node)), None)
    existing_text = input_node.text if input_node else ""
    if not existing_text:
        return
    for _ in range(max(len(existing_text), 24)):
        run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_DEL"), check=False)
    time.sleep(0.2)


def maybe_login(adb: Path, serial: str, package_name: str, nodes: list[UiNode], phone: str, password: str) -> bool:
    if not phone or not password:
        return False
    phone_label = find_node(nodes, ("手机号",))
    password_label = find_node(nodes, ("密码",))
    phone_field = clickable_container_for(nodes, phone_label, require_enabled=False) or phone_label
    password_field = clickable_container_for(nodes, password_label, require_enabled=False) or password_label
    login_label = find_node(nodes, ("登录",))
    login_button = clickable_container_for(nodes, login_label) or login_label
    if phone_field is None or password_field is None or login_button is None:
        return False
    if not fill_text_input(adb, serial, phone_field, phone):
        return False
    if not fill_text_input(adb, serial, password_field, password):
        return False
    refreshed_nodes = capture_ui_snapshot(adb, serial, package_name).nodes
    refreshed_label = find_node(refreshed_nodes, ("登录",))
    refreshed_button = clickable_container_for(refreshed_nodes, refreshed_label, require_enabled=False) or refreshed_label
    if refreshed_button is None or refreshed_button.bounds is None:
        return False
    center = node_center(refreshed_button)
    if center is None:
        return False
    tap_screen(adb, serial, center)
    time.sleep(1.2)
    return True


def send_text_to_device(adb: Path, serial: str, text: str) -> None:
    if text.isascii():
        run(adb_cmd(adb, serial, "shell", "input", "text", shell_input_text(text)), check=False)
        return
    clipboard_result = run(adb_cmd(adb, serial, "shell", "cmd", "clipboard", "set", "text", text), check=False)
    if clipboard_result.returncode == 0:
        run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_PASTE"), check=False)
        return
    run(adb_cmd(adb, serial, "shell", "input", "text", shell_input_text(text)), check=False)


def send_chat_message(adb: Path, serial: str, package_name: str, nodes: list[UiNode], question: str) -> bool:
    if not focus_chat_input(adb, serial, nodes):
        return False
    clear_chat_input(adb, serial, nodes)
    send_text_to_device(adb, serial, question)
    time.sleep(0.8)
    run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_BACK"), check=False)
    time.sleep(0.4)
    refreshed_nodes = capture_ui_snapshot(adb, serial, package_name).nodes
    raw_send_node = find_node(refreshed_nodes, ("发送", "send"))
    send_node = (
        find_node(refreshed_nodes, ("发送", "send"), clickable_only=True)
        or clickable_container_for(refreshed_nodes, raw_send_node)
        or clickable_container_for(refreshed_nodes, raw_send_node, require_enabled=False)
        or raw_send_node
    )
    if send_node and send_node.bounds:
        center = node_center(send_node)
        if center is not None:
            tap_screen(adb, serial, center)
            time.sleep(0.4)
            tap_screen(adb, serial, center)
            time.sleep(0.3)
            return True
    run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_ENTER"), check=False)
    return True


def maybe_open_chat_from_home(
    adb: Path,
    serial: str,
    nodes: list[UiNode],
    chat_entry_text: str,
) -> bool:
    entry_terms = tuple(filter(None, [chat_entry_text, "开始一次真实 Agent 对话", "真实 Agent 对话"]))
    entry = find_node(nodes, entry_terms, clickable_only=True) or find_node(nodes, entry_terms)
    entry = clickable_container_for(nodes, entry) or entry
    if entry is None or entry.bounds is None:
        return False
    center = node_center(entry)
    if center is None:
        return False
    tap_screen(adb, serial, center)
    return True


def maybe_open_assistant_tab(adb: Path, serial: str, nodes: list[UiNode]) -> bool:
    assistant_tab = find_node(nodes, ("助手",), clickable_only=True) or find_node(nodes, ("助手",))
    assistant_tab = clickable_container_for(nodes, assistant_tab) or assistant_tab
    if assistant_tab is None or assistant_tab.bounds is None:
        return False
    center = node_center(assistant_tab)
    if center is None:
        return False
    tap_screen(adb, serial, center)
    return True


def status_from_texts(
    texts: list[str],
    package_seen: bool,
    *,
    evidence_texts: list[str] | None = None,
    screenshot_bytes: int = 1,
    device_locked: bool = False,
    scenario: str = "chat",
) -> tuple[str, str, dict[str, list[str]]]:
    evidence_texts = evidence_texts or texts
    hits = {
        "lockscreen": matching_terms(texts, LOCKSCREEN_TERMS),
        "chat_required": matching_terms(texts, CHAT_REQUIRED_ANCHORS),
        "chat_shell": matching_terms(texts, CHAT_SHELL_ANCHORS),
        "answer": matching_terms(evidence_texts, ANSWER_ANCHORS),
        "result_block": matching_terms(evidence_texts, RESULT_BLOCK_ANCHORS),
        "tool": matching_terms(evidence_texts, TOOL_ANCHORS),
        "safety_block": matching_terms(evidence_texts, SAFETY_BLOCK_ANCHORS),
        "stop_cancel": matching_terms(evidence_texts, STOP_CANCEL_ANCHORS),
        "clear_chat": matching_terms(evidence_texts, CLEAR_CHAT_ANCHORS),
        "home_only": matching_terms(texts, HOME_ONLY_ANCHORS),
    }
    if device_locked or hits["lockscreen"]:
        return (
            "blocked-by-locked-device",
            "Keyguard/window state or UI tree indicates the device is locked instead of showing chat content.",
            hits,
        )
    if not package_seen:
        return (
            "partial-not-in-app",
            "The package was launched, but current window evidence does not prove app content.",
            hits,
        )
    if screenshot_bytes <= 0:
        return (
            "partial-missing-screenshot",
            "The app/window checks ran, but the screenshot file is empty.",
            hits,
        )
    if hits["home_only"] and not hits["answer"] and not hits["result_block"]:
        return (
            "partial-still-on-ai-home",
            "The UI appears to be the AI home entry instead of a completed or streaming chat.",
            hits,
        )
    chat_detected = len(hits["chat_required"]) >= len(CHAT_REQUIRED_ANCHORS) or (
        "AI 对话" in hits["chat_required"] and bool(hits["chat_shell"])
    )
    if not chat_detected:
        return (
            "partial-ai-chat-not-detected",
            "App content was captured, but AI chat title and assistant identity were not both detected.",
            hits,
        )
    if scenario == "safety-block":
        if not hits["safety_block"]:
            return (
                "partial-safety-block-not-visible",
                "Safety-block scenario was requested, but no safety block / safety review failure anchor was visible.",
                hits,
            )
        if hits["stop_cancel"] and "停止接收" in hits["stop_cancel"]:
            return (
                "partial-safety-stop-still-visible",
                "Safety-block state is visible, but the stop receiving affordance still appears in the UI tree.",
                hits,
            )
        return (
            "pass-for-device-safety-block-evidence",
            "UI tree contains AI chat and safety-block terminal anchors, and the stop receiving affordance is not visible.",
            hits,
        )
    if scenario == "stop":
        if not hits["stop_cancel"]:
            return (
                "partial-stop-cancel-feedback-not-visible",
                "Stop scenario was requested, but no stop/cancel feedback anchor was visible.",
                hits,
            )
        return (
            "pass-for-device-stop-feedback-evidence",
            "UI tree contains AI chat and stop/cancel feedback anchors. Pair this with interface cancel evidence before full pass.",
            hits,
        )
    if scenario == "clear":
        if not hits["clear_chat"]:
            return (
                "partial-clear-chat-state-not-visible",
                "Clear scenario was requested, but no clear-chat entry/empty-state anchor was visible.",
                hits,
            )
        return (
            "pass-for-device-clear-chat-evidence",
            "UI tree contains AI chat and clear-chat or clean-entry anchors. Pair this with cancel HTTP/SSE evidence before full pass.",
            hits,
        )
    if not hits["answer"]:
        return (
            "partial-answer-not-visible",
            "AI chat chrome is visible, but no answer/streaming text anchor was detected.",
            hits,
        )
    if not hits["result_block"]:
        return (
            "partial-result-block-not-visible",
            "An answer is visible, but no result block, evidence card, table, chart, or query-result anchor was detected.",
            hits,
        )
    if not hits["tool"]:
        return (
            "partial-tool-status-not-visible",
            "Answer and result block anchors are visible, but no real tool hint or tool name anchor was detected.",
            hits,
        )
    return (
        "pass-for-device-ai-chat-evidence",
        "UI tree contains AI chat, answer, result/evidence, and real tool anchors with the app visible and unlocked.",
        hits,
    )


def capture(args: argparse.Namespace) -> Path:
    adb = Path(args.adb)
    if not adb.exists():
        raise FileNotFoundError(f"adb not found: {adb}")

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = Path(args.output_root) / f"{timestamp}-device-ai-chat"
    output_dir.mkdir(parents=True, exist_ok=False)

    write_text(
        output_dir / "00-env.md",
        "\n".join(
            [
                "# Device AI Chat Evidence Environment",
                "",
                f"- Captured at UTC: `{utc_now()}`",
                f"- Device serial: `{args.serial or 'auto'}`",
                f"- Package: `{args.package}`",
                f"- Activity: `{args.activity}`",
                f"- Backend reverse port: `{args.backend_port}`",
                f"- Scenario: `{args.scenario}`",
                f"- Question hint: `{args.question}`",
                f"- Raw SSE for non-model delta visibility: `{args.raw_sse or 'not provided'}`",
                f"- Wake before capture: `{args.wake}`",
                f"- Auto open chat: `{args.auto_open_chat}`",
                f"- Chat entry text: `{args.chat_entry_text}`",
                f"- Send question automatically: `{args.send_question}`",
                f"- Launch question via intent: `{args.launch_intent_question}`",
                f"- Poll timeout seconds: `{args.poll_timeout_seconds}`",
                "",
                "This package proves only the visible device state captured here.",
                "It does not prove provider model streaming unless logcat/SSE evidence in the same package shows `delta_source=model_stream`.",
                "",
                "Scenario guidance:",
                "",
                *(f"- {item}" for item in scenario_guidance(args.scenario)),
            ]
        ),
    )

    devices = run(adb_cmd(adb, None, "devices", "-l"), check=True)
    (output_dir / "01-adb-devices.txt").write_bytes(devices.stdout)
    serial = args.serial or infer_single_device(devices.text)
    if not serial:
        write_verdict(
            output_dir,
            "blocked-no-device",
            "No single adb device could be inferred.",
            [],
            {},
            False,
            0,
            False,
            args.scenario,
        )
        return output_dir

    window_before = collect_window_state(adb, serial)
    write_text(output_dir / "02-window-state-before.txt", window_before)
    reverse = run(adb_cmd(adb, serial, "reverse", f"tcp:{args.backend_port}", f"tcp:{args.backend_port}"), check=False)
    (output_dir / "03-adb-reverse.txt").write_bytes(reverse.stdout + reverse.stderr)
    reverse_list = run(adb_cmd(adb, serial, "reverse", "--list"), check=False)
    (output_dir / "03-adb-reverse-list.txt").write_bytes(reverse_list.stdout + reverse_list.stderr)

    if args.wake:
        run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP"), check=False)
        time.sleep(args.settle_seconds)
        run(adb_cmd(adb, serial, "shell", "input", "swipe", "540", "1900", "540", "600", "400"), check=False)
        time.sleep(args.settle_seconds)

    run(adb_cmd(adb, serial, "logcat", "-c"), check=False)
    start_args = ["shell", "am", "start", "-n", args.activity]
    if args.launch_intent_question and args.question.strip():
        start_args.extend(
            [
                "--ez",
                EXTRA_AGENT_OPEN_CHAT,
                "true",
                "--es",
                EXTRA_AGENT_INITIAL_QUESTION,
                args.question.strip(),
            ]
        )
    start = run(adb_cmd(adb, serial, *start_args), check=False)
    (output_dir / "04-am-start.txt").write_bytes(start.stdout + start.stderr)
    time.sleep(args.settle_seconds)
    poll_history: list[dict[str, object]] = []
    sent_question = False
    opened_chat = False
    opened_assistant_tab = False
    attempted_login = False
    deadline = time.time() + max(args.poll_timeout_seconds, args.chat_wait_seconds, 1)
    latest_snapshot = capture_ui_snapshot(adb, serial, args.package)
    latest_status = "partial-ai-chat-not-detected"
    latest_reason = "Initial snapshot not evaluated yet."
    latest_hits: dict[str, list[str]] = {}
    latest_evidence_texts: list[str] = []

    while True:
        latest_snapshot = capture_ui_snapshot(adb, serial, args.package)
        latest_evidence_texts = evidence_texts_from_nodes(latest_snapshot.nodes)
        screenshot_probe = run(adb_cmd(adb, serial, "exec-out", "screencap", "-p"), check=False, timeout=20)
        screenshot_bytes = len(screenshot_probe.stdout)
        latest_status, latest_reason, latest_hits = status_from_texts(
            latest_snapshot.texts,
            latest_snapshot.package_seen,
            evidence_texts=latest_evidence_texts,
            screenshot_bytes=screenshot_bytes,
            device_locked=latest_snapshot.device_locked,
            scenario=args.scenario,
        )
        poll_history.append(
            {
                "timestamp_utc": utc_now(),
                "status": latest_status,
                "reason": latest_reason,
                "ui_state": classify_ui_state(latest_snapshot.texts, latest_hits),
                "device_locked": latest_snapshot.device_locked,
                "package_seen": latest_snapshot.package_seen,
                "login_attempted": attempted_login,
                "assistant_tab_attempted": opened_assistant_tab,
                "chat_open_attempted": opened_chat,
                "question_send_attempted": sent_question,
            }
        )
        if latest_status.startswith("pass-for-device-"):
            break
        if latest_status == "blocked-by-locked-device":
            break
        if args.login_phone and args.login_password and not attempted_login:
            attempted_login = True
            if maybe_login(adb, serial, args.package, latest_snapshot.nodes, args.login_phone, args.login_password):
                time.sleep(args.settle_seconds)
                continue
        if args.auto_open_chat and not opened_assistant_tab and not latest_hits.get("chat_required"):
            if maybe_open_assistant_tab(adb, serial, latest_snapshot.nodes):
                opened_assistant_tab = True
                time.sleep(args.settle_seconds)
                continue
        if args.auto_open_chat and not opened_chat:
            if maybe_open_chat_from_home(adb, serial, latest_snapshot.nodes, args.chat_entry_text):
                opened_chat = True
                time.sleep(args.settle_seconds)
                continue
        if args.send_question and not args.launch_intent_question and not sent_question and latest_hits.get("chat_required"):
            if send_chat_message(adb, serial, args.package, latest_snapshot.nodes, args.question):
                sent_question = True
                time.sleep(args.chat_wait_seconds)
                continue
        if time.time() >= deadline:
            break
        time.sleep(min(args.settle_seconds, 1.5))

    write_json(output_dir / "05-state-poll.json", poll_history)
    write_text(output_dir / "05-window-state-after.txt", latest_snapshot.window_state)
    write_text(output_dir / "06-ui-tree-ai-chat.xml", latest_snapshot.raw_xml)
    write_text(output_dir / "06-ui-tree-ai-chat-clean.xml", latest_snapshot.clean_xml)

    screenshot = run(adb_cmd(adb, serial, "exec-out", "screencap", "-p"), check=False, timeout=20)
    screenshot_path = output_dir / "07-screenshot-ai-chat.png"
    screenshot_path.write_bytes(screenshot.stdout)

    logcat = run(adb_cmd(adb, serial, "logcat", "-d"), check=False, timeout=20)
    filtered = "\n".join(
        line
        for line in logcat.text.splitlines()
        if re.search(r"v2/agent|chat/stream|result_block|answer_delta|tool_|run_|OkHttpClient|MainActivity|zhihuiji", line, re.IGNORECASE)
    )
    write_text(output_dir / "08-logcat-filtered.txt", filtered or "(no matching logcat lines)")

    gfxinfo = run(adb_cmd(adb, serial, "shell", "dumpsys", "gfxinfo", args.package), check=False, timeout=20)
    write_text(output_dir / "09-gfxinfo.txt", gfxinfo.text)

    texts = latest_snapshot.texts
    write_text(output_dir / "10-ui-texts.txt", "\n".join(texts))
    device_locked = latest_snapshot.device_locked
    package_seen = latest_snapshot.package_seen
    screenshot_bytes = screenshot_path.stat().st_size if screenshot_path.exists() else 0
    status, reason, hits = status_from_texts(
        texts,
        package_seen,
        evidence_texts=latest_evidence_texts,
        screenshot_bytes=screenshot_bytes,
        device_locked=device_locked,
        scenario=args.scenario,
    )
    non_model_visible, visible_non_model_deltas = non_model_delta_visibility(latest_evidence_texts, args.raw_sse)
    write_verdict(
        output_dir,
        status,
        reason,
        texts,
        hits,
        package_seen,
        screenshot_bytes,
        device_locked,
        args.scenario,
        non_model_delta_visible_as_reply=non_model_visible,
        visible_non_model_deltas=visible_non_model_deltas,
    )
    return output_dir


def collect_window_state(adb: Path, serial: str) -> str:
    pieces: list[str] = []
    for command in (
        ("shell", "dumpsys", "window"),
        ("shell", "dumpsys", "activity", "activities"),
    ):
        result = run(adb_cmd(adb, serial, *command), check=False, timeout=20)
        filtered = "\n".join(
            line
            for line in result.text.splitlines()
            if re.search(r"mCurrentFocus|mFocusedApp|mDreamingLockscreen|mShowingLockscreen|Keyguard|ResumedActivity|topResumedActivity|com\.zhihuiji\.app", line)
        )
        pieces.append(f"$ adb {' '.join(command)}\n{filtered or '(no matching lines)'}")
    return "\n\n".join(pieces)


def is_device_locked(window_state: str) -> bool:
    lock_patterns = (
        r"isKeyguardShowing=true",
        r"KeyguardShowing=true",
        r"mDreamingLockscreen=true",
        r"NotificationShade",
    )
    return any(re.search(pattern, window_state) for pattern in lock_patterns)


def package_visible(package_name: str, clean_xml: str, window_state: str, *, device_locked: bool = False) -> bool:
    if device_locked:
        return False
    return package_name in clean_xml or package_name in window_state


def infer_single_device(devices_text: str) -> str | None:
    serials: list[str] = []
    for line in devices_text.splitlines():
        if "\tdevice" in line or re.search(r"\sdevice\s", line):
            serials.append(line.split()[0])
    return serials[0] if len(serials) == 1 else None


def shell_input_text(text: str) -> str:
    # adb input text has a tiny escaping language; keep this best-effort and
    # prefer ASCII prompts when automation must type.
    return text.replace(" ", "%s").replace("&", r"\&")


def write_text(path: Path, text: str) -> None:
    path.write_text(text.rstrip() + "\n", encoding="utf-8")


def write_json(path: Path, payload: object) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_verdict(
    output_dir: Path,
    status: str,
    reason: str,
    texts: list[str],
    hits: dict[str, list[str]],
    package_seen: bool,
    screenshot_bytes: int,
    device_locked: bool,
    scenario: str,
    non_model_delta_visible_as_reply: bool | None = None,
    visible_non_model_deltas: list[dict[str, str]] | None = None,
) -> None:
    write_json(
        output_dir / "11-chat-evidence.json",
        {
            "status": status,
            "reason": reason,
            "scenario": scenario,
            "package_seen": package_seen,
            "device_locked": device_locked,
            "screenshot_bytes": screenshot_bytes,
            "required_chat_anchors": list(CHAT_REQUIRED_ANCHORS),
            "chat_shell_anchors_any": list(CHAT_SHELL_ANCHORS),
            "answer_anchors_any": list(ANSWER_ANCHORS),
            "result_block_anchors_any": list(RESULT_BLOCK_ANCHORS),
            "tool_anchors_any": list(TOOL_ANCHORS),
            "safety_block_anchors_any": list(SAFETY_BLOCK_ANCHORS),
            "stop_cancel_anchors_any": list(STOP_CANCEL_ANCHORS),
            "clear_chat_anchors_any": list(CLEAR_CHAT_ANCHORS),
            "scenario_guidance": scenario_guidance(scenario),
            "non_model_delta_visible_as_reply": non_model_delta_visible_as_reply,
            "visible_non_model_deltas": visible_non_model_deltas or [],
            "hits": hits,
            "ui_text_preview": texts[:100],
        },
    )
    write_text(
        output_dir / "12-conclusion.md",
        conclusion_md(
            status,
            reason,
            texts,
            hits,
            scenario,
            non_model_delta_visible_as_reply=non_model_delta_visible_as_reply,
            visible_non_model_deltas=visible_non_model_deltas or [],
        ),
    )


def conclusion_md(
    status: str,
    reason: str,
    texts: list[str],
    hits: dict[str, list[str]],
    scenario: str,
    non_model_delta_visible_as_reply: bool | None = None,
    visible_non_model_deltas: list[dict[str, str]] | None = None,
) -> str:
    preview = texts[:80]
    lines = [
        "# Device AI Chat Evidence Conclusion",
        "",
        f"Status: `{status}`",
        "",
        f"Scenario: `{scenario}`",
        "",
        reason,
        "",
        "Captured checks:",
        "",
    ]
    for key in ("lockscreen", "chat_required", "chat_shell", "answer", "result_block", "tool", "safety_block", "stop_cancel", "clear_chat", "home_only"):
        values = hits.get(key, [])
        lines.append(f"- {key}: `{', '.join(values) if values else 'none'}`")
    lines.extend(
        [
            "",
            f"Non-model delta visible as reply: `{str(non_model_delta_visible_as_reply).lower() if non_model_delta_visible_as_reply is not None else 'unknown'}`",
            "",
            "Visible non-model deltas:",
            "",
            *(f"- {item['source']}: {item['text']}" for item in (visible_non_model_deltas or [])),
            *([] if visible_non_model_deltas else ["- none"]),
            "",
            "Scenario guidance:",
            "",
            *(f"- {item}" for item in scenario_guidance(scenario)),
            "",
            "UI text preview:",
            "",
            *(f"- {item}" for item in preview),
            "",
            "If status is not `pass-for-device-ai-chat-evidence`, keep this evidence as a failed/partial attempt only.",
            "Scenario pass statuses prove only the visible device state for that scenario.",
            "Do not use this file alone to claim AI chat, Markdown, result-block, tool-hint, safety-block, cancel, clear-chat, or agent streaming acceptance without the matching HTTP/SSE/audit evidence.",
        ]
    )
    return "\n".join(lines)


def self_test() -> None:
    locked = ["5:35", "手电筒", "拍照", "电池电量为百分之 89。"]
    assert status_from_texts(locked, package_seen=True)[0] == "blocked-by-locked-device"
    assert status_from_texts(["AI 对话", "智慧记 AI"], package_seen=True, device_locked=True)[0] == "blocked-by-locked-device"
    not_app = ["AI 对话", "智慧记 AI", "客户应收", "查询结果", "customer_receivable_lookup"]
    assert status_from_texts(not_app, package_seen=False)[0] == "partial-not-in-app"
    home = ["AI 助手", "主屏保持干净", "开始一次真实 Agent 对话"]
    assert status_from_texts(home, package_seen=True)[0] == "partial-ai-chat-not-detected"
    no_answer = ["AI 对话", "智慧记 AI"]
    assert status_from_texts(no_answer, package_seen=True)[0] == "partial-answer-not-visible"
    no_block = ["AI 对话", "智慧记 AI", "客户应收", "真实查询"]
    assert status_from_texts(no_block, package_seen=True)[0] == "partial-result-block-not-visible"
    no_tool = ["AI 对话", "智慧记 AI", "客户应收", "查询结果", "依据"]
    assert status_from_texts(no_tool, package_seen=True)[0] == "partial-tool-status-not-visible"
    passed = ["AI 对话", "智慧记 AI", "客户应收", "查询结果", "依据", "调用", "customer_receivable_lookup"]
    assert status_from_texts(passed, package_seen=True)[0] == "pass-for-device-ai-chat-evidence"
    safety_missing = ["AI 对话", "智慧记 AI", "客户应收"]
    assert status_from_texts(safety_missing, package_seen=True, scenario="safety-block")[0] == "partial-safety-block-not-visible"
    safety_with_stop = ["AI 对话", "智慧记 AI", "安全拦截", "停止接收"]
    assert status_from_texts(safety_with_stop, package_seen=True, scenario="safety-block")[0] == "partial-safety-stop-still-visible"
    safety_passed = ["AI 对话", "智慧记 AI", "安全拦截", "安全审查未通过"]
    assert status_from_texts(safety_passed, package_seen=True, scenario="safety-block")[0] == "pass-for-device-safety-block-evidence"
    stop_missing = ["AI 对话", "智慧记 AI", "客户应收"]
    assert status_from_texts(stop_missing, package_seen=True, scenario="stop")[0] == "partial-stop-cancel-feedback-not-visible"
    stop_passed = ["AI 对话", "智慧记 AI", "已停止本机接收", "服务端取消"]
    assert status_from_texts(stop_passed, package_seen=True, scenario="stop")[0] == "pass-for-device-stop-feedback-evidence"
    clear_missing = ["AI 对话", "智慧记 AI", "客户应收"]
    assert status_from_texts(clear_missing, package_seen=True, scenario="clear")[0] == "partial-clear-chat-state-not-visible"
    clear_passed = ["AI 对话", "智慧记 AI", "清空对话", "开始一次真实 Agent 对话"]
    assert status_from_texts(clear_passed, package_seen=True, scenario="clear")[0] == "pass-for-device-clear-chat-evidence"
    locked_window = "mFocusedApp=ActivityRecord{... com.zhihuiji.app/.MainActivity}\nisKeyguardShowing=true"
    assert is_device_locked(locked_window)
    assert not package_visible("com.zhihuiji.app", "", locked_window, device_locked=True)
    unlocked_window = "mCurrentFocus=Window{... com.zhihuiji.app/com.zhihuiji.app.MainActivity}"
    assert package_visible("com.zhihuiji.app", "", unlocked_window, device_locked=False)
    miui_wallpaper_residue = "\n".join(
        [
            "WindowStateAnimator{4e54161 com.miui.miwallpaper.wallpaperservice.MiuiKeyguardPictorialWallpaper}",
            "mCurrentFocus=Window{... com.zhihuiji.app/com.zhihuiji.app.MainActivity}",
            "isKeyguardShowing=false",
        ]
    )
    assert not is_device_locked(miui_wallpaper_residue)
    raw = "ignored\n<?xml version='1.0'?><hierarchy><node text='AI 对话'/></hierarchy>\nUI hierchary dumped"
    assert clean_ui_xml(raw).startswith("<?xml")
    assert extract_ui_texts(clean_ui_xml(raw)) == ["AI 对话"]
    sse = "\n".join(
        [
            'data: {"event_type":"answer_delta","deltaSource":"model_stream","delta":"模型回答"}',
            "",
            'data: {"event_type":"answer_delta","deltaSource":"server_notice","delta":"查询说明"}',
            "",
        ]
    )
    assert non_model_delta_texts_from_raw_sse(sse) == [{"source": "server_notice", "text": "查询说明"}]
    assert normalize_visibility_text("查询\n 说明") == "查询说明"
    assert visibility_probe_fragments("查询说明") == ["查询说明"]
    with tempfile.TemporaryDirectory() as raw_dir:
        raw_sse = Path(raw_dir) / "02-raw-sse.log"
        raw_sse.write_text(sse, encoding="utf-8")
        assert non_model_delta_visibility(["AI 对话", "模型回答"], str(raw_sse)) == (False, [])
        assert non_model_delta_visibility(["AI 对话", "查询说明"], str(raw_sse)) == (
            True,
            [{"source": "server_notice", "text": "查询说明", "matched_fragment": "查询说明"}],
        )
        long_sse = "\n".join(
            [
                'data: {"event_type":"answer_delta","deltaSource":"server_notice","delta":"这是服务端补充的查询边界说明，请不要当作模型正文展示。"}',
                "",
            ]
        )
        raw_sse.write_text(long_sse, encoding="utf-8")
        visible, details = non_model_delta_visibility(["查询边界说明请不要当作模型"], str(raw_sse))
        assert visible is True
        assert details[0]["source"] == "server_notice"
        assert details[0]["matched_fragment"]
        visible, details = non_model_delta_visibility(["完全不同的正文"], str(raw_sse))
        assert visible is False
        assert details == []
    print("capture_ai_chat_device_evidence self-test passed")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", default=os.environ.get("ADB", str(DEFAULT_ADB)))
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--package", default=os.environ.get("PACKAGE_NAME", DEFAULT_PACKAGE))
    parser.add_argument("--activity", default=os.environ.get("ACTIVITY", DEFAULT_ACTIVITY))
    parser.add_argument("--backend-port", type=int, default=int(os.environ.get("BACKEND_PORT", DEFAULT_BACKEND_PORT)))
    parser.add_argument("--output-root", default=os.environ.get("EVIDENCE_ROOT", str(DEFAULT_OUTPUT_ROOT)))
    parser.add_argument("--question", default=os.environ.get("AI_CHAT_QUESTION", DEFAULT_QUESTION))
    parser.add_argument("--raw-sse", default=os.environ.get("AI_CHAT_RAW_SSE"))
    parser.add_argument("--auto-open-chat", action="store_true", default=os.environ.get("AUTO_OPEN_CHAT") == "1")
    parser.add_argument("--chat-entry-text", default=os.environ.get("CHAT_ENTRY_TEXT", "开始一次真实 Agent 对话"))
    parser.add_argument("--launch-intent-question", action="store_true", default=os.environ.get("LAUNCH_INTENT_QUESTION") == "1")
    parser.add_argument("--login-phone", default=os.environ.get("LOGIN_PHONE", ""))
    parser.add_argument("--login-password", default=os.environ.get("LOGIN_PASSWORD", ""))
    parser.add_argument(
        "--scenario",
        choices=("chat", "safety-block", "stop", "clear"),
        default=os.environ.get("AI_CHAT_SCENARIO", "chat"),
        help="Evidence scenario to evaluate. Non-chat scenarios require matching HTTP/SSE/audit evidence before full acceptance.",
    )
    parser.add_argument("--settle-seconds", type=float, default=float(os.environ.get("SETTLE_SECONDS", "3")))
    parser.add_argument("--chat-wait-seconds", type=float, default=float(os.environ.get("CHAT_WAIT_SECONDS", "8")))
    parser.add_argument("--poll-timeout-seconds", type=float, default=float(os.environ.get("POLL_TIMEOUT_SECONDS", "20")))
    parser.add_argument("--wake", action="store_true", default=os.environ.get("WAKE_DEVICE") == "1")
    parser.add_argument("--send-question", action="store_true", default=os.environ.get("SEND_QUESTION") == "1")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        self_test()
        return 0
    output_dir = capture(args)
    print(f"Device AI chat evidence written to: {output_dir}")
    conclusion = output_dir / "12-conclusion.md"
    if conclusion.exists():
        first_lines = "\n".join(conclusion.read_text(encoding="utf-8").splitlines()[:5])
        print(first_lines)
    return 0


if __name__ == "__main__":
    sys.exit(main())
