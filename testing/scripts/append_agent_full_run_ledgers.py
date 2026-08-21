#!/usr/bin/env python3
"""Append one real server Agent run to all Agent ledgers.

The source run is immutable. This script adds run-scoped rows and keeps older
rows with the same test_id untouched, because test_id identifies the scenario
while category_id identifies a concrete execution.
"""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def read_csv(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        return list(reader.fieldnames or []), list(reader)


def compact(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def join_tools(case: dict[str, Any]) -> str:
    actual = case.get("actual") or {}
    trace = case.get("model_and_tool_trace") or {}
    names = actual.get("tool_names") or [
        item.get("tool_name")
        for item in trace.get("tool_calls") or []
        if item.get("tool_name")
    ]
    return "|".join(str(name) for name in names if name)


def answer_present(case: dict[str, Any]) -> bool:
    response = case.get("response") or {}
    data = response.get("data") if isinstance(response, dict) else {}
    return bool(isinstance(data, dict) and str(data.get("answer") or "").strip())


def response_data(case: dict[str, Any]) -> dict[str, Any]:
    response = case.get("response") or {}
    data = response.get("data") if isinstance(response, dict) else {}
    return data if isinstance(data, dict) else {}


def pre_state_text(case: dict[str, Any], owner_id: int) -> str:
    state = case.get("pre_state") or {}
    selected = [
        f"{key}={state[key]}"
        for key in sorted(state)
        if key in {"products", "customers", "suppliers", "sale_orders", "purchase_orders", "finance_records", "inventory_snapshots", "inventory_ledger", "accounts", "payments", "agent_drafts", "agent_conversations"}
    ]
    return f"owner_user_id={owner_id};database=zhihuiji;" + ";".join(selected)


def artifact_text(run_dir: Path, case_path: Path) -> str:
    relative_case = case_path.relative_to(run_dir).as_posix()
    base = run_dir.relative_to(ROOT).as_posix()
    return ";".join([
        f"{base}/{relative_case}",
        f"{base}/summary.json",
        f"{base}/case-status.tsv",
        f"{base}/provider.log",
        f"{base}/runner.log",
    ])


def source_metadata(feature_rows: list[dict[str, str]], test_id: str) -> dict[str, str]:
    matching = [row for row in feature_rows if row.get("test_id") == test_id]
    if matching:
        row = matching[-1]
        return {
            "module": row.get("module") or "backend:application/service/v2",
            "feature_domain": row.get("feature_domain") or "agent",
            "source_file": row.get("source_file") or "Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java",
            "source_symbol": row.get("source_symbol") or "V2AgentAiService.chat",
            "line_number": row.get("line_number") or "272",
        }
    return {
        "module": "backend:application/service/v2",
        "feature_domain": "agent",
        "source_file": "Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java",
        "source_symbol": "V2AgentAiService.chat",
        "line_number": "272",
    }


def append_rows(path: Path, rows: list[dict[str, str]], key_field: str = "category_id") -> int:
    fields, existing = read_csv(path)
    existing_keys = {row.get(key_field) for row in existing}
    pending = [row for row in rows if row.get(key_field) not in existing_keys]
    if not pending:
        return 0
    with path.open("a", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writerows(pending)
    return len(pending)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--owner-id", type=int, default=7)
    parser.add_argument(
        "--env",
        default="154.217.241.207 production-server-backend;model=deepseek-v4-flash;wire=chat_completions;tool_choice=auto",
        help="运行环境描述；必须显式传入当前环境，避免把历史服务器写入新台账",
    )
    args = parser.parse_args()

    evidence = args.evidence.resolve()
    summary = load_json(evidence / "summary.json")
    run_id = str(summary["run_id"])
    stamp = run_id.removeprefix("agent-full-current-").replace("+", "")
    cases = sorted(
        ((load_json(path), path) for path in (evidence / "cases").glob("*.json")),
        key=lambda item: item[1].name,
    )
    if len(cases) != int(summary["total"]):
        raise SystemExit(f"case count mismatch: {len(cases)} != {summary['total']}")

    feature_path = ROOT / "testing/Agent/功能测试/functional_feature_matrix.csv"
    _, feature_rows = read_csv(feature_path)
    functional_rows: list[dict[str, str]] = []
    feature_matrix_rows: list[dict[str, str]] = []
    unit_live_rows: list[dict[str, str]] = []
    unit_coverage_rows: list[dict[str, str]] = []
    performance_live_rows: list[dict[str, str]] = []
    performance_matrix_rows: list[dict[str, str]] = []
    total_rows: list[dict[str, str]] = []

    for case, case_path in cases:
        test_id = str(case["test_id"])
        number = test_id.rsplit("-", 1)[-1]
        tool = str((case.get("expected") or {}).get("target_tool") or case.get("tool") or "unknown")
        raw_result = str(case.get("result") or (case.get("actual") or {}).get("result") or "Failed")
        actual = case.get("actual") or {}
        trace = case.get("model_and_tool_trace") or {}
        data = response_data(case)
        actions = case.get("actions") or {}
        cleanup = case.get("cleanup") or {}
        source = source_metadata(feature_rows, test_id)
        category = f"AG-FT-BE-ALL-CURRENT-{stamp}-{number}"
        unit_test_id = f"AG-UT-W1-RUNTIME-CURRENT-{stamp}-{number}"
        unit_category = f"AG-UT-RUNTIME-CURRENT-{stamp}-{number}"
        performance_test_id = f"AG-PT-W1-CURRENT-{stamp}-{number}"
        performance_category = f"AG-PT-BE-ALL-CURRENT-{stamp}-{number}"
        tools = join_tools(case)
        reasons = "|".join(str(value) for value in actual.get("reasons") or [])
        artifacts = artifact_text(evidence, case_path)
        pre_state = pre_state_text(case, args.owner_id)
        env = args.env
        account = f"owner_user_id={args.owner_id};database=zhihuiji;real account"
        actions_text = f"POST /v2/agent/chat;prompt={(actions.get('request') or {}).get('message', '')};inspect native Function Calling, formal answer, DB before-after and cleanup;elapsed_ms={actions.get('elapsed_ms')}"
        expected_text = f"model autonomously selects relevant tool chain;target={tool};formal_answer=true;business_delta=0;cleanup=true"
        actual_text = f"result={raw_result};selected_tools={tools};plan_source={trace.get('plan_source')};llm_status={trace.get('llm_status')};answer_present={answer_present(case)};business_delta={compact(actual.get('business_delta') or {})};draft_delta={actual.get('draft_delta')};cleanup_pass={actual.get('cleanup_pass')};reasons={reasons}"
        cleanup_text = f"conversation={compact(cleanup.get('conversation') or {})};draft_outcomes={compact(cleanup.get('draft_outcomes') or [])};post_cleanup_counts={compact(case.get('post_cleanup_counts') or {})}"
        notes = "current 60-case real server run; raw runner result retained; historical rows untouched"

        functional_rows.append({
            "category_id": category,
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": expected_text,
            "actual": actual_text,
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": raw_result,
            "notes": notes,
        })
        feature_matrix_rows.append({
            "platform": "Agent",
            **source,
            "scenario_id": f"agent_full_current_{stamp}_{number}",
            "scenario_name": f"server Agent full current {test_id} {tool}",
            "test_status": raw_result,
            "evidence_path": artifacts,
            "notes": f"raw runner result={raw_result};{reasons or 'native tool selection and answer recorded'}",
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": expected_text,
            "actual": actual_text,
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": raw_result,
        })
        unit_live_rows.append({
            "category_id": unit_category,
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only;no JUnit command",
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": "record online function execution without claiming unit-test pass",
            "actual": actual_text,
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": "NotRun",
            "notes": "本行只记录线上函数运行证据；本轮未执行 JUnit，不计入单元测试通过率",
        })
        unit_coverage_rows.append({
            "platform": "Agent",
            "module": source["module"],
            "category_id": unit_category,
            "category_name": "线上函数运行证据（非单元测试）",
            "source_file": source["source_file"],
            "class_or_object": source["source_symbol"].split(".", 1)[0],
            "function_name": "chat",
            "line_number": source["line_number"],
            "test_status": "未执行单元测试",
            "test_file": "",
            "test_case": f"real server Agent case {test_id}",
            "evidence_path": artifacts,
            "notes": "线上运行结果不等价于 JUnit；保留 raw functional result=" + raw_result,
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only",
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": "function path is observable; no unit pass claim",
            "actual": actual_text,
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": "NotRun",
        })
        performance = data.get("performance_summary") or {}
        performance_live_rows.append({
            "category_id": performance_category,
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": "capture a real non-stream latency sample; do not treat one sample as the performance gate",
            "actual": f"result={raw_result};elapsed_ms={actions.get('elapsed_ms')};model_duration_ms={performance.get('model_duration_ms')};tool_duration_ms={performance.get('tool_duration_ms')};tool_calls={len(trace.get('tool_calls') or [])};answer_present={answer_present(case)}",
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": "Observed",
            "metric_snapshot": f"elapsed_ms={actions.get('elapsed_ms')};model_duration_ms={performance.get('model_duration_ms')};tool_duration_ms={performance.get('tool_duration_ms')};single_serial_sample;aggregate_SSE_cancel_soak_gate_not_run",
            "notes": "仅记录真实单次非流式观测；不计入性能门槛通过率；raw functional result=" + raw_result,
        })
        performance_matrix_rows.append({
            "platform": "Agent",
            "module": source["module"],
            "source_file": source["source_file"],
            "class_or_object": source["source_symbol"].split(".", 1)[0],
            "function_name": "chat",
            "line_number": source["line_number"],
            "scenario_name": f"server Agent full current latency observation {test_id}",
            "metric_family": "latency|model_duration|tool_duration|tool_count",
            "target_or_threshold": "single observation only; release performance threshold not established",
            "priority": "P1",
            "status": "已执行",
            "notes": "真实服务端样本已采集；Wave 2/3 SSE、cancel、并发、soak 和 JVM/Android 性能门禁仍需单独执行",
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions_text,
            "expected": "retain latency and tool timing evidence",
            "actual": f"raw_result={raw_result};elapsed_ms={actions.get('elapsed_ms')};model_duration_ms={performance.get('model_duration_ms')};tool_duration_ms={performance.get('tool_duration_ms')};tool_calls={len(trace.get('tool_calls') or [])}",
            "artifacts": artifacts,
            "cleanup": cleanup_text,
            "result": "Observed",
        })
        total_rows.append({
            "test_type": "本轮实测",
            "category_id": category,
            "category_name": f"Agent full current {test_id} {tool}",
            "primary_scope": "backend Agent|V2AgentAiService|provider",
            "must_cover": "model auto Function Calling;real tool result;formal answer;DB isolation;cleanup",
            "evidence_standard": "per-case JSON|tool trace|DB before-after|provider log|runner log|CSV",
            "priority": "P0" if (case.get("expected") or {}).get("kind") == "create" else "P1",
            "status": raw_result,
            "script_path": "testing/scripts/run_server_agent_all_tools.py",
            "ledger_link": "testing/Agent/功能测试/live_execution_ledger.csv",
            "matched_module_count": "3",
            "matched_modules": "V2AgentAiService|ToolPlanner|" + tool,
            "matched_file_count": "2",
            "matched_file_examples": "Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java|testing/scripts/run_server_agent_all_tools.py",
            "matched_function_count": "3",
            "notes": f"run_id={run_id};raw_result={raw_result};selected_tools={tools};reasons={reasons};historical rows retained",
        })

    targets = [
        (ROOT / "testing/Agent/功能测试/live_execution_ledger.csv", functional_rows, "category_id"),
        (feature_path, feature_matrix_rows, "scenario_id"),
        (ROOT / "testing/Agent/单元测试/live_execution_ledger.csv", unit_live_rows, "category_id"),
        (ROOT / "testing/Agent/单元测试/unit_function_coverage.csv", unit_coverage_rows, "category_id"),
        (ROOT / "testing/Agent/性能测试/live_execution_ledger.csv", performance_live_rows, "category_id"),
        (ROOT / "testing/Agent/性能测试/performance_scope_matrix.csv", performance_matrix_rows, "test_id"),
        (ROOT / "testing/Agent/测试分类总台账.csv", total_rows, "category_id"),
    ]
    appended = {str(path): append_rows(path, rows, key_field) for path, rows, key_field in targets}
    report = {
        "run_id": run_id,
        "evidence": str(evidence),
        "source_counts": {key: summary.get(key) for key in ("total", "passed", "failed", "blocked")},
        "case_count": len(cases),
        "appended_rows": appended,
        "note": "Historical rows were retained; unit rows are NotRun and performance rows are Observed.",
    }
    (evidence / "ledger-append-report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
