#!/usr/bin/env python3
"""Generate the 20260830 Luna blocked Agent performance evidence wave.

The script performs metadata-only preflight probes. It never inspects an
authorization value, sends an authenticated request, or runs a workload.
Every output path is new and opened in exclusive-create mode.
"""

from __future__ import annotations

import csv
import datetime as dt
import json
import platform
import shutil
import socket
import subprocess
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


ROOT = Path(__file__).resolve().parents[4]
WAVE = "20260830-luna-perf-reliability-blocked-01"
ADB = Path("/Users/sunyiyang/Library/Android/sdk/platform-tools/adb")
PACKAGE = "com.zhihuiji.app"

PERFORMANCE_CASES: Dict[str, str] = {
    "AG-P-001": "单用户冷启动与热请求基线",
    "AG-P-002": "单只读工具时延与查询数",
    "AG-P-003": "参数复杂度与结果规模",
    "AG-P-004": "创建草稿时延与写入边界",
    "AG-P-005": "确认写入时延与事务结果",
    "AG-P-006": "TTFB、首 SSE、首工具和首回答",
    "AG-P-007": "SSE 完整流、丢失、重复和终态",
    "AG-P-008": "多工具 Loop 轮次与耗时",
    "AG-P-009": "短会话上下文构建",
    "AG-P-010": "上下文压缩触发与耗时",
    "AG-P-011": "超长问题拒绝与资源峰值",
    "AG-P-012": "并发 1/5/10/20 吞吐与资源",
    "AG-P-013": "同一会话并发与顺序",
    "AG-P-014": "草稿确认竞争",
    "AG-P-015": "重复请求、重复付款与重复写入",
    "AG-P-016": "取消时延与残余事件",
    "AG-P-017": "断线重连、补发与去重",
    "AG-P-018": "结果块规模与客户端解析",
    "AG-P-019": "Provider 慢响应、超时与重试",
    "AG-P-020": "搜索上限与响应规模",
    "AG-P-021": "PostgreSQL 分页与查询计划",
    "AG-P-022": "长会话 Soak 与资源回落",
    "AG-P-023": "Android Agent 聚焦流程性能",
    "AG-P-024": "iOS Agent 展示性能",
    "AG-P-025": "记忆召回与提取耗时",
    "AG-P-026": "多模态图片性能",
    "AG-P-027": "Agent 生图资源与并发",
}

RELIABILITY_CASES: Dict[str, str] = {
    "AG-R-001": "Provider 超时收敛",
    "AG-R-002": "Provider 429 与有限重试",
    "AG-R-003": "空响应与非法 JSON",
    "AG-R-004": "流中断线与恢复",
    "AG-R-005": "客户端早 EOF 与资源释放",
    "AG-R-006": "取消与 Provider 中断竞争",
    "AG-R-007": "重复事件与消息重试",
    "AG-R-008": "Last-Event-ID 重连",
    "AG-R-009": "压缩失败降级",
    "AG-R-010": "确认中断与事务回滚",
    "AG-R-011": "异常 JSON 工具参数",
    "AG-R-012": "慢速下游与超时窗口",
    "AG-R-013": "生图 Provider 故障收敛",
}

SECURITY_LOAD_CASES: Dict[str, str] = {
    "AG-S-004": "会话跨 owner 访问负载",
    "AG-S-005": "草稿跨 owner 访问负载",
    "AG-S-006": "run 跨 owner 访问与取消负载",
    "AG-S-007": "跨 store 参数负载",
    "AG-S-008": "owner 参数伪造负载",
    "AG-S-015": "绕过草稿确认的写入负载",
    "AG-S-016": "草稿确认重放与并发",
    "AG-S-017": "付款幂等冲突与并发",
    "AG-S-021": "SSE 事件篡改与串线",
    "AG-S-022": "错误响应敏感信息检查",
    "AG-S-023": "并发与故障场景审计完整性",
    "AG-S-024": "并发账号与门店切换",
    "AG-S-025": "上下文压缩脱敏",
    "AG-S-026": "写入频率限制负载",
    "AG-S-027": "否定写入语义长会话",
    "AG-S-031": "生图确认、跨域引用与故障负载",
}

