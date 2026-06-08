#!/usr/bin/env python3
"""Capture repeatable backend-side performance evidence for report endpoints.

The output is intentionally marked partial: endpoint latency evidence does not
replace Android screenshots, UI tree dumps, or frame timing.
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
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


DEFAULT_ENDPOINTS = [
    {
        "name": "dashboard_cashflow_summary",
        "method": "GET",
        "path": "/v1/reports/cashflow-summary",
        "query": {"start_at": "{start_at}", "end_at": "{end_at}"},
        "require_json_code_zero": True,
    },
    {
        "name": "finance_records_page_for_cashflow_reconcile",
        "method": "GET",
        "path": "/v1/finance-records",
        "query": {"created_after": "{start_at}", "created_before": "{end_at}", "page": "{page}", "size": "{size}"},
        "require_json_code_zero": True,
    },
    {
        "name": "report_profit_summary",
        "method": "GET",
        "path": "/v1/reports/profit-summary",
        "query": {"start_at": "{start_at}", "end_at": "{end_at}"},
        "require_json_code_zero": True,
    },
    {
        "name": "report_stock_out_records",
        "method": "GET",
        "path": "/v1/reports/stock-out-records",
        "query": {"start_at": "{start_at}", "end_at": "{end_at}", "limit": "{limit}"},
        "require_json_code_zero": True,
    },
    {
        "name": "report_inventory_flow",
        "method": "GET",
        "path": "/v1/reports/inventory-flow",
        "query": {"start_at": "{start_at}", "end_at": "{end_at}", "limit": "{limit}"},
        "require_json_code_zero": True,
    },
    {
        "name": "v2_sale_orders_page",
        "method": "GET",
        "path": "/v2/sale-orders",
        "query": {"page": "{page}", "size": "{size}"},
        "require_json_code_zero": True,
    },
    {
        "name": "v2_sale_orders_filtered_page",
        "method": "GET",
        "path": "/v2/sale-orders",
        "query": {
            "status": "{sale_order_status}",
            "created_after": "{start_at}",
            "created_before": "{end_at}",
            "page": "{page}",
            "size": "{size}",
        },
        "require_json_code_zero": True,
    },
]


def now_ms() -> int:
    return int(time.time() * 1000)


def iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def percentile(values: list[float], ratio: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    index = min(len(ordered) - 1, max(0, math.ceil(ratio * len(ordered)) - 1))
    return ordered[index]


def safe_json_preview(payload: object, max_chars: int = 1200) -> object:
    text = json.dumps(payload, ensure_ascii=False, sort_keys=True)
    if len(text) <= max_chars:
        return payload
    if isinstance(payload, dict):
        preview: dict[str, object] = {
            "truncated_preview": text[:max_chars],
            "original_chars": len(text),
        }
        for key in ("code", "message", "timestamp"):
            if key in payload:
                preview[key] = payload[key]
        data = payload.get("data")
        if isinstance(data, list):
            preview["data_count_in_preview_source"] = len(data)
            preview["data_preview"] = data[:2]
        elif isinstance(data, dict):
            preview["data_preview"] = data
        return preview
    return {"truncated_preview": text[:max_chars], "original_chars": len(text)}


class Client:
    def __init__(self, base_url: str, token: str | None, timeout_seconds: float) -> None:
        self.base_url = base_url.rstrip("/") + "/"
        self.token = token
        self.timeout_seconds = timeout_seconds

    def request(
        self,
        method: str,
        path: str,
        query: dict[str, str] | None = None,
        payload: dict[str, object] | None = None,
    ) -> dict[str, object]:
        url = urllib.parse.urljoin(self.base_url, path.lstrip("/"))
        if query:
            url = url + "?" + urllib.parse.urlencode(query)
        body = None
        if payload is not None:
            body = json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(url=url, method=method.upper(), data=body)
        request.add_header("Accept", "application/json")
        if body is not None:
            request.add_header("Content-Type", "application/json")
        if self.token:
            request.add_header("Authorization", f"Bearer {self.token}")

        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                headers_received = time.perf_counter()
                response_body = response.read().decode("utf-8", errors="replace")
                return self._build_result(response.getcode(), response_body, time.perf_counter() - started, headers_received - started)
        except urllib.error.HTTPError as exc:
            headers_received = time.perf_counter()
            response_body = exc.read().decode("utf-8", errors="replace")
            return self._build_result(
                exc.code,
                response_body,
                time.perf_counter() - started,
                headers_received - started,
                error=f"HTTPError {exc.code}",
            )
        except Exception as exc:  # noqa: BLE001
            return {
                "status": None,
                "duration_ms": round((time.perf_counter() - started) * 1000, 2),
                "json": None,
                "body_preview": None,
                "error": repr(exc),
            }

    def _build_result(
        self,
        status: int,
        body: str,
        duration_seconds: float,
        headers_seconds: float,
        error: str | None = None,
    ) -> dict[str, object]:
        try:
            parsed = json.loads(body)
        except json.JSONDecodeError:
            parsed = None
        return {
            "status": status,
            "time_to_headers_ms": round(headers_seconds * 1000, 2),
            "duration_ms": round(duration_seconds * 1000, 2),
            "json": safe_json_preview(parsed) if isinstance(parsed, dict) else parsed,
            "body_preview": None if parsed is not None else body[:1200],
            "error": error,
        }


def login_for_token(base_url: str, phone: str, password: str, timeout_seconds: float) -> str:
    client = Client(base_url, None, timeout_seconds)
    result = client.request("POST", "/v1/auth/login", payload={"phone": phone, "password": password})
    payload = result.get("json")
    if result.get("status") != 200 or not isinstance(payload, dict):
        raise RuntimeError(f"login failed: {result}")
    token = payload.get("data", {}).get("token") if isinstance(payload.get("data"), dict) else None
    if not token:
        raise RuntimeError("login response did not include data.token")
    return str(token)


def render_query(template: dict[str, str], variables: dict[str, str]) -> dict[str, str]:
    return {key: value.format(**variables) for key, value in template.items()}


def summarize_endpoint(endpoint_name: str, samples: list[dict[str, object]]) -> dict[str, object]:
    durations = [float(row["duration_ms"]) for row in samples if row.get("duration_ms") is not None]
    status_counts: dict[str, int] = {}
    logical_code_counts: dict[str, int] = {}
    for row in samples:
        status_counts[str(row.get("status"))] = status_counts.get(str(row.get("status")), 0) + 1
        payload = row.get("json")
        if isinstance(payload, dict) and "code" in payload:
            code = str(payload.get("code"))
            logical_code_counts[code] = logical_code_counts.get(code, 0) + 1
    ok = sum(1 for row in samples if 200 <= int(row.get("status") or 0) < 300)
    logical_ok = sum(
        1
        for row in samples
        if isinstance(row.get("json"), dict) and row["json"].get("code") == 0
    )
    return {
        "name": endpoint_name,
        "requests": len(samples),
        "http_ok": ok,
        "logical_ok": logical_ok,
        "http_status_counts": status_counts,
        "logical_code_counts": logical_code_counts,
        "p50_ms": percentile(durations, 0.50),
        "p95_ms": percentile(durations, 0.95),
        "max_ms": max(durations) if durations else None,
        "mean_ms": round(statistics.mean(durations), 2) if durations else None,
        "sample_errors": [row for row in samples if row.get("error")][:3],
    }


def capture(args: argparse.Namespace) -> Path:
    start_at = str(args.start_at if args.start_at is not None else now_ms() - args.window_days * 86_400_000)
    end_at = str(args.end_at if args.end_at is not None else now_ms())
    variables = {
        "start_at": start_at,
        "end_at": end_at,
        "limit": str(args.limit),
        "page": str(args.page),
        "size": str(args.size),
        "sale_order_status": str(args.sale_order_status),
    }
    token = args.token or os.environ.get("TOKEN")
    token_source = "provided" if token else "none"
    if not token and args.login_phone and args.login_password:
        token = login_for_token(args.base_url, args.login_phone, args.login_password, args.timeout_seconds)
        token_source = "login"
    elif not token and not args.allow_no_auth:
        raise RuntimeError("TOKEN or --login-phone/--login-password is required unless --allow-no-auth is set")

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = Path(args.output_root) / f"{timestamp}-backend-report-performance"
    output_dir.mkdir(parents=True, exist_ok=False)
    client = Client(args.base_url, token, args.timeout_seconds)
    endpoints = DEFAULT_ENDPOINTS
    raw_results: dict[str, list[dict[str, object]]] = {}

    for endpoint in endpoints:
        name = endpoint["name"]
        query = render_query(endpoint.get("query", {}), variables)
        samples = []
        for index in range(args.warmup + args.samples):
            result = client.request(endpoint["method"], endpoint["path"], query=query)
            result["sample_index"] = index - args.warmup
            result["warmup"] = index < args.warmup
            result["endpoint"] = name
            if not result["warmup"]:
                samples.append(result)
        raw_results[name] = samples

    summaries = [summarize_endpoint(name, samples) for name, samples in raw_results.items()]
    report = {
        "captured_at": iso_now(),
        "base_url": args.base_url,
        "account_label": args.account_label,
        "backend_profile": args.backend_profile,
        "token_source": token_source,
        "window": {"start_at": int(start_at), "end_at": int(end_at), "window_days": args.window_days},
        "request_config": {
            "samples": args.samples,
            "warmup": args.warmup,
            "limit": args.limit,
            "page": args.page,
            "size": args.size,
            "sale_order_status": args.sale_order_status,
            "timeout_seconds": args.timeout_seconds,
        },
        "endpoints": endpoints,
        "summaries": summaries,
        "raw_results": raw_results,
    }
    (output_dir / "00-env.md").write_text(render_env_md(report), encoding="utf-8")
    (output_dir / "01-request-plan.json").write_text(
        json.dumps({key: report[key] for key in ["captured_at", "base_url", "window", "request_config", "endpoints"]}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    (output_dir / "02-raw-results.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    (output_dir / "03-summary.md").write_text(render_summary_md(report), encoding="utf-8")
    (output_dir / "04-conclusion.md").write_text(render_conclusion_md(report), encoding="utf-8")
    return output_dir


def render_env_md(report: dict[str, object]) -> str:
    window = report["window"]
    config = report["request_config"]
    return "\n".join(
        [
            "# Backend Report Performance Evidence Environment",
            "",
            f"- Captured at UTC: `{report['captured_at']}`",
            f"- Base URL: `{report['base_url']}`",
            f"- Account label: `{report['account_label']}`",
            f"- Backend profile note: `{report['backend_profile']}`",
            f"- Token source: `{report['token_source']}`",
            f"- Query window: `{window['start_at']}` to `{window['end_at']}`",
            f"- Samples per endpoint: `{config['samples']}` plus `{config['warmup']}` warmup",
            "",
            "Secrets are intentionally not written. Android first visible latency and frame timing are not captured by this script.",
            "",
        ]
    )


def render_summary_md(report: dict[str, object]) -> str:
    lines = [
        "# Backend Report Performance Summary",
        "",
        "| Endpoint | Requests | HTTP OK | Logical OK | p50 ms | p95 ms | max ms | mean ms |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for row in report["summaries"]:
        lines.append(
            "| {name} | {requests} | {http_ok} | {logical_ok} | {p50_ms} | {p95_ms} | {max_ms} | {mean_ms} |".format(
                **row
            )
        )
    lines.extend(
        [
            "",
            "Captured endpoints map to the current performance-debt ledger:",
            "",
            "- `dashboard_cashflow_summary`: Dashboard net cashflow backend aggregation.",
            "- `finance_records_page_for_cashflow_reconcile`: finance-record page used as a cashflow reconciliation anchor.",
            "- `report_profit_summary`: report profit scalar aggregation.",
            "- `report_stock_out_records`: stock-out paged item/order query.",
            "- `report_inventory_flow`: three-source inventory flow candidate query.",
            "- `v2_sale_orders_page`: V2 sale order repository pagination plus batched items.",
            "- `v2_sale_orders_filtered_page`: V2 sale order filtered repository pagination path.",
            "",
        ]
    )
    return "\n".join(lines)


def render_conclusion_md(report: dict[str, object]) -> str:
    failures = [
        row for row in report["summaries"]
        if row["http_ok"] != row["requests"] or row["logical_ok"] != row["requests"]
    ]
    status = "partial" if failures else "partial"
    lines = [
        "# Backend Report Performance Conclusion",
        "",
        f"Status: `{status}`",
        "",
        "This package is backend-interface evidence only. It can support the performance-debt ledger, but it cannot by itself pass the full goal because Android first-visible latency, screenshots, UI tree, and frame timing still require real-device capture.",
        "",
    ]
    if failures:
        lines.append("Endpoint failures or non-zero logical codes were observed:")
        lines.append("")
        for row in failures:
            lines.append(f"- `{row['name']}`: HTTP OK {row['http_ok']}/{row['requests']}, logical OK {row['logical_ok']}/{row['requests']}")
    else:
        lines.append("All sampled backend requests returned HTTP success and logical `code=0`.")
    lines.append("")
    return "\n".join(lines)


def self_test() -> None:
    summary = summarize_endpoint(
        "demo",
        [
            {"status": 200, "duration_ms": 10.0, "json": {"code": 0}},
            {"status": 200, "duration_ms": 30.0, "json": {"code": 0}},
        ],
    )
    assert summary["p50_ms"] == 10.0
    assert summary["p95_ms"] == 30.0
    assert summary["logical_ok"] == 2
    query = render_query({"a": "{start_at}", "b": "{limit}"}, {"start_at": "1", "limit": "2"})
    assert query == {"a": "1", "b": "2"}
    print("report_performance_evidence self-test passed")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=os.environ.get("BASE_URL", "http://localhost:8080"))
    parser.add_argument("--token", default=os.environ.get("TOKEN"))
    parser.add_argument("--login-phone", default=os.environ.get("LOGIN_PHONE"))
    parser.add_argument("--login-password", default=os.environ.get("LOGIN_PASSWORD"))
    parser.add_argument("--allow-no-auth", action="store_true", default=os.environ.get("ALLOW_NO_AUTH") == "1")
    parser.add_argument("--account-label", default=os.environ.get("ACCOUNT_LABEL", "manual"))
    parser.add_argument("--backend-profile", default=os.environ.get("BACKEND_PROFILE", "unknown"))
    parser.add_argument("--output-root", default="docs/acceptance-evidence/performance")
    parser.add_argument("--window-days", type=int, default=30)
    parser.add_argument("--start-at", type=int)
    parser.add_argument("--end-at", type=int)
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--page", type=int, default=0)
    parser.add_argument("--size", type=int, default=20)
    parser.add_argument("--sale-order-status", type=int, default=1)
    parser.add_argument("--samples", type=int, default=5)
    parser.add_argument("--warmup", type=int, default=1)
    parser.add_argument("--timeout-seconds", type=float, default=10.0)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        self_test()
        return 0
    output_dir = capture(args)
    print(f"Backend report performance evidence written to: {output_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
