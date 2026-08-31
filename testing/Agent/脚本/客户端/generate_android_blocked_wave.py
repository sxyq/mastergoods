#!/usr/bin/env python3
import csv
import json
import shutil
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo


WAVE_ID = "20260830-luna-android-wave-01"
SOURCE_HEAD = "c4ae9b86dc8582e48613b43d6318c8ef3e02204e"
SERVICE_URL = "https://zhj-api.sxyq27.online/"
BLOCK_REASON = (
    "已批准开发 fixture 未通过安全输入通道提供；未从日志、数据库、进程、网络或历史命令读取认证信息"
)

FLOW_DETAILS = [
    ("AG-CLI-AND-001", "登录与会话列表", "Blocked", BLOCK_REASON),
    ("AG-CLI-AND-002", "单只读工具流式", "Deferred", "认证及 owner/store 会话未建立"),
    ("AG-CLI-AND-003", "多工具、Loop 与图表", "Deferred", "认证及 Agent run 前置未建立"),
    ("AG-CLI-AND-004", "草稿确认与拒绝", "Deferred", "认证及 Agent run 前置未建立"),
    ("AG-CLI-AND-005", "历史恢复", "Deferred", "没有可恢复的本批 Agent run"),
    ("AG-CLI-AND-006", "取消、断线与重连", "Deferred", "没有可取消或重连的本批 Agent run"),
    ("AG-CLI-AND-007", "上下文压缩", "Deferred", "没有达到上下文压缩条件的本批 Agent run"),
    ("AG-CLI-AND-008", "错误与重试", "Deferred", "未建立认证会话，未进入 Agent 错误矩阵"),
    ("AG-CLI-AND-009", "前后台切换", "Deferred", "没有运行中的本批 Agent run"),
    ("AG-CLI-AND-010", "生图草稿确认", "Deferred", "认证、Agent run 与 Provider 前置未建立"),
]

REQUIRED_FILES = [
    "00-environment.md",
    "01-input-redacted.json",
    "02-http-response.json",
    "03-raw-sse.log",
    "04-tool-trace.jsonl",
    "05-run-audit.json",
    "06-database-before.json",
    "07-database-after.json",
    "08-app-observation.md",
    "09-cleanup.json",
    "10-conclusion.md",
]


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def write_json(path: Path, payload: object) -> None:
    write_text(path, json.dumps(payload, ensure_ascii=False, indent=2))


