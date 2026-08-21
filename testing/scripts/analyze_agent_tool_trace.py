#!/usr/bin/env python3
"""Analyze Agent tool traces without treating HTTP 200 as a quality pass.

The server-side evaluator intentionally keeps its functional result separate
from this report. A request can return a correct answer while still making
unnecessary neighboring calls; this script makes that behavior visible for
prompt and tool-selection regression work.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


VISUALIZATION_WORDS = (
    "图表", "趋势图", "统计图", "柱状图", "折线图", "饼图", "表格", "排行",
    "统计卡", "可视化", "画一张", "用图", "展示成图", "展示成表",
)


def load_cases(path: Path) -> list[dict]:
    files = sorted(path.glob("*.json")) if path.is_dir() else [path]
    cases = []
    for file in files:
        try:
            body = json.loads(file.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(body, dict) and body.get("actual") and body.get("expected"):
            cases.append(body)
    return cases


def expected_sets(case: dict) -> list[set[str]]:
    expected = case.get("expected") or {}
    values = expected.get("expected_tool_sets") or [expected.get("expected_tools") or []]
    return [set(value) for value in values if isinstance(value, list)] or [set()]


def choose_expected_set(actual: set[str], candidates: list[set[str]]) -> set[str]:
    """Prefer an allowed set that contains every actual tool call."""
    containing = [candidate for candidate in candidates if actual <= candidate]
    if containing:
        return min(containing, key=lambda candidate: (len(candidate), sorted(candidate)))
    return min(
        candidates,
        key=lambda candidate: (
            len(candidate - actual),
            len(actual - candidate),
            len(candidate),
        ),
    )


def sequence_matches(actual: list[str], candidates: list[list[str]]) -> bool:
    return any(actual == candidate for candidate in candidates)


def tool_events(trace: dict[str, Any]) -> list[dict[str, Any]]:
    """Return tool events while preserving repeated events for protocol checks."""
    events: list[dict[str, Any]] = []
    for key in ("tool_calls", "tool_events"):
        value = trace.get(key) or []
        if isinstance(value, list):
            events.extend(item for item in value if isinstance(item, dict))
    return events


def answer_text(case: dict[str, Any]) -> str:
    response = case.get("response") or {}
    data = response.get("data") if isinstance(response, dict) else {}
    if isinstance(data, dict) and isinstance(data.get("answer"), str):
        return data["answer"]
    actual = case.get("actual") or {}
    return actual.get("answer") if isinstance(actual.get("answer"), str) else ""


def http_status(case: dict[str, Any]) -> int | None:
    actions = case.get("actions") or {}
    value = actions.get("http_status")
    return value if isinstance(value, int) else None


def failure_is_transparent(case: dict[str, Any], failed_events: list[dict[str, Any]]) -> bool:
    """Allow only explicitly permitted failures that the answer discloses."""
    expected = case.get("expected") or {}
    allowed = set(expected.get("allowed_failed_tools") or expected.get("allow_failed_tools") or [])
    failed_names = {event.get("tool_name") for event in failed_events if event.get("tool_name")}
    if not failed_names or not failed_names <= allowed:
        return False
    answer = answer_text(case).lower()
    disclosure_words = ("失败", "错误", "无法", "未完成", "failed", "error", "unavailable")
    return any(word in answer for word in disclosure_words)


def analyze_case(case: dict) -> dict:
    allowed_sets = expected_sets(case)
    expected = choose_expected_set(
        set((case.get("actual") or {}).get("tool_names") or []),
        allowed_sets,
    )
    actual = case.get("actual") or {}
    trace = case.get("model_and_tool_trace") or {}
    calls = tool_events(trace)
    actual_tools = {
        call.get("tool_name") for call in calls if call.get("tool_name")
    } or set(actual.get("tool_names") or [])
    completed_sequence = [
        call.get("tool_name")
        for call in calls
        if call.get("status") == "completed" and not call.get("error_code") and call.get("tool_name")
    ]
    message = ((case.get("actions") or {}).get("request") or {}).get("message", "")
    unexpected = sorted(actual_tools - expected)
    missing = sorted(expected - actual_tools)
    call_ids = [call.get("tool_call_id") for call in calls if call.get("tool_call_id")]
    duplicate_call_ids = sorted(
        call_id for call_id, count in Counter(call_ids).items() if count > 1
    )
    failed_events = [
        {
            "tool_call_id": call.get("tool_call_id"),
            "tool_name": call.get("tool_name"),
            "status": call.get("status"),
        }
        for call in calls
        if str(call.get("status", "")).lower() == "failed"
    ]
    failed_event_transparent = failure_is_transparent(case, failed_events)
    status = http_status(case)
    http_pass = status is not None and 200 <= status < 300
    answer_present = bool(answer_text(case).strip())
    selection_pass = not unexpected and not missing
    expected_sequences = (case.get("expected") or {}).get("expected_tool_sequences") or []
    sequence_pass = not expected_sequences or sequence_matches(completed_sequence, expected_sequences)
    tool_execution_pass = not failed_events or failed_event_transparent
    strict_pass = (
        http_pass
        and answer_present
        and selection_pass
        and sequence_pass
        and tool_execution_pass
        and not duplicate_call_ids
    )
    visualization_called = "result_visualization" in actual_tools
    explicit_visualization = any(word in message for word in VISUALIZATION_WORDS)
    visualization_allowed_by_case = any(
        "result_visualization" in candidate for candidate in allowed_sets
    )
    single_topic_overcall = len(expected) == 1 and bool(unexpected)
    return {
        "test_id": case.get("test_id"),
        "message": message,
        "source_result": case.get("result") or actual.get("result"),
        "functional_result": "Passed" if strict_pass else "Failed",
        "http_status": status,
        "http_pass": http_pass,
        "answer_present": answer_present,
        "answer_pass": answer_present,
        "tool_selection_pass": selection_pass,
        "tool_execution_pass": tool_execution_pass,
        "strict_pass": strict_pass,
        "expected_tools": sorted(expected),
        "actual_tools": sorted(actual_tools),
        "unexpected_tools": unexpected,
        "selection_quality": "pass" if not unexpected else "over_call",
        "missing_tools": missing,
        "expected_tool_sequences": expected_sequences,
        "completed_tool_sequence": completed_sequence,
        "tool_sequence_pass": sequence_pass,
        "single_topic_overcall": single_topic_overcall,
        "unexpected_visualization": (
            visualization_called
            and not explicit_visualization
            and not visualization_allowed_by_case
        ),
        "duplicate_call_ids": duplicate_call_ids,
        "failed_tool_events": failed_events,
        "failed_event_transparent": failed_event_transparent,
        "tool_count": len(calls),
        "elapsed_ms": (case.get("actions") or {}).get("elapsed_ms"),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path, help="case JSON file or directory")
    parser.add_argument("--output", type=Path, help="write JSON report")
    args = parser.parse_args()

    reports = [analyze_case(case) for case in load_cases(args.input)]
    summary = {
        "case_count": len(reports),
        "source_functional_pass_count": sum(item["source_result"] == "Passed" for item in reports),
        "http_pass_count": sum(item["http_pass"] for item in reports),
        "answer_pass_count": sum(item["answer_pass"] for item in reports),
        "tool_selection_pass_count": sum(item["tool_selection_pass"] for item in reports),
        "tool_execution_pass_count": sum(item["tool_execution_pass"] for item in reports),
        "functional_pass_count": sum(item["strict_pass"] for item in reports),
        "single_topic_overcall_count": sum(item["single_topic_overcall"] for item in reports),
        "unapproved_over_call_count": sum(bool(item["unexpected_tools"]) for item in reports),
        "unexpected_visualization_count": sum(item["unexpected_visualization"] for item in reports),
        "duplicate_call_id_case_count": sum(bool(item["duplicate_call_ids"]) for item in reports),
        "failed_tool_event_case_count": sum(bool(item["failed_tool_events"]) for item in reports),
        "tool_call_count": sum(item["tool_count"] for item in reports),
        "tool_frequency": dict(Counter(
            tool for item in reports for tool in item["actual_tools"]
        )),
    }
    report = {"summary": summary, "cases": reports}
    encoded = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded, encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    for item in reports:
        if item["single_topic_overcall"] or item["unexpected_visualization"] or item["duplicate_call_ids"]:
            print(json.dumps(item, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