LEDGER_HEADER = [
    "test_id",
    "category_id",
    "wave_id",
    "environment",
    "account_store_label",
    "preconditions",
    "input",
    "operation",
    "expected_tools",
    "expected_order",
    "loop_and_compaction",
    "expected_response",
    "expected_answer",
    "actual",
    "db_changes",
    "boundaries",
    "acceptance",
    "evidence_path",
    "cleanup_action",
    "result",
]


def command_output(command: List[str], timeout: float = 5.0) -> str:
    try:
        completed = subprocess.run(
            command,
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
        return completed.stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return ""


def port_listening(port: int) -> bool:
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=0.5):
            return True
    except OSError:
        return False


def anonymous_status(url: str) -> int | None:
    request = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code
    except (urllib.error.URLError, TimeoutError, OSError):
        return None


def first_matching_value(text: str, prefix: str) -> str | None:
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith(prefix):
            return stripped[len(prefix) :].strip()
    return None


def safe_device_value(value: str) -> str:
    return "".join(char if char.isalnum() or char in "._ -" else "_" for char in value)


def android_preflight() -> Dict[str, object]:
    result: Dict[str, object] = {
        "adb_present": ADB.exists(),
        "device_count": 0,
        "emulator_count": 0,
        "physical_count": 0,
        "selected_device_label": None,
        "boot_completed": False,
        "api_level": None,
        "android_release": None,
        "model": None,
        "physical_size": None,
        "package": PACKAGE,
        "package_installed": False,
        "version_name": None,
        "version_code": None,
        "debuggable": None,
        "main_activity_resolved": False,
        "app_process_running": False,
        "ui_inspected": False,
        "focused_agent_flow_run_count": 0,
        "perfetto_count": 0,
        "gfxinfo_count": 0,
        "meminfo_count": 0,
        "simpleperf_count": 0,
    }
    if not ADB.exists():
        return result

    device_lines = command_output([str(ADB), "devices"]).splitlines()[1:]
    serials = []
    for line in device_lines:
        fields = line.split()
        if len(fields) >= 2 and fields[1] == "device":
            serials.append(fields[0])
    result["device_count"] = len(serials)
    result["emulator_count"] = sum(serial.startswith("emulator-") for serial in serials)
    result["physical_count"] = len(serials) - int(result["emulator_count"])
    if len(serials) != 1:
        return result

    serial = serials[0]
    result["selected_device_label"] = "emulator-redacted-01" if serial.startswith("emulator-") else "physical-redacted-01"
    result["boot_completed"] = command_output([str(ADB), "-s", serial, "shell", "getprop", "sys.boot_completed"]) == "1"
    result["api_level"] = safe_device_value(command_output([str(ADB), "-s", serial, "shell", "getprop", "ro.build.version.sdk"]))
    result["android_release"] = safe_device_value(command_output([str(ADB), "-s", serial, "shell", "getprop", "ro.build.version.release"]))
    result["model"] = safe_device_value(command_output([str(ADB), "-s", serial, "shell", "getprop", "ro.product.model"]))
    size_output = command_output([str(ADB), "-s", serial, "shell", "wm", "size"])
    result["physical_size"] = safe_device_value(first_matching_value(size_output, "Physical size:") or "") or None
    result["package_installed"] = bool(command_output([str(ADB), "-s", serial, "shell", "pm", "path", PACKAGE]))
    if not result["package_installed"]:
        return result

    package_dump = command_output([str(ADB), "-s", serial, "shell", "dumpsys", "package", PACKAGE])
    result["version_name"] = safe_device_value(first_matching_value(package_dump, "versionName=") or "") or None
    version_line = next((line.strip() for line in package_dump.splitlines() if "versionCode=" in line), "")
    version_code = next((field.split("=", 1)[1] for field in version_line.split() if field.startswith("versionCode=")), None)
    result["version_code"] = safe_device_value(version_code or "") or None
    result["debuggable"] = "DEBUGGABLE" in package_dump
    resolved = command_output([str(ADB), "-s", serial, "shell", "cmd", "package", "resolve-activity", "--brief", PACKAGE])
    result["main_activity_resolved"] = any(line.startswith(PACKAGE + "/") for line in resolved.splitlines())
    result["app_process_running"] = bool(command_output([str(ADB), "-s", serial, "shell", "pidof", PACKAGE]))
    return result


