#!/usr/bin/env python3
# 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
"""Real cloud Agent concurrency collector for the first-layer closeout."""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import json
import os
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any


BASE_URL = "https://zhj-api.sxyq27.online"
REST_PATH = "/v2/agent/chat"
STREAM_PATH = "/v2/agent/chat/stream"
LOGIN_PATH = "/v1/auth/login"
PROMPT = os.environ.get("AGENT_TEST_PROMPT", "请查询一个商品的基本信息并简要回答。")
KEYCHAIN_SERVICE = "master-goods-cloud-test-account"
KEYCHAIN_ACCOUNT = "all-existing-accounts"
SSH_KEY = os.environ.get(
    "AGENT_CLOUD_SSH_KEY",
    "/Users/sunyiyang/Desktop/Project/master-goods/.ssh-check/8220-readonly.pem",
)
SSH_TARGET = "root@8.220.206.9"
API_CONTAINER = "sxyq27-zhj-api"
DB_CONTAINER = "sxyq27-zhj-postgres"
REDIS_CONTAINER = "sxyq27-zhj-redis"
TERMINALS = {"run_completed", "run_failed", "run_blocked", "run_exhausted", "run_cancelled"}
STREAM_READ_TIMEOUT_SECONDS = float(os.environ.get("AGENT_STREAM_TIMEOUT_SECONDS", "300"))
SERIAL_COLLECTOR_VERSION = "phase2-serial-20260922-r2"
BUSINESS_TABLES = (
    "account_transfers", "accounts", "bill_fund_links", "cash_change_records",
    "credit_transactions", "customers", "finance_records", "import_jobs",
    "inventory_adjustments", "inventory_ledger", "inventory_monthly_stats",
    "inventory_snapshots", "media_assets", "media_bindings", "partner_contacts",
    "partner_groups", "pay_orders", "payments", "poster_generations",
    "product_categories", "product_price_levels", "product_supplier_relations",
    "product_units", "products", "purchase_order_items", "purchase_orders",
    "purchase_receipt_items", "purchase_receipts", "purchase_return_items",
    "purchase_return_refunds", "purchase_returns", "sale_order_items", "sale_orders",
    "sales_return_items", "sales_returns", "store_memberships", "stores", "suppliers",
    "sync_change_log", "sync_cursors", "sync_operation_log", "sync_tombstones", "user_credits",
)


def now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def digest(value: Any) -> str | None:
    if value is None or not str(value).strip():
        return None
    return hashlib.sha256(str(value)[:512].encode("utf-8", "replace")).hexdigest()


def local(command: list[str], timeout: float = 20) -> tuple[int, str, str]:
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout, check=False)
        return result.returncode, result.stdout.strip(), result.stderr.strip()
    except (OSError, subprocess.SubprocessError) as exc:
        return 124, "", type(exc).__name__


def ssh(command: str, timeout: float = 30) -> tuple[int, str, str]:
    return local(
        [
            "ssh", "-i", SSH_KEY, "-o", "IdentitiesOnly=yes", "-o", "BatchMode=yes",
            "-o", "ProxyJump=none", "-o", "ProxyCommand=none", "-o", "ConnectTimeout=8",
            "-o", "StrictHostKeyChecking=accept-new", SSH_TARGET, command,
        ],
        timeout,
    )


def run_remote_script(script: str, timeout: float = 35) -> tuple[int, str, str]:
    encoded = base64.b64encode(script.encode("utf-8")).decode("ascii")
    return ssh(f"echo {encoded} | base64 -d | bash", timeout=timeout)


def read_secret() -> str:
    code, value, error = local(
        ["security", "find-generic-password", "-s", KEYCHAIN_SERVICE, "-a", KEYCHAIN_ACCOUNT, "-w"],
        timeout=10,
    )
    if code != 0 or not value:
        raise RuntimeError(f"keychain_unavailable:{error or 'empty'}")
    return value


def request_json(
    method: str,
    path: str,
    token: str | None,
    body: dict[str, Any] | None = None,
    timeout: float = 30,
    accept: str = "application/json",
) -> tuple[int, str, bytes]:
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    headers = {"Accept": accept}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.headers.get("Content-Type", ""), response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.headers.get("Content-Type", ""), error.read()


def find_token(value: Any) -> str | None:
    if isinstance(value, dict):
        for key, item in value.items():
            if key.lower() in {"token", "access_token", "accesstoken"} and isinstance(item, str):
                return item
            found = find_token(item)
            if found:
                return found
    if isinstance(value, list):
        for item in value:
            found = find_token(item)
            if found:
                return found
    return None


def login(phone: str) -> str:
    password = read_secret()
    status, _, content = request_json("POST", LOGIN_PATH, None, {"phone": phone, "password": password})
    password = ""
    if status < 200 or status >= 300:
        raise RuntimeError(f"login_http_{status}")
    token = find_token(json.loads(content))
    if not token:
        raise RuntimeError("login_token_missing")
    return token


def parse_event(line: bytes) -> dict[str, Any] | None:
    line = line.strip()
    if not line.startswith(b"data:"):
        return None
    payload = line[5:].strip()
    if not payload or payload == b"[DONE]":
        return None
    try:
        event = json.loads(payload)
    except json.JSONDecodeError:
        return None
    return event if isinstance(event, dict) else None


def classify_safe_message(value: Any) -> str | None:
    text = str(value or "").lower()
    if not text:
        return None
    if "tool_arguments_invalid" in text or "参数不符合声明" in text or "参数约束" in text:
        return "tool_arguments_invalid"
    if any(marker in text for marker in ("database", "postgres", "sql", "数据库", "连接池", "connection pool")):
        return "database_error"
    if "timeout" in text or "超时" in text:
        return "timeout"
    if any(marker in text for marker in ("permission", "access denied", "权限", "无权")):
        return "permission_error"
    return None


def event_metadata(event: dict[str, Any]) -> dict[str, Any]:
    error = event.get("error") if isinstance(event.get("error"), dict) else {}
    code = event.get("error_code") or event.get("code") or error.get("error_code") or error.get("code")
    message = event.get("safe_message") or error.get("safe_message")
    return {
        "event_type": event.get("event_type") or event.get("type"),
        "event_id": event.get("event_id"),
        "seq": event.get("seq"),
        "run_id": event.get("run_id"),
        "conversation_id": event.get("conversation_id"),
        "audit_id": event.get("audit_id"),
        "trace_id": event.get("trace_id"),
        "tool_name": event.get("tool_name"),
        "code": code,
        "terminal_status": event.get("terminal_status"),
        "safe_message_digest": digest(message),
        "safe_message_class": classify_safe_message(message),
    }


def new_record(level: int, index: int, start: float) -> dict[str, Any]:
    return {
        "concurrency_level": level,
        "request_index": index,
        "request_start": dt.datetime.fromtimestamp(start, dt.timezone.utc).isoformat(),
        "response_header_time": None,
        "first_sse_byte_time": None,
        "first_event_time": None,
        "first_tool_time": None,
        "first_answer_time": None,
        "stream_end_time": None,
        "completion_time": None,
        "request_start_epoch_ms": round(start * 1000),
        "response_header_ms": None,
        "first_sse_byte_ms": None,
        "first_event_ms": None,
        "first_tool_ms": None,
        "first_answer_ms": None,
        "completion_ms": None,
        "http_status": None,
        "content_type": None,
        "run_id": None,
        "conversation_id": None,
        "audit_id": None,
        "trace_id": None,
        "event_id": None,
        "seq": None,
        "event_types": [],
        "sse_events": [],
        "event_count": 0,
        "tool_names": [],
        "terminal_status": None,
        "terminal_error_code": None,
        "failure_class": None,
        "error_categories": [],
        "safe_message_digest": None,
        "retry_count": 0,
        "response_bytes": 0,
        "cleanup_id": None,
        "cleanup_status": None,
        "cancel_request_start_time": None,
        "cancel_request_time": None,
        "cancel_rtt_ms": None,
        "disconnect_audit_wait_ms": None,
        "redaction_status": "redacted_no_payloads",
        "last_event": None,
        "audit_observation": None,
        "stream_validation": None,
        "collector_error": None,
    }


def mark_time(record: dict[str, Any], name: str, start: float) -> None:
    timestamp = time.time()
    record[name] = dt.datetime.fromtimestamp(timestamp, dt.timezone.utc).isoformat()
    record[name.replace("_time", "_ms")] = round((timestamp - start) * 1000, 2)


