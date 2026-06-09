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
HOME_ONLY_ANCHORS = (
    "主屏保持干净",
    "开始一次真实 Agent 对话",
)


@dataclass
class CommandResult:
    args: list[str]
    returncode: int
    stdout: bytes
    stderr: bytes

    @property
    def text(self) -> str:
        return (self.stdout + self.stderr).decode("utf-8", errors="replace")


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


def matching_terms(texts: list[str], terms: tuple[str, ...]) -> list[str]:
    joined = "\n".join(texts)
    lowered = joined.lower()
    return [term for term in terms if term.lower() in lowered]


def status_from_texts(
    texts: list[str],
    package_seen: bool,
    *,
    screenshot_bytes: int = 1,
    device_locked: bool = False,
) -> tuple[str, str, dict[str, list[str]]]:
    hits = {
        "lockscreen": matching_terms(texts, LOCKSCREEN_TERMS),
        "chat_required": matching_terms(texts, CHAT_REQUIRED_ANCHORS),
        "answer": matching_terms(texts, ANSWER_ANCHORS),
        "result_block": matching_terms(texts, RESULT_BLOCK_ANCHORS),
        "tool": matching_terms(texts, TOOL_ANCHORS),
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
    if len(hits["chat_required"]) < len(CHAT_REQUIRED_ANCHORS):
        return (
            "partial-ai-chat-not-detected",
            "App content was captured, but AI chat title and assistant identity were not both detected.",
            hits,
        )
    if hits["home_only"] and not hits["answer"] and not hits["result_block"]:
        return (
            "partial-still-on-ai-home",
            "The UI appears to be the AI home entry instead of a completed or streaming chat.",
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
                f"- Question hint: `{args.question}`",
                f"- Wake before capture: `{args.wake}`",
                f"- Send question automatically: `{args.send_question}`",
                "",
                "This package proves only the visible device state captured here.",
                "It does not prove provider model streaming unless logcat/SSE evidence in the same package shows `delta_source=model_stream`.",
            ]
        ),
    )

    devices = run(adb_cmd(adb, None, "devices", "-l"), check=True)
    (output_dir / "01-adb-devices.txt").write_bytes(devices.stdout)
    serial = args.serial or infer_single_device(devices.text)
    if not serial:
        write_verdict(output_dir, "blocked-no-device", "No single adb device could be inferred.", [], {}, False, 0, False)
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
    start = run(adb_cmd(adb, serial, "shell", "am", "start", "-n", args.activity), check=False)
    (output_dir / "04-am-start.txt").write_bytes(start.stdout + start.stderr)
    time.sleep(args.settle_seconds)

    if args.send_question:
        # Best effort only. The verdict remains partial unless the UI tree proves
        # chat/result/tool anchors after input.
        run(adb_cmd(adb, serial, "shell", "input", "text", shell_input_text(args.question)), check=False)
        run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_ENTER"), check=False)
        time.sleep(args.chat_wait_seconds)

    window_after = collect_window_state(adb, serial)
    write_text(output_dir / "05-window-state-after.txt", window_after)

    raw_tree = run(adb_cmd(adb, serial, "exec-out", "uiautomator", "dump", "/dev/tty"), check=False, timeout=20)
    raw_text = raw_tree.text
    clean_xml = clean_ui_xml(raw_text)
    write_text(output_dir / "06-ui-tree-ai-chat.xml", raw_text)
    write_text(output_dir / "06-ui-tree-ai-chat-clean.xml", clean_xml)

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

    texts = extract_ui_texts(clean_xml)
    write_text(output_dir / "10-ui-texts.txt", "\n".join(texts))
    device_locked = is_device_locked(window_after)
    package_seen = package_visible(args.package, clean_xml, window_after, device_locked=device_locked)
    screenshot_bytes = screenshot_path.stat().st_size if screenshot_path.exists() else 0
    status, reason, hits = status_from_texts(
        texts,
        package_seen,
        screenshot_bytes=screenshot_bytes,
        device_locked=device_locked,
    )
    write_verdict(output_dir, status, reason, texts, hits, package_seen, screenshot_bytes, device_locked)
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
        r"MiuiKeyguard",
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
) -> None:
    write_json(
        output_dir / "11-chat-evidence.json",
        {
            "status": status,
            "reason": reason,
            "package_seen": package_seen,
            "device_locked": device_locked,
            "screenshot_bytes": screenshot_bytes,
            "required_chat_anchors": list(CHAT_REQUIRED_ANCHORS),
            "answer_anchors_any": list(ANSWER_ANCHORS),
            "result_block_anchors_any": list(RESULT_BLOCK_ANCHORS),
            "tool_anchors_any": list(TOOL_ANCHORS),
            "hits": hits,
            "ui_text_preview": texts[:100],
        },
    )
    write_text(output_dir / "12-conclusion.md", conclusion_md(status, reason, texts, hits))


def conclusion_md(status: str, reason: str, texts: list[str], hits: dict[str, list[str]]) -> str:
    preview = texts[:80]
    lines = [
        "# Device AI Chat Evidence Conclusion",
        "",
        f"Status: `{status}`",
        "",
        reason,
        "",
        "Captured checks:",
        "",
    ]
    for key in ("lockscreen", "chat_required", "answer", "result_block", "tool", "home_only"):
        values = hits.get(key, [])
        lines.append(f"- {key}: `{', '.join(values) if values else 'none'}`")
    lines.extend(
        [
            "",
            "UI text preview:",
            "",
            *(f"- {item}" for item in preview),
            "",
            "If status is not `pass-for-device-ai-chat-evidence`, keep this evidence as a failed/partial attempt only.",
            "Do not use it to claim AI chat, Markdown, result-block, tool-hint, or agent streaming acceptance.",
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
    locked_window = "mFocusedApp=ActivityRecord{... com.zhihuiji.app/.MainActivity}\nisKeyguardShowing=true"
    assert is_device_locked(locked_window)
    assert not package_visible("com.zhihuiji.app", "", locked_window, device_locked=True)
    unlocked_window = "mCurrentFocus=Window{... com.zhihuiji.app/com.zhihuiji.app.MainActivity}"
    assert package_visible("com.zhihuiji.app", "", unlocked_window, device_locked=False)
    raw = "ignored\n<?xml version='1.0'?><hierarchy><node text='AI 对话'/></hierarchy>\nUI hierchary dumped"
    assert clean_ui_xml(raw).startswith("<?xml")
    assert extract_ui_texts(clean_ui_xml(raw)) == ["AI 对话"]
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
    parser.add_argument("--settle-seconds", type=float, default=float(os.environ.get("SETTLE_SECONDS", "3")))
    parser.add_argument("--chat-wait-seconds", type=float, default=float(os.environ.get("CHAT_WAIT_SECONDS", "8")))
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
