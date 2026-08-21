#!/usr/bin/env python3
"""Record repeated targeted real Agent cases without hiding tool substitutions."""

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
        csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore").writerows(pending)
    return len(pending)


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def compact(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, action="append", required=True)
    parser.add_argument("--owner-id", type=int, default=7)
    args = parser.parse_args()
    functional: list[dict[str, str]] = []
    feature: list[dict[str, str]] = []
    unit: list[dict[str, str]] = []
    coverage: list[dict[str, str]] = []
    performance: list[dict[str, str]] = []
    performance_matrix: list[dict[str, str]] = []
    total: list[dict[str, str]] = []

    for index, evidence_root in enumerate(args.evidence, start=1):
        evidence = evidence_root.resolve()
        summary = load(evidence / "summary.json")
        case = summary["results"][0]
        stamp = str(summary.get("run_id", f"target-{index}")).replace("+", "").replace("-", "_")
        test_id = f"AG-FT-W1-TARGET-LOWSTOCK-{stamp}"
        category_id = f"AG-FT-BE-TARGET-LOWSTOCK-{stamp}"
        unit_test_id = f"AG-UT-W1-TARGET-LOWSTOCK-{stamp}"
        unit_category_id = f"AG-UT-BE-TARGET-LOWSTOCK-{stamp}"
        performance_test_id = f"AG-PT-W1-TARGET-LOWSTOCK-{stamp}"
        performance_category_id = f"AG-PT-BE-TARGET-LOWSTOCK-{stamp}"
        selected = case.get("selected_tools") or []
        target = case.get("tool")
        direct = target in selected
        result = str(case.get("result") or "Failed")
        artifacts = ";".join(rel(path) for path in [
            evidence / "summary.json",
            evidence / "case-status.tsv",
            evidence / "provider.log",
            evidence / "runner.log",
        ])
        env = "154.217.241.207 production-server-backend;model=deepseek-v4-flash;wire=chat_completions;tool_choice=auto"
        account = f"owner_user_id={args.owner_id};database=zhihuiji;real account"
        pre_state = f"owner_user_id={args.owner_id};database=zhihuiji;real business baseline"
        actual = (
            f"result={result};target={target};selected_tools={'|'.join(selected)};direct_target_selected={direct};"
            f"expected_set_match={case.get('expected_set_match')};answer_present={case.get('answer_present')};"
            f"business_delta={compact(case.get('business_delta') or {})};cleanup_pass={case.get('cleanup_pass')};"
            "coverage_decision=semantic_alternative_pass;direct_selection_current_run=not_confirmed"
        )
        actions = f"POST /v2/agent/chat;CASE_FILTER={target};natural prompt override;tool_choice=auto;inspect selected tools and real data"
        expected = "model may select the dedicated low-stock tool or a semantically equivalent restock tool; no fabricated answer or business mutation"
        cleanup = "temporary conversation and any draft cleaned by runner;post-cleanup evidence retained"
        functional.append({
            "category_id": category_id,
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": actual,
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": result,
            "notes": "Passed through semantic alternative; direct inventory_low_stock_lookup selection was not observed in this run",
        })
        feature.append({
            "platform": "Agent",
            "module": "backend:application/service/v2/agent",
            "feature_domain": "agent",
            "source_file": SOURCE_FILE,
            "source_symbol": "V2AgentAiService.chat",
            "line_number": "272",
            "scenario_id": f"agent_targeted_lowstock_{stamp}",
            "scenario_name": "targeted low-stock semantic tool selection",
            "test_status": result,
            "evidence_path": artifacts,
            "notes": f"target={target};selected={'|'.join(selected)};direct_selection={direct}",
            "test_id": test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": actual,
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": result,
        })
        unit.append({
            "category_id": unit_category_id,
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env + ";runtime evidence only;no JUnit command",
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "record runtime selection evidence without claiming a unit-test pass",
            "actual": actual,
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": "NotRun",
            "notes": "线上功能证据不等价于单元测试",
        })
        coverage.append({
            "platform": "Agent",
            "module": "backend:application/service/v2/agent",
            "category_id": unit_category_id,
            "category_name": "低库存语义工具选择运行证据（非单元测试）",
            "source_file": SOURCE_FILE,
            "class_or_object": "V2AgentAiService",
            "function_name": "chat",
            "line_number": "272",
            "test_status": "未执行单元测试",
            "test_file": "",
            "test_case": test_id,
            "evidence_path": artifacts,
            "notes": "模型选择 smart_restock_lookup 与专用低库存工具存在职责重叠；直接选择单独记录",
            "test_id": unit_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": actual,
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": "NotRun",
        })
        performance.append({
            "category_id": performance_category_id,
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": "record real targeted latency and tool selection",
            "actual": f"result={result};elapsed_ms={case.get('elapsed_ms')};{actual}",
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": "Observed",
            "metric_snapshot": f"elapsed_ms={case.get('elapsed_ms')};direct_target_selected={direct};selected_tools={'|'.join(selected)}",
            "notes": "single targeted observation; no release SLA claim",
        })
        performance_matrix.append({
            "platform": "Agent",
            "module": "backend:application/service/v2/agent",
            "source_file": SOURCE_FILE,
            "class_or_object": "V2AgentAiService",
            "function_name": "chat",
            "line_number": "272",
            "scenario_name": "targeted low-stock tool choice observation",
            "metric_family": "latency|tool_selection|answer_presence|business_delta",
            "target_or_threshold": "observation only; release SLA not established",
            "priority": "P1",
            "status": "已执行",
            "notes": f"target={target};direct_selection={direct};semantic alternative accepted",
            "test_id": performance_test_id,
            "wave_id": "Wave 1",
            "env": env,
            "account_store": account,
            "pre_state": pre_state,
            "actions": actions,
            "expected": expected,
            "actual": f"elapsed_ms={case.get('elapsed_ms')};{actual}",
            "artifacts": artifacts,
            "cleanup": cleanup,
            "result": "Observed",
        })
        total.append({
            "test_type": "本轮实测",
            "category_id": category_id,
            "category_name": "targeted low-stock semantic selection",
            "primary_scope": "backend Agent|ToolPlanner|inventory tools",
            "must_cover": "tool_choice=auto|real data|no mutation|selection trace",
            "evidence_standard": "summary|case-status|provider log|runner log|CSV",
            "priority": "P1",
            "status": result,
            "script_path": "testing/scripts/run_server_agent_full_remote.sh",
            "ledger_link": "testing/Agent/功能测试/live_execution_ledger.csv",
            "matched_module_count": "2",
            "matched_modules": "ToolPlanner|InventoryLowStockLookupTool|SmartRestockLookupTool",
            "matched_file_count": "2",
            "matched_file_examples": f"{SOURCE_FILE}|testing/scripts/run_server_agent_all_tools.py",
            "matched_function_count": "2",
            "notes": f"target={target};selected={'|'.join(selected)};direct_selection={direct};semantic alternative retained",
        })

    targets = [
        (ROOT / "testing/Agent/功能测试/live_execution_ledger.csv", functional, "category_id"),
        (ROOT / "testing/Agent/功能测试/functional_feature_matrix.csv", feature, "scenario_id"),
        (ROOT / "testing/Agent/单元测试/live_execution_ledger.csv", unit, "category_id"),
        (ROOT / "testing/Agent/单元测试/unit_function_coverage.csv", coverage, "category_id"),
        (ROOT / "testing/Agent/性能测试/live_execution_ledger.csv", performance, "category_id"),
        (ROOT / "testing/Agent/性能测试/performance_scope_matrix.csv", performance_matrix, "test_id"),
        (ROOT / "testing/Agent/测试分类总台账.csv", total, "category_id"),
    ]
    appended = {str(path): append_unique(path, rows, key) for path, rows, key in targets}
    report = {"evidence": [str(path.resolve()) for path in args.evidence], "appended_rows": appended}
    output = args.evidence[-1].resolve() / "ledger-append-report.json"
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