def absorb_event(record: dict[str, Any], event: dict[str, Any], start: float) -> None:
    metadata = event_metadata(event)
    event_type = metadata.get("event_type")
    if not event_type:
        return
    if not record["event_types"]:
        mark_time(record, "first_event_time", start)
    record["sse_events"].append(metadata)
    record["event_types"].append(event_type)
    record["event_count"] += 1
    for field in ("run_id", "conversation_id", "audit_id", "trace_id"):
        if metadata.get(field) is not None:
            record[field] = metadata[field]
    if metadata.get("event_id") is not None:
        record["event_id"] = metadata["event_id"]
    if metadata.get("seq") is not None:
        record["seq"] = metadata["seq"]
    if metadata.get("tool_name") and metadata["tool_name"] not in record["tool_names"]:
        record["tool_names"].append(metadata["tool_name"])
    if event_type == "tool_started" and record["first_tool_time"] is None:
        mark_time(record, "first_tool_time", start)
    if event_type == "answer_delta" and record["first_answer_time"] is None:
        mark_time(record, "first_answer_time", start)
    if event_type in TERMINALS:
        record["terminal_status"] = metadata.get("terminal_status") or event_type
    if metadata.get("code"):
        record["terminal_error_code"] = metadata["code"]
    if metadata.get("safe_message_digest"):
        record["safe_message_digest"] = metadata["safe_message_digest"]
    if metadata.get("safe_message_class") and metadata["safe_message_class"] not in record["error_categories"]:
        record["error_categories"].append(metadata["safe_message_class"])
    record["last_event"] = metadata


def audit_record(token: str, record: dict[str, Any]) -> None:
    run_id = record.get("run_id")
    if not run_id:
        return
    record["audit_lookup_started_at"] = now_iso()
    try:
        status, _, content = request_json("GET", f"/v2/agent/runs/{run_id}/audit", token, timeout=30)
        if status != 200:
            record["audit_observation"] = {"available": False, "http_status": status}
            return
        root = json.loads(content)
        data = root.get("data") if isinstance(root, dict) else None
        if not isinstance(data, dict):
            record["audit_observation"] = {"available": False, "http_status": status}
            return
        for field in ("conversation_id", "audit_id", "trace_id"):
            if data.get(field) is not None:
                record[field] = data[field]
        record["terminal_status"] = data.get("status") or record["terminal_status"]
        record["terminal_error_code"] = data.get("error_code") or record["terminal_error_code"]
        record["safe_message_digest"] = digest(data.get("error_message")) or record["safe_message_digest"]
        events = []
        for item in data.get("events") or []:
            if not isinstance(item, dict):
                continue
            payload = item.get("payload") if isinstance(item.get("payload"), dict) else {}
            event = {
                "event_type": item.get("event_type") or payload.get("event_type"),
                "event_id": item.get("event_id") or payload.get("event_id"),
                "seq": item.get("seq") or payload.get("seq"),
                "run_id": data.get("run_id"),
                "conversation_id": data.get("conversation_id"),
                "audit_id": data.get("audit_id"),
                "trace_id": data.get("trace_id"),
                "tool_name": payload.get("tool_name"),
                "code": payload.get("code") or payload.get("error_code"),
                "terminal_status": payload.get("terminal_status"),
                "safe_message": payload.get("safe_message"),
            }
            events.append(event_metadata(event))
        record["audit_observation"] = {
            "available": True,
            "http_status": status,
            "status": data.get("status"),
            "mode": data.get("mode"),
            "llm_status": data.get("llm_status"),
            "plan_source": data.get("plan_source"),
            "tool_count": data.get("tool_count"),
            "event_count": data.get("event_count"),
            "audit_lossy": data.get("audit_lossy"),
            "emitted_event_count": data.get("emitted_event_count"),
            "error_code": data.get("error_code"),
            "error_message_digest": digest(data.get("error_message")),
            "events": events,
        }
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError) as exc:
        record["audit_observation"] = {"available": False, "error": type(exc).__name__}
    finally:
        record["audit_lookup_completed_at"] = now_iso()


def audit_record_after_disconnect(token: str, record: dict[str, Any], timeout: float = 30.0) -> None:
    """Let a detached worker persist its terminal audit before cleanup."""
    started = time.monotonic()
    deadline = started + timeout
    while True:
        audit_record(token, record)
        status = str((record.get("audit_observation") or {}).get("status") or "").lower()
        if status and status not in {"running", "pending"}:
            break
        if time.monotonic() >= deadline:
            break
        time.sleep(0.25)
    record["disconnect_audit_wait_ms"] = round((time.monotonic() - started) * 1000, 2)


def validate_stream(record: dict[str, Any]) -> dict[str, Any]:
    """Validate only redacted SSE/audit metadata; event payloads never enter evidence."""
    sse_events = record.get("sse_events") or []
    sse_ids = [item.get("event_id") for item in sse_events if item.get("event_id") is not None]
    sse_seqs = [item.get("seq") for item in sse_events]
    terminal_count = sum(item.get("event_type") in TERMINALS for item in sse_events)
    identity_sets = {
        field: sorted({str(item[field]) for item in sse_events if item.get(field) is not None})
        for field in ("run_id", "conversation_id", "audit_id", "trace_id")
    }
    seq_contiguous = (
        bool(sse_seqs)
        and all(isinstance(value, int) for value in sse_seqs)
        and sse_seqs == list(range(1, len(sse_seqs) + 1))
    )
    audit = record.get("audit_observation") or {}
    audit_events = audit.get("events") if isinstance(audit, dict) else None
    audit_events = audit_events if isinstance(audit_events, list) else []
    audit_ids = [item.get("event_id") for item in audit_events if item.get("event_id") is not None]
    audit_seqs = [item.get("seq") for item in audit_events]
    audit_types = [item.get("event_type") for item in audit_events]
    sse_types = [item.get("event_type") for item in sse_events]
    audit_available = bool(isinstance(audit, dict) and audit.get("available"))
    return {
        "sse_event_count": len(sse_events),
        "sse_event_id_duplicate_count": len(sse_ids) - len(set(sse_ids)),
        "sse_seq_contiguous": seq_contiguous,
        "sse_terminal_count": terminal_count,
        "sse_terminal_unique": terminal_count == 1,
        "identity_mixed_within_stream": any(len(values) > 1 for values in identity_sets.values()),
        "identity_values": identity_sets,
        "audit_available": audit_available,
        "audit_event_count": len(audit_events) if audit_available else None,
        "audit_sse_event_types_match": audit_types == sse_types if audit_available else None,
        "audit_sse_event_ids_match": audit_ids == sse_ids if audit_available else None,
        "audit_sse_seq_match": audit_seqs == sse_seqs if audit_available else None,
        "sse_audit_consistent": (
            bool(sse_events)
            and seq_contiguous
            and terminal_count == 1
            and audit_available
            and audit_types == sse_types
            and audit_ids == sse_ids
            and audit_seqs == sse_seqs
        ),
        "redaction_status": "metadata_only_no_payloads",
    }


def normalize_status(value: Any) -> str:
    return str(value or "").strip().upper()


def validate_rest(record: dict[str, Any]) -> dict[str, Any]:
    """Validate REST response/audit metadata without retaining response payloads."""
    response = record.get("rest_response") or {}
    audit = record.get("audit_observation") or {}
    audit_events = audit.get("events") if isinstance(audit, dict) else None
    audit_events = audit_events if isinstance(audit_events, list) else []
    audit_ids = [item.get("event_id") for item in audit_events if item.get("event_id") is not None]
    audit_seqs = [item.get("seq") for item in audit_events]
    audit_event_count = audit.get("event_count") if isinstance(audit, dict) else None
    response_tool_names = sorted({str(name) for name in response.get("tool_names", []) if name})
    audit_tool_names = sorted({
        str(item.get("tool_name")) for item in audit_events if item.get("tool_name")
    })
    response_status = normalize_status(response.get("terminal_status"))
    audit_status = normalize_status(audit.get("status"))
    audit_available = bool(isinstance(audit, dict) and audit.get("available"))
    audit_seq_contiguous = (
        bool(audit_seqs)
        and all(isinstance(value, int) for value in audit_seqs)
        and audit_seqs == list(range(1, len(audit_seqs) + 1))
    )
    audit_event_count_matches = audit_event_count == len(audit_events)
    audit_event_ids_unique = len(audit_ids) == len(set(audit_ids))
    terminal_status_matches = bool(response_status and audit_status and response_status == audit_status)
    tool_names_match = response_tool_names == audit_tool_names
    response_identity = {
        field: response.get(field) for field in ("run_id", "conversation_id", "audit_id", "trace_id")
    }
    audit_identity = {
        "run_id": record.get("run_id"),
        "conversation_id": record.get("conversation_id"),
        "audit_id": record.get("audit_id"),
        "trace_id": record.get("trace_id"),
    }
    identity_matches = all(
        response_identity[field] is not None
        and str(response_identity[field]) == str(audit_identity[field])
        for field in response_identity
    )
    return {
        "transport": "rest",
        "sse_not_applicable": True,
        "audit_available": audit_available,
        "rest_audit_event_count": len(audit_events) if audit_available else None,
        "audit_event_count_matches_list": audit_event_count_matches if audit_available else None,
        "audit_event_id_duplicate_count": len(audit_ids) - len(set(audit_ids)),
        "audit_seq_contiguous": audit_seq_contiguous,
        "rest_terminal_status_matches_audit": terminal_status_matches,
        "rest_identity_matches_audit": identity_matches,
        "response_tool_names": response_tool_names,
        "audit_tool_names": audit_tool_names,
        "rest_tool_names_match_audit": tool_names_match,
        "audit_terminal_event_count": sum(
            item.get("event_type") in TERMINALS for item in audit_events
        ),
        "rest_audit_consistent": (
            audit_available
            and audit_event_count_matches
            and audit_event_ids_unique
            and audit_seq_contiguous
            and terminal_status_matches
            and identity_matches
            and tool_names_match
        ),
        "redaction_status": "metadata_only_no_payloads",
    }


