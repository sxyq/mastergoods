#!/usr/bin/env python3
"""Run real server-side Agent stream cancellation and concurrency checks.

The harness creates disposable conversations, captures raw SSE and audit JSON,
and deletes every conversation in a finally block. It never writes a business
table because all prompts are read-only questions.
"""

from __future__ import annotations

import argparse
import json
import os
import queue
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def request_json(
    base_url: str,
    token: str,
    method: str,
    path: str,
    payload: dict[str, Any] | None = None,
    timeout: float = 120,
) -> dict[str, Any]:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        base_url.rstrip("/") + path,
        data=body,
        method=method,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        },
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8", errors="replace")
            return {
                "status": response.status,
                "duration_ms": round((time.perf_counter() - started) * 1000, 2),
                "body": json.loads(raw) if raw.strip() else None,
                "error": None,
            }
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        try:
            body_value: Any = json.loads(raw) if raw.strip() else None
        except json.JSONDecodeError:
            body_value = raw[:2000]
        return {
            "status": error.code,
            "duration_ms": round((time.perf_counter() - started) * 1000, 2),
            "body": body_value,
            "error": f"HTTPError {error.code}",
        }
    except Exception as error:  # noqa: BLE001
        return {
            "status": None,
            "duration_ms": round((time.perf_counter() - started) * 1000, 2),
            "body": None,
            "error": repr(error),
        }


def response_data(response: dict[str, Any]) -> dict[str, Any]:
    body = response.get("body")
    data = body.get("data") if isinstance(body, dict) else None
    return data if isinstance(data, dict) else {}


def create_conversation(base_url: str, token: str, title: str) -> tuple[int | None, dict[str, Any]]:
    response = request_json(
        base_url,
        token,
        "POST",
        "/v2/agent/conversations",
        {"title": title, "status": "active"},
    )
    value = response_data(response).get("id")
    try:
        return int(value), response
    except (TypeError, ValueError):
        return None, response


def delete_conversation(base_url: str, token: str, conversation_id: int) -> dict[str, Any]:
    return request_json(base_url, token, "DELETE", f"/v2/agent/conversations/{conversation_id}", {})


def parse_sse(raw: str) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    event_name: str | None = None
    data_lines: list[str] = []

    def flush() -> None:
        nonlocal event_name, data_lines
        if not data_lines:
            event_name = None
            return
        data_text = "\n".join(data_lines)
        try:
            payload: Any = json.loads(data_text)
        except json.JSONDecodeError:
            payload = {"raw_data": data_text}
        event_type = payload.get("event_type") if isinstance(payload, dict) else None
        events.append({
            "event": event_name,
            "event_type": event_type or event_name or "message",
            "payload": payload,
        })
        event_name = None
        data_lines = []

    for line in raw.splitlines():
        if not line.strip():
            flush()
        elif line.startswith("event:"):
            event_name = line[6:].strip()
        elif line.startswith("data:"):
            data_lines.append(line[5:].lstrip())
    flush()
    return events


def event_run_id(event: dict[str, Any]) -> str | None:
    payload = event.get("payload")
    if not isinstance(payload, dict):
        return None
    value = payload.get("run_id") or payload.get("runId")
    return str(value) if value else None


def capture_stream(
    base_url: str,
    token: str,
    conversation_id: int,
    prompt: str,
    run_id_sink: Callable[[str], None] | None = None,
    timeout: float = 120,
) -> dict[str, Any]:
    body = json.dumps(
        {"conversation_id": conversation_id, "message": prompt, "stream": True},
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        base_url.rstrip("/") + "/v2/agent/chat/stream",
        data=body,
        method="POST",
        headers={
            "Accept": "text/event-stream",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        },
    )
    started = time.perf_counter()
    chunks: list[bytes] = []
    status: int | None = None
    error: str | None = None
    headers_ms: float | None = None
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            headers_ms = round((time.perf_counter() - started) * 1000, 2)
            status = response.status
            while True:
                line = response.readline()
                if not line:
                    break
                chunks.append(line)
                if line.startswith(b"data:"):
                    try:
                        payload = json.loads(line[5:].lstrip().decode("utf-8"))
                    except (json.JSONDecodeError, UnicodeDecodeError):
                        payload = None
                    if isinstance(payload, dict) and payload.get("event_type") == "run_started":
                        run_id = payload.get("run_id") or payload.get("runId")
                        if run_id and run_id_sink:
                            run_id_sink(str(run_id))
    except urllib.error.HTTPError as error_response:
        status = error_response.code
        chunks.append(error_response.read())
        error = f"HTTPError {error_response.code}"
    except Exception as exception:  # noqa: BLE001
        error = repr(exception)
    raw = b"".join(chunks).decode("utf-8", errors="replace")
    return {
        "status": status,
        "headers_ms": headers_ms,
        "total_ms": round((time.perf_counter() - started) * 1000, 2),
        "error": error,
        "events": parse_sse(raw),
        "raw_sse": raw,
    }


