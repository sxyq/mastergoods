#!/usr/bin/env python3
"""Capture a real multi-turn, multi-tool Agent SSE conversation.

The prompts are ordinary read-only business questions. The harness never
supplies a tool name to the request; expected tool groups are evidence checks.
"""

from __future__ import annotations

import json
import os
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:18080").rstrip("/")
TOKEN = os.environ.get("TOKEN", "")
OUTPUT_ROOT = Path(os.environ.get("OUTPUT_ROOT", "/tmp/mg-agent-stream-long"))
DB_CONTAINER = os.environ.get("DB_CONTAINER", "zhihuiji154-postgres")
DB_USER = os.environ.get("DB_USER", "zhihuiji")
DB_NAME = os.environ.get("DB_NAME", "zhihuiji")

TURNS: list[dict[str, Any]] = [
    {
        "id": "01-sales-cash",
        "prompt": "最近一周销售和回款情况怎么样？把重点数字说一下。",
        "expected_groups": [["sales_overview_lookup"], ["sales_trend_lookup", "payment_lookup"]],
    },
    {
        "id": "02-inventory",
        "prompt": "现在库存里哪些东西快没货了？按紧急程度帮我排一下。",
        "expected_groups": [["inventory_low_stock_lookup"], ["smart_restock_lookup"], ["inventory_panorama_lookup"]],
    },
    {
        "id": "03-receivable-payable",
        "prompt": "客户欠我的钱和我欠供应商的钱分别有多少？重点对象列出来。",
        "expected_groups": [
            ["receivable_payable_lookup"],
            ["customer_receivable_lookup", "supplier_payable_lookup"],
        ],
    },
    {
        "id": "04-business-scan",
        "prompt": "把刚才看到的销售、库存和资金情况放一起看一下，告诉我现在最需要注意什么。",
        "expected_groups": [
            ["sales_overview_lookup", "inventory_panorama_lookup"],
            ["sales_overview_lookup", "cashflow_summary_lookup"],
            ["cross_analysis_lookup"],
            ["anomaly_alert_lookup"],
        ],
    },
    {
        "id": "05-real-chart",
        "prompt": "把最近销售和回款的真实数据画成一张趋势图，有数据再画，没有就直接告诉我。",
        "expected_groups": [
            ["sales_overview_lookup", "result_visualization"],
            ["sales_trend_lookup", "payment_lookup", "result_visualization"],
            ["sales_overview_lookup"],
            ["sales_trend_lookup"],
        ],
    },
    {
        "id": "06-purchase-chain",
        "prompt": "最近采购、入库和退货之间的情况帮我串起来看看。",
        "expected_groups": [
            ["purchase_tracking_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup", "purchase_receipt_lookup"],
            ["purchase_tracking_lookup", "purchase_order_lookup", "purchase_receipt_lookup", "purchase_return_lookup"],
        ],
    },
    {
        "id": "07-follow-up",
        "prompt": "按刚才采购和入库的结果，再看看哪些商品的库存周转需要留意。",
        "expected_groups": [
            ["inventory_panorama_lookup"],
            ["inventory_low_stock_lookup"],
            ["smart_restock_lookup"],
        ],
    },
]


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def request_json(method: str, path: str, payload: dict[str, Any] | None = None, timeout: float = 180) -> dict[str, Any]:
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        BASE_URL + path,
        data=body,
        method=method,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {TOKEN}",
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
            body_value = raw[:4000]
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
    value = body.get("data") if isinstance(body, dict) else None
    return value if isinstance(value, dict) else {}


def response_list(response: dict[str, Any]) -> list[Any]:
    body = response.get("body")
    value = body.get("data") if isinstance(body, dict) else None
    return value if isinstance(value, list) else []


def parse_sse(raw: str, started_at: float) -> list[dict[str, Any]]:
    events: list[dict[str, Any]] = []
    event_name: str | None = None
    data_lines: list[str] = []
    received_at = started_at

    def flush() -> None:
        nonlocal event_name, data_lines, received_at
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
            "received_ms": round((received_at - started_at) * 1000, 2),
        })
        event_name = None
        data_lines = []

    for line in raw.splitlines(keepends=True):
        received_at = time.perf_counter()
        stripped = line.rstrip("\r\n")
        if not stripped:
            flush()
        elif stripped.startswith("event:"):
            event_name = stripped[6:].strip()
        elif stripped.startswith("data:"):
            data_lines.append(stripped[5:].lstrip())
    flush()
    return events