def transport_audit_consistent(record: dict[str, Any]) -> bool:
    validation = record.get("stream_validation") or {}
    if record.get("transport") == "rest":
        return bool(validation.get("rest_audit_consistent", False))
    return bool(validation.get("sse_audit_consistent", False))


def classify(record: dict[str, Any]) -> str | None:
    if not transport_audit_consistent(record):
        return "collector_error"
    code = record.get("terminal_error_code")
    categories = set(record.get("error_categories") or [])
    if "tool_arguments_invalid" in categories:
        return "agent_orchestration_error"
    if "permission_error" in categories:
        return "permission_error"
    if "database_error" in categories or code == "DATABASE_ERROR":
        return "database_error"
    if "timeout" in categories:
        return "timeout"
    if code in {"LLM_UNAVAILABLE", "LLM_PLANNING_FAILED", "LLM_ANSWER_UNAVAILABLE", "PROVIDER_ERROR"}:
        return "provider_error"
    if code in {"TIMEOUT", "PROVIDER_TIMEOUT"}:
        return "timeout"
    if code in {"CONNECTION_POOL_EXHAUSTED", "POOL_TIMEOUT"}:
        return "connection_pool_error"
    if code in {"MODEL_TOOL_SELECTION_FAILED", "AGENT_ITERATION_EXHAUSTED", "TOOL_ARGUMENTS_INVALID", "STREAM_ERROR"}:
        return "agent_orchestration_error"
    if code == "TOOL_QUERY_FAILED":
        return "tool_execution_error"
    terminal_status = str(record.get("terminal_status") or "").upper()
    if terminal_status == "COMPLETED":
        return None
    return "unknown" if terminal_status else "collector_error"


def cleanup(token: str, record: dict[str, Any]) -> None:
    conversation_id = record.get("conversation_id")
    if conversation_id is None:
        return
    record["cleanup_id"] = conversation_id
    record["cleanup_started_at"] = now_iso()
    try:
        status, _, _ = request_json("DELETE", f"/v2/agent/conversations/{conversation_id}", token, timeout=30)
        record["cleanup_status"] = status
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        record["cleanup_status"] = f"error:{type(exc).__name__}"
    finally:
        record["cleanup_completed_at"] = now_iso()


def collect_one(level: int, index: int, token: str) -> dict[str, Any]:
    start = time.time()
    record = new_record(level, index, start)
    try:
        request = urllib.request.Request(
            BASE_URL + STREAM_PATH,
            data=json.dumps({"conversation_id": None, "message": PROMPT, "stream": True}, ensure_ascii=False).encode("utf-8"),
            headers={"Accept": "text/event-stream", "Content-Type": "application/json", "Authorization": f"Bearer {token}"},
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=STREAM_READ_TIMEOUT_SECONDS) as response:
            record["http_status"] = response.status
            record["content_type"] = response.headers.get("Content-Type", "")
            mark_time(record, "response_header_time", start)
            while True:
                line = response.readline()
                if not line:
                    break
                if record["first_sse_byte_time"] is None:
                    mark_time(record, "first_sse_byte_time", start)
                record["response_bytes"] += len(line)
                event = parse_event(line)
                if event:
                    absorb_event(record, event, start)
        mark_time(record, "completion_time", start)
    except urllib.error.HTTPError as exc:
        record["http_status"] = exc.code
        record["content_type"] = exc.headers.get("Content-Type", "")
        record["collector_error"] = f"http_{exc.code}"
        mark_time(record, "completion_time", start)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        record["collector_error"] = "timeout" if isinstance(exc, TimeoutError) else type(exc).__name__
        mark_time(record, "completion_time", start)
    except Exception as exc:
        record["collector_error"] = type(exc).__name__
        mark_time(record, "completion_time", start)
    audit_record(token, record)
    cleanup(token, record)
    record["stream_validation"] = validate_stream(record)
    record["failure_class"] = classify(record)
    if not record.get("terminal_status") and record.get("collector_error"):
        record["terminal_status"] = "collector_error"
    return record


def remote_snapshot(stage: str, level: int) -> dict[str, Any]:
    script = f'''set +e
echo "captured_at=$(date -Is)"
echo "stage={stage}"
echo "concurrency_level={level}"
echo "load=$(uptime | sed 's/^[[:space:]]*//')"
echo "host_cpu_cores=$(nproc 2>/dev/null || echo unavailable)"
awk '/^cpu / {{print "host_cpu_stat="$2","$3","$4","$5","$6","$7","$8","$9}}' /proc/stat 2>/dev/null
free -b | awk 'NR==2 {{print "host_mem_total=" $2; print "host_mem_used=" $3; print "host_mem_available=" $7}} NR==3 {{print "host_swap_used=" $3}}'
docker stats --no-stream --format 'container={{{{.Name}}}} cpu={{{{.CPUPerc}}}} mem={{{{.MemUsage}}}} pids={{{{.PIDs}}}}' 2>/dev/null
pid=$(pgrep -f '[j]ava' | head -1)
if [ -n "$pid" ]; then awk '/^VmRSS:|^VmSize:|^Threads:/ {{print $1 "=" $2 " " $3}}' /proc/$pid/status 2>/dev/null; else echo 'java_metrics=unavailable'; fi
dbpass=$(docker inspect -f '{{{{range .Config.Env}}}}{{{{println .}}}}{{{{end}}}}' {API_CONTAINER} | sed -n 's/^DB_PASSWORD=//p')
docker exec -e PGPASSWORD="$dbpass" {DB_CONTAINER} psql -U zhj -d zhj -Atc "select 'pg_state|'||coalesce(state,'unknown')||'|'||count(*) from pg_stat_activity group by state order by state; select 'active_runs|'||count(*) from agent_run_audits where completed_at is null;" 2>/dev/null
echo "redis_info=$(docker exec {REDIS_CONTAINER} redis-cli INFO clients memory 2>/dev/null | tr '\\n' ';')"
echo "nginx_tcp_connections=$(ss -Htan 2>/dev/null | awk '$4 ~ /:80$/ || $4 ~ /:443$/ {{n++}} END {{print n+0}}')"
echo "tcp_connections=$(ss -Htan 2>/dev/null | wc -l | tr -d ' ')"
'''
    code, output, error = run_remote_script(script)
    return {
        "captured_at": now_iso(),
        "stage": stage,
        "concurrency_level": level,
        "ssh_exit_code": code,
        "output": output,
        "error_type": error or None,
        "jvm_heap_used": "unavailable; current runtime probe does not expose heap used",
        "connection_pool_wait": "unavailable; current runtime probe does not expose pool wait",
        "redaction_status": "resource_summary_only",
    }


def db_summary(include_business: bool = False) -> dict[str, Any]:
    tables = [
        "agent_conversations", "agent_messages", "agent_drafts", "agent_run_audits",
        "agent_run_audit_events", "products", "customers", "finance_records",
    ]
    if include_business:
        tables.extend(table for table in BUSINESS_TABLES if table not in tables)
        tables.extend(("agent_context_checkpoints", "agent_memories"))
    queries = [f"select '{table}|'||count(*) from {table};" for table in tables]
    if include_business:
        queries.append("select 'conversation_id_seen|'||id from agent_conversations order by id;")
    queries.extend((
        "select 'active_runs|'||count(*) from agent_run_audits where completed_at is null;",
        "select 'active_run_id|'||run_id from agent_run_audits where completed_at is null order by run_id;",
    ))
    sql = " ".join(queries)
    script = f'''set -e
docker exec {DB_CONTAINER} psql -X -v ON_ERROR_STOP=1 -U zhj -d zhj -Atc "{sql}"
'''
    code, output, error = run_remote_script(script)
    values: dict[str, Any] = {}
    active_ids: list[str] = []
    conversation_ids: list[str] = []
    for line in output.splitlines():
        if "|" not in line:
            continue
        key, value = line.split("|", 1)
        if key == "active_run_id":
            active_ids.append(value)
            continue
        if key == "conversation_id_seen":
            conversation_ids.append(value)
            continue
        try:
            values[key] = int(value)
        except ValueError:
            values[key] = "unavailable"
    result = {
        "captured_at": now_iso(), "ssh_exit_code": code, "counts": values,
        "active_run_ids": active_ids, "error_type": error or None,
        "redaction_status": "counts_and_run_ids_only",
    }
    if include_business:
        result["_existing_conversation_ids"] = conversation_ids
    return result