def preflight() -> Dict[str, object]:
    local_service = port_listening(18080)
    postgres = port_listening(5432)
    return {
        "captured_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "wave_id": WAVE,
        "requested_executor": "gpt-5.6-luna / max",
        "source_commit": command_output(["git", "rev-parse", "HEAD"]),
        "branch": command_output(["git", "branch", "--show-current"]),
        "worktree_dirty": bool(command_output(["git", "status", "--porcelain"])),
        "host": {
            "os": platform.platform(),
            "python": platform.python_version(),
            "java_first_line": command_output(["java", "-version"]).splitlines()[:1],
        },
        "service": {
            "local_agent_base_url": "http://127.0.0.1:18080",
            "local_agent_port_listening": local_service,
            "local_health_anonymous_status": anonymous_status("http://127.0.0.1:18080/actuator/health"),
            "port_8080_listening": port_listening(8080),
            "port_8080_health_anonymous_status": anonymous_status("http://127.0.0.1:8080/actuator/health"),
            "port_8080_excluded": True,
            "remote_root": "https://zhj-api.sxyq27.online/",
            "remote_root_anonymous_status": anonymous_status("https://zhj-api.sxyq27.online/"),
            "remote_approved_for_load": False,
        },
        "authorization_session": {
            "approved_session_supplied": False,
            "authentication_values_inspected": False,
            "account_store_labels_available": False,
        },
        "database": {
            "postgres_port_listening": postgres,
            "psql_present": shutil.which("psql") is not None,
            "pg_isready_present": shutil.which("pg_isready") is not None,
            "approved_postgres_target": False,
            "h2_used_as_substitute": False,
            "production_query_plan_executed": False,
        },
        "provider": {
            "request_sent": False,
            "approved_isolated_target": False,
            "real_provider_load_executed": False,
        },
        "android": android_preflight(),
        "sample_policy": {
            "sample_count": 0,
            "valid_request_count": 0,
            "executed_concurrency_levels": [],
            "requested_concurrency_levels": [1, 5, 10, 20],
            "p50_ms": None,
            "p95_ms": None,
            "p99_ms": None,
            "ttfb_ms": None,
            "first_sse_ms": None,
            "first_tool_ms": None,
            "first_answer_ms": None,
            "completion_ms": None,
            "tool_duration_ms": None,
            "error_rate": None,
            "five_xx_count": 0,
            "sse_loss_count": 0,
            "sse_duplicate_count": 0,
            "sse_loss_rate": None,
            "sse_duplicate_rate": None,
            "jvm_samples": 0,
            "thread_samples": 0,
            "connection_pool_samples": 0,
            "database_query_samples": 0,
        },
    }


def retry_condition(test_id: str) -> str:
    service_session = "启动与 source_commit 对应的隔离 Agent 服务，提供获批测试会话和脱敏 owner/store 标签后重试。"
    if test_id in {"AG-P-004", "AG-P-005", "AG-P-014", "AG-P-015", "AG-R-010", "AG-S-015", "AG-S-016", "AG-S-017", "AG-S-026"}:
        return service_session + " 另需可清理的写入数据、事务观测和重复提交控制。"
    if test_id in {"AG-P-019", "AG-P-020", "AG-P-026", "AG-P-027", "AG-R-001", "AG-R-002", "AG-R-003", "AG-R-009", "AG-R-013", "AG-S-031"}:
        return service_session + " 另需获批的隔离 Provider Mock；真实 Provider 需单独批准。"
    if test_id == "AG-P-021":
        return "提供获批 PostgreSQL 目标、脱敏数据规模和查询计数后重试；生产查询计划需单独批准。"
    if test_id == "AG-P-023":
        return "在当前模拟器上提供可达的匹配服务和获批 App 会话，选定一个 Agent 流程并完成 10 次独立运行后采集 Perfetto、gfxinfo 或 meminfo。"
    if test_id == "AG-P-024":
        return "提供可用 iOS 设备、Xcode 运行条件、匹配服务和获批 App 会话后重试。"
    if test_id in {"AG-S-004", "AG-S-005", "AG-S-006", "AG-S-007", "AG-S-008", "AG-S-024"}:
        return "提供获批的双 owner、双 store 隔离数据和两组脱敏会话，并在隔离服务上执行交错负载。"
    if test_id == "AG-S-025":
        return service_session + " 仅使用合成敏感标记触发压缩并检查脱敏结果。"
    return service_session