def main() -> None:
    repo_root = Path(__file__).resolve().parents[4]
    client_root = repo_root / "testing" / "Agent" / "客户端"
    artifact_root = client_root / "artifacts"
    report_root = client_root / "reports"
    log_root = client_root / "logs"
    tool_source = report_root / "20260830-terra-android-wave-01-tool-status.csv"
    captured_at = datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(timespec="seconds")

    flow_dirs = [artifact_root / f"{WAVE_ID}-{test_id}" for test_id, _, _, _ in FLOW_DETAILS]
    report_path = report_root / f"{WAVE_ID}.md"
    flow_ledger_path = report_root / f"{WAVE_ID}-flow-status.csv"
    tool_ledger_path = report_root / f"{WAVE_ID}-tool-status.csv"
    environment_path = report_root / f"environment-{WAVE_ID}.json"
    summary_path = report_root / f"run-summary-{WAVE_ID}.json"
    log_path = log_root / f"{WAVE_ID}-app-redacted.log"

    targets = flow_dirs + [
        report_path,
        flow_ledger_path,
        tool_ledger_path,
        environment_path,
        summary_path,
        log_path,
    ]
    existing = [str(path) for path in targets if path.exists()]
    if existing:
        raise SystemExit("refusing to overwrite existing evidence: " + ", ".join(existing))

    environment = {
        "wave_id": WAVE_ID,
        "captured_at": captured_at,
        "requested_model": "gpt-5.6-luna / max",
        "runtime_model_identifier_independently_verified": False,
        "source_head": SOURCE_HEAD,
        "branch": "codex/publish-local-updates",
        "preexisting_worktree_changes_preserved": True,
        "preexisting_changed_or_untracked_path_count": 1831,
        "device": {
            "avd": "Zhihuiji_API34",
            "serial": "emulator-5554",
            "boot_completed": True,
            "android": "14",
            "api": 34,
            "display": "720x1280",
            "density": 320,
        },
        "apk": {
            "package": "com.zhihuiji.app",
            "variant": "debug",
            "version_name": "1.0.0",
            "version_code": 1,
            "target_sdk": 35,
            "build_command": "./gradlew :app:assembleDebug --console=plain",
            "build_result": "Passed; BUILD SUCCESSFUL; 648 tasks up-to-date",
            "install_result": "Passed; adb install -r returned Success",
            "activity": "com.zhihuiji.app/.MainActivity",
        },
        "service": {
            "app_url": SERVICE_URL,
            "host_anonymous_probe_http_status": 403,
            "reachable": True,
        },
        "authentication": {
            "login_submitted": False,
            "owner_session_established": False,
            "store_session_established": False,
            "reason": BLOCK_REASON,
        },
        "real_request_counts": {
            "host_anonymous_probe": 1,
            "app_login": 0,
            "agent": 0,
            "provider": 0,
        },
    }
    write_json(environment_path, environment)

    with flow_ledger_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            [
                "test_id",
                "flow",
                "result",
                "app_login_requests",
                "agent_requests",
                "tool_calls",
                "reason",
                "evidence_path",
            ]
        )
        for test_id, flow_name, result, reason in FLOW_DETAILS:
            writer.writerow(
                [
                    test_id,
                    flow_name,
                    result,
                    0,
                    0,
                    0,
                    reason,
                    f"testing/Agent/客户端/artifacts/{WAVE_ID}-{test_id}/10-conclusion.md",
                ]
            )

    with tool_source.open(encoding="utf-8", newline="") as source_handle:
        tool_rows = list(csv.DictReader(source_handle))
    if len(tool_rows) != 61:
        raise SystemExit(f"expected 61 tools in source ledger, found {len(tool_rows)}")
    with tool_ledger_path.open("w", encoding="utf-8", newline="") as target_handle:
        fieldnames = ["test_id", "tool_class", "tool_name", "expected", "actual", "result", "evidence_path"]
        writer = csv.DictWriter(target_handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in tool_rows:
            evidence_flow = "AG-CLI-AND-002" if row["tool_class"] == "READ_ONLY" else "AG-CLI-AND-004"
            if row["tool_name"] == "image_generate":
                evidence_flow = "AG-CLI-AND-010"
            writer.writerow(
                {
                    "test_id": row["test_id"],
                    "tool_class": row["tool_class"],
                    "tool_name": row["tool_name"],
                    "expected": row["expected"],
                    "actual": "not reached; authenticated owner/store session unavailable",
                    "result": "Deferred",
                    "evidence_path": (
                        f"testing/Agent/客户端/artifacts/{WAVE_ID}-{evidence_flow}/10-conclusion.md"
                    ),
                }
            )

    for test_id, flow_name, result, reason in FLOW_DETAILS:
        flow_dir = artifact_root / f"{WAVE_ID}-{test_id}"
        write_text(
            flow_dir / "00-environment.md",
            f"""# {test_id} environment

- wave_id: `{WAVE_ID}`
- captured_at: `{captured_at}`
- source_head: `{SOURCE_HEAD}`
- requested runner: `gpt-5.6-luna / max`; runtime identifier未独立暴露
- device: `Zhihuiji_API34` / `emulator-5554` / Android 14 / API 34 / 720x1280 / density 320
- device state: `sys.boot_completed=1`; AVD 在本批安装、启动和 UI 采集期间在线
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; Activity `com.zhihuiji.app/.MainActivity`
- build: `./gradlew :app:assembleDebug --console=plain` -> `BUILD SUCCESSFUL`
- install: `adb install -r` -> `Success`
- service URL: `{SERVICE_URL}`; host anonymous probe -> HTTP 403
- login submitted: `false`; owner/store session: `false/false`
- pre-existing worktree files: preserved and excluded from this batch
- flow: `{flow_name}`
- status: `{result}`
- reason: {reason}
""",
        )
        write_json(
            flow_dir / "01-input-redacted.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "input_sent": False,
                "input": None,
                "redaction": "No credential or Agent prompt was read, printed, saved, or submitted.",
                "reason": reason,
            },
        )
        write_json(
            flow_dir / "02-http-response.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "app_login_requests": 0,
                "agent_requests": 0,
                "provider_requests": 0,
                "response": None,
                "host_anonymous_service_probe": (
                    {"url": SERVICE_URL, "http_status": 403, "response_body_saved": False}
                    if test_id == "AG-CLI-AND-001"
                    else None
                ),
                "reason": reason,
            },
        )
        write_text(
            flow_dir / "03-raw-sse.log",
            f"NOT_REACHED test_id={test_id} status={result} reason=authenticated_agent_run_unavailable",
        )
        write_text(
            flow_dir / "04-tool-trace.jsonl",
            json.dumps(
                {
                    "wave_id": WAVE_ID,
                    "test_id": test_id,
                    "status": result,
                    "tool_calls": 0,
                    "reason": reason,
                },
                ensure_ascii=False,
            ),
        )
        write_json(
            flow_dir / "05-run-audit.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "queried": False,
                "run_id": None,
                "records": [],
                "reason": "No Agent run was created in this batch.",
            },
        )
        write_json(
            flow_dir / "06-database-before.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "queried": False,
                "snapshot": None,
                "reason": "No authenticated flow or approved business-data observation was reached.",
            },
        )
        write_json(
            flow_dir / "07-database-after.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "queried": False,
                "snapshot": None,
                "business_writes": 0,
                "reason": "No Agent run or business write was executed.",
            },
        )
        attachment_note = (
            "- retained UI evidence: `08-login-screen.png`, `08-ui-tree-redacted.xml`\n"
            "- observation: login fields were empty; login action was present; no credential was entered"
            if test_id == "AG-CLI-AND-001"
            else (
                f"- retained UI evidence: see `{WAVE_ID}-AG-CLI-AND-001/`; "
                "this flow did not begin"
            )
        )
        write_text(
            flow_dir / "08-app-observation.md",
            f"""# {test_id} app observation

- result: `{result}`
- device remained online while the App login screen was inspected
- flow-specific UI or Agent state was not reached: {reason}
{attachment_note}
- no screenshot containing entered credentials was captured
""",
        )
        write_json(
            flow_dir / "09-cleanup.json",
            {
                "wave_id": WAVE_ID,
                "test_id": test_id,
                "business_data_created": False,
                "provider_called": False,
                "cleanup_actions": [],
                "emulator_left_running": True,
                "reason": "No business or Provider side effect was created.",
            },
        )
        write_text(
            flow_dir / "10-conclusion.md",
            f"""# {test_id} conclusion

- flow: {flow_name}
- result: `{result}`
- real App login requests: `0`
- real Agent requests: `0`
- tool calls: `0`
- database observations: `0`
- reason: {reason}
- no pass is claimed from historical Android, Web, backend, or unit-test evidence
""",
        )
        missing = [name for name in REQUIRED_FILES if not (flow_dir / name).is_file()]
        if missing:
            raise SystemExit(f"missing required files for {test_id}: {missing}")

    shutil.copyfile(
        "/tmp/master-goods-luna-login-top-ui-redacted.xml",
        flow_dirs[0] / "08-ui-tree-redacted.xml",
    )
    shutil.copyfile(
        "/tmp/master-goods-luna-login-screen.png",
        flow_dirs[0] / "08-login-screen.png",
    )
    shutil.copyfile("/tmp/master-goods-luna-app-redacted.log", log_path)

    flow_rows = "\n".join(
        f"| `{test_id}` | {flow_name} | `{result}` | {reason} |"
        for test_id, flow_name, result, reason in FLOW_DETAILS
    )
    write_text(
        report_path,
        f"""# Android Agent Luna live test report

- wave_id: `{WAVE_ID}`
- requested model: `gpt-5.6-luna / max`; runtime未暴露可独立核对的模型标识
- source commit: `{SOURCE_HEAD}`
- device: `Zhihuiji_API34`, `emulator-5554`, Android 14/API 34, 720x1280
- app: `com.zhihuiji.app` debug `1.0.0 (1)`; current-worktree build and install passed
- App service URL: `{SERVICE_URL}`; host anonymous reachability probe returned HTTP 403

## Result

| scope | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|
| AVD boot and stable access | 1 | 0 | 0 | 0 |
| APK build/install/launch | 3 | 0 | 0 | 0 |
| App login and owner/store session | 0 | 0 | 1 | 0 |
| AG-CLI-AND-001..010 | 0 | 0 | 1 | 9 |
| Agent tools: 46 READ_ONLY + 15 CREATE_ONLY | 0 | 0 | 0 | 61 |

## Real requests

| request type | count | note |
|---|---:|---|
| host anonymous service probe | 1 | HTTP 403; response body not retained |
| App login | 0 | fixture did not enter the App |
| Agent | 0 | no authenticated run |
| tool | 0 | no tool event |
| image Provider | 0 | image flow not reached |

## Client flows

| test_id | flow | result | reason |
|---|---|---|---|
{flow_rows}

## Evidence

- per-flow directories: `testing/Agent/客户端/artifacts/{WAVE_ID}-AG-CLI-AND-001/` through `...-010/`
- flow ledger: `testing/Agent/客户端/reports/{WAVE_ID}-flow-status.csv`
- 61-tool ledger: `testing/Agent/客户端/reports/{WAVE_ID}-tool-status.csv`
- environment: `testing/Agent/客户端/reports/environment-{WAVE_ID}.json`
- run summary: `testing/Agent/客户端/reports/run-summary-{WAVE_ID}.json`
- redacted app logcat: `testing/Agent/客户端/logs/{WAVE_ID}-app-redacted.log`

The approved development fixture was not exposed through a safe input channel in this runtime. No credential, Cookie, Token, Authorization value, password, private key, API key, or complete authentication payload was read or retained. Historical 20260829 and Terra outcomes remain separate evidence.
""",
    )

    write_json(
        summary_path,
        {
            "wave_id": WAVE_ID,
            "captured_at": captured_at,
            "device_started": True,
            "apk_built": True,
            "apk_installed": True,
            "app_launched": True,
            "login_submitted": False,
            "owner_session_established": False,
            "store_session_established": False,
            "real_requests": {"host_anonymous_probe": 1, "app_login": 0, "agent": 0, "provider": 0},
            "flows": {"passed": 0, "failed": 0, "blocked": 1, "deferred": 9},
            "tools": {"passed": 0, "failed": 0, "blocked": 0, "deferred": 61},
            "required_flow_files": {"expected": 110, "present": 110},
            "sensitive_auth_material_retained": False,
        },
    )

    print(f"generated {len(flow_dirs)} flow directories and {len(tool_rows)} tool rows for {WAVE_ID}")


if __name__ == "__main__":
    main()