def serial_cleanup_allowed(record: dict[str, Any], protected_ids: set[str]) -> bool:
    conversation_id = record.get("conversation_id")
    if conversation_id is None or str(conversation_id) in protected_ids:
        return False
    source = ([record.get("rest_response") or {}] if record.get("transport") == "rest"
              else record.get("sse_events") or [])
    audit = record.get("audit_observation") or {}
    audit_events = audit.get("events") or []
    if not source or not audit.get("available") or not audit_events:
        return False
    for field in ("run_id", "conversation_id", "audit_id", "trace_id"):
        source_values = {str(event.get(field)) for event in source if event.get(field) is not None}
        audit_values = {str(event.get(field)) for event in audit_events if event.get(field) is not None}
        if len(source_values) != 1 or source_values != audit_values:
            return False
        if field == "conversation_id" and source_values != {str(conversation_id)}:
            return False
    return True


def serial_stop_reasons(
    record: dict[str, Any], before: dict[str, Any], after: dict[str, Any], error_streak: int,
) -> list[str]:
    reasons: list[str] = []
    cleanup_status = record.get("cleanup_status")
    if not isinstance(cleanup_status, int) or not 200 <= cleanup_status < 300:
        reasons.append("cleanup_failed_or_missing")
    if after.get("ssh_exit_code") != 0:
        reasons.append("database_observation_unavailable")
    expected = before.get("counts") or {}
    observed = after.get("counts") or {}
    tracked = set(BUSINESS_TABLES) | {
        "agent_conversations", "agent_messages", "agent_drafts", "agent_context_checkpoints", "agent_memories",
    }
    if any(not isinstance(observed.get(table), int) for table in tracked):
        reasons.append("database_counts_unavailable")
    elif any(observed[table] != expected.get(table) for table in tracked):
        reasons.append("database_counts_changed")
    if (
        observed.get("active_runs") != expected.get("active_runs")
        or sorted(after.get("active_run_ids") or []) != sorted(before.get("active_run_ids") or [])
    ):
        reasons.append("existing_runs_not_restored")
    validation = record.get("stream_validation") or {}
    if not transport_audit_consistent(record):
        reasons.append("transport_audit_inconsistent")
    if validation.get("identity_mixed_within_stream"):
        reasons.append("stream_identity_mixed")
    if (
        validation.get("sse_event_id_duplicate_count", 0)
        or validation.get("audit_event_id_duplicate_count", 0)
    ):
        reasons.append("duplicate_event_id")
    audit_events = (record.get("audit_observation") or {}).get("events") or []
    sse_events = record.get("sse_events") or []
    if record.get("transport", "stream") == "stream" and any(
        {str(event[field]) for event in sse_events if event.get(field) is not None}
        != {str(event[field]) for event in audit_events if event.get(field) is not None}
        for field in ("run_id", "conversation_id", "audit_id", "trace_id")
    ):
        reasons.append("transport_audit_identity_mismatch")
    if record.get("tool_names"):
        reasons.append("unexpected_tool_for_no_tool_scenario")
    audit_done = record.get("audit_lookup_completed_at")
    cleanup_started = record.get("cleanup_started_at")
    if not audit_done or not cleanup_started or audit_done > cleanup_started:
        reasons.append("audit_before_cleanup_unproven")
    if error_streak >= 2:
        reasons.append("consecutive_provider_or_http_errors")
    return reasons


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def append_jsonl(handle: Any, value: Any) -> None:
    handle.write(json.dumps(value, ensure_ascii=True, separators=(",", ":")) + "\n")
    handle.flush()
    os.fsync(handle.fileno())


def artifact_path(output: Path, number: int, run_label: str, stem: str) -> Path:
    prefix = f"{run_label}-" if run_label else ""
    return output / f"{number:02d}-{prefix}{stem}"


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    values = sorted(values)
    position = (len(values) - 1) * p / 100
    low = int(position)
    high = min(low + 1, len(values) - 1)
    return round(values[low] + (values[high] - values[low]) * (position - low), 2)


def summary(level: int, records: list[dict[str, Any]], warmup: list[dict[str, Any]]) -> dict[str, Any]:
    terminal_completed = [r for r in records if str(r.get("terminal_status") or "").upper() == "COMPLETED"]
    completed = [r for r in terminal_completed if not r.get("failure_class")]
    degraded_completed = [r for r in terminal_completed if r.get("failure_class")]
    durations = [r["completion_ms"] for r in completed if isinstance(r.get("completion_ms"), (int, float))]
    def count(status: str) -> int:
        return sum(str(r.get("terminal_status") or "").upper() == status for r in records)
    codes = sorted({r.get("terminal_error_code") for r in records if r.get("terminal_error_code")})
    classes = sorted({r.get("failure_class") for r in records if r.get("failure_class")})
    return {
        "concurrency_level": level,
        "requested": len(records),
        "http_200": sum(r.get("http_status") == 200 for r in records),
        "completed": len(completed),
        "valid_completed": len(completed),
        "terminal_completed": len(terminal_completed),
        "degraded_completed": len(degraded_completed),
        "failed": count("FAILED"),
        "blocked": count("BLOCKED"),
        "exhausted": count("EXHAUSTED"),
        "cancelled": count("CANCELLED"),
        "timeout_count": sum(r.get("failure_class") == "timeout" for r in records),
        "five_xx": sum(isinstance(r.get("http_status"), int) and 500 <= r["http_status"] < 600 for r in records),
        "429": sum(r.get("http_status") == 429 for r in records),
        "unique_terminal_count": sum(bool(r.get("terminal_status")) for r in records),
        "missing_terminal_count": sum(not r.get("terminal_status") for r in records),
        "sse_loss_count": sum(not (r.get("stream_validation") or {}).get("sse_audit_consistent", False) for r in records),
        "sse_duplicate_count": sum((r.get("stream_validation") or {}).get("sse_event_id_duplicate_count", 0) > 0 for r in records),
        "terminal_uniqueness_failure_count": sum(not (r.get("stream_validation") or {}).get("sse_terminal_unique", False) for r in records),
        "identity_mixing_count": sum((r.get("stream_validation") or {}).get("identity_mixed_within_stream", False) for r in records),
        "audit_event_mismatch_count": sum(
            (r.get("stream_validation") or {}).get("audit_available")
            and not all((r.get("stream_validation") or {}).get(key) for key in (
                "audit_sse_event_types_match", "audit_sse_event_ids_match", "audit_sse_seq_match"
            ))
            for r in records
        ),
        "p50_ms": percentile(durations, 50),
        "p95_ms": percentile(durations, 95),
        "p99_ms": percentile(durations, 99),
        "max_ms": max(durations) if durations else None,
        "first_event_p50_ms": percentile([r["first_event_ms"] for r in completed if r.get("first_event_ms") is not None], 50),
        "first_tool_p50_ms": percentile([r["first_tool_ms"] for r in completed if r.get("first_tool_ms") is not None], 50),
        "first_answer_p50_ms": percentile([r["first_answer_ms"] for r in completed if r.get("first_answer_ms") is not None], 50),
        "error_codes": {code: sum(r.get("terminal_error_code") == code for r in records) for code in codes},
        "failure_classes": {kind: sum(r.get("failure_class") == kind for r in records) for kind in classes},
        "tool_failure_event_count": sum(
            sum(event.get("event_type") == "tool_failed" for event in (r.get("sse_events") or []))
            for r in records
        ),
        "cleanup_success": sum(isinstance(r.get("cleanup_status"), int) and 200 <= r["cleanup_status"] < 300 for r in records),
        "warmup": warmup,
        "redaction_status": "redacted_no_payloads",
    }


def response_data(content: bytes) -> Any:
    try:
        root = json.loads(content)
    except (TypeError, json.JSONDecodeError):
        return None
    return root.get("data") if isinstance(root, dict) else None


