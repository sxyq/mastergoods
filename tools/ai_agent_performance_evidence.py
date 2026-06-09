#!/usr/bin/env python3
"""Capture repeatable performance evidence for AI agent chat runs.

This script measures the real `/v2/agent/chat/stream` timeline across several
business questions. It does not fabricate Android or provider-stream evidence:
missing auth, missing backend, missing model_stream, or data-before-answer
ordering risks are reported honestly in the generated conclusion.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import statistics
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DEFAULT_BASE_URL = "http://localhost:8080"
DEFAULT_OUTPUT_ROOT = Path("docs/acceptance-evidence/performance")
DEFAULT_QUESTIONS = [
    "库存和客户应收情况",
    "客户应收情况",
    "最近销售采购和财务情况怎么样？",
]


@dataclass
class StreamCapture:
    status: int | None
    time_to_headers_ms: float | None
    total_response_ms: float
    raw_text: str
    events: list[dict[str, Any]]
    error: str | None = None


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def timestamp_dir() -> str:
    return datetime.now().strftime("%Y%m%d-%H%M%S-ai-agent-performance")


def percentile(values: list[float], ratio: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, math.ceil(ratio * len(ordered)) - 1))
    return ordered[index]


def round_ms(value: float | None) -> float | None:
    return None if value is None else round(value, 2)


def safe_json(value: Any, max_chars: int = 3000) -> Any:
    text = json.dumps(value, ensure_ascii=False, sort_keys=True)
    if len(text) <= max_chars:
        return value
    if isinstance(value, dict):
        preview = {
            "truncated_preview": text[:max_chars],
            "original_chars": len(text),
        }
        for key in ("code", "message", "status", "run_id", "runId"):
            if key in value:
                preview[key] = value[key]
        data = value.get("data")
        if isinstance(data, dict):
            for key in ("run_id", "runId", "status", "mode", "llm_status", "llmStatus"):
                if key in data:
                    preview[f"data.{key}"] = data[key]
        return preview
    return {"truncated_preview": text[:max_chars], "original_chars": len(text)}


def request_json(
    base_url: str,
    method: str,
    path: str,
    token: str | None,
    payload: dict[str, Any] | None,
    timeout_seconds: float,
) -> dict[str, Any]:
    url = base_url.rstrip("/") + "/" + path.lstrip("/")
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(url=url, method=method.upper(), data=body)
    request.add_header("Accept", "application/json")
    if body is not None:
        request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            response_body = response.read().decode("utf-8", errors="replace")
            return {
                "status": response.getcode(),
                "duration_ms": round_ms((time.perf_counter() - started) * 1000),
                "json": json.loads(response_body) if response_body.strip() else None,
                "error": None,
            }
    except urllib.error.HTTPError as exc:
        response_body = exc.read().decode("utf-8", errors="replace")
        try:
            parsed: Any = json.loads(response_body)
        except json.JSONDecodeError:
            parsed = None
        return {
            "status": exc.code,
            "duration_ms": round_ms((time.perf_counter() - started) * 1000),
            "json": parsed,
            "body_preview": None if parsed is not None else response_body[:1200],
            "error": f"HTTPError {exc.code}",
        }
    except Exception as exc:  # noqa: BLE001
        return {
            "status": None,
            "duration_ms": round_ms((time.perf_counter() - started) * 1000),
            "json": None,
            "error": repr(exc),
        }


def login_for_token(base_url: str, phone: str, password: str, timeout_seconds: float) -> str:
    result = request_json(
        base_url,
        "POST",
        "/v1/auth/login",
        None,
        {"phone": phone, "password": password},
        timeout_seconds,
    )
    payload = result.get("json")
    token = payload.get("data", {}).get("token") if isinstance(payload, dict) and isinstance(payload.get("data"), dict) else None
    if result.get("status") != 200 or not token:
        raise RuntimeError(f"login failed or missing token: {safe_json(result)}")
    return str(token)


def parse_sse_stream(raw: str, arrival_ms: list[float]) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    current_event: str | None = None
    data_lines: list[str] = []
    data_started_at: float | None = None

    def flush() -> None:
        nonlocal current_event, data_lines, data_started_at
        if not data_lines:
            current_event = None
            data_started_at = None
            return
        data_text = "\n".join(data_lines)
        try:
            payload: Any = json.loads(data_text)
        except json.JSONDecodeError:
            payload = {"raw_data": data_text}
        event_type = ""
        if isinstance(payload, dict):
            event_type = str(payload.get("event_type") or payload.get("eventType") or "")
        if not event_type:
            event_type = current_event or "message"
        events.append(
            {
                "event": current_event,
                "event_type": event_type,
                "arrival_ms": round_ms(data_started_at),
                "payload": payload,
            }
        )
        current_event = None
        data_lines = []
        data_started_at = None

    line_index = 0
    for line in raw.splitlines():
        line_arrival = arrival_ms[min(line_index, len(arrival_ms) - 1)] if arrival_ms else None
        line_index += 1
        if not line.strip():
            flush()
            continue
        if line.startswith(":"):
            continue
        if line.startswith("event:"):
            current_event = line[6:].strip()
            continue
        if line.startswith("data:"):
            if data_started_at is None:
                data_started_at = line_arrival
            data_lines.append(line[5:].lstrip())
            continue
    flush()
    return events


def capture_stream(
    base_url: str,
    token: str | None,
    question: str,
    timeout_seconds: float,
) -> StreamCapture:
    url = base_url.rstrip("/") + "/v2/agent/chat/stream"
    body = json.dumps({"message": question, "stream": True}, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(url=url, method="POST", data=body)
    request.add_header("Accept", "text/event-stream")
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", f"Bearer {token}")
    started = time.perf_counter()
    chunks: list[bytes] = []
    arrival_by_line: list[float] = []
    buffered = b""
    headers_ms: float | None = None
    status: int | None = None
    error: str | None = None
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            headers_ms = (time.perf_counter() - started) * 1000
            status = response.getcode()
            while True:
                chunk = response.read(1)
                if not chunk:
                    break
                chunks.append(chunk)
                buffered += chunk
                if chunk == b"\n":
                    arrival_by_line.append((time.perf_counter() - started) * 1000)
                    buffered = b""
            if buffered:
                arrival_by_line.append((time.perf_counter() - started) * 1000)
    except urllib.error.HTTPError as exc:
        headers_ms = (time.perf_counter() - started) * 1000
        status = exc.code
        chunks.append(exc.read())
        error = f"HTTPError {exc.code}"
    except Exception as exc:  # noqa: BLE001
        error = repr(exc)
    total_ms = (time.perf_counter() - started) * 1000
    raw_text = b"".join(chunks).decode("utf-8", errors="replace")
    events = parse_sse_stream(raw_text, arrival_by_line)
    return StreamCapture(status, round_ms(headers_ms), round_ms(total_ms) or total_ms, raw_text, events, error)


def nested(payload: Any, *keys: str) -> Any:
    value = payload
    for key in keys:
        if not isinstance(value, dict):
            return None
        value = value.get(key)
    return value


def payload_value(event: dict[str, Any], *keys: str) -> Any:
    payload = event.get("payload")
    if not isinstance(payload, dict):
        return None
    for key in keys:
        if key in payload:
            return payload[key]
    data = payload.get("data")
    if isinstance(data, dict):
        for key in keys:
            if key in data:
                return data[key]
    return None


def event_type(event: dict[str, Any]) -> str:
    return str(event.get("event_type") or "")


def delta_source(event: dict[str, Any]) -> str:
    return str(payload_value(event, "delta_source", "deltaSource") or "")


def first_arrival(events: list[dict[str, Any]], predicate) -> float | None:  # type: ignore[no-untyped-def]
    values = [event.get("arrival_ms") for event in events if predicate(event) and isinstance(event.get("arrival_ms"), (int, float))]
    return min(values) if values else None


def infer_run_id(events: list[dict[str, Any]], audit: dict[str, Any] | None = None) -> str | None:
    for event in events:
        for key in ("run_id", "runId"):
            value = payload_value(event, key)
            if value:
                return str(value)
    data = audit.get("data") if isinstance(audit, dict) else None
    if isinstance(data, dict):
        return str(data.get("run_id") or data.get("runId") or "") or None
    return None


def summarize_sample(
    question: str,
    iteration: int,
    capture: StreamCapture,
    audit: dict[str, Any] | None,
) -> dict[str, Any]:
    events = capture.events
    run_id = infer_run_id(events, audit)
    first_event_ms = first_arrival(events, lambda _event: True)
    first_tool_started_ms = first_arrival(events, lambda event: event_type(event) == "tool_started")
    first_tool_completed_ms = first_arrival(events, lambda event: event_type(event) == "tool_completed")
    first_tool_failed_ms = first_arrival(events, lambda event: event_type(event) == "tool_failed")
    first_answer_delta_ms = first_arrival(events, lambda event: event_type(event) == "answer_delta")
    first_model_stream_ms = first_arrival(
        events,
        lambda event: event_type(event) == "answer_delta" and delta_source(event) == "model_stream",
    )
    first_server_notice_ms = first_arrival(
        events,
        lambda event: event_type(event) == "answer_delta" and delta_source(event) == "server_notice",
    )
    answer_completed_ms = first_arrival(events, lambda event: event_type(event) == "answer_completed")
    first_result_block_ms = first_arrival(events, lambda event: event_type(event) == "result_block")
    run_completed_ms = first_arrival(events, lambda event: event_type(event) == "run_completed")

    event_counts: dict[str, int] = {}
    for event in events:
        key = event_type(event)
        event_counts[key] = event_counts.get(key, 0) + 1

    audit_data = audit.get("data") if isinstance(audit, dict) else None
    tool_durations: list[float] = []
    if isinstance(audit_data, dict):
        for audit_event in audit_data.get("events") or []:
            if not isinstance(audit_event, dict):
                continue
            if str(audit_event.get("event_type") or audit_event.get("eventType") or "") != "tool_completed":
                continue
            payload = audit_event.get("payload") if isinstance(audit_event.get("payload"), dict) else {}
            duration = payload.get("duration_ms") or payload.get("durationMs")
            if isinstance(duration, (int, float)):
                tool_durations.append(float(duration))

    model_stream_count = sum(
        1 for event in events if event_type(event) == "answer_delta" and delta_source(event) == "model_stream"
    )
    server_notice_count = sum(
        1 for event in events if event_type(event) == "answer_delta" and delta_source(event) == "server_notice"
    )
    result_before_model_delta = (
        first_result_block_ms is not None
        and first_model_stream_ms is not None
        and first_result_block_ms < first_model_stream_ms
    )
    result_before_answer_completed_without_model = (
        first_result_block_ms is not None
        and model_stream_count == 0
        and answer_completed_ms is not None
        and first_result_block_ms < answer_completed_ms
    )
    server_notice_before_model = (
        first_server_notice_ms is not None
        and model_stream_count == 0
    ) or (
        first_server_notice_ms is not None
        and first_model_stream_ms is not None
        and first_server_notice_ms < first_model_stream_ms
    )

    return {
        "question": question,
        "iteration": iteration,
        "run_id": run_id,
        "http_status": capture.status,
        "error": capture.error,
        "time_to_headers_ms": capture.time_to_headers_ms,
        "total_response_ms": capture.total_response_ms,
        "first_event_latency_ms": first_event_ms,
        "first_tool_started_latency_ms": first_tool_started_ms,
        "first_tool_completed_latency_ms": first_tool_completed_ms,
        "first_tool_failed_latency_ms": first_tool_failed_ms,
        "first_answer_delta_latency_ms": first_answer_delta_ms,
        "first_model_stream_delta_latency_ms": first_model_stream_ms,
        "first_server_notice_delta_latency_ms": first_server_notice_ms,
        "answer_completed_latency_ms": answer_completed_ms,
        "first_result_block_latency_ms": first_result_block_ms,
        "run_completed_latency_ms": run_completed_ms,
        "event_counts": event_counts,
        "tool_duration_sum_ms": round_ms(sum(tool_durations)) if tool_durations else None,
        "tool_duration_max_ms": round_ms(max(tool_durations)) if tool_durations else None,
        "model_stream_delta_count": model_stream_count,
        "server_notice_delta_count": server_notice_count,
        "result_before_model_delta": result_before_model_delta,
        "result_before_answer_completed_without_model": result_before_answer_completed_without_model,
        "server_notice_before_model": server_notice_before_model,
        "audit_status": nested(audit, "data", "status") if audit else None,
        "audit_mode": nested(audit, "data", "mode") if audit else None,
        "audit_llm_status": nested(audit, "data", "llm_status") if audit else None,
        "audit_plan_source": nested(audit, "data", "plan_source") if audit else None,
        "audit_event_count": nested(audit, "data", "event_count") if audit else None,
        "audit_tool_count": nested(audit, "data", "tool_count") if audit else None,
    }


def summarize_all(samples: list[dict[str, Any]]) -> dict[str, Any]:
    metrics = [
        "time_to_headers_ms",
        "total_response_ms",
        "first_event_latency_ms",
        "first_tool_started_latency_ms",
        "first_tool_completed_latency_ms",
        "first_answer_delta_latency_ms",
        "first_model_stream_delta_latency_ms",
        "answer_completed_latency_ms",
        "first_result_block_latency_ms",
        "run_completed_latency_ms",
        "tool_duration_sum_ms",
        "tool_duration_max_ms",
    ]
    metric_summary: dict[str, dict[str, float | int | None]] = {}
    for metric in metrics:
        values = [float(sample[metric]) for sample in samples if isinstance(sample.get(metric), (int, float))]
        metric_summary[metric] = {
            "count": len(values),
            "p50_ms": percentile(values, 0.50),
            "p95_ms": percentile(values, 0.95),
            "max_ms": max(values) if values else None,
            "mean_ms": round(statistics.mean(values), 2) if values else None,
        }
    http_ok = sum(1 for sample in samples if isinstance(sample.get("http_status"), int) and 200 <= int(sample["http_status"]) < 300)
    completed = sum(1 for sample in samples if sample.get("audit_status") == "completed" or sample.get("event_counts", {}).get("run_completed"))
    return {
        "sample_count": len(samples),
        "http_ok": http_ok,
        "completed": completed,
        "model_stream_samples": sum(1 for sample in samples if (sample.get("model_stream_delta_count") or 0) > 0),
        "tool_failed_samples": sum(1 for sample in samples if sample.get("event_counts", {}).get("tool_failed")),
        "result_before_model_delta_samples": sum(1 for sample in samples if sample.get("result_before_model_delta")),
        "result_before_answer_completed_without_model_samples": sum(
            1 for sample in samples if sample.get("result_before_answer_completed_without_model")
        ),
        "server_notice_before_model_samples": sum(1 for sample in samples if sample.get("server_notice_before_model")),
        "metrics": metric_summary,
    }


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def markdown_value(value: Any) -> str:
    if value is None:
        return "missing"
    if isinstance(value, float):
        return str(round(value, 2))
    return str(value)


def write_env(output_dir: Path, args: argparse.Namespace, questions: list[str], token_source: str) -> None:
    output_dir.joinpath("00-env.md").write_text(
        "\n".join(
            [
                "# AI Agent Performance Evidence Environment",
                "",
                f"- Captured at UTC: `{utc_now()}`",
                f"- Base URL: `{args.base_url}`",
                f"- Iterations per question: `{args.iterations}`",
                f"- Timeout seconds: `{args.timeout_seconds}`",
                f"- Token source: `{token_source}`",
                f"- Backend profile note: `{args.backend_profile}`",
                f"- LLM status note: `{args.llm_status_note}`",
                "- Questions:",
                *[f"  - `{question}`" for question in questions],
                "",
                "This package is backend AI-agent performance evidence only.",
                "It does not prove Android first-visible latency, frame timing, or provider-native model streaming unless those signals appear in captured samples.",
            ]
        )
        + "\n",
        encoding="utf-8",
    )


def write_summary_markdown(output_dir: Path, samples: list[dict[str, Any]], summary: dict[str, Any]) -> None:
    lines = [
        "# AI Agent Performance Summary",
        "",
        f"Status: `{status_for_summary(summary)}`",
        "",
        "## Sample Table",
        "",
        "| Question | Iteration | Run | HTTP | Mode | LLM | First event | First tool | First answer delta | Answer completed | First result block | Run completed | Total |",
        "|---|---:|---|---:|---|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for sample in samples:
        lines.append(
            "| "
            + " | ".join(
                [
                    str(sample.get("question", "")).replace("|", "\\|"),
                    markdown_value(sample.get("iteration")),
                    f"`{sample.get('run_id') or 'missing'}`",
                    markdown_value(sample.get("http_status")),
                    f"`{sample.get('audit_mode') or 'missing'}`",
                    f"`{sample.get('audit_llm_status') or 'missing'}`",
                    markdown_value(sample.get("first_event_latency_ms")),
                    markdown_value(sample.get("first_tool_started_latency_ms")),
                    markdown_value(sample.get("first_answer_delta_latency_ms")),
                    markdown_value(sample.get("answer_completed_latency_ms")),
                    markdown_value(sample.get("first_result_block_latency_ms")),
                    markdown_value(sample.get("run_completed_latency_ms")),
                    markdown_value(sample.get("total_response_ms")),
                ]
            )
            + " |"
        )
    lines.extend(
        [
            "",
            "## Aggregate Metrics",
            "",
            "| Metric | Count | P50 ms | P95 ms | Max ms | Mean ms |",
            "|---|---:|---:|---:|---:|---:|",
        ]
    )
    metrics = summary.get("metrics", {})
    for metric, row in metrics.items():
        if not isinstance(row, dict):
            continue
        lines.append(
            f"| `{metric}` | `{markdown_value(row.get('count'))}` | `{markdown_value(row.get('p50_ms'))}` | "
            f"`{markdown_value(row.get('p95_ms'))}` | `{markdown_value(row.get('max_ms'))}` | `{markdown_value(row.get('mean_ms'))}` |"
        )
    lines.extend(
        [
            "",
            "## Review Notes",
            "",
            f"- HTTP successful samples: `{summary.get('http_ok')}/{summary.get('sample_count')}`.",
            f"- Completed samples: `{summary.get('completed')}/{summary.get('sample_count')}`.",
            f"- Provider `model_stream` samples: `{summary.get('model_stream_samples')}/{summary.get('sample_count')}`.",
            f"- Result-before-model-delta samples: `{summary.get('result_before_model_delta_samples')}`.",
            f"- Result-before-answer-completed non-model samples: `{summary.get('result_before_answer_completed_without_model_samples')}`.",
            f"- Server-notice-before-model samples: `{summary.get('server_notice_before_model_samples')}`.",
            "",
            "If provider `model_stream` count is zero, this package only supports rule-summary / non-model timing and must remain partial for ChatGPT-like streaming acceptance.",
            "If Android frame timing is not attached, this package cannot pass the high-refresh mobile performance requirement by itself.",
        ]
    )
    output_dir.joinpath("03-summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def status_for_summary(summary: dict[str, Any]) -> str:
    sample_count = int(summary.get("sample_count") or 0)
    if sample_count == 0:
        return "blocked-no-samples"
    if int(summary.get("http_ok") or 0) < sample_count:
        return "partial-http-failures"
    if int(summary.get("completed") or 0) < sample_count:
        return "partial-incomplete-runs"
    if int(summary.get("result_before_model_delta_samples") or 0) > 0:
        return "partial-result-before-model-risk"
    if int(summary.get("result_before_answer_completed_without_model_samples") or 0) > 0:
        return "partial-result-before-answer-risk"
    if int(summary.get("model_stream_samples") or 0) == 0:
        return "partial-rule-summary-performance"
    return "partial-provider-stream-performance"


def run_capture(args: argparse.Namespace) -> Path:
    questions = args.question or DEFAULT_QUESTIONS
    token = args.token or os.environ.get("TOKEN") or None
    token_source = "provided" if token else "none"
    if not token and (args.login_phone or args.login_password):
        if not args.login_phone or not args.login_password:
            raise SystemExit("LOGIN_PHONE and LOGIN_PASSWORD must be provided together")
        token = login_for_token(args.base_url, args.login_phone, args.login_password, args.timeout_seconds)
        token_source = "login"
    elif not token and not args.allow_no_auth:
        raise SystemExit("TOKEN is required unless login credentials or --allow-no-auth are provided")

    output_dir = Path(args.output_root) / timestamp_dir()
    output_dir.mkdir(parents=True, exist_ok=False)

    write_env(output_dir, args, questions, token_source)
    samples: list[dict[str, Any]] = []
    raw_index: list[dict[str, Any]] = []

    for question_index, question in enumerate(questions, start=1):
        for iteration in range(1, args.iterations + 1):
            prefix = f"sample-{question_index:02d}-{iteration:02d}"
            capture = capture_stream(args.base_url, token, question, args.timeout_seconds)
            output_dir.joinpath(f"{prefix}-raw-sse.log").write_text(capture.raw_text, encoding="utf-8")
            write_json(output_dir / f"{prefix}-events.json", capture.events)
            run_id = infer_run_id(capture.events)
            audit: dict[str, Any] | None = None
            if run_id:
                audit_result = request_json(
                    args.base_url,
                    "GET",
                    f"/v2/agent/runs/{run_id}/audit",
                    token,
                    None,
                    args.timeout_seconds,
                )
                audit = audit_result.get("json") if isinstance(audit_result.get("json"), dict) else audit_result
            write_json(output_dir / f"{prefix}-audit.json", audit or {"error": "missing run_id or audit response"})
            sample = summarize_sample(question, iteration, capture, audit)
            samples.append(sample)
            raw_index.append(
                {
                    "question": question,
                    "iteration": iteration,
                    "run_id": sample.get("run_id"),
                    "raw_sse": f"{prefix}-raw-sse.log",
                    "events": f"{prefix}-events.json",
                    "audit": f"{prefix}-audit.json",
                }
            )

    summary = summarize_all(samples)
    write_json(output_dir / "01-samples.json", samples)
    write_json(output_dir / "02-summary.json", summary)
    write_json(output_dir / "04-file-index.json", raw_index)
    write_summary_markdown(output_dir, samples, summary)
    return output_dir


def self_test() -> None:
    raw = "\n".join(
        [
            "event: run_started",
            'data: {"event_type":"run_started","run_id":"run-test","seq":1}',
            "",
            "event: tool_started",
            'data: {"event_type":"tool_started","run_id":"run-test","seq":2,"tool_name":"customer_receivable_lookup"}',
            "",
            "event: answer_delta",
            'data: {"event_type":"answer_delta","run_id":"run-test","seq":3,"delta":"正在分析","delta_source":"model_stream"}',
            "",
            "event: result_block",
            'data: {"event_type":"result_block","run_id":"run-test","seq":4,"block":{"block_type":"table"}}',
            "",
            "event: answer_completed",
            'data: {"event_type":"answer_completed","run_id":"run-test","seq":5,"llm_status":"streaming"}',
            "",
            "event: run_completed",
            'data: {"event_type":"run_completed","run_id":"run-test","seq":6}',
            "",
        ]
    )
    events = parse_sse_stream(raw, [10, 11, 20, 21, 30, 31, 40, 41, 50, 51, 60, 61])
    audit = {
        "data": {
            "status": "completed",
            "mode": "tool_query_llm_streamed",
            "llm_status": "streaming",
            "plan_source": "provider",
            "event_count": 6,
            "tool_count": 1,
            "events": [
                {"event_type": "tool_completed", "payload": {"duration_ms": 17}},
            ],
        }
    }
    sample = summarize_sample("客户应收情况", 1, StreamCapture(200, 9, 65, raw, events), audit)
    assert sample["run_id"] == "run-test"
    assert sample["first_model_stream_delta_latency_ms"] == 41
    assert sample["first_result_block_latency_ms"] == 60
    assert not sample["result_before_model_delta"]
    assert sample["tool_duration_sum_ms"] == 17

    bad_raw = "\n".join(
        [
            'data: {"event_type":"run_started","run_id":"run-bad"}',
            "",
            'data: {"event_type":"result_block","run_id":"run-bad"}',
            "",
            'data: {"event_type":"answer_completed","run_id":"run-bad"}',
            "",
        ]
    )
    bad_events = parse_sse_stream(bad_raw, [5, 6, 9, 10, 20, 21])
    bad_sample = summarize_sample("库存", 1, StreamCapture(200, 4, 21, bad_raw, bad_events), {"data": {"status": "completed"}})
    assert bad_sample["result_before_answer_completed_without_model"]
    summary = summarize_all([sample, bad_sample])
    assert summary["result_before_answer_completed_without_model_samples"] == 1
    assert status_for_summary(summary) == "partial-result-before-answer-risk"
    print("ai_agent_performance_evidence self-test passed")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.environ.get("BASE_URL", DEFAULT_BASE_URL))
    parser.add_argument("--token", default=os.environ.get("TOKEN"))
    parser.add_argument("--login-phone", default=os.environ.get("LOGIN_PHONE"))
    parser.add_argument("--login-password", default=os.environ.get("LOGIN_PASSWORD"))
    parser.add_argument("--allow-no-auth", action="store_true", default=os.environ.get("ALLOW_NO_AUTH") == "1")
    parser.add_argument("--output-root", default=str(DEFAULT_OUTPUT_ROOT))
    parser.add_argument("--question", action="append", help="Business question to sample. Can be repeated.")
    parser.add_argument("--iterations", type=int, default=1)
    parser.add_argument("--timeout-seconds", type=float, default=60)
    parser.add_argument("--backend-profile", default=os.environ.get("BACKEND_PROFILE", "unknown"))
    parser.add_argument("--llm-status-note", default=os.environ.get("LLM_STATUS_NOTE", "unknown"))
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.iterations < 1:
        parser.error("--iterations must be >= 1")
    return args


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    if args.self_test:
        self_test()
        return 0
    output_dir = run_capture(args)
    print(output_dir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
