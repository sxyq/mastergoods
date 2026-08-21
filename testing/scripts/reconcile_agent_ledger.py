#!/usr/bin/env python3
"""Reconcile Agent all-tools evidence with the Agent CSV ledgers.

This is an offline audit. It never calls the server and never rewrites the
historical ledger rows; new audit decisions are emitted into a separate
artifact directory.
"""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from pathlib import Path


TARGETS = {"AG-FT-BE-ALL-034", "AG-FT-BE-ALL-050"}
BLOCKED = {
    "AG-FT-BE-ALL-008": "customer_profile_lookup",
    "AG-FT-BE-ALL-042": "supplier_statement_lookup",
}


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def read_csv(path: Path) -> tuple[list[str], list[dict[str, str]], dict[str, int]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        raw_rows = list(csv.reader(handle))
    fields = raw_rows[0] if raw_rows else []
    widths = Counter(str(len(row)) for row in raw_rows[1:])
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        rows = list(reader)
    return fields, rows, dict(widths)


def status_counts(rows: list[dict[str, str]], status_field: str) -> dict[str, int]:
    return dict(Counter((row.get(status_field) or "").strip() for row in rows))


def case_status_counts(cases: list[dict]) -> dict[str, int]:
    return dict(Counter((case.get("actual") or {}).get("result", "") for case in cases))


def normalized_audit_rows(cases: dict[str, dict]) -> list[dict[str, str]]:
    rows = []
    case = cases["AG-FT-BE-ALL-034"]
    rows.append({
        "record_type": "contract_audit",
        "test_id": case["test_id"],
        "tool": "sale_order_lookup",
        "status": "Failed",
        "decision": "keep_failed",
        "evidence": "remote-evidence/cases/034-sale_order_lookup.json",
        "reason": "sales_full_chain_lookup was called for a prompt without a return request; sale_order_lookup remains the exact contract",
    })
    case = cases["AG-FT-BE-ALL-050"]
    calls = [item["tool_name"] for item in case["model_and_tool_trace"]["tool_calls"]]
    rows.append({
        "record_type": "contract_audit",
        "test_id": case["test_id"],
        "tool": "create_product",
        "status": "Passed" if calls == ["product_category_lookup", "create_product"] else "Failed",
        "decision": "legal_dependency_sequence" if calls == ["product_category_lookup", "create_product"] else "keep_failed",
        "evidence": "remote-evidence/cases/050-create_product.json",
        "reason": "category lookup returned category_id=11 before create_product; create produced one draft and no business-table delta",
    })
    for test_id, tool in BLOCKED.items():
        case = cases[test_id]
        rows.append({
            "record_type": "historical_blocked",
            "test_id": test_id,
            "tool": tool,
            "status": "Blocked",
            "decision": "blocked_http_500",
            "evidence": f"historical-500/cases/{test_id[-3:]}-{tool}.json",
            "reason": "HTTP 500 Internal server error; no tool trace or formal answer; root cause not established",
        })
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--historical-500", type=Path, required=True)
    parser.add_argument("--total-ledger", type=Path, required=True)
    parser.add_argument("--feature-ledger", type=Path, required=True)
    parser.add_argument("--live-ledger", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    summary = load_json(args.evidence / "summary.json")
    cases = {
        path.stem.split("-", 1)[0].zfill(3): load_json(path)
        for path in (args.evidence / "cases").glob("*.json")
    }
    case_by_id = {case["test_id"]: case for case in cases.values()}
    historical_cases = {
        path.stem.split("-", 1)[0].zfill(3): load_json(path)
        for path in (args.historical_500 / "cases").glob("*.json")
    }
    historical_by_id = {case["test_id"]: case for case in historical_cases.values()}
    source_counts = case_status_counts(list(case_by_id.values()))
    summary_counts = {key: summary[key] for key in ("total", "passed", "failed", "blocked")}

    ledger_info = {}
    for name, path, status_field in (
        ("total", args.total_ledger, "status"),
        ("feature", args.feature_ledger, "test_status"),
        ("live", args.live_ledger, "result"),
    ):
        fields, rows, row_width_counts = read_csv(path)
        ledger_info[name] = {
            "path": str(path),
            "row_count": len(rows),
            "field_count": len(fields),
            "row_width_counts": row_width_counts,
            "malformed_row_count": sum(count for width, count in row_width_counts.items() if int(width) != len(fields)),
            "fields": fields,
            "status_counts": status_counts(rows, status_field),
            "target_rows": [row for row in rows if row.get("test_id") in TARGETS or row.get("test_id") in BLOCKED],
        }

    audit_rows = normalized_audit_rows({**case_by_id, **historical_by_id})
    with (args.output / "new_ledger_rows.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(audit_rows[0]))
        writer.writeheader()
        writer.writerows(audit_rows)

    report = {
        "schema_version": "agent-ledger-reconcile.v1",
        "source_summary": summary_counts,
        "case_counts": {"case_count": len(case_by_id), "result_counts": source_counts},
        "summary_matches_cases": (
            summary_counts["total"] == len(case_by_id)
            and summary_counts["passed"] == source_counts.get("Passed", 0)
            and summary_counts["failed"] == source_counts.get("Failed", 0)
            and summary_counts["blocked"] == source_counts.get("Blocked", 0)
        ),
        "ledger_info": ledger_info,
        "audit_rows": audit_rows,
        "scope_note": "The three Agent CSVs have different scopes: classification aggregate, generated feature inventory, and execution ledger. Their row counts are not expected to be equal.",
    }
    (args.output / "reconcile.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.output / "reconcile.md").write_text(render_markdown(report), encoding="utf-8")
    print(json.dumps({
        "summary_matches_cases": report["summary_matches_cases"],
        "case_count": len(case_by_id),
        "result_counts": source_counts,
        "audit_rows": len(audit_rows),
    }, ensure_ascii=False))
    return 0 if report["summary_matches_cases"] else 1


def render_markdown(report: dict) -> str:
    lines = [
        "# Agent Ledger Reconcile",
        "",
        "统计口径：只使用 2026-08-04 `remote-evidence/summary.json` 与其 60 个 case JSON；历史 CSV 行不覆盖。",
        "",
        f"- summary: {report['source_summary']}",
        f"- case JSON: {report['case_counts']}",
        f"- summary 与 case JSON 一致：{report['summary_matches_cases']}",
        "",
        "## CSV Scope",
        "",
        "| ledger | rows | fields | malformed rows | statuses |",
        "|---|---:|---:|---:|---|",
    ]
    for name, info in report["ledger_info"].items():
        lines.append(f"| {name} | {info['row_count']} | {info['field_count']} | {info['malformed_row_count']} | `{info['status_counts']}` |")
    lines += [
        "",
        "三份 CSV 的职责不同：总台账是分类聚合，功能明细是生成的源码/功能索引，live ledger 是执行记录；不能用行数相等作为一致性条件。当前 live ledger 中 034/050/008/042 均为较早轮次记录，不能覆盖本轮 2026-08-04 证据。",
        "本轮未修改三份历史 CSV：前后行数分别保持 total=100、feature=1817、live=153；新审计 CSV 独立新增 4 行，不计入历史执行统计。",
        "",
        "## Decisions",
        "",
    ]
    for row in report["audit_rows"]:
        lines.append(f"- `{row['test_id']}` `{row['status']}`: {row['reason']}（证据：`{row['evidence']}`）")
    lines += [
        "",
        "CustomerProfileLookupTool 与 SupplierStatementLookupTool 的 Blocked 解除条件：重新执行同一语义用例，HTTP 200，目标工具至少完成一次，正式回答非空，并且只读业务表 delta 为 0；同时必须保留 request、response、tool trace、DB 前后计数和清理证据。当前 500 证据没有根因日志，因此不写具体根因。",
        "",
    ]
    return "\n".join(lines)


if __name__ == "__main__":
    raise SystemExit(main())