def collect_stream_request(
    level: int,
    index: int,
    token: str,
    message: str,
    conversation_id: int | None = None,
    cleanup_after: bool = True,
    cancel_after_event: int | None = None,
    disconnect_after_event: int | None = None,
    cancel_after_completion: bool = False,
) -> dict[str, Any]:
    start = time.time()
    record = new_record(level, index, start)
    record["prompt_digest"] = digest(message)
    record["scenario_control"] = {
        "cancel_after_event": cancel_after_event,
        "disconnect_after_event": disconnect_after_event,
        "cancel_after_completion": cancel_after_completion,
    }
    if conversation_id is not None:
        record["conversation_id"] = conversation_id
    cancel_sent = False
    intentional_disconnect = False
    try:
        request = urllib.request.Request(
            BASE_URL + STREAM_PATH,
            data=json.dumps(
                {"conversation_id": conversation_id, "message": message, "stream": True},
                ensure_ascii=False,
            ).encode("utf-8"),
            headers={
                "Accept": "text/event-stream",
                "Content-Type": "application/json",
                "Authorization": f"Bearer {token}",
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=STREAM_READ_TIMEOUT_SECONDS) as response:
            record["http_status"] = response.status
            record["content_type"] = response.headers.get("Content-Type", "")
            mark_time(record, "response_header_time", start)
            while True:
                line = response.readline()
                if not line:
                    break
                if record["first_sse_byte_time"] is None:
                    mark_time(record, "first_sse_byte_time", start)
                record["response_bytes"] += len(line)
                event = parse_event(line)
                if event:
                    absorb_event(record, event, start)
                    event_count = len(record["sse_events"])
                    if (
                        cancel_after_event is not None
                        and not cancel_sent
                        and event_count >= cancel_after_event
                        and record.get("run_id")
                    ):
                        cancel_started = time.time()
                        record["cancel_request_start_time"] = dt.datetime.fromtimestamp(
                            cancel_started, dt.timezone.utc
                        ).isoformat()
                        status, _, content = request_json(
                            "POST",
                            f"/v2/agent/runs/{record['run_id']}/cancel",
                            token,
                            timeout=30,
                        )
                        record["cancel_request_status"] = status
                        record["cancel_response_data"] = response_data(content)
                        record["cancel_request_time"] = now_iso()
                        record["cancel_rtt_ms"] = round((time.time() - cancel_started) * 1000, 2)
                        cancel_sent = True
                    if (
                        disconnect_after_event is not None
                        and event_count >= disconnect_after_event
                    ):
                        intentional_disconnect = True
                        break
        mark_time(record, "stream_end_time", start)
        if (
            cancel_after_completion
            and not cancel_sent
            and record.get("run_id")
        ):
            cancel_started = time.time()
            record["cancel_request_start_time"] = dt.datetime.fromtimestamp(
                cancel_started, dt.timezone.utc
            ).isoformat()
            status, _, content = request_json(
                "POST",
                f"/v2/agent/runs/{record['run_id']}/cancel",
                token,
                timeout=30,
            )
            record["cancel_request_status"] = status
            record["cancel_response_data"] = response_data(content)
            record["cancel_request_time"] = now_iso()
            record["cancel_rtt_ms"] = round((time.time() - cancel_started) * 1000, 2)
            cancel_sent = True
        if intentional_disconnect and not record.get("terminal_status"):
            record["collector_error"] = "intentional_disconnect"
        mark_time(record, "completion_time", start)
    except urllib.error.HTTPError as exc:
        record["http_status"] = exc.code
        record["content_type"] = exc.headers.get("Content-Type", "")
        record["collector_error"] = f"http_{exc.code}"
        mark_time(record, "completion_time", start)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        record["collector_error"] = "timeout" if isinstance(exc, TimeoutError) else type(exc).__name__
        mark_time(record, "completion_time", start)
    except Exception as exc:
        record["collector_error"] = type(exc).__name__
        mark_time(record, "completion_time", start)
    if record.get("run_id"):
        if intentional_disconnect:
            audit_record_after_disconnect(token, record)
        else:
            audit_record(token, record)
    if cleanup_after:
        cleanup(token, record)
    record["stream_validation"] = validate_stream(record)
    record["failure_class"] = classify(record)
    record["intentional_disconnect"] = intentional_disconnect
    if not record.get("terminal_status") and record.get("collector_error"):
        record["terminal_status"] = "collector_error"
    return record


def collect_rest_request(
    level: int,
    index: int,
    token: str,
    message: str,
    conversation_id: int | None = None,
    cleanup_after: bool = True,
) -> dict[str, Any]:
    start = time.time()
    record = new_record(level, index, start)
    record["prompt_digest"] = digest(message)
    record["transport"] = "rest"
    response_metadata: dict[str, Any] = {}
    try:
        request = urllib.request.Request(
            BASE_URL + REST_PATH,
            data=json.dumps(
                {"conversation_id": conversation_id, "message": message, "stream": False},
                ensure_ascii=False,
            ).encode("utf-8"),
            headers={
                "Accept": "application/json",
                "Content-Type": "application/json",
                "Authorization": f"Bearer {token}",
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=STREAM_READ_TIMEOUT_SECONDS) as response:
            record["http_status"] = response.status
            record["content_type"] = response.headers.get("Content-Type", "")
            mark_time(record, "response_header_time", start)
            content = response.read()
            record["response_bytes"] = len(content)
        mark_time(record, "completion_time", start)
        data = response_data(content)
        if not isinstance(data, dict):
            record["collector_error"] = "rest_response_data_missing"
        else:
            for field in ("run_id", "conversation_id", "audit_id", "trace_id"):
                if data.get(field) is not None:
                    record[field] = data[field]
                    response_metadata[field] = data[field]
            record["terminal_status"] = data.get("terminal_status")
            record["terminal_error_code"] = data.get("error_code")
            record["safe_message_digest"] = digest(data.get("safe_message"))
            safe_message_class = classify_safe_message(data.get("safe_message"))
            if safe_message_class:
                record["error_categories"].append(safe_message_class)
            tool_calls = data.get("tool_calls") if isinstance(data.get("tool_calls"), list) else []
            response_tool_names = sorted({
                str(item.get("tool_name"))
                for item in tool_calls
                if isinstance(item, dict) and item.get("tool_name")
            })
            record["tool_names"] = response_tool_names
            response_metadata.update({
                "terminal_status": data.get("terminal_status"),
                "tool_names": response_tool_names,
                "tool_call_count": len(tool_calls),
                "completed_tools": sorted({str(name) for name in (data.get("completed_tools") or []) if name}),
                "missing_target_tools": sorted({
                    str(name) for name in (data.get("missing_target_tools") or []) if name
                }),
                "mode": data.get("mode"),
                "llm_status": data.get("llm_status"),
                "plan_source": data.get("plan_source"),
                "safety_passed": data.get("safety_passed"),
                "performance_summary": data.get("performance_summary")
                if isinstance(data.get("performance_summary"), dict)
                else None,
            })
    except urllib.error.HTTPError as exc:
        record["http_status"] = exc.code
        record["content_type"] = exc.headers.get("Content-Type", "")
        record["response_bytes"] = len(exc.read())
        record["collector_error"] = f"http_{exc.code}"
        mark_time(record, "completion_time", start)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        record["collector_error"] = "timeout" if isinstance(exc, TimeoutError) else type(exc).__name__
        mark_time(record, "completion_time", start)
    except Exception as exc:
        record["collector_error"] = type(exc).__name__
        mark_time(record, "completion_time", start)
    record["rest_response"] = response_metadata
    audit_record(token, record)
    audit_events = (record.get("audit_observation") or {}).get("events") or []
    record["tool_names"] = sorted({
        *record.get("tool_names", []),
        *(str(item.get("tool_name")) for item in audit_events if item.get("tool_name")),
    })
    if cleanup_after:
        cleanup(token, record)
    record["stream_validation"] = validate_rest(record)
    record["failure_class"] = classify(record)
    if not record.get("terminal_status") and record.get("collector_error"):
        record["terminal_status"] = "collector_error"
    return record


def collect_multi_turn(
    level: int,
    index: int,
    token: str,
    messages: list[str],
) -> dict[str, Any]:
    started = time.time()
    turns: list[dict[str, Any]] = []
    conversation_id: int | None = None
    for turn_number, message in enumerate(messages, start=1):
        record = collect_stream_request(
            level,
            index,
            token,
            message,
            conversation_id=conversation_id,
            cleanup_after=False,
        )
        record["turn_number"] = turn_number
        turns.append(record)
        conversation_id = record.get("conversation_id") or conversation_id
        if str(record.get("terminal_status") or "").upper() != "COMPLETED":
            break

    final = dict(turns[-1]) if turns else new_record(level, index, started)
    final["prompt_digest"] = digest("|".join(messages))
    final["turn_count_requested"] = len(messages)
    final["turn_count_completed"] = len(turns)
    final["turns"] = turns
    final["conversation_id"] = conversation_id
    final["request_start"] = dt.datetime.fromtimestamp(started, dt.timezone.utc).isoformat()
    final["request_start_epoch_ms"] = round(started * 1000)
    final["completion_ms"] = round((time.time() - started) * 1000, 2)
    final["completion_time"] = now_iso()
    all_events = [event for turn in turns for event in turn.get("sse_events", [])]
    final["sse_events"] = all_events
    final["event_types"] = [event.get("event_type") for event in all_events]
    final["event_count"] = len(all_events)
    final["tool_names"] = sorted({name for turn in turns for name in turn.get("tool_names", [])})
    final["failure_class"] = next(
        (turn.get("failure_class") for turn in turns if turn.get("failure_class")),
        None,
    )
    final["stream_validation"] = {
        "turns": [turn.get("stream_validation") for turn in turns],
        "all_turns_consistent": bool(turns)
        and all((turn.get("stream_validation") or {}).get("sse_audit_consistent", False) for turn in turns),
        "sse_event_count": len(all_events),
        "redaction_status": "metadata_only_no_payloads",
    }
    final["audit_observation"] = {
        "available": bool(turns) and all(
            (turn.get("audit_observation") or {}).get("available", False) for turn in turns
        ),
        "turns": [turn.get("audit_observation") for turn in turns],
        "redaction_status": "metadata_only_no_payloads",
    }
    cleanup(token, final)
    return final


def scenario_valid(record: dict[str, Any]) -> bool:
    validation = record.get("stream_validation") or {}
    consistent = validation.get("all_turns_consistent", transport_audit_consistent(record))
    return (
        str(record.get("terminal_status") or "").upper() == "COMPLETED"
        and not record.get("failure_class") and not record.get("stop_reasons") and consistent
    )


def scenario_summary(level: int, records: list[dict[str, Any]], warmup: list[dict[str, Any]], scenario: str) -> dict[str, Any]:
    valid = [record for record in records if scenario_valid(record)]
    durations = [record["completion_ms"] for record in valid if isinstance(record.get("completion_ms"), (int, float))]
    transport = records[0].get("transport", "stream") if records else "stream"
    terminal_statuses = [str(record.get("terminal_status") or "").upper() for record in records]
    return {
        "scenario": scenario,
        "transport": transport,
        "concurrency_level": level,
        "requested": len(records),
        "http_200": sum(record.get("http_status") == 200 for record in records),
        "completed": len(valid),
        "terminal_completed": sum(status == "COMPLETED" for status in terminal_statuses),
        "failed": sum(status == "FAILED" for status in terminal_statuses),
        "cancelled": sum(status == "CANCELLED" for status in terminal_statuses),
        "collector_error": sum(record.get("failure_class") == "collector_error" for record in records),
        "timeout_count": sum(record.get("failure_class") == "timeout" for record in records),
        "agent_429_count": sum(record.get("http_status") == 429 for record in records),
        "five_xx": sum(isinstance(record.get("http_status"), int) and 500 <= record["http_status"] < 600 for record in records),
        "tool_failure_event_count": sum(
            sum(
                event.get("event_type") == "tool_failed"
                for event in (
                    ((record.get("audit_observation") or {}).get("events") or [])
                    if record.get("transport") == "rest"
                    else (record.get("sse_events") or [])
                )
            )
            for record in records
        ),
        "audit_inconsistent_count": sum(not transport_audit_consistent(record) for record in records),
        "sse_audit_inconsistent_count": sum(
            record.get("transport") != "rest" and not transport_audit_consistent(record)
            for record in records
        ),
        "rest_audit_inconsistent_count": sum(
            record.get("transport") == "rest" and not transport_audit_consistent(record)
            for record in records
        ),
        "identity_mixing_count": sum(
            (record.get("stream_validation") or {}).get("identity_mixed_within_stream", False)
            for record in records
        ),
        "p50_ms": percentile(durations, 50),
        "p95_ms": percentile(durations, 95),
        "p99_ms": percentile(durations, 99),
        "max_ms": max(durations) if durations else None,
        "first_event_p50_ms": percentile([record["first_event_ms"] for record in valid if record.get("first_event_ms") is not None], 50),
        "first_tool_p50_ms": percentile([record["first_tool_ms"] for record in valid if record.get("first_tool_ms") is not None], 50),
        "first_answer_p50_ms": percentile([record["first_answer_ms"] for record in valid if record.get("first_answer_ms") is not None], 50),
        "tool_names": sorted({name for record in records for name in record.get("tool_names", [])}),
        "response_bytes_total": sum(record.get("response_bytes") or 0 for record in records),
        "cleanup_success": sum(isinstance(record.get("cleanup_status"), int) and 200 <= record["cleanup_status"] < 300 for record in records),
        "warmup": warmup,
        "token_usage": "unavailable",
        "token_per_second": "unavailable",
        "provider_request_id": "unavailable",
        "provider_window_signal": "not_request_level_observed_by_agent_collector",
        "redaction_status": "redacted_no_payloads",
    }


def scenario_main(args: argparse.Namespace) -> int:
    if not args.phone:
        print("AGENT_TEST_PHONE is required", file=sys.stderr)
        return 2
    try:
        levels = tuple(int(value.strip()) for value in args.levels.split(",") if value.strip())
    except ValueError:
        print("--levels must be comma-separated integers", file=sys.stderr)
        return 2
    if len(levels) != 1 or levels[0] <= 0:
        print("scenario mode requires exactly one positive --levels value", file=sys.stderr)
        return 2
    if args.formal_count <= 0 or args.warmup_count < 0:
        print("--formal-count must be positive and --warmup-count must be non-negative", file=sys.stderr)
        return 2
    metadata: dict[str, Any] = {}
    if args.stop_on_anomaly:
        if getattr(args, "initial_provider_error_count", 0) not in (0, 1):
            print("initial_provider_error_count_must_be_zero_or_one", file=sys.stderr)
            return 2
        if (
            args.scenario != "AG-P-001"
            or levels != (1,) or args.turns != 1 or args.shared_conversation
            or args.cancel_after_event is not None or args.disconnect_after_event is not None
            or args.cancel_after_completion
        ):
            print("--stop-on-anomaly requires AG-P-001 single-turn requests at concurrency 1", file=sys.stderr)
            return 2
        try:
            supplied = json.loads(os.environ.get("AGENT_WAVE_METADATA", "{}"))
            metadata = {key: supplied[key] for key in (
                "model", "provider_base", "wire_api", "image_tag", "account_label",
                "owner_label", "store_label", "permission_summary", "runtime_observed_at",
                "api_started_at", "api_restart_count", "api_status", "postgres_status",
                "postgres_health", "redis_status", "public_healthz_http",
            ) if key in supplied}
        except (TypeError, json.JSONDecodeError):
            print("wave_metadata_invalid", file=sys.stderr)
            return 2
        if (
            metadata.get("model") != "glm-5.3-flash"
            or metadata.get("provider_base") != "https://oneapi.sxyq27.online/v1"
            or metadata.get("wire_api") != "chat_completions"
            or any(not metadata.get(key) for key in (
                "image_tag", "account_label", "owner_label", "store_label", "runtime_observed_at",
            ))
        ):
            print("wave_runtime_or_account_metadata_unconfirmed", file=sys.stderr)
            return 2
        metadata.update({"script_version": SERIAL_COLLECTOR_VERSION, "test_id": args.scenario})
    output = Path(args.output).resolve()
    if args.stop_on_anomaly:
        if not output.is_dir():
            print("existing_evidence_directory_required", file=sys.stderr)
            return 2
        existing_numbers = [int(path.name.split("-", 1)[0]) for path in output.iterdir()
                            if path.is_file() and path.name.split("-", 1)[0].isdigit()]
        if existing_numbers and args.start_number != max(existing_numbers) + 1:
            print("evidence_start_must_follow_existing_maximum", file=sys.stderr)
            return 2
    else:
        output.mkdir(parents=True, exist_ok=True)
    token = login(args.phone)
    if args.stop_on_anomaly:
        profile_status, _, profile_content = request_json("GET", "/v1/auth/users/me", token)
        profile = response_data(profile_content)
        if (
            profile_status != 200 or not isinstance(profile, dict)
            or profile.get("phone") != args.phone or profile.get("status") != 1
        ):
            print("authenticated_account_unconfirmed", file=sys.stderr)
            return 2
    level = levels[0]
    scenario = args.scenario
    prompt_list = args.prompt or ["请查询商品的基本信息，只读回答，不要创建或写入数据。"]
    run_label = args.run_label or scenario.lower().replace("_", "-")
    format_path = artifact_path(output, args.start_number, run_label, "collector-format-validation.json")
    before_path = artifact_path(output, args.start_number + 1, run_label, "db-before.json")
    warmup_path = artifact_path(output, args.start_number + 2, run_label, "warmup.jsonl")
    formal_path = artifact_path(output, args.start_number + 3, run_label, "formal.jsonl")
    failure_path = artifact_path(output, args.start_number + 4, run_label, "failure-analysis.json")
    resource_path = artifact_path(output, args.start_number + 5, run_label, "resource-samples.jsonl")
    summary_path = artifact_path(output, args.start_number + 6, run_label, "summary.json")
    after_path = artifact_path(output, args.start_number + 7, run_label, "db-after.json")
    before = db_summary(include_business=args.stop_on_anomaly)
    protected_conversation_ids = set(before.pop("_existing_conversation_ids", []))
    if args.stop_on_anomaly and (
        before.get("ssh_exit_code") != 0
        or not all(isinstance(before.get("counts", {}).get(table), int) for table in BUSINESS_TABLES)
        or before.get("counts", {}).get("active_runs") != len(before.get("active_run_ids", []))
        or before.get("counts", {}).get("agent_conversations") != len(protected_conversation_ids)
    ):
        print("initial_database_state_unavailable", file=sys.stderr)
        return 2
    metadata.update({"wave_id": run_label, "endpoint_kind": args.transport})
    write_json(format_path, {
        "mode": "phase2_scenario",
        "scenario": scenario,
        "transport": args.transport,
        "endpoint": REST_PATH if args.transport == "rest" else STREAM_PATH,
        "levels": [level],
        "formal_count": args.formal_count,
        "warmup_count": args.warmup_count,
        "turns": args.turns,
        "shared_conversation": args.shared_conversation,
        "cancel_after_event": args.cancel_after_event,
        "disconnect_after_event": args.disconnect_after_event,
        "per_request_flush": True,
        "fsync_after_each_record": True,
        "audit_lookup_before_cleanup": True,
        "stop_on_anomaly": args.stop_on_anomaly,
        "provider_error_streak_limit": 2 if args.stop_on_anomaly else None,
        "initial_provider_error_count": getattr(args, "initial_provider_error_count", 0),
        "initial_active_runs": before.get("counts", {}).get("active_runs"),
        "initial_active_run_ids": before.get("active_run_ids"),
        "protected_existing_conversation_count": len(protected_conversation_ids),
        "wave_metadata": metadata,
        "redaction_status": "redacted_no_payloads",
    })
    write_json(before_path, before)
    resources: list[dict[str, Any]] = []

    def sample_resources(stage: str) -> None:
        row = remote_snapshot(stage, level)
        resources.append(row)
        if args.stop_on_anomaly:
            with resource_path.open("a", encoding="utf-8") as resource_handle:
                append_jsonl(resource_handle, row)

    sample_resources("before_warmup")
    warmup: list[dict[str, Any]] = []
    records: list[dict[str, Any]] = []
    messages = [prompt_list[index % len(prompt_list)] for index in range(max(1, args.turns))]
    stop_reasons: list[str] = []
    error_streak = getattr(args, "initial_provider_error_count", 0) if args.stop_on_anomaly else 0
    last_database = before

    def finish_serial_record(item: dict[str, Any], sample_kind: str) -> None:
        nonlocal error_streak, last_database, stop_reasons
        item.update(metadata)
        item["sample_kind"] = sample_kind
        item["sample_id"] = f"{run_label}-{sample_kind}-{item['request_index']}"
        item["input_category"] = "short_no_tool"
        item["database_before_evidence"] = before_path.name
        item["identity_field_missing_counts"] = {
            field: sum(event.get(field) is None for event in item.get("sse_events") or [])
            for field in ("run_id", "conversation_id", "audit_id", "trace_id")
        }
        item["cleanup_identity_confirmed"] = serial_cleanup_allowed(item, protected_conversation_ids)
        if item["cleanup_identity_confirmed"]:
            cleanup(token, item)
        else:
            item["cleanup_status"] = "skipped_unconfirmed_or_existing_conversation"
        item["unavailable_metrics"] = [
            {"name": name, "reason": reason} for name, reason in (
                ("jvm_heap_used", "current resource probe exposes RSS, not JVM heap-used"),
                ("connection_pool_wait", "pool wait is not exposed by the current runtime probe"),
                ("sql_count_and_duration", "the current Agent response and audit omit per-request SQL metrics"),
                ("token_usage_and_rate", "the current Agent response and audit omit per-request token usage"),
                ("provider_request_id", "the current Agent response and audit omit the upstream request ID"),
            )
        ]
        http_status = item.get("http_status")
        provider_or_http_error = (
            item.get("failure_class") in {"provider_error", "timeout"}
            or item.get("terminal_error_code") in {
                "LLM_UNAVAILABLE", "LLM_PLANNING_FAILED", "LLM_ANSWER_UNAVAILABLE", "PROVIDER_ERROR",
            }
            or http_status == 429
            or (isinstance(http_status, int) and 500 <= http_status < 600)
        )
        error_streak = error_streak + 1 if provider_or_http_error else 0
        last_database = db_summary(include_business=True)
        last_database.pop("_existing_conversation_ids", None)
        item["database_after_cleanup"] = last_database
        stop_reasons = serial_stop_reasons(item, before, last_database, error_streak)
        item["stop_reasons"] = stop_reasons
        item["consecutive_provider_or_http_errors"] = error_streak

    def run_case(index: int, do_cleanup: bool = True, conversation_id: int | None = None) -> dict[str, Any]:
        if args.stop_on_anomaly:
            do_cleanup = False
        if args.turns > 1:
            return collect_multi_turn(level, index, token, messages)
        if args.transport == "rest":
            return collect_rest_request(
                level,
                index,
                token,
                messages[0],
                conversation_id=conversation_id,
                cleanup_after=do_cleanup,
            )
        return collect_stream_request(
            level,
            index,
            token,
            messages[0],
            conversation_id=conversation_id,
            cleanup_after=do_cleanup,
            cancel_after_event=args.cancel_after_event,
            disconnect_after_event=args.disconnect_after_event,
            cancel_after_completion=args.cancel_after_completion,
        )

    with warmup_path.open("w", encoding="utf-8") as handle:
        for index in range(args.warmup_count):
            item = run_case(-index - 1)
            if args.stop_on_anomaly:
                finish_serial_record(item, "warmup")
            warmup.append(item)
            append_jsonl(handle, item)
            if stop_reasons:
                break
    sample_resources("after_warmup")

    shared_conversation_id: int | None = None
    shared_cleanup_record: dict[str, Any] | None = None
    if args.shared_conversation:
        status, _, content = request_json(
            "POST",
            "/v2/agent/conversations",
            token,
            {"title": f"phase2-{scenario}"},
            timeout=30,
        )
        data = response_data(content)
        if status < 200 or status >= 300 or not isinstance(data, dict):
            raise RuntimeError(f"shared_conversation_create_http_{status}")
        shared_conversation_id = data.get("id")

    with formal_path.open("w", encoding="utf-8") as handle:
        if args.stop_on_anomaly:
            def capture_during_request() -> None:
                time.sleep(1)
                sample_resources("during_formal")

            for index in range(args.formal_count):
                if stop_reasons:
                    break
                sampler = None
                if index % 5 == 0:
                    sampler = threading.Thread(target=capture_during_request, daemon=True)
                    sampler.start()
                item = run_case(index)
                if sampler is not None:
                    sampler.join()
                finish_serial_record(item, "formal")
                records.append(item)
                append_jsonl(handle, item)
                print(json.dumps({
                    "sample": index + 1, "valid": scenario_valid(item),
                    "terminal_status": item.get("terminal_status"),
                    "failure_class": item.get("failure_class"),
                    "cleanup_status": item.get("cleanup_status"),
                    "active_runs": last_database.get("counts", {}).get("active_runs"),
                    "stop_reasons": stop_reasons,
                }), flush=True)
        else:
            with ThreadPoolExecutor(max_workers=level) as pool:
                futures = [
                    pool.submit(
                        run_case,
                        index,
                        not args.shared_conversation,
                        shared_conversation_id,
                    )
                    for index in range(args.formal_count)
                ]
                time.sleep(1)
                sample_resources("during_formal")
                for future in as_completed(futures):
                    item = future.result()
                    records.append(item)
                    append_jsonl(handle, item)
    if shared_conversation_id is not None:
        shared_cleanup_record = {"conversation_id": shared_conversation_id, "cleanup_status": None}
        cleanup(token, shared_cleanup_record)
    sample_resources("after_formal")
    sample_resources("after_cleanup")
    final_database = db_summary(include_business=args.stop_on_anomaly)
    final_database.pop("_existing_conversation_ids", None)
    if args.stop_on_anomaly and (records or warmup):
        final_item = records[-1] if records else warmup[-1]
        stop_reasons = list(dict.fromkeys(
            stop_reasons + serial_stop_reasons(final_item, before, final_database, error_streak)
        ))
    failures = [
        {
            key: record.get(key)
            for key in (
                "concurrency_level", "request_index", "run_id", "conversation_id", "audit_id", "trace_id",
                "terminal_status", "terminal_error_code", "failure_class", "error_categories", "collector_error",
                "cancel_request_status", "cancel_request_start_time", "cancel_request_time", "cancel_rtt_ms",
                "disconnect_audit_wait_ms",
                "intentional_disconnect", "stream_validation", "audit_observation",
                "cleanup_status", "cleanup_identity_confirmed", "stop_reasons", "redaction_status",
            )
        }
        for record in records
        if not scenario_valid(record)
    ]
    write_json(failure_path, {
        "scenario": scenario,
        "failure_count": len(failures),
        "failures": failures,
        "shared_cleanup": shared_cleanup_record,
        "stop_reasons": stop_reasons,
        "warmup_failure_count": sum(not scenario_valid(item) for item in warmup),
        "warmup_failures": [item for item in warmup if not scenario_valid(item)],
        "redaction_status": "redacted_no_payloads",
    })
    if not args.stop_on_anomaly:
        with resource_path.open("w", encoding="utf-8") as handle:
            for row in resources:
                append_jsonl(handle, row)
    level_summary = scenario_summary(level, records, warmup, scenario)
    if shared_cleanup_record is not None:
        cleanup_status = shared_cleanup_record.get("cleanup_status")
        level_summary["cleanup_success"] = int(
            isinstance(cleanup_status, int) and 200 <= cleanup_status < 300
        )
        level_summary["cleanup_target_count"] = 1
        level_summary["per_request_cleanup_deferred"] = True
        level_summary["shared_cleanup_status"] = cleanup_status
    write_json(summary_path, {
        "scenario": scenario,
        "levels_requested": [level],
        "levels_executed": [level],
        "levels": [level_summary],
        "stopped_early": bool(stop_reasons),
        "stop_reasons": stop_reasons,
        "formal_count_planned": args.formal_count,
        "formal_count_executed": len(records),
        "wave_metadata": metadata,
        "redaction_status": "redacted_no_payloads",
    })
    write_json(after_path, final_database)
    if args.stop_on_anomaly:
        print(json.dumps({
            "scenario": scenario, "formal_count": len(records), "valid": level_summary["completed"],
            "stop_reasons": stop_reasons, "summary_evidence": str(summary_path),
        }), flush=True)
    else:
        print(json.dumps({"scenario": scenario, "summary": level_summary, "output": str(output)}, ensure_ascii=True))
    return 3 if stop_reasons else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--phone", default=os.environ.get("AGENT_TEST_PHONE"))
    parser.add_argument("--levels", default="1,2,4,8", help="comma-separated concurrency levels")
    parser.add_argument("--formal-count", type=int, default=20)
    parser.add_argument("--warmup-count", type=int, default=2)
    parser.add_argument("--start-number", type=int, default=30)
    parser.add_argument("--run-label", default="", help="prefix for an appended evidence set")
    parser.add_argument("--scenario", default="", help="phase2 scenario mode")
    parser.add_argument("--transport", choices=("stream", "rest"), default="stream")
    parser.add_argument("--prompt", action="append", help="scenario prompt; may be repeated")
    parser.add_argument("--turns", type=int, default=1)
    parser.add_argument("--shared-conversation", action="store_true")
    parser.add_argument("--cancel-after-event", type=int)
    parser.add_argument("--disconnect-after-event", type=int)
    parser.add_argument("--cancel-after-completion", action="store_true")
    parser.add_argument("--stop-on-anomaly", action="store_true")
    parser.add_argument("--initial-provider-error-count", type=int, default=0)
    args = parser.parse_args()
    if args.scenario:
        if args.turns <= 0:
            print("--turns must be positive", file=sys.stderr)
            return 2
        if args.cancel_after_event is not None and args.cancel_after_event <= 0:
            print("--cancel-after-event must be positive", file=sys.stderr)
            return 2
        if args.disconnect_after_event is not None and args.disconnect_after_event <= 0:
            print("--disconnect-after-event must be positive", file=sys.stderr)
            return 2
        if args.transport == "rest" and (
            args.turns != 1
            or args.shared_conversation
            or args.cancel_after_event is not None
            or args.disconnect_after_event is not None
            or args.cancel_after_completion
        ):
            print("REST scenario mode supports only independent single-turn requests", file=sys.stderr)
            return 2
        return scenario_main(args)
    if args.transport != "stream":
        print("--transport rest requires --scenario", file=sys.stderr)
        return 2
    if not args.phone:
        print("AGENT_TEST_PHONE is required", file=sys.stderr)
        return 2
    try:
        levels = tuple(int(value.strip()) for value in args.levels.split(",") if value.strip())
    except ValueError:
        print("--levels must be comma-separated integers", file=sys.stderr)
        return 2
    if not levels or any(level <= 0 for level in levels):
        print("--levels must contain positive integers", file=sys.stderr)
        return 2
    if args.formal_count <= 0 or args.warmup_count <= 0:
        print("--formal-count and --warmup-count must be positive", file=sys.stderr)
        return 2
    output = Path(args.output).resolve()
    # Reuse the single caller-provided scratch directory; each named output is
    # replaced in place so interrupted attempts do not create duplicate waves.
    output.mkdir(parents=True, exist_ok=True)
    token = login(args.phone)
    format_path = artifact_path(output, args.start_number, args.run_label, "collector-format-validation.json")
    before_number = args.start_number if not args.run_label else args.start_number + 1
    warmup_path = artifact_path(output, args.start_number + 2, args.run_label, "warmup.jsonl") if args.run_label else None
    formal_start_number = args.start_number + 1 if not args.run_label else args.start_number + 3
    failure_number = formal_start_number + len(levels)
    resource_number = failure_number + 1
    summary_number = failure_number + 2
    after_number = failure_number + 3
    write_json(format_path, {
        "required_fields": list(new_record(1, 0, time.time()).keys()),
        "per_request_flush": True,
        "fsync_after_each_record": True,
        "audit_lookup_before_cleanup": True,
        "timeout_recording": True,
        "http_success_distinct_from_agent_completion": True,
        "levels": list(levels),
        "formal_count_per_level": args.formal_count,
        "warmup_count_per_level": args.warmup_count,
        "run_label": args.run_label or None,
        "redaction_status": "redacted_no_payloads",
    })
    write_json(artifact_path(output, before_number, args.run_label, "db-before.json"), db_summary())
    resource_rows: list[dict[str, Any]] = []
    summaries: list[dict[str, Any]] = []
    failures: list[dict[str, Any]] = []
    warmup_handle = warmup_path.open("w", encoding="utf-8") if warmup_path else None
    try:
      for position, level in enumerate(levels):
        resource_rows.append(remote_snapshot("before_warmup", level))
        with ThreadPoolExecutor(max_workers=level) as pool:
            warmup = [future.result() for future in as_completed([pool.submit(collect_one, level, -i - 1, token) for i in range(args.warmup_count)])]
        if warmup_handle:
            for record in warmup:
                append_jsonl(warmup_handle, record)
        resource_rows.append(remote_snapshot("after_warmup", level))
        records: list[dict[str, Any]] = []
        if args.run_label:
            evidence_file = artifact_path(output, formal_start_number + position, args.run_label, f"concurrency-{level}.jsonl")
        else:
            evidence_file = artifact_path(output, formal_start_number + position, args.run_label, f"concurrency-{level}-rerun.jsonl")
        with evidence_file.open("w", encoding="utf-8") as handle:
            with ThreadPoolExecutor(max_workers=level) as pool:
                futures = [pool.submit(collect_one, level, i, token) for i in range(args.formal_count)]
                time.sleep(1)
                resource_rows.append(remote_snapshot("during_formal", level))
                for future in as_completed(futures):
                    record = future.result()
                    records.append(record)
                    append_jsonl(handle, record)
                    if record.get("failure_class") or str(record.get("terminal_status") or "").upper() != "COMPLETED":
                        failures.append({
                            key: record.get(key) for key in (
                                "concurrency_level", "request_index", "run_id", "conversation_id", "audit_id", "trace_id",
                                "event_id", "seq", "event_types", "event_count", "tool_names", "terminal_status",
                                "terminal_error_code", "failure_class", "error_categories", "safe_message_digest", "last_event", "audit_observation",
                                "sse_events", "stream_validation", "cleanup_id", "cleanup_status", "redaction_status",
                            )
                        })
        resource_rows.append(remote_snapshot("after_formal", level))
        summaries.append(summary(level, records, warmup))
        resource_rows.append(remote_snapshot("after_cleanup", level))
        if any(
            record.get("failure_class")
            or str(record.get("terminal_status") or "").upper() != "COMPLETED"
            for record in records
        ):
            break
    finally:
      if warmup_handle:
        warmup_handle.close()
    write_json(artifact_path(output, failure_number, args.run_label, "failure-analysis.json"), {
        "failure_count": len(failures),
        "terminal_failure_count": sum(
            str(record.get("terminal_status") or "").upper() != "COMPLETED" for record in failures
        ),
        "degraded_completion_count": sum(
            str(record.get("terminal_status") or "").upper() == "COMPLETED" for record in failures
        ),
        "tool_failure_event_count": sum(
            sum(event.get("event_type") == "tool_failed" for event in (record.get("sse_events") or []))
            for record in failures
        ),
        "levels": list(levels),
        "formal_count_per_level": args.formal_count,
        "failures": failures,
        "classification_rule": "Observed terminal and intermediate error codes are classified; a completed run with tool_failed remains a degraded completion",
        "redaction_status": "redacted_no_payloads",
    })
    with artifact_path(output, resource_number, args.run_label, "resource-samples.jsonl").open("w", encoding="utf-8") as handle:
        for row in resource_rows:
            append_jsonl(handle, row)
    write_json(artifact_path(output, summary_number, args.run_label, "summary.json"), {
        "run_label": args.run_label or None,
        "levels_requested": list(levels),
        "levels_executed": [item["concurrency_level"] for item in summaries],
        "formal_count_per_level": args.formal_count,
        "warmup_count_per_level": args.warmup_count,
        "levels": summaries,
        "stop_after_level": summaries[-1]["concurrency_level"] if summaries else None,
        "stopped_early": len(summaries) < len(levels),
        "concurrency_16": "not_run_by_request",
        "redaction_status": "redacted_no_payloads",
    })
    write_json(artifact_path(output, after_number, args.run_label, "db-after.json"), db_summary())
    print(json.dumps({"levels": summaries, "output": str(output)}, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