def deferred_scope(test_id: str) -> List[str]:
    scopes: List[str] = []
    if test_id in {"AG-P-019", "AG-P-026", "AG-P-027", "AG-R-001", "AG-R-002", "AG-R-003", "AG-R-013", "AG-S-031"}:
        scopes.append("真实 Provider：Deferred，等待隔离环境、费用和调用批准。")
    if test_id == "AG-P-021":
        scopes.append("生产 PostgreSQL EXPLAIN/EXPLAIN ANALYZE：Deferred，等待批准目标和维护窗口。")
    if test_id in {"AG-S-004", "AG-S-005", "AG-S-006", "AG-S-007", "AG-S-008", "AG-S-024"}:
        scopes.append("真实跨 owner/store：Deferred，等待获批的双身份与隔离数据。")
    return scopes


def blocker(test_id: str, env: Dict[str, object]) -> str:
    android = env["android"]
    if test_id == "AG-P-023":
        if int(android["device_count"]) == 0:
            return "没有 Android 设备，无法进入聚焦 Agent 流程。"
        return "模拟器和 App 已就绪；匹配的 Agent 服务与获批 App 会话缺失，聚焦流程无法开始。"
    if test_id == "AG-P-024":
        return "本批没有可用 iOS 设备、Xcode 运行条件和获批 App 会话。"
    if test_id == "AG-P-021":
        return "没有 PostgreSQL 服务或获批目标；未使用 H2 替代。"
    if test_id in {"AG-S-004", "AG-S-005", "AG-S-006", "AG-S-007", "AG-S-008", "AG-S-024"}:
        return "没有获批的双 owner/store 会话和隔离数据，跨域负载未启动。"
    if test_id in {"AG-P-019", "AG-P-020", "AG-P-026", "AG-P-027", "AG-R-001", "AG-R-002", "AG-R-003", "AG-R-009", "AG-R-013", "AG-S-031"}:
        return "本地 Agent 服务、获批会话和隔离 Provider Mock 均未就绪。"
    return "本地 Agent 服务未监听，且没有获批测试会话；有效请求无法发送。"


def create_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as handle:
        handle.write(content)


