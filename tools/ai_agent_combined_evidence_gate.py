#!/usr/bin/env python3
"""Gate AI agent acceptance with both interface and Android device evidence.

Single-sided evidence is useful, but it must not be promoted to full
ChatGPT-like agent acceptance. This script reads an interface evidence package
and a device evidence package, then emits a combined verdict for one scenario.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


SCENARIO_DEVICE_PASS = {
    "chat": "pass-for-device-ai-chat-evidence",
    "safety-block": "pass-for-device-safety-block-evidence",
    "stop": "pass-for-device-stop-feedback-evidence",
    "clear": "pass-for-device-clear-chat-evidence",
}

SCENARIO_INTERFACE_FILE = {
    "chat": "18-result-block-evidence.md",
    "safety-block": "12-safety-block-evidence.md",
    "stop": "11-cancel-evidence.md",
    "clear": "11-cancel-evidence.md",
}


@dataclass(frozen=True)
class GateResult:
    status: str
    reasons: list[str]
    interface_status: str
    device_status: str
    has_model_stream: bool
    has_stream_order_risk: bool
    non_model_delta_sources: list[str]
    non_model_delta_visible_as_reply: bool | None
    visible_non_model_delta_sources: list[str]


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def markdown_status(text: str) -> str:
    match = re.search(r"^Status:\s*`?([^`\n]+)`?", text, re.MULTILINE)
    return match.group(1).strip() if match else "missing"


def device_status(device_dir: Path) -> str:
    evidence_file = device_dir / "11-chat-evidence.json"
    if not evidence_file.exists():
        return "missing"
    try:
        payload = json.loads(evidence_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return "invalid"
    return str(payload.get("status") or "missing")


def device_non_model_delta_visible_as_reply(device_dir: Path) -> bool | None:
    evidence_file = device_dir / "11-chat-evidence.json"
    if not evidence_file.exists():
        return None
    try:
        payload = json.loads(evidence_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None
    value = payload.get("non_model_delta_visible_as_reply")
    return value if isinstance(value, bool) else None


def device_visible_non_model_delta_sources(device_dir: Path) -> list[str]:
    evidence_file = device_dir / "11-chat-evidence.json"
    if not evidence_file.exists():
        return []
    try:
        payload = json.loads(evidence_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return []
    visible = payload.get("visible_non_model_deltas")
    if not isinstance(visible, list):
        return []
    sources: list[str] = []
    for item in visible:
        if not isinstance(item, dict):
            continue
        source = item.get("source")
        if not isinstance(source, str):
            continue
        if source not in sources:
            sources.append(source)
    return sources


def interface_status(interface_dir: Path, scenario: str) -> str:
    evidence_name = SCENARIO_INTERFACE_FILE[scenario]
    status = markdown_status(read_text(interface_dir / evidence_name))
    if status != "missing":
        return status
    if scenario == "chat":
        return markdown_status(read_text(interface_dir / "13-sse-audit-ui-reconciliation.md"))
    return status


def parse_sse_events(raw: str) -> list[dict[str, object]]:
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
        events.append(
            {
                "event_type": event_type,
                "payload": payload,
            }
        )
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


def nested_payload_value(payload: dict[str, object], *keys: str) -> object | None:
    for key in keys:
        if key in payload:
            return payload[key]
    data = payload.get("data")
    if isinstance(data, dict):
        for key in keys:
            if key in data:
                return data[key]
    return None


def has_provider_model_stream(interface_dir: Path) -> bool:
    raw_sse = read_text(interface_dir / "02-raw-sse.log")
    latency = read_text(interface_dir / "11-latency.md")
    for event in parse_sse_events(raw_sse):
        payload = event.get("payload")
        if not isinstance(payload, dict):
            continue
        if str(event.get("event_type") or "") != "answer_delta":
            continue
        if nested_payload_value(payload, "delta_source", "deltaSource") == "model_stream":
            return True
    return (
        "`model_stream_delta_count` | `0`" not in latency
        and "`model_stream_delta_count`" in latency
        and "Provider-backed `model_stream` timing is present" in latency
    )


def non_model_delta_sources(interface_dir: Path) -> list[str]:
    sources: list[str] = []
    for event in parse_sse_events(read_text(interface_dir / "02-raw-sse.log")):
        payload = event.get("payload")
        if not isinstance(payload, dict):
            continue
        if str(event.get("event_type") or "") != "answer_delta":
            continue
        source = nested_payload_value(payload, "delta_source", "deltaSource")
        if source is None or source == "model_stream":
            continue
        source_text = str(source)
        if source_text not in sources:
            sources.append(source_text)
    return sources


def stream_order_risks(interface_dir: Path) -> list[str]:
    events = parse_sse_events(read_text(interface_dir / "02-raw-sse.log"))
    if not events:
        return ["raw SSE is missing or contains no parseable events"]

    first_model_stream_index: int | None = None
    first_visible_non_model_answer_index: int | None = None
    first_answer_completed_index: int | None = None
    first_result_block_index: int | None = None
    first_server_notice_index: int | None = None
    model_stream_count = 0

    for index, event in enumerate(events):
        event_name = str(event.get("event_type") or "")
        payload = event.get("payload")
        payload = payload if isinstance(payload, dict) else {}
        if event_name == "answer_delta":
            source = nested_payload_value(payload, "delta_source", "deltaSource")
            if source == "model_stream":
                model_stream_count += 1
                if first_model_stream_index is None:
                    first_model_stream_index = index
            elif source != "server_notice" and first_visible_non_model_answer_index is None:
                first_visible_non_model_answer_index = index
            elif source == "server_notice" and first_server_notice_index is None:
                first_server_notice_index = index
        elif event_name == "answer_completed" and first_answer_completed_index is None:
            first_answer_completed_index = index
        elif event_name == "result_block" and first_result_block_index is None:
            first_result_block_index = index

    risks: list[str] = []
    if first_result_block_index is not None and first_model_stream_index is not None:
        if first_result_block_index < first_model_stream_index:
            risks.append("result_block appeared before the first provider model_stream delta")
    if first_server_notice_index is not None:
        if first_model_stream_index is None or first_server_notice_index < first_model_stream_index:
            risks.append("server_notice appeared before the first provider model_stream delta")
    if (
        first_result_block_index is not None
        and model_stream_count == 0
        and first_visible_non_model_answer_index is not None
        and first_result_block_index < first_visible_non_model_answer_index
    ):
        risks.append("result_block appeared before the first visible non-model answer delta")
    if (
        first_result_block_index is not None
        and model_stream_count == 0
        and first_visible_non_model_answer_index is None
        and first_answer_completed_index is not None
        and first_result_block_index < first_answer_completed_index
    ):
        risks.append("result_block appeared before answer_completed on a non-model run without any visible answer delta")
    return risks


def evaluate(interface_dir: Path, device_dir: Path, scenario: str) -> GateResult:
    reasons: list[str] = []
    i_status = interface_status(interface_dir, scenario)
    d_status = device_status(device_dir)
    model_stream = has_provider_model_stream(interface_dir)
    non_model_sources = non_model_delta_sources(interface_dir) if scenario == "chat" else []
    non_model_visible = device_non_model_delta_visible_as_reply(device_dir) if scenario == "chat" else None
    visible_non_model_sources = device_visible_non_model_delta_sources(device_dir) if scenario == "chat" else []
    order_risks = stream_order_risks(interface_dir) if scenario == "chat" else []

    if not interface_dir.exists():
        reasons.append(f"interface evidence dir missing: {interface_dir}")
    if not device_dir.exists():
        reasons.append(f"device evidence dir missing: {device_dir}")

    expected_device = SCENARIO_DEVICE_PASS[scenario]
    if d_status != expected_device:
        reasons.append(f"device status is `{d_status}`, expected `{expected_device}`")

    if scenario == "chat":
        allowed_visible_non_model_sources = {"rule_summary"}
        suspicious_non_model_sources = [
            source for source in non_model_sources if source not in allowed_visible_non_model_sources and source != "server_notice"
        ]
        visible_reply_risk_sources = [
            source for source in visible_non_model_sources if source not in allowed_visible_non_model_sources
        ]
        if i_status != "pass-for-interface":
            reasons.append(f"chat interface status is `{i_status}`, expected `pass-for-interface`")
        if not model_stream:
            reasons.append("provider `model_stream` was not observed; ChatGPT-like streaming remains partial")
        if suspicious_non_model_sources:
            reasons.append(
                "raw SSE includes unsupported non-model answer_delta sources "
                f"`{', '.join(suspicious_non_model_sources)}`"
            )
        if visible_reply_risk_sources:
            reasons.append(
                "Android UI appears to show non-rule-summary non-model answer_delta sources as reply text "
                f"`{', '.join(visible_reply_risk_sources)}`"
            )
        reasons.extend(order_risks)
        if not reasons:
            status = "pass-for-combined-provider-chat-evidence"
        elif d_status == expected_device and i_status == "pass-for-interface" and order_risks:
            status = "partial-combined-chat-stream-order-risk"
        elif d_status == expected_device and i_status == "pass-for-interface" and model_stream and visible_reply_risk_sources:
            status = "partial-combined-chat-non-model-delta-visibility-unknown"
        elif d_status == expected_device and i_status == "pass-for-interface":
            status = "partial-combined-chat-missing-provider-stream"
        else:
            status = "partial-combined-chat-evidence"
    elif scenario == "safety-block":
        if i_status != "pass-for-interface":
            reasons.append(f"safety interface status is `{i_status}`, expected `pass-for-interface`")
        status = "pass-for-combined-safety-block-evidence" if not reasons else "partial-combined-safety-block-evidence"
    else:
        if i_status != "pass-for-interface":
            reasons.append(f"cancel interface status is `{i_status}`, expected `pass-for-interface`")
        pass_status = f"pass-for-combined-{scenario}-evidence"
        partial_status = f"partial-combined-{scenario}-evidence"
        status = pass_status if not reasons else partial_status

    if not reasons:
        reasons.append("interface evidence and Android device evidence both satisfy this scenario gate")
    return GateResult(
        status,
        reasons,
        i_status,
        d_status,
        model_stream,
        bool(order_risks),
        non_model_sources,
        non_model_visible,
        visible_non_model_sources,
    )


def render_markdown(result: GateResult, interface_dir: Path, device_dir: Path, scenario: str) -> str:
    lines = [
        "# AI Agent Combined Evidence Gate",
        "",
        f"Status: `{result.status}`",
        "",
        f"- Scenario: `{scenario}`",
        f"- Interface evidence: `{interface_dir}`",
        f"- Device evidence: `{device_dir}`",
        f"- Interface status: `{result.interface_status}`",
        f"- Device status: `{result.device_status}`",
        f"- Provider model_stream observed: `{str(result.has_model_stream).lower()}`",
        f"- Stream ordering risk observed: `{str(result.has_stream_order_risk).lower()}`",
        f"- Non-model answer_delta sources: `{', '.join(result.non_model_delta_sources) if result.non_model_delta_sources else 'none'}`",
        f"- Non-model delta visible as reply: `{str(result.non_model_delta_visible_as_reply).lower() if result.non_model_delta_visible_as_reply is not None else 'unknown'}`",
        f"- Visible non-model delta sources in Android UI: `{', '.join(result.visible_non_model_delta_sources) if result.visible_non_model_delta_sources else 'none'}`",
        "",
        "Reasons:",
        "",
    ]
    lines.extend(f"- {reason}" for reason in result.reasons)
    lines.extend(
        [
            "",
            "This combined gate intentionally does not replace manual review of screenshots, raw SSE, audit JSON, logcat, or frame timing.",
            "It prevents interface-only or device-only evidence from being used as a full acceptance claim.",
            "",
        ]
    )
    return "\n".join(lines)


def write_file(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def self_test() -> None:
    with tempfile.TemporaryDirectory() as raw:
        root = Path(raw)
        interface = root / "interface"
        device = root / "device"
        interface.mkdir()
        device.mkdir()
        write_file(interface / "18-result-block-evidence.md", "Status: pass-for-interface\n")
        write_file(
            interface / "02-raw-sse.log",
            "\n".join(
                [
                    "event: answer_delta",
                    'data: {"event_type":"answer_delta",',
                    'data: "deltaSource":"model_stream",',
                    'data: "delta":"真实模型流"}',
                    "",
                ]
            ),
        )
        write_file(device / "11-chat-evidence.json", json.dumps({"status": "pass-for-device-ai-chat-evidence"}))
        result = evaluate(interface, device, "chat")
        assert result.status == "pass-for-combined-provider-chat-evidence", result
        assert parse_sse_events(read_text(interface / "02-raw-sse.log"))[0]["event_type"] == "answer_delta"

        write_file(
            interface / "02-raw-sse.log",
            "\n".join(
                [
                    'data: {"event_type":"result_block","run_id":"run-risk"}',
                    "",
                    'data: {"event_type":"answer_delta","run_id":"run-risk","deltaSource":"model_stream","delta":"真实模型流"}',
                    "",
                ]
            ),
        )
        result = evaluate(interface, device, "chat")
        assert result.status == "partial-combined-chat-stream-order-risk", result
        assert result.has_stream_order_risk, result
        assert any("result_block appeared before" in reason for reason in result.reasons), result

        write_file(
            interface / "02-raw-sse.log",
            "\n".join(
                [
                    'data: {"event_type":"answer_delta","run_id":"run-notice","deltaSource":"model_stream","delta":"真实模型流"}',
                    "",
                    'data: {"event_type":"answer_delta","run_id":"run-notice","deltaSource":"server_notice","delta":"查询说明"}',
                    "",
                ]
            ),
        )
        result = evaluate(interface, device, "chat")
        assert result.status == "pass-for-combined-provider-chat-evidence", result
        assert result.non_model_delta_sources == ["server_notice"], result
        assert result.visible_non_model_delta_sources == [], result

        write_file(
            device / "11-chat-evidence.json",
            json.dumps(
                {
                    "status": "pass-for-device-ai-chat-evidence",
                    "non_model_delta_visible_as_reply": False,
                    "visible_non_model_deltas": [{"source": "rule_summary", "text": "规则摘要"}],
                }
            ),
        )
        result = evaluate(interface, device, "chat")
        assert result.status == "pass-for-combined-provider-chat-evidence", result

        write_file(device / "11-chat-evidence.json", json.dumps({"status": "pass-for-device-ai-chat-evidence"}))
        write_file(interface / "02-raw-sse.log", 'data: {"event_type":"answer_completed"}\n')
        result = evaluate(interface, device, "chat")
        assert result.status == "partial-combined-chat-missing-provider-stream", result

        write_file(interface / "12-safety-block-evidence.md", "Status: pass-for-interface\n")
        write_file(device / "11-chat-evidence.json", json.dumps({"status": "pass-for-device-safety-block-evidence"}))
        result = evaluate(interface, device, "safety-block")
        assert result.status == "pass-for-combined-safety-block-evidence", result

        write_file(interface / "11-cancel-evidence.md", "Status: partial-honest-not-cancelled\n")
        write_file(device / "11-chat-evidence.json", json.dumps({"status": "pass-for-device-stop-feedback-evidence"}))
        result = evaluate(interface, device, "stop")
        assert result.status == "partial-combined-stop-evidence", result

    print("ai_agent_combined_evidence_gate self-test passed")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--interface-dir", type=Path, help="Interface evidence package directory.")
    parser.add_argument("--device-dir", type=Path, help="Android device evidence package directory.")
    parser.add_argument("--scenario", choices=tuple(SCENARIO_DEVICE_PASS), default="chat")
    parser.add_argument("--output", type=Path, help="Write markdown report to this path.")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        self_test()
        return 0
    if args.interface_dir is None or args.device_dir is None:
        parser.error("--interface-dir and --device-dir are required unless --self-test is used")

    result = evaluate(args.interface_dir, args.device_dir, args.scenario)
    report = render_markdown(result, args.interface_dir, args.device_dir, args.scenario)
    if args.output:
        output = args.output if args.output.is_absolute() else Path.cwd() / args.output
        write_file(output, report)
    else:
        print(report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