def capture_stream(conversation_id: int, prompt: str, timeout: float = 240) -> dict[str, Any]:
    body = json.dumps(
        {"conversation_id": conversation_id, "message": prompt, "stream": True},
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        BASE_URL + "/v2/agent/chat/stream",
        data=body,
        method="POST",
        headers={
            "Accept": "text/event-stream",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {TOKEN}",
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
        "events": parse_sse(raw, started),
        "raw_sse": raw,
    }


def payload_of(event: dict[str, Any]) -> dict[str, Any]:
    value = event.get("payload")
    return value if isinstance(value, dict) else {}


def tool_names(events: list[dict[str, Any]]) -> list[str]:
    names: list[str] = []
    for event in events:
        if event.get("event_type") not in {"tool_started", "tool_completed", "tool_failed"}:
            continue
        name = payload_of(event).get("tool_name") or payload_of(event).get("toolName")
        if name and name not in names:
            names.append(str(name))
    return names


def tool_call_ids(events: list[dict[str, Any]], event_type: str) -> list[str]:
    values: list[str] = []
    for event in events:
        if event.get("event_type") != event_type:
            continue
        payload = payload_of(event)
        value = payload.get("tool_call_id") or payload.get("toolCallId")
        if value:
            values.append(str(value))
    return values


def db_counts() -> dict[str, int] | None:
    query = (
        "select json_build_object(" 
        "'products',(select count(*) from products),"
        "'customers',(select count(*) from customers),"
        "'suppliers',(select count(*) from suppliers),"
        "'sale_orders',(select count(*) from sale_orders),"
        "'purchase_orders',(select count(*) from purchase_orders),"
        "'finance_records',(select count(*) from finance_records),"
        "'inventory_snapshots',(select count(*) from inventory_snapshots),"
        "'inventory_ledger',(select count(*) from inventory_ledger),"
        "'accounts',(select count(*) from accounts),"
        "'payments',(select count(*) from payments),"
        "'media_assets',(select count(*) from media_assets),"
        "'agent_conversations',(select count(*) from agent_conversations),"
        "'agent_messages',(select count(*) from agent_messages),"
        "'agent_run_audits',(select count(*) from agent_run_audits),"
        "'agent_run_audit_events',(select count(*) from agent_run_audit_events),"
        "'agent_drafts',(select count(*) from agent_drafts));"
    )
    try:
        result = subprocess.run(
            ["docker", "exec", DB_CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME, "-At", "-c", query],
            check=True,
            capture_output=True,
            text=True,
            timeout=30,
        )
        value = json.loads(result.stdout.strip().splitlines()[-1])
        return {str(key): int(value) for key, value in value.items()}
    except Exception:  # noqa: BLE001
        return None


def validate_turn(capture: dict[str, Any], audit: dict[str, Any], messages: dict[str, Any], expected_groups: list[list[str]]) -> dict[str, Any]:
    events = capture.get("events") or []
    event_types = [event.get("event_type") for event in events]
    names = tool_names(events)
    audit_data = response_data(audit)
    audit_events = audit_data.get("events") if isinstance(audit_data.get("events"), list) else []
    audit_names = []
    for event in audit_events:
        name = (event.get("payload") or {}).get("tool_name") if isinstance(event, dict) else None
        if name and name not in audit_names:
            audit_names.append(str(name))
    completed_ids = tool_call_ids(events, "tool_completed")
    duplicate_completed_ids = sorted({value for value in completed_ids if completed_ids.count(value) > 1})
    started_ids = tool_call_ids(events, "tool_started")
    answer_deltas = [event for event in events if event.get("event_type") == "answer_delta"]
    model_deltas = [
        event for event in answer_deltas
        if payload_of(event).get("delta_source") == "model_stream"
    ]
    assistant_messages = [
        item for item in response_list(messages)
        if isinstance(item, dict) and item.get("role") == "assistant"
    ]
    expected_match = any(set(names) == set(group) for group in expected_groups)
    plan_source = audit_data.get("plan_source")
    plan_delta_sources = [
        str(payload_of(event).get("plan_source"))
        for event in events
        if event.get("event_type") == "plan_delta" and payload_of(event).get("plan_source")
    ]
    autonomy_pass = bool(
        any("native_tool_use" in source for source in plan_delta_sources)
        or "native_tool_use" in str(plan_source or "")
        or any(payload_of(event).get("selection_origin") == "model_tool_call" for event in events if event.get("event_type") == "tool_started")
    )
    started_positions: dict[str, int] = {}
    ordering_pass = True
    for position, event in enumerate(events):
        event_type = event.get("event_type")
        payload = payload_of(event)
        call_id = payload.get("tool_call_id") or payload.get("toolCallId")
        if not call_id:
            continue
        if event_type == "tool_started":
            started_positions[str(call_id)] = position
        elif event_type in {"tool_completed", "tool_failed"}:
            ordering_pass = ordering_pass and str(call_id) in started_positions and started_positions[str(call_id)] < position
    stream_pass = (
        capture.get("status") == 200
        and not capture.get("error")
        and "run_started" in event_types
        and "answer_delta" in event_types
        and "answer_completed" in event_types
        and "run_completed" in event_types
        and audit_data.get("status") == "completed"
        and len(assistant_messages) > 0
    )
    return {
        "stream_pass": stream_pass,
        "autonomy_pass": autonomy_pass,
        "expected_tool_group_match": expected_match,
        "ordering_pass": ordering_pass,
        "tool_names": names,
        "audit_tool_names": audit_names,
        "event_types": event_types,
        "event_count": len(events),
        "answer_delta_count": len(answer_deltas),
        "model_stream_delta_count": len(model_deltas),
        "started_tool_call_ids": started_ids,
        "completed_tool_call_ids": completed_ids,
        "duplicate_completed_tool_call_ids": duplicate_completed_ids,
        "plan_source": plan_source,
        "plan_delta_sources": plan_delta_sources,
        "audit_status": audit_data.get("status"),
        "audit_event_count": audit_data.get("event_count"),
        "assistant_message_count": len(assistant_messages),
        "answer_completed_count": event_types.count("answer_completed"),
        "run_completed_count": event_types.count("run_completed"),
    }


def main() -> int:
    if not TOKEN:
        raise SystemExit("TOKEN is required")
    output = OUTPUT_ROOT / datetime.now().strftime("%Y%m%d-%H%M%S-long-stream")
    output.mkdir(parents=True, exist_ok=False)
    summary: dict[str, Any] = {
        "captured_at": now_iso(),
        "base_url": BASE_URL,
        "host": os.environ.get("TEST_HOST", "154.217.241.207"),
        "model": os.environ.get("AGENT_LLM_MODEL", "unknown"),
        "wire_api": os.environ.get("AGENT_LLM_WIRE_API", "unknown"),
        "tool_choice": os.environ.get("AGENT_TOOL_CHOICE", "unknown"),
        "turn_count": len(TURNS),
        "turns": [],
        "pre_counts": db_counts(),
    }
    conversation_id: int | None = None
    try:
        created = request_json("POST", "/v2/agent/conversations", {"title": "long stream read-only fixture", "status": "active"})
        value = response_data(created).get("id")
        conversation_id = int(value) if value is not None else None
        summary["conversation_create"] = created
        if conversation_id is None:
            raise RuntimeError("conversation creation did not return an id")
        for index, turn in enumerate(TURNS):
            if index:
                time.sleep(float(os.environ.get("TURN_GAP_SECONDS", "1.5")))
            capture = capture_stream(conversation_id, turn["prompt"])
            run_id = next((payload_of(event).get("run_id") for event in capture.get("events", []) if payload_of(event).get("run_id")), None)
            audit = request_json("GET", f"/v2/agent/runs/{run_id}/audit", timeout=60) if run_id else {"status": None, "body": None, "error": "run_id missing"}
            messages = request_json("GET", f"/v2/agent/conversations/{conversation_id}/messages?page=0&limit=100", timeout=60)
            validation = validate_turn(capture, audit, messages, turn["expected_groups"])
            turn_record = {
                "index": index + 1,
                "turn_id": turn["id"],
                "prompt": turn["prompt"],
                "conversation_id": conversation_id,
                "run_id": run_id,
                "expected_groups": turn["expected_groups"],
                "capture": {key: value for key, value in capture.items() if key != "raw_sse"},
                "audit": audit,
                "messages": messages,
                "validation": validation,
            }
            turn_dir = output / "turns" / f"{index + 1:02d}-{turn['id']}"
            turn_dir.mkdir(parents=True, exist_ok=True)
            (turn_dir / "request.json").write_text(json.dumps({"conversation_id": conversation_id, "message": turn["prompt"], "stream": True}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            (turn_dir / "raw.sse").write_text(capture.get("raw_sse", ""), encoding="utf-8")
            (turn_dir / "events.json").write_text(json.dumps(capture.get("events", []), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            (turn_dir / "audit.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            (turn_dir / "messages.json").write_text(json.dumps(messages, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            (turn_dir / "validation.json").write_text(json.dumps(validation, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            summary["turns"].append(turn_record)
            (output / "summary-live.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        summary["unique_tools_across_turns"] = sorted({name for turn in summary["turns"] for name in turn["validation"]["tool_names"]})
        summary["multi_tool_turns"] = sum(len(turn["validation"]["tool_names"]) >= 2 for turn in summary["turns"])
        summary["stream_passed_turns"] = sum(bool(turn["validation"]["stream_pass"]) for turn in summary["turns"])
        summary["autonomous_turns"] = sum(bool(turn["validation"]["autonomy_pass"]) for turn in summary["turns"])
    finally:
        if conversation_id is not None:
            summary["conversation_delete"] = request_json("DELETE", f"/v2/agent/conversations/{conversation_id}", {})
        summary["post_counts"] = db_counts()
        if summary.get("pre_counts") and summary.get("post_counts"):
            business_keys = [
                "products", "customers", "suppliers", "sale_orders", "purchase_orders",
                "finance_records", "inventory_snapshots", "inventory_ledger", "accounts",
                "payments", "media_assets",
            ]
            summary["business_count_delta"] = {
                key: summary["post_counts"].get(key, 0) - summary["pre_counts"].get(key, 0)
                for key in business_keys
            }
        summary["result"] = (
            "Passed"
            if summary.get("stream_passed_turns") == len(TURNS)
            and summary.get("autonomous_turns") == len(TURNS)
            and summary.get("multi_tool_turns", 0) >= 2
            and all(not turn["validation"]["duplicate_completed_tool_call_ids"] for turn in summary.get("turns", []))
            else "Failed"
        )
        (output / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(output)
    return 0 if summary.get("result") == "Passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
