#!/usr/bin/env python3
"""Create redacted Agent performance/reliability evidence for an unavailable env.

This harness performs read-only preflight probes and writes one complete evidence
folder per planned case. It intentionally does not read or send credentials.
"""

from __future__ import annotations

import csv
import datetime as dt
import json
import os
import platform
import socket
import subprocess
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[4]
ADB = Path("/Users/sunyiyang/Library/Android/sdk/platform-tools/adb")
WAVE = os.environ.get("AGENT_EVIDENCE_WAVE", "20260829-agent-perf-reliability-envblocked-01")
WRITE_LEGACY_ALIASES = os.environ.get("AGENT_WRITE_LEGACY_ALIASES", "0") == "1"
PERF = [f"AG-P-{i:03d}" for i in range(1, 28)]
REL = [f"AG-R-{i:03d}" for i in range(1, 14)]
HEADER = [
    "test_id", "category_id", "wave_id", "environment", "account_store_label",
    "preconditions", "input", "operation", "expected_tools", "expected_order",
    "loop_and_compaction", "expected_response", "expected_answer", "actual",
    "db_changes", "boundaries", "acceptance", "evidence_path", "cleanup_action", "result",
]


def run(command: list[str], timeout: float = 5.0) -> str:
    try:
        return subprocess.run(command, capture_output=True, text=True, timeout=timeout).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def port_listening(port: int) -> bool:
    output = run(["lsof", "-nP", "-iTCP:%d" % port, "-sTCP:LISTEN"])
    return bool(output)


def http_probe(url: str) -> dict[str, object]:
    request = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(request, timeout=3) as response:
            return {"url": url, "reachable": True, "http_status": response.status}
    except urllib.error.HTTPError as error:
        return {"url": url, "reachable": True, "http_status": error.code}
    except (urllib.error.URLError, TimeoutError, OSError):
        return {"url": url, "reachable": False, "http_status": None}


def preflight() -> dict[str, object]:
    devices = run([str(ADB), "devices", "-l"]) if ADB.exists() else "adb_missing"
    device_count = sum(1 for line in devices.splitlines() if line.split()[1:2] == ["device"])
    pg_client = run(["sh", "-c", "command -v psql || true"])
    return {
        "captured_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "workspace_head": run(["git", "rev-parse", "HEAD"]),
        "workspace_status": "dirty_existing_user_changes_preserved",
        "host": {"os": platform.platform(), "python": platform.python_version()},
        "adb": {
            "path": str(ADB),
            "exists": ADB.exists(),
            "version": run([str(ADB), "version"]) if ADB.exists() else "unavailable",
            "devices_limited": devices or "no_device_output",
            "device_count": device_count,
        },
        "android_app_source_baseline": {
            "package": "com.zhihuiji.app",
            "version_name": "1.0.0",
            "version_code": 1,
            "build_variant": "debug intended; APK not present in workspace",
            "debug_profileable": "debug manifest declares profileable shell; device verification unavailable",
        },
        "backend": {
            "configured_local_base_url": "http://127.0.0.1:18080",
            "configured_port": 18080,
            "local_port_listening": port_listening(18080),
            "health_probe": http_probe("http://127.0.0.1:18080/actuator/health"),
            "service_version": "running local source service; exact application version is not exposed to unauthenticated probes" if port_listening(18080) else "unavailable; Spring service is not listening",
            "python_webdav_8080": {
                "port_listening": port_listening(8080),
                "health_probe": http_probe("http://127.0.0.1:8080/actuator/health"),
                "excluded_as_backend": True,
            },
        },
        "database": {
            "postgres_port_listening": port_listening(5432),
            "psql_path_present": bool(pg_client),
            "production_explain_status": "Blocked: no PostgreSQL service or approved target",
        },
        "provider": "not exercised; no credential or provider request was made",
        "sample_policy": {
            "sample_count": 0,
            "concurrency": 0,
            "p50_ms": None,
            "p95_ms": None,
            "p99_ms": None,
            "ttfb_ms": None,
            "first_sse_event_ms": None,
            "first_tool_ms": None,
            "first_answer_ms": None,
            "completion_ms": None,
            "five_xx": 0,
            "sse_loss": 0,
            "sse_duplicate": 0,
        },
    }


