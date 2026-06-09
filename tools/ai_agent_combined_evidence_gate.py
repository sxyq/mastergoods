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


def interface_status(interface_dir: Path, scenario: str) -> str:
    evidence_name = SCENARIO_INTERFACE_FILE[scenario]
    status = markdown_status(read_text(interface_dir / evidence_name))
    if status != "missing":
        return status
    if scenario == "chat":
        return markdown_status(read_text(interface_dir / "13-sse-audit-ui-reconciliation.md"))
    return status


def has_provider_model_stream(interface_dir: Path) -> bool:
    raw_sse = read_text(interface_dir / "02-raw-sse.log")
    latency = read_text(interface_dir / "11-latency.md")
    return (
        '"delta_source":"model_stream"' in raw_sse
        or '"delta_source": "model_stream"' in raw_sse
        or "`model_stream_delta_count` | `0`" not in latency
        and "`model_stream_delta_count`" in latency
        and "Provider-backed `model_stream` timing is present" in latency
    )


def evaluate(interface_dir: Path, device_dir: Path, scenario: str) -> GateResult:
    reasons: list[str] = []
    i_status = interface_status(interface_dir, scenario)
    d_status = device_status(device_dir)
    model_stream = has_provider_model_stream(interface_dir)

    if not interface_dir.exists():
        reasons.append(f"interface evidence dir missing: {interface_dir}")
    if not device_dir.exists():
        reasons.append(f"device evidence dir missing: {device_dir}")

    expected_device = SCENARIO_DEVICE_PASS[scenario]
    if d_status != expected_device:
        reasons.append(f"device status is `{d_status}`, expected `{expected_device}`")

    if scenario == "chat":
        if i_status != "pass-for-interface":
            reasons.append(f"chat interface status is `{i_status}`, expected `pass-for-interface`")
        if not model_stream:
            reasons.append("provider `model_stream` was not observed; ChatGPT-like streaming remains partial")
        if not reasons:
            status = "pass-for-combined-provider-chat-evidence"
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
    return GateResult(status, reasons, i_status, d_status, model_stream)


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
        write_file(interface / "02-raw-sse.log", 'data: {"event_type":"answer_delta","delta_source":"model_stream"}\n')
        write_file(device / "11-chat-evidence.json", json.dumps({"status": "pass-for-device-ai-chat-evidence"}))
        result = evaluate(interface, device, "chat")
        assert result.status == "pass-for-combined-provider-chat-evidence", result

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
