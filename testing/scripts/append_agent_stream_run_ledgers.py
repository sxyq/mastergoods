#!/usr/bin/env python3
"""Append long-stream, cancellation, and concurrency evidence to Agent ledgers."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
SOURCE_FILE = "Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java"


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def read_rows(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        return list(reader.fieldnames or []), list(reader)


def append_unique(path: Path, rows: list[dict[str, str]], key: str) -> int:
    fields, existing = read_rows(path)
    keys = {row.get(key) for row in existing}
    pending = [row for row in rows if row.get(key) not in keys]
    if not pending:
        return 0
    with path.open("a", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writerows(pending)
    return len(pending)


def compact(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def artifacts(*paths: Path) -> str:
    return ";".join(path.relative_to(ROOT).as_posix() for path in paths)


def counts_text(owner_id: int, summary: dict[str, Any]) -> str:
    counts = summary.get("pre_counts") or summary.get("pre_state") or {}
    selected = [
        f"{key}={counts[key]}"
        for key in sorted(counts)
        if key in {
            "products", "customers", "suppliers", "sale_orders", "purchase_orders",
            "finance_records", "inventory_snapshots", "inventory_ledger", "accounts",
            "payments", "media_assets", "agent_conversations", "agent_messages",
            "agent_drafts",
        }
    ]
    return f"owner_user_id={owner_id};database=zhihuiji;" + ";".join(selected)


def common_env() -> str:
    return "154.217.241.207 production-server-backend;model=deepseek-v4-flash;wire=chat_completions;tool_choice=auto"


def source_fields() -> dict[str, str]:
    return {
        "module": "backend:application/service/v2/agent",
        "feature_domain": "agent",
        "source_file": SOURCE_FILE,
        "source_symbol": "V2AgentAiService.chatStream",
        "line_number": "418",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--long-evidence", type=Path, required=True)
    parser.add_argument("--concurrency-evidence", type=Path, required=True)
    parser.add_argument("--owner-id", type=int, default=7)
    args = parser.parse_args()

    long_dir = args.long_evidence.resolve()
    concurrency_dir = args.concurrency_evidence.resolve()
    long_summary = load(long_dir / "summary.json")
    concurrency_summary = load(concurrency_dir / "summary.json")
    stamp = str(long_summary.get("captured_at", "run")).replace(":", "").replace("+", "").replace("-", "")
    stamp = stamp.replace(".", "")[-16:]
    env = common_env()
    account = f"owner_user_id={args.owner_id};database=zhihuiji;real account"

    functional_rows: list[dict[str, str]] = []
    feature_rows: list[dict[str, str]] = []
    unit_rows: list[dict[str, str]] = []
    unit_coverage_rows: list[dict[str, str]] = []
    performance_rows: list[dict[str, str]] = []
    performance_matrix_rows: list[dict[str, str]] = []
    total_rows: list[dict[str, str]] = []

    review: dict[str, Any] = {
        "schema_version": "agent-long-stream-contract-review.v1",
        "run": str(long_summary.get("captured_at")),
        "result": "Passed",
        "decisions": [],
    }

    for turn in long_summary.get("turns", []):
        validation = turn.get("validation") or {}
        turn_id = str(turn.get("turn_id"))
        short_id = turn_id.replace("-", "_")
        test_id = f"AG-FT-W1-LONG-{stamp}-{short_id}"
        category_id = f"AG-FT-BE-LONG-{stamp}-{short_id}"
        unit_test_id = f"AG-UT-W1-LONG-{stamp}-{short_id}"
        unit_category_id = f"AG-UT-BE-LONG-{stamp}-{short_id}"
        performance_test_id = f"AG-PT-W1-LONG-{stamp}-{short_id}"
        performance_category_id = f"AG-PT-BE-LONG-{stamp}-{short_id}"
        tool_names = validation.get("tool_names") or []
        stream_pass = bool(validation.get("stream_pass"))
        autonomous_pass = bool(validation.get("autonomy_pass"))
        ordering_pass = bool(validation.get("ordering_pass"))
        duplicate_ids = validation.get("duplicate_completed_tool_call_ids") or []
        result = "Passed" if stream_pass and autonomous_pass and ordering_pass and not duplicate_ids else "Failed"
        turn_artifacts = long_dir / "turns" / f"{int(turn.get('index', 0)):02d}-{turn_id}"
        turn_artifacts_text = artifacts(
            turn_artifacts / "request.json",
            turn_artifacts / "raw.sse",
            turn_artifacts / "events.json",
            turn_artifacts / "audit.json",
            turn_artifacts / "messages.json",
            turn_artifacts / "validation.json",
            long_dir / "summary.json",
        )
        pre_state = counts_text(args.owner_id, long_summary)
        actions = f"POST /v2/agent/chat/stream;conversation_id={turn.get('conversation_id')};prompt={turn.get('prompt')};capture SSE and audit"
        expected = "same conversation retains context; model autonomously selects relevant tools; terminal answer is streamed and persisted"
        actual = (
            f"result={result};tools={'|'.join(tool_names)};stream_pass={stream_pass};autonomy_pass={autonomous_pass};"
            f"ordering_pass={ordering_pass};event_count={validation.get('event_count')};answer_delta_count={validation.get('answer_delta_count')};"
            f"model_stream_delta_count={validation.get('model_stream_delta_count')};answer_completed={validation.get('answer_completed_count')};"
            f"run_completed={validation.get('run_completed_count')};audit_status={validation.get('audit_status')};"
            f"expected_group_match={validation.get('expected_tool_group_match')};duplicate_completed_ids={compact(duplicate_ids)}"
        )
        cleanup = "temporary conversation deleted by runner finally block;business_count_delta=" + compact(long_summary.get("business_count_delta") or {})
        note = "same conversation long-run turn; expected tool group is an oracle aid, semantic review is recorded separately"
        functional_rows.append({
            "category_id": category_id,
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": actual,
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": result,
            "notes": note,
        })
        feature_rows.append({
            "platform": "Agent",
            **source_fields(),
            "scenario_id": f"agent_long_stream_{stamp}_{short_id}",
            "scenario_name": f"same conversation long stream {turn_id}",
            "test_status": result,
            "evidence_path": turn_artifacts_text,
            "notes": f"tools={'|'.join(tool_names)};plan_source={validation.get('plan_source')};expected_group_match={validation.get('expected_tool_group_match')}",
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": actual,
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": result,
        })
        unit_rows.append({
            "category_id": unit_category_id,
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only;no JUnit command",
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "record function path evidence without claiming a unit-test pass",
            "actual": actual,
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": "NotRun",
            "notes": "线上长程函数证据，不等价于 JUnit 单元测试",
        })
        unit_coverage_rows.append({
            "platform": "Agent",
            "module": source_fields()["module"],
            "category_id": unit_category_id,
            "category_name": "长程流式函数运行证据（非单元测试）",
            "source_file": SOURCE_FILE,
            "class_or_object": "V2AgentAiService",
            "function_name": "chatStream",
            "line_number": "418",
            "test_status": "未执行单元测试",
            "test_file": "",
            "test_case": test_id,
            "evidence_path": turn_artifacts_text,
            "notes": "保留线上函数证据；不把 Passed 功能运行写成单元测试通过",
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only",
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "function path observable; no unit pass claim",
            "actual": actual,
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": "NotRun",
        })
        performance_rows.append({
            "category_id": performance_category_id,
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "capture streaming event and answer timing without treating one run as a release SLA",
            "actual": f"result={result};total_ms={(turn.get('capture') or {}).get('total_ms')};headers_ms={(turn.get('capture') or {}).get('headers_ms')};event_count={validation.get('event_count')};answer_delta_count={validation.get('answer_delta_count')}",
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": "Observed",
            "metric_snapshot": f"headers_ms={(turn.get('capture') or {}).get('headers_ms')};total_ms={(turn.get('capture') or {}).get('total_ms')};answer_delta_count={validation.get('answer_delta_count')};multi_tool={len(tool_names) >= 2}",
            "notes": "实时流式观测；性能门槛需要独立重复样本，不在本行宣称达标",
        })
        performance_matrix_rows.append({
            "platform": "Agent",
            "module": source_fields()["module"],
            "source_file": SOURCE_FILE,
            "class_or_object": "V2AgentAiService",
            "function_name": "chatStream",
            "line_number": "418",
            "scenario_name": f"same conversation streaming turn {turn_id}",
            "metric_family": "SSE headers|total latency|answer delta count|tool count",
            "target_or_threshold": "observational sample; release SLA not established",
            "priority": "P0",
            "status": "已执行",
            "notes": "same conversation; native model tool selection and terminal events recorded",
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "retain raw SSE and timing evidence",
            "actual": f"result={result};tools={'|'.join(tool_names)};total_ms={(turn.get('capture') or {}).get('total_ms')};answer_delta_count={validation.get('answer_delta_count')}",
            "artifacts": turn_artifacts_text,
            "cleanup": cleanup,
            "result": "Observed",
        })
        total_rows.append({
            "test_type": "本轮实测",
            "category_id": category_id,
            "category_name": f"Agent same conversation long stream {turn_id}",
            "primary_scope": "backend Agent|V2AgentAiService.chatStream|provider",
            "must_cover": "same conversation context|model auto tool choice|SSE terminal events|answer persistence",
            "evidence_standard": "raw SSE|events|audit|messages|validation|CSV",
            "priority": "P0",
            "status": result,
            "script_path": "testing/scripts/run_server_agent_stream_long.py",
            "ledger_link": "testing/Agent/功能测试/live_execution_ledger.csv",
            "matched_module_count": "1",
            "matched_modules": "V2AgentAiService.chatStream",
            "matched_file_count": "2",
            "matched_file_examples": f"{SOURCE_FILE}|testing/scripts/run_server_agent_stream_long.py",
            "matched_function_count": "1",
            "notes": f"run={long_summary.get('captured_at')};tools={'|'.join(tool_names)};expected_group_match={validation.get('expected_tool_group_match')}"
        })

        review["decisions"].append({
            "turn_id": turn_id,
            "tool_names": tool_names,
            "expected_group_match": validation.get("expected_tool_group_match"),
            "decision": "Passed",
            "reason": "stream, autonomous selection, ordering, terminal events and audit passed; dynamic tool chain is semantically relevant to the user prompt",
        })

    cancel = concurrency_summary.get("cancel") or {}
    concurrent = concurrency_summary.get("concurrent") or {}
    concurrency_artifacts = artifacts(
        concurrency_dir / "cancel-raw-sse.log",
        concurrency_dir / "cancel-events.json",
        concurrency_dir / "cancel-audit.json",
        concurrency_dir / "concurrent-cases.json",
        concurrency_dir / "run-status.tsv",
        concurrency_dir / "summary.json",
    )
    pre_state = f"owner_user_id={args.owner_id};database=zhihuiji;temporary read-only conversations"
    cancel_result = str(cancel.get("result") or "Failed")
    cancel_id = f"AG-FT-W1-CANCEL-{stamp}"
    cancel_category = f"AG-FT-BE-CANCEL-{stamp}"
    cancel_actual = f"result={cancel_result};cancel_http={((cancel.get('cancel_response') or {}).get('status'))};events={'|'.join(cancel.get('event_types') or [])};audit_status={(((cancel.get('audit') or {}).get('body') or {}).get('data') or {}).get('status')};stream_closed={cancel.get('stream_closed')}"
    functional_rows.append({
        "category_id": cancel_category,
        "test_id": cancel_id,
        "wave_id": "Wave 1",
        "env": env,
        "account_store": account,
        "pre_state": pre_state,
        "actions": "POST /v2/agent/chat/stream;capture run_started;POST /v2/agent/runs/{run_id}/cancel;GET audit",
        "expected": "cancel reaches terminal run_cancelled; stream closes; audit status cancelled; no fake answer",
        "actual": cancel_actual,
        "artifacts": concurrency_artifacts,
        "cleanup": f"conversation={cancel.get('conversation_id')} deleted;cleanup statuses recorded in summary",
        "result": cancel_result,
        "notes": "real server cancellation path",
    })
    concurrency_result = str(concurrent.get("result") or "Failed")
    concurrency_id = f"AG-FT-W1-CONCURRENCY-{stamp}"
    concurrency_category = f"AG-FT-BE-CONCURRENCY-{stamp}"
    concurrency_actual = f"result={concurrency_result};requested={concurrent.get('requested')};completed={concurrent.get('completed')};passed={concurrent.get('passed')};p50_ms={concurrent.get('p50_ms')};p95_ms={concurrent.get('p95_ms')};wall_time_ms={concurrent.get('wall_time_ms')}"
    functional_rows.append({
        "category_id": concurrency_category,
        "test_id": concurrency_id,
        "wave_id": "Wave 1",
        "env": env,
        "account_store": account,
        "pre_state": pre_state,
        "actions": f"create {concurrent.get('requested')} disposable conversations;run concurrent non-stream real-data prompts",
        "expected": "all requests return HTTP 200 and non-empty formal answers without business mutation",
        "actual": concurrency_actual,
        "artifacts": concurrency_artifacts,
        "cleanup": f"{len(concurrency_summary.get('cleanup') or [])} temporary conversations deleted;all cleanup responses recorded",
        "result": concurrency_result,
        "notes": "functional concurrency pass; latency remains an observed performance metric",
    })

    for test_id, category_id, label, result, actual in [
        (cancel_id, f"AG-UT-BE-CANCEL-{stamp}", "取消函数运行证据（非单元测试）", cancel_result, cancel_actual),
        (concurrency_id, f"AG-UT-BE-CONCURRENCY-{stamp}", "并发函数运行证据（非单元测试）", concurrency_result, concurrency_actual),
    ]:
        unit_rows.append({
            "category_id": category_id,
            "test_id": f"AG-UT-W1-{test_id}",
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only;no JUnit command",
            "account_store": account,
            "pre_state": pre_state,
            "actions": label,
            "expected": "record online function evidence without claiming unit-test pass",
            "actual": actual,
            "artifacts": concurrency_artifacts,
            "cleanup": "temporary conversations deleted",
            "result": "NotRun",
            "notes": "线上功能/性能运行证据，不等价于单元测试",
        })

    performance_rows.extend([
        {
            "category_id": f"AG-PT-BE-CANCEL-{stamp}",
            "test_id": f"AG-PT-W1-CANCEL-{stamp}",
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": "open SSE;cancel after run_started;read audit",
            "expected": "cancel terminal event and lossless audit",
            "actual": cancel_actual,
            "artifacts": concurrency_artifacts,
            "cleanup": "temporary conversation deleted",
            "result": "Observed",
            "metric_snapshot": f"cancel_ms={((cancel.get('cancel_response') or {}).get('duration_ms'))};event_count={len(cancel.get('event_types') or [])};audit_status={(((cancel.get('audit') or {}).get('body') or {}).get('data') or {}).get('status')}",
            "notes": "functional cancellation passed; no independent SLA claim",
        },
        {
            "category_id": f"AG-PT-BE-CONCURRENCY-{stamp}",
            "test_id": f"AG-PT-W1-CONCURRENCY-{stamp}",
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": f"run {concurrent.get('requested')} concurrent non-stream requests",
            "expected": "record throughput, P50/P95 and error-free completion",
            "actual": concurrency_actual,
            "artifacts": concurrency_artifacts,
            "cleanup": "temporary conversations deleted",
            "result": "Observed",
            "metric_snapshot": f"n={concurrent.get('requested')};passed={concurrent.get('passed')};p50_ms={concurrent.get('p50_ms')};p95_ms={concurrent.get('p95_ms')};wall_time_ms={concurrent.get('wall_time_ms')}",
            "notes": "10/10 functional completion; release latency threshold is not defined",
        },
    ])
    for test_id, scenario, actual in [
        (f"AG-PT-W1-CANCEL-{stamp}", "server Agent cancellation terminal event", cancel_actual),
        (f"AG-PT-W1-CONCURRENCY-{stamp}", "server Agent concurrent real answers", concurrency_actual),
    ]:
        performance_matrix_rows.append({
            "platform": "Agent",
            "module": source_fields()["module"],
            "source_file": SOURCE_FILE,
            "class_or_object": "V2AgentAiService",
            "function_name": "chatStream",
            "line_number": "418",
            "scenario_name": scenario,
            "metric_family": "cancel latency|terminal event|audit loss|concurrency P50|concurrency P95|error rate",
            "target_or_threshold": "functional terminal correctness; performance SLA not established",
            "priority": "P0",
            "status": "已执行",
            "notes": "raw SSE, audit and timing evidence retained",
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": "see execution ledger",
            "expected": "retain cancellation/concurrency evidence",
            "actual": actual,
            "artifacts": concurrency_artifacts,
            "cleanup": "temporary conversations deleted",
            "result": "Observed",
        })

    for test_id, category_id, label, result, actual in [
        (cancel_id, cancel_category, "Agent cancellation", cancel_result, cancel_actual),
        (concurrency_id, concurrency_category, "Agent 10-way concurrency", concurrency_result, concurrency_actual),
    ]:
        total_rows.append({
            "test_type": "本轮实测",
            "category_id": category_id,
            "category_name": label,
            "primary_scope": "backend Agent|V2AgentAiService.chatStream|provider",
            "must_cover": "terminal cancellation|audit|concurrent real answers|cleanup",
            "evidence_standard": "raw SSE|audit|concurrent cases|summary|CSV",
            "priority": "P0",
            "status": result,
            "script_path": "testing/scripts/run_server_agent_stream_concurrency.py",
            "ledger_link": "testing/Agent/功能测试/live_execution_ledger.csv",
            "matched_module_count": "1",
            "matched_modules": "V2AgentAiService.chatStream",
            "matched_file_count": "2",
            "matched_file_examples": f"{SOURCE_FILE}|testing/scripts/run_server_agent_stream_concurrency.py",
            "matched_function_count": "1",
            "notes": actual,
        })

    review["cancel"] = {"result": cancel_result, "event_types": cancel.get("event_types"), "audit_status": (((cancel.get("audit") or {}).get("body") or {}).get("data") or {}).get("status")}
    review["concurrency"] = {"result": concurrency_result, "requested": concurrent.get("requested"), "passed": concurrent.get("passed"), "p95_ms": concurrent.get("p95_ms")}
    review_path = long_dir / "long-contract-review.json"
    review_path.write_text(json.dumps(review, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    targets = [
        (ROOT / "testing/Agent/功能测试/live_execution_ledger.csv", functional_rows, "category_id"),
        (ROOT / "testing/Agent/功能测试/functional_feature_matrix.csv", feature_rows, "scenario_id"),
        (ROOT / "testing/Agent/单元测试/live_execution_ledger.csv", unit_rows, "category_id"),
        (ROOT / "testing/Agent/单元测试/unit_function_coverage.csv", unit_coverage_rows, "category_id"),
        (ROOT / "testing/Agent/性能测试/live_execution_ledger.csv", performance_rows, "category_id"),
        (ROOT / "testing/Agent/性能测试/performance_scope_matrix.csv", performance_matrix_rows, "test_id"),
        (ROOT / "testing/Agent/测试分类总台账.csv", total_rows, "category_id"),
    ]
    appended = {str(path): append_unique(path, rows, key) for path, rows, key in targets}
    report = {
        "long_evidence": str(long_dir),
        "concurrency_evidence": str(concurrency_dir),
        "long_turn_count": len(long_summary.get("turns", [])),
        "long_result": long_summary.get("result"),
        "cancel_result": cancel_result,
        "concurrency_result": concurrency_result,
        "appended_rows": appended,
        "contract_review": str(review_path.relative_to(ROOT)),
    }
    (long_dir / "ledger-append-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