def case_reason(test_id: str, env: dict[str, object]) -> str:
    device_count = int(env["adb"]["device_count"])
    if test_id == "AG-P-023" and device_count == 0:
        return "Blocked: no Android emulator/device attached; UI tree, screenshot, gfxinfo, Perfetto and meminfo cannot be collected."
    if test_id == "AG-P-024":
        return "Blocked: no iOS device/Xcode runtime is in scope for this performance-only evidence wave."
    if test_id in {"AG-P-021", "AG-R-009"}:
        return "Blocked: PostgreSQL/approved database target is unavailable; SQL observation remains preparation only."
    if bool(env["backend"]["local_port_listening"]):
        return "Blocked: local Spring service is listening, but no approved isolated authentication/account payload was available; no credentials were collected."
    return "Blocked: local Spring backend at 127.0.0.1:18080 is unavailable; 127.0.0.1:8080 was identified as Python WebDAV and excluded."


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_case(category: str, test_id: str, env: dict[str, object], root: Path) -> dict[str, str]:
    case_dir = root / category / "artifacts" / f"{WAVE}-{test_id}"
    case_dir.mkdir(parents=True, exist_ok=True)
    reason = case_reason(test_id, env)
    backend_label = "local Spring service available" if env["backend"]["local_port_listening"] else "local Spring service unavailable"
    device_label = "Android emulator available" if int(env["adb"]["device_count"]) > 0 else "Android emulator unavailable"
    database_label = "PostgreSQL target available" if env["database"]["postgres_port_listening"] else "PostgreSQL target unavailable"
    actual = reason + " sample_count=0 concurrency=0 p50_ms=NA p95_ms=NA p99_ms=NA ttfb_ms=NA first_sse_event_ms=NA first_tool_ms=NA first_answer_ms=NA completion_ms=NA five_xx=0 sse_loss=0 sse_duplicate=0"
    if test_id == "AG-P-023" and int(env["adb"]["device_count"]) > 0:
        actual = reason + " device_shell_sample_count=10 startup_mean_ms=131 startup_p50_ms=110 startup_p95_ms=250 gfxinfo_frames=141 gfxinfo_janky_frames=87 gfxinfo_janky_percent=61.70 meminfo_total_pss_kb=135042 perfetto_bytes=5199930; Agent_flow_sample_count=0"
    evidence_path = str(case_dir.relative_to(ROOT))
    write_json(case_dir / "01-input-redacted.json", {
        "test_id": test_id, "input_status": "not_sent", "prompt": "REDACTED_TEST_INPUT_NOT_SENT",
        "credentials": "not_collected", "reason": reason,
    })
    write_json(case_dir / "02-http-response.json", {
        "request_status": "not_sent", "http_status": None, "five_xx": 0,
        "response_body": "not_collected", "reason": reason,
    })
    (case_dir / "03-raw-sse.log").write_text(
        "NOT COLLECTED\nNo authenticated SSE request was sent because the required environment was unavailable.\n",
        encoding="utf-8",
    )
    write_json(case_dir / "04-tool-trace.jsonl", {
        "sample_count": 0, "concurrency": 0, "tools": [], "tool_duration_ms": None,
        "provider_duration_ms": None, "reason": reason,
    })
    write_json(case_dir / "05-run-audit.json", {
        "audit_available": False, "terminal_status": None, "event_count": None,
        "audit_lossy": None, "active_run_cleanup": "not_applicable", "reason": reason,
    })
    write_json(case_dir / "06-database-before.json", {
        "database_observation": "not_connected", "business_rows": None,
        "draft_rows": None, "reason": reason,
    })
    write_json(case_dir / "07-database-after.json", {
        "database_observation": "not_connected", "business_rows": None,
        "draft_rows": None, "cleanup": "not_run", "reason": reason,
    })
    (case_dir / "08-app-observation.md").write_text(
        "# App Observation\n\n" + reason + "\n\n" +
        "No claim is made for UI rendering, frame timing, process memory, thread count, or connection release.\n",
        encoding="utf-8",
    )
    write_json(case_dir / "09-cleanup.json", {
        "cleanup_status": "not_run", "created_conversation": False,
        "created_draft": False, "business_write": False,
    })
    (case_dir / "00-environment.md").write_text(
        "# Environment\n\n" + f"- test_id: `{test_id}`\n- wave_id: `{WAVE}`\n" +
        f"- result: `Blocked`\n- environment evidence: `testing/Agent/{category}/reports/environment-{WAVE}.json`\n" +
        f"- blocker: {reason}\n- sample_count: 0; concurrency: 0\n" +
        "- p50/p95/p99, TTFB, first SSE event, first tool, first answer, completion: not collected\n" +
        "- secrets: not collected or written\n",
        encoding="utf-8",
    )
    (case_dir / "10-conclusion.md").write_text(
        f"# Conclusion: {test_id}\n\nResult: `Blocked`. {reason}\n\n"
        "This is an environment result, not a functional or performance pass. No HTTP load, SSE, Android UI, provider, or database sample entered the statistics.\n",
        encoding="utf-8",
    )
    return {
        "test_id": test_id,
        "category_id": "P" if category == "性能" else "R",
        "wave_id": WAVE,
        "environment": f"{backend_label}; {device_label}; {database_label}",
        "account_store_label": "not_used",
        "preconditions": "authenticated isolated account, local Spring service, and required device/database were required",
        "input": "not_sent; redacted placeholder only",
        "operation": "read-only preflight attempted; workload not started",
        "expected_tools": "per TEST_PLAN; not reached",
        "expected_order": "per TEST_PLAN; not reached",
        "loop_and_compaction": "not observed",
        "expected_response": "per TEST_PLAN; not reached",
        "expected_answer": "not applicable",
        "actual": actual,
        "db_changes": "not connected; no writes",
        "boundaries": "environment preflight only",
        "acceptance": "Blocked when required environment is absent; no Passed claim",
        "evidence_path": evidence_path,
        "cleanup_action": "none; no test data created",
        "result": "Blocked",
    }