def tool_names(body: Any) -> list[str]:
    data = body.get("data") if isinstance(body, dict) else None
    calls = data.get("tool_calls") if isinstance(data, dict) else None
    return [item.get("tool_name") for item in calls or [] if isinstance(item, dict) and item.get("tool_name")]


def non_stream_case(base_url: str, token: str, conversation_id: int, prompt: str) -> dict[str, Any]:
    response = request_json(
        base_url,
        token,
        "POST",
        "/v2/agent/chat",
        {"conversation_id": conversation_id, "message": prompt, "stream": False},
    )
    data = response_data(response)
    answer = data.get("answer")
    return {
        "conversation_id": conversation_id,
        "prompt": prompt,
        "http_status": response.get("status"),
        "duration_ms": response.get("duration_ms"),
        "error": response.get("error"),
        "tool_names": tool_names(response.get("body")),
        "answer_present": isinstance(answer, str) and bool(answer.strip()),
        "response": response.get("body"),
    }


def percentile(values: list[float], ratio: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, int((len(ordered) * ratio + 0.999999) - 1)))
    return ordered[index]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.environ.get("BASE_URL", "http://127.0.0.1:18080"))
    parser.add_argument("--token", default=os.environ.get("TOKEN"))
    parser.add_argument("--output-root", default=os.environ.get("OUTPUT_ROOT", "/tmp/mg-agent-stream-concurrency"))
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--timeout-seconds", type=float, default=120)
    args = parser.parse_args()
    if not args.token:
        parser.error("TOKEN is required")

    output = Path(args.output_root) / datetime.now().strftime("%Y%m%d-%H%M%S-stream-concurrency")
    output.mkdir(parents=True, exist_ok=False)
    created: list[int] = []
    cancel_prompt = "把最近销售、采购、库存和现金流一起看一下，先告诉我你查到了什么。"
    concurrent_prompts = [
        "最近一周销售情况怎么样？",
        "现在库存里哪些商品需要补货？",
        "客户欠款情况帮我看一下。",
        "供应商应付款现在有多少？",
        "最近的收款和付款记录帮我理一下。",
        "当前门店信息和成员情况帮我看下。",
        "最近采购单和到货情况如何？",
        "商品、库存和价格一起列一下。",
        "最近有没有销售退货？",
        "这个月经营情况给我一个汇总。",
    ]
    summary: dict[str, Any] = {
        "captured_at": utc_now(),
        "base_url": args.base_url,
        "concurrency": args.concurrency,
        "cancel": {},
        "concurrent": {},
        "cleanup": [],
    }
    try:
        cancel_id, cancel_create = create_conversation(args.base_url, args.token, "SSE cancellation fixture")
        if cancel_id is None:
            summary["cancel"] = {"result": "Blocked", "create": cancel_create}
        else:
            created.append(cancel_id)
            run_queue: queue.Queue[str] = queue.Queue()
            holder: dict[str, Any] = {}

            def run_stream() -> None:
                holder["capture"] = capture_stream(
                    args.base_url,
                    args.token,
                    cancel_id,
                    cancel_prompt,
                    run_id_sink=lambda run_id: run_queue.put(run_id),
                    timeout=args.timeout_seconds,
                )

            worker = threading.Thread(target=run_stream, daemon=True)
            worker_started = time.perf_counter()
            worker.start()
            try:
                run_id = run_queue.get(timeout=30)
                cancel_response = request_json(
                    args.base_url,
                    args.token,
                    "POST",
                    f"/v2/agent/runs/{run_id}/cancel",
                    {},
                    timeout=30,
                )
            except queue.Empty:
                run_id = None
                cancel_response = {"status": None, "duration_ms": None, "error": "run_started not observed"}
            worker.join(timeout=args.timeout_seconds + 10)
            capture = holder.get("capture", {})
            events = capture.get("events", [])
            audit = request_json(
                args.base_url,
                args.token,
                "GET",
                f"/v2/agent/runs/{run_id}/audit" if run_id else "/v2/agent/runs/invalid/audit",
                None,
                timeout=30,
            )
            event_types = [event.get("event_type") for event in events]
            summary["cancel"] = {
                "conversation_id": cancel_id,
                "prompt": cancel_prompt,
                "run_id": run_id,
                "create": cancel_create,
                "cancel_response": cancel_response,
                "stream": {key: value for key, value in capture.items() if key != "raw_sse"},
                "event_types": event_types,
                "run_cancelled_event": "run_cancelled" in event_types,
                "stream_closed": not worker.is_alive(),
                "audit": audit,
                "result": "Passed" if cancel_response.get("status") == 200 and "run_cancelled" in event_types else "Failed",
            }
            Path(output / "cancel-raw-sse.log").write_text(capture.get("raw_sse", ""), encoding="utf-8")
            Path(output / "cancel-events.json").write_text(json.dumps(events, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            Path(output / "cancel-audit.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        concurrent_cases: list[tuple[int, str]] = []
        for index, prompt in enumerate(concurrent_prompts[: args.concurrency], start=1):
            conversation_id, create_response = create_conversation(
                args.base_url,
                args.token,
                f"Concurrent read fixture {index}",
            )
            if conversation_id is None:
                concurrent_cases.append((0, prompt))
                summary.setdefault("concurrent_create_failures", []).append(create_response)
            else:
                created.append(conversation_id)
                concurrent_cases.append((conversation_id, prompt))

        started = time.perf_counter()
        cases: list[dict[str, Any]] = []
        with ThreadPoolExecutor(max_workers=max(1, len(concurrent_cases))) as executor:
            futures = {
                executor.submit(non_stream_case, args.base_url, args.token, conversation_id, prompt): (conversation_id, prompt)
                for conversation_id, prompt in concurrent_cases
                if conversation_id
            }
            for future in as_completed(futures):
                try:
                    cases.append(future.result())
                except Exception as error:  # noqa: BLE001
                    conversation_id, prompt = futures[future]
                    cases.append({"conversation_id": conversation_id, "prompt": prompt, "error": repr(error), "http_status": None})
        durations = [float(item["duration_ms"]) for item in cases if isinstance(item.get("duration_ms"), (int, float))]
        passed = [item for item in cases if item.get("http_status") == 200 and item.get("answer_present")]
        summary["concurrent"] = {
            "requested": len(concurrent_cases),
            "completed": len(cases),
            "passed": len(passed),
            "wall_time_ms": round((time.perf_counter() - started) * 1000, 2),
            "p50_ms": percentile(durations, 0.50),
            "p95_ms": percentile(durations, 0.95),
            "max_ms": max(durations) if durations else None,
            "cases": cases,
            "result": "Passed" if len(passed) == len(concurrent_cases) else "Failed",
        }
        (output / "concurrent-cases.json").write_text(json.dumps(cases, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    finally:
        for conversation_id in created:
            summary["cleanup"].append({"conversation_id": conversation_id, **delete_conversation(args.base_url, args.token, conversation_id)})
        (output / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        (output / "run-status.tsv").write_text(
            "case\tresult\thttp_status\tduration_ms\n"
            + f"cancel\t{summary.get('cancel', {}).get('result')}\t{summary.get('cancel', {}).get('cancel_response', {}).get('status')}\t{summary.get('cancel', {}).get('cancel_response', {}).get('duration_ms')}\n"
            + f"concurrent\t{summary.get('concurrent', {}).get('result')}\t{summary.get('concurrent', {}).get('passed')}/{summary.get('concurrent', {}).get('requested')}\t{summary.get('concurrent', {}).get('wall_time_ms')}\n",
            encoding="utf-8",
        )
    print(output)
    return 0 if summary.get("cancel", {}).get("result") == "Passed" and summary.get("concurrent", {}).get("result") == "Passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