def create_json(path: Path, value: object) -> None:
    create_text(path, json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def ensure_targets_absent(paths: Iterable[Path]) -> None:
    existing = [str(path.relative_to(ROOT)) for path in paths if path.exists()]
    if existing:
        raise RuntimeError("Refusing to overwrite existing Luna outputs: " + ", ".join(existing))


def evidence_root(category: str, test_id: str) -> Path:
    return ROOT / "testing" / "Agent" / category / "artifacts" / f"{WAVE}-{test_id}"


def report_path(category: str, filename: str) -> Path:
    return ROOT / "testing" / "Agent" / category / "reports" / filename


def log_path(category: str) -> Path:
    return ROOT / "testing" / "Agent" / category / "logs" / f"{WAVE}-preflight.redacted.log"


def environment_summary(env: Dict[str, object]) -> str:
    service = env["service"]
    database = env["database"]
    android = env["android"]
    return (
        f"source={env['source_commit']}; local_service={service['local_agent_port_listening']}; "
        f"approved_session=False; postgres={database['postgres_port_listening']}; "
        f"android_devices={android['device_count']}"
    )


def write_case(category: str, category_id: str, test_id: str, objective: str, env: Dict[str, object]) -> Dict[str, str]:
    case_dir = evidence_root(category, test_id)
    reason = blocker(test_id, env)
    retry = retry_condition(test_id)
    deferred = deferred_scope(test_id)
    relative = str(case_dir.relative_to(ROOT))
    android = env["android"]

    create_text(
        case_dir / "00-environment.md",
        "# 环境\n\n"
        f"- test_id: `{test_id}`\n"
        f"- wave_id: `{WAVE}`\n"
        f"- source_commit: `{env['source_commit']}`\n"
        "- requested_executor: `gpt-5.6-luna / max`\n"
        f"- result: `Blocked`\n"
        "- sample_count: `0`\n"
        f"- local_agent_service: `{'available' if env['service']['local_agent_port_listening'] else 'unavailable'}`\n"
        "- approved_authorization_session: `unavailable`\n"
        f"- PostgreSQL: `{'available' if env['database']['postgres_port_listening'] else 'unavailable'}`\n"
        f"- Android: device_count={android['device_count']}, package_installed={android['package_installed']}, focused_flow_run_count=0\n"
        f"- blocker: {reason}\n"
        f"- retry_condition: {retry}\n"
        f"- deferred_scope: {'；'.join(deferred) if deferred else 'none'}\n"
        "- authentication_values_inspected: `false`\n",
    )
    create_json(
        case_dir / "01-input-redacted.json",
        {
            "test_id": test_id,
            "sample_count": 0,
            "input_sent": False,
            "input_summary": "前置条件不足，未发送测试输入。",
            "authentication_values_inspected": False,
        },
    )
    create_json(
        case_dir / "02-http-response.json",
        {
            "test_id": test_id,
            "sample_count": 0,
            "request_sent": False,
            "http_status": None,
            "response_body_collected": False,
            "error_rate": None,
            "five_xx_count": 0,
            "blocker": reason,
        },
    )
    create_text(
        case_dir / "03-raw-sse.log",
        f"test_id={test_id}\nwave_id={WAVE}\nsample_count=0\nrequest_sent=false\nraw_sse_collected=false\nblocker={reason}\n",
    )
    create_text(
        case_dir / "04-tool-trace.jsonl",
        json.dumps(
            {
                "test_id": test_id,
                "sample_count": 0,
                "tool_calls": 0,
                "tool_duration_ms": None,
                "loop_rounds": None,
                "compaction_ms": None,
                "blocker": reason,
            },
            ensure_ascii=False,
        )
        + "\n",
    )
    create_json(
        case_dir / "05-run-audit.json",
        {
            "test_id": test_id,
            "sample_count": 0,
            "run_created": False,
            "audit_available": False,
            "terminal_status": None,
            "event_loss_count": 0,
            "duplicate_event_count": 0,
            "event_loss_rate": None,
            "duplicate_event_rate": None,
            "blocker": reason,
        },
    )
    create_json(
        case_dir / "06-database-before.json",
        {
            "test_id": test_id,
            "sample_count": 0,
            "database_connected": False,
            "postgresql_target_approved": False,
            "query_count": None,
            "business_rows": None,
            "draft_rows": None,
        },
    )
    create_json(
        case_dir / "07-database-after.json",
        {
            "test_id": test_id,
            "sample_count": 0,
            "database_connected": False,
            "query_count": None,
            "business_rows": None,
            "draft_rows": None,
            "writes_performed": False,
        },
    )
    if test_id == "AG-P-023":
        app_observation = (
            "# Android App 观察\n\n"
            f"- device_label: `{android['selected_device_label'] or 'none'}`\n"
            f"- Android/API: `{android['android_release'] or 'NA'}` / `{android['api_level'] or 'NA'}`\n"
            f"- model/size: `{android['model'] or 'NA'}` / `{android['physical_size'] or 'NA'}`\n"
            f"- package/version: `{PACKAGE}` / `{android['version_name'] or 'NA'} ({android['version_code'] or 'NA'})`\n"
            f"- build_debuggable: `{android['debuggable']}`\n"
            f"- app_process_running: `{android['app_process_running']}`\n"
            "- focused_flow: `Android Agent 对话冷/热进入`\n"
            "- run_count: `0`\n"
            "- Perfetto/gfxinfo/meminfo/Simpleperf: `not_collected`\n"
            f"- blocker: {reason}\n"
            "- caveat: 空闲进程和登录页不计入 Agent 流程性能样本。\n"
        )
    else:
        app_observation = (
            "# App 观察\n\n"
            "本用例没有可执行的已授权 Agent 流程。App 性能样本未采集。\n\n"
            f"- sample_count: `0`\n- blocker: {reason}\n"
        )
    create_text(case_dir / "08-app-observation.md", app_observation)
    create_json(
        case_dir / "09-cleanup.json",
        {
            "test_id": test_id,
            "cleanup_status": "not_needed",
            "test_data_created": False,
            "database_write_performed": False,
            "device_state_changed": False,
        },
    )
    create_text(
        case_dir / "10-conclusion.md",
        f"# {test_id} 结论\n\n"
        f"- objective: {objective}\n"
        "- result: `Blocked`\n"
        "- sample_count: `0`\n"
        "- metrics: P50/P95/P99、TTFB、首 SSE、首工具、首回答、完成时延、工具耗时和错误率均未采集。\n"
        f"- blocker: {reason}\n"
        f"- retry_condition: {retry}\n"
        f"- deferred_scope: {'；'.join(deferred) if deferred else 'none'}\n"
        "- evidence_note: 这是环境边界结果，不代表功能、性能、可靠性或安全通过。\n",
    )

    return {
        "test_id": test_id,
        "category_id": category_id,
        "wave_id": WAVE,
        "environment": environment_summary(env),
        "account_store_label": "not_assigned",
        "preconditions": retry,
        "input": "not_sent; redacted placeholder",
        "operation": "metadata-only preflight; workload not started",
        "expected_tools": "per TEST_PLAN; not reached",
        "expected_order": "per TEST_PLAN; not reached",
        "loop_and_compaction": "not observed",
        "expected_response": "per TEST_PLAN; not reached",
        "expected_answer": "not applicable",
        "actual": (
            "sample_count=0; valid_request_count=0; p50_ms=NA; p95_ms=NA; p99_ms=NA; "
            "ttfb_ms=NA; first_sse_ms=NA; first_tool_ms=NA; first_answer_ms=NA; "
            f"completion_ms=NA; error_rate=NA; blocker={reason}; retry_condition={retry}"
        ),
        "db_changes": "not connected; no reads or writes",
        "boundaries": "service/session/database/device/provider preconditions",
        "acceptance": "Blocked while required preconditions are unavailable; no pass claim",
        "evidence_path": relative,
        "cleanup_action": "none; no test data or workload created",
        "result": "Blocked",
    }


def create_csv(path: Path, rows: List[Dict[str, str]], header: List[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=header, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def write_preflight_log(path: Path, env: Dict[str, object]) -> None:
    service = env["service"]
    database = env["database"]
    android = env["android"]
    create_text(
        path,
        f"captured_at_utc={env['captured_at_utc']}\n"
        f"wave_id={WAVE}\n"
        f"source_commit={env['source_commit']}\n"
        f"local_agent_port_listening={str(service['local_agent_port_listening']).lower()}\n"
        f"local_health_anonymous_status={service['local_health_anonymous_status']}\n"
        f"port_8080_listening={str(service['port_8080_listening']).lower()}\n"
        f"port_8080_health_anonymous_status={service['port_8080_health_anonymous_status']}\n"
        f"remote_root_anonymous_status={service['remote_root_anonymous_status']}\n"
        "remote_approved_for_load=false\n"
        "approved_authorization_session=false\n"
        "authentication_values_inspected=false\n"
        f"postgres_port_listening={str(database['postgres_port_listening']).lower()}\n"
        "h2_used_as_substitute=false\n"
        f"android_device_count={android['device_count']}\n"
        f"android_emulator_count={android['emulator_count']}\n"
        f"android_boot_completed={str(android['boot_completed']).lower()}\n"
        f"android_package_installed={str(android['package_installed']).lower()}\n"
        "android_focused_agent_flow_run_count=0\n"
        "workload_sample_count=0\n",
    )


def focus_rows() -> List[Dict[str, str]]:
    mappings: List[Tuple[str, str, str]] = [
        ("单用户冷/热基线", "AG-P-001", "服务和授权会话就绪后分别执行冷、热样本。"),
        ("并发 1/5/10/20", "AG-P-012", "隔离服务、授权会话和资源指标接口就绪后重试。"),
        ("TTFB 与 SSE/工具/回答时延", "AG-P-001/002/006/007", "有效流式请求可发送后重试。"),
        ("SSE 丢失/重复与 Loop", "AG-P-007/008; AG-R-007/008", "可控 SSE 断点和审计接口就绪后重试。"),
        ("上下文压缩与长会话", "AG-P-009/010/011/022; AG-R-009", "长会话数据与可控压缩 Provider 就绪后重试。"),
        ("取消/重连/重试", "AG-P-016/017/019; AG-R-001/002/004/005/006/008/012", "故障注入和资源观测就绪后重试。"),
        ("草稿竞争与重复付款/写入", "AG-P-014/015; AG-R-010; AG-S-015/016/017/026", "可清理写入数据和获批会话就绪后重试。"),
        ("Provider 慢响应与 Soak", "AG-P-019/022; AG-R-001/002/003/012/013", "隔离 Provider Mock 和长时运行窗口就绪后重试。"),
        ("JVM/线程/连接池/查询数", "AG-P-001/002/012/021/022", "匹配服务、PostgreSQL 和指标接口就绪后重试。"),
        ("跨 owner/store 串扰", "AG-S-004/005/006/007/008/021/024", "获批双身份与隔离数据就绪后重试。"),
        ("Android Agent 聚焦流程", "AG-P-023", "匹配服务和获批 App 会话就绪后执行单一流程。"),
    ]
    return [
        {
            "requirement": requirement,
            "mapped_cases": cases,
            "result": "Blocked",
            "sample_count": "0",
            "retry_condition": retry,
        }
        for requirement, cases, retry in mappings
    ]


def stage_report(category: str, case_count: int, env: Dict[str, object], security_count: int = 0) -> str:
    android = env["android"]
    additional = (
        f"\n| 相关安全负载 | {security_count} | 0 | 0 | {security_count} | 0 |\n"
        if security_count
        else ""
    )
    return (
        f"# Agent {category} 阶段报告\n\n"
        f"- wave_id: `{WAVE}`\n"
        "- requested_executor: `gpt-5.6-luna / max`\n"
        f"- source_commit: `{env['source_commit']}`\n"
        "- evidence_mode: `环境边界与脱敏占位`\n\n"
        "## 当前需求与状态\n\n"
        "本批未开始有效负载。真实 sample_count=0。没有可用的本地 Agent 服务、获批授权会话或 PostgreSQL。Android 模拟器可用，匹配服务与 App 会话缺失。\n\n"
        "| 范围 | 总数 | Passed | Failed | Blocked | Deferred |\n"
        "|---|---:|---:|---:|---:|---:|\n"
        f"| {category}父用例 | {case_count} | 0 | 0 | {case_count} | 0 |\n"
        + additional
        + "\n## 本轮实际完成\n\n"
        "完成只读环境探测和逐用例 00-10 证据。并发 1/5/10/20、SSE、Provider、数据库与写入负载均未发送。\n\n"
        "## 修改或操作对象\n\n"
        f"新增本波次 `{category}/artifacts`、`{category}/reports` 和 `{category}/logs` 文件。未修改业务源码、迁移、生产配置或生产数据。\n\n"
        "## 验证结果\n\n"
        "| 指标 | 结果 |\n"
        "|---|---:|\n"
        "| valid_request_count | 0 |\n"
        "| P50/P95/P99 | NA |\n"
        "| TTFB/首 SSE/首工具/首回答/完成时延 | NA |\n"
        "| tool_duration/error_rate | NA |\n"
        "| Perfetto/gfxinfo/meminfo/Simpleperf | 0 份 |\n"
        f"| Android device/build/run count | {android['selected_device_label'] or 'none'} / {android['version_name'] or 'NA'} ({android['version_code'] or 'NA'}) / 0 |\n\n"
        "## 剩余工作与风险\n\n"
        "真实 Provider、生产 PostgreSQL 查询计划和真实跨 owner/store 负载保持 Deferred 子范围。各父用例当前为 Blocked，重试条件已写入台账和 10-conclusion.md。\n"
    )


def planned_targets() -> List[Path]:
    targets: List[Path] = []
    for test_id in PERFORMANCE_CASES:
        targets.append(evidence_root("性能", test_id))
    for test_id in list(RELIABILITY_CASES) + list(SECURITY_LOAD_CASES):
        targets.append(evidence_root("可靠性", test_id))
    report_names = [
        f"environment-{WAVE}.json",
        f"live_execution_ledger-{WAVE}.csv",
        f"阶段报告-{WAVE}.md",
    ]
    targets.extend(report_path("性能", name) for name in report_names)
    targets.extend(report_path("可靠性", name) for name in report_names)
    targets.extend(
        [
            report_path("性能", f"run-summary-{WAVE}.json"),
            report_path("性能", f"focus-coverage-{WAVE}.csv"),
            report_path("可靠性", f"security-load-ledger-{WAVE}.csv"),
            log_path("性能"),
            log_path("可靠性"),
        ]
    )
    return targets


def main() -> None:
    ensure_targets_absent(planned_targets())
    env = preflight()

    performance_rows = [
        write_case("性能", "P", test_id, objective, env)
        for test_id, objective in PERFORMANCE_CASES.items()
    ]
    reliability_rows = [
        write_case("可靠性", "R", test_id, objective, env)
        for test_id, objective in RELIABILITY_CASES.items()
    ]
    security_rows = [
        write_case("可靠性", "S", test_id, objective, env)
        for test_id, objective in SECURITY_LOAD_CASES.items()
    ]

    for category in ("性能", "可靠性"):
        create_json(report_path(category, f"environment-{WAVE}.json"), env)
        write_preflight_log(log_path(category), env)

    create_csv(
        report_path("性能", f"live_execution_ledger-{WAVE}.csv"),
        performance_rows,
        LEDGER_HEADER,
    )
    create_csv(
        report_path("可靠性", f"live_execution_ledger-{WAVE}.csv"),
        reliability_rows,
        LEDGER_HEADER,
    )
    create_csv(
        report_path("可靠性", f"security-load-ledger-{WAVE}.csv"),
        security_rows,
        LEDGER_HEADER,
    )
    create_csv(
        report_path("性能", f"focus-coverage-{WAVE}.csv"),
        focus_rows(),
        ["requirement", "mapped_cases", "result", "sample_count", "retry_condition"],
    )
    create_json(
        report_path("性能", f"run-summary-{WAVE}.json"),
        {
            "wave_id": WAVE,
            "source_commit": env["source_commit"],
            "requested_executor": "gpt-5.6-luna / max",
            "performance": {"total": 27, "Passed": 0, "Failed": 0, "Blocked": 27, "Deferred": 0},
            "reliability": {"total": 13, "Passed": 0, "Failed": 0, "Blocked": 13, "Deferred": 0},
            "related_security_load": {"total": 16, "Passed": 0, "Failed": 0, "Blocked": 16, "Deferred": 0},
            "deferred_external_scopes": {
                "real_provider": True,
                "production_postgresql_query_plan": True,
                "real_cross_owner_store": True,
            },
            "samples": env["sample_policy"],
            "android": {
                "device_label": env["android"]["selected_device_label"],
                "api_level": env["android"]["api_level"],
                "package": PACKAGE,
                "version_name": env["android"]["version_name"],
                "version_code": env["android"]["version_code"],
                "focused_flow_run_count": 0,
            },
            "overall_result": "Blocked",
        },
    )
    create_text(
        report_path("性能", f"阶段报告-{WAVE}.md"),
        stage_report("性能", len(PERFORMANCE_CASES), env),
    )
    create_text(
        report_path("可靠性", f"阶段报告-{WAVE}.md"),
        stage_report("可靠性", len(RELIABILITY_CASES), env, len(SECURITY_LOAD_CASES)),
    )


if __name__ == "__main__":
    main()