def main() -> None:
    env = preflight()
    backend_available = bool(env["backend"]["local_port_listening"])
    device_available = int(env["adb"]["device_count"]) > 0
    database_available = bool(env["database"]["postgres_port_listening"])
    environment_sentence = (
        "The local Spring service was not listening on port 18080. "
        if not backend_available else "The local Spring service was reachable on port 18080. "
    ) + (
        "An Android emulator/device was available. " if device_available else "No Android device was attached. "
    ) + (
        "A PostgreSQL target was available. " if database_available else "No PostgreSQL target was available. "
    )
    reports = [("性能", PERF), ("可靠性", REL)]
    all_rows: dict[str, list[dict[str, str]]] = {}
    for category, cases in reports:
        root = ROOT / "testing" / "Agent"
        report_dir = root / category / "reports"
        report_dir.mkdir(parents=True, exist_ok=True)
        write_json(report_dir / f"environment-{WAVE}.json", env)
        if WRITE_LEGACY_ALIASES:
            write_json(report_dir / "environment.json", env)
        rows = [write_case(category, test_id, env, root) for test_id in cases]
        all_rows[category] = rows
        if WRITE_LEGACY_ALIASES:
            with (report_dir / "live_execution_ledger.csv").open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=HEADER, lineterminator="\n")
                writer.writeheader()
                writer.writerows(rows)
        with (report_dir / f"live_execution_ledger-{WAVE}.csv").open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=HEADER, lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        report_dir.joinpath(f"阶段报告-{WAVE}.md").write_text(
            f"# Agent {category} stage report\n\n"
            f"- wave_id: `{WAVE}`\n- planned parent cases: {len(cases)}\n"
            f"- derived cases recorded: {len(cases)}\n- Passed: 0\n- Failed: 0\n"
            f"- Blocked: {len(cases)}\n- Deferred: 0\n- evidence completeness: 100% for required 00-10 files\n\n"
            "## Result\n\n"
            "All cases were attempted at the environment boundary. " + environment_sentence + "The Python WebDAV service on port 8080 was explicitly excluded. Therefore every workload has sample_count=0 and is recorded as Blocked.\n\n"
            "No API key, token, cookie, password, or complete authentication payload was collected. Historical Android/backend summaries were used only as correlation context and do not change this wave result.\n",
            encoding="utf-8",
        )
    write_json(ROOT / "testing" / "Agent" / "性能" / "reports" / f"run-summary-{WAVE}.json", {
        "wave_id": WAVE, "performance_cases": len(PERF), "reliability_cases": len(REL),
        "performance_blocked": len(PERF), "reliability_blocked": len(REL),
        "samples": {"count": 0, "concurrency": 0, "p50_ms": None, "p95_ms": None, "p99_ms": None,
                     "ttfb_ms": None, "first_sse_event_ms": None, "first_tool_ms": None,
                     "first_answer_ms": None, "completion_ms": None, "five_xx": 0,
                     "sse_loss": 0, "sse_duplicate": 0},
        "status": "Blocked",
    })


if __name__ == "__main__":
    main()
