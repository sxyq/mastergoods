#!/usr/bin/env python3
"""Capture Android AI home screenshot/UI-tree evidence without faking a pass.

The script intentionally marks evidence as partial/fail when the device is
locked, not in the app, or the UI tree does not show the AI home entry screen.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


DEFAULT_ADB = Path("/Users/sunyiyang/Library/Android/sdk/platform-tools/adb")
DEFAULT_OUTPUT_ROOT = Path("docs/acceptance-evidence/ai-agent")
DEFAULT_PACKAGE = "com.zhihuiji.app"
DEFAULT_ACTIVITY = "com.zhihuiji.app/.MainActivity"
DEFAULT_BACKEND_PORT = 18080

LOCKSCREEN_TERMS = (
    "手电筒",
    "拍照",
    "电池电量",
    "WLAN 信号",
    "5G信号",
)
AI_HOME_TITLE = "AI 助手"
AI_HOME_SECONDARY_ANCHORS = (
    "你好，我是智慧记 AI 助手",
    "主屏保持干净",
    "开始一次真实 Agent 对话",
    "已同步远端 Agent 状态",
    "远端工作台未同步，仅保留对话入口",
)
REPORT_DASHBOARD_FORBIDDEN_STRONG = (
    "销售额",
    "今日经营",
    "今日经营摘要",
    "经营摘要",
    "风险列表",
    "经营图表",
    "报表看板",
    "KPI",
    "排行",
    "热销商品",
    "应收排行",
    "销售趋势",
    "净现金流",
    "利润",
    "库存预警",
)


@dataclass
class CommandResult:
    args: list[str]
    returncode: int
    stdout: bytes
    stderr: bytes

    @property
    def text(self) -> str:
        output = self.stdout + self.stderr
        return output.decode("utf-8", errors="replace")


def utc_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def run(args: list[str], *, check: bool = False, timeout: float = 30) -> CommandResult:
    result = subprocess.run(args, capture_output=True, timeout=timeout, check=False)
    wrapped = CommandResult(args=args, returncode=result.returncode, stdout=result.stdout, stderr=result.stderr)
    if check and result.returncode != 0:
        raise RuntimeError(f"command failed ({result.returncode}): {' '.join(args)}\n{wrapped.text}")
    return wrapped


def adb_cmd(adb: Path, serial: str | None, *args: str) -> list[str]:
    cmd = [str(adb)]
    if serial:
        cmd.extend(["-s", serial])
    cmd.extend(args)
    return cmd


def clean_ui_xml(raw: str) -> str:
    start = raw.find("<?xml")
    end = raw.rfind("</hierarchy>")
    if start >= 0 and end >= 0:
        return raw[start : end + len("</hierarchy>")]
    return raw


def extract_ui_texts(xml_text: str) -> list[str]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return []
    texts: list[str] = []
    for node in root.iter("node"):
        for key in ("text", "content-desc"):
            value = (node.attrib.get(key) or "").strip()
            if value:
                texts.append(value)
    return texts


def contains_any(texts: list[str], terms: tuple[str, ...]) -> bool:
    joined = "\n".join(texts)
    return any(term in joined for term in terms)


def matching_terms(texts: list[str], terms: tuple[str, ...]) -> list[str]:
    joined = "\n".join(texts)
    return [term for term in terms if term in joined]


def status_from_texts(
    texts: list[str],
    package_seen: bool,
    *,
    screenshot_bytes: int = 1,
    device_locked: bool = False,
) -> tuple[str, str, list[str], list[str], list[str]]:
    lock_terms = matching_terms(texts, LOCKSCREEN_TERMS)
    forbidden_terms = matching_terms(texts, REPORT_DASHBOARD_FORBIDDEN_STRONG)
    positive_anchors = []
    if contains_any(texts, (AI_HOME_TITLE,)):
        positive_anchors.append(AI_HOME_TITLE)
    positive_anchors.extend(matching_terms(texts, AI_HOME_SECONDARY_ANCHORS))
    if device_locked or lock_terms:
        return (
            "blocked-by-locked-device",
            "Keyguard/window state or UI tree indicates the device is locked instead of showing app content.",
            lock_terms,
            forbidden_terms,
            positive_anchors,
        )
    if not package_seen:
        return (
            "partial-not-in-app",
            "The package was launched, but the current window evidence does not prove app content.",
            lock_terms,
            forbidden_terms,
            positive_anchors,
        )
    if screenshot_bytes <= 0:
        return (
            "partial-missing-screenshot",
            "The app/window checks ran, but the screenshot file is empty.",
            lock_terms,
            forbidden_terms,
            positive_anchors,
        )
    if AI_HOME_TITLE not in positive_anchors or len(positive_anchors) < 2:
        return (
            "partial-ai-home-not-detected",
            "App content was captured, but the AI home title plus hero anchors were not both detected.",
            lock_terms,
            forbidden_terms,
            positive_anchors,
        )
    if forbidden_terms:
        return (
            "fail-report-dashboard-content-visible",
            "AI home-like content was captured, but report/dashboard terms are visible on the initial screen.",
            lock_terms,
            forbidden_terms,
            positive_anchors,
        )
    return (
        "pass-for-ai-home-cleanliness",
        "UI tree contains AI home title and hero anchors, with no default report/dashboard terms.",
        lock_terms,
        forbidden_terms,
        positive_anchors,
    )


def write_text(path: Path, text: str) -> None:
    path.write_text(text.rstrip() + "\n", encoding="utf-8")


def capture(args: argparse.Namespace) -> Path:
    adb = Path(args.adb)
    if not adb.exists():
        raise FileNotFoundError(f"adb not found: {adb}")

    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = Path(args.output_root) / f"{timestamp}-device-ai-home"
    output_dir.mkdir(parents=True, exist_ok=False)

    write_text(
        output_dir / "00-env.md",
        "\n".join(
            [
                "# Device AI Home Evidence Environment",
                "",
                f"- Captured at UTC: `{utc_now()}`",
                f"- Device serial: `{args.serial or 'auto'}`",
                f"- Package: `{args.package}`",
                f"- Activity: `{args.activity}`",
                f"- Backend reverse port: `{args.backend_port}`",
                f"- Wake before capture: `{args.wake}`",
                f"- Scope: AI home screenshot and UI tree cleanliness only.",
                "",
                "This package does not prove model streaming, cancel-run behavior, RunTrace, or frame timing.",
            ]
        ),
    )

    devices = run(adb_cmd(adb, None, "devices", "-l"), check=True)
    (output_dir / "01-adb-devices.txt").write_bytes(devices.stdout)
    serial = args.serial or infer_single_device(devices.text)
    if not serial:
        write_json(output_dir / "08-home-cleanliness.json", verdict_json("blocked-no-device", "No single adb device could be inferred.", [], [], [], False, 0))
        write_text(output_dir / "10-conclusion.md", conclusion_md("blocked-no-device", "No single adb device could be inferred.", [], [], []))
        return output_dir

    resolve = run(adb_cmd(adb, serial, "shell", "cmd", "package", "resolve-activity", "--brief", args.package), check=False)
    (output_dir / "02-resolve-activity.txt").write_bytes(resolve.stdout + resolve.stderr)
    window_before = collect_window_state(adb, serial)
    write_text(output_dir / "03-window-state-before.txt", window_before)
    reverse = run(adb_cmd(adb, serial, "reverse", f"tcp:{args.backend_port}", f"tcp:{args.backend_port}"), check=False)
    (output_dir / "04-adb-reverse.txt").write_bytes(reverse.stdout + reverse.stderr)
    reverse_list = run(adb_cmd(adb, serial, "reverse", "--list"), check=False)
    (output_dir / "04-adb-reverse-list.txt").write_bytes(reverse_list.stdout + reverse_list.stderr)

    if args.wake:
        run(adb_cmd(adb, serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP"), check=False)
        time.sleep(args.settle_seconds)
        run(adb_cmd(adb, serial, "shell", "input", "swipe", "540", "1900", "540", "600", "400"), check=False)
        time.sleep(args.settle_seconds)

    run(adb_cmd(adb, serial, "logcat", "-c"), check=False)
    start = run(adb_cmd(adb, serial, "shell", "am", "start", "-n", args.activity), check=False)
    (output_dir / "05-am-start.txt").write_bytes(start.stdout + start.stderr)
    time.sleep(args.settle_seconds)
    window_after = collect_window_state(adb, serial)
    write_text(output_dir / "05-window-state-after-start.txt", window_after)

    raw_tree = run(adb_cmd(adb, serial, "exec-out", "uiautomator", "dump", "/dev/tty"), check=False, timeout=20)
    raw_text = raw_tree.text
    clean_xml = clean_ui_xml(raw_text)
    write_text(output_dir / "05-ui-tree-ai-home.xml", raw_text)
    write_text(output_dir / "05-ui-tree-ai-home-clean.xml", clean_xml)

    screenshot = run(adb_cmd(adb, serial, "exec-out", "screencap", "-p"), check=False, timeout=20)
    screenshot_path = output_dir / "06-screenshot-ai-home.png"
    screenshot_path.write_bytes(screenshot.stdout)

    logcat = run(adb_cmd(adb, serial, "logcat", "-d"), check=False, timeout=20)
    filtered = "\n".join(
        line
        for line in logcat.text.splitlines()
        if re.search(r"v2/agent|chat/stream|workbench|OkHttpClient|MainActivity|zhihuiji", line, re.IGNORECASE)
    )
    write_text(output_dir / "09-logcat-filtered.txt", filtered or "(no matching logcat lines)")

    texts = extract_ui_texts(clean_xml)
    write_text(output_dir / "07-ui-texts.txt", "\n".join(texts))
    device_locked = is_device_locked(window_after)
    package_seen = package_visible(args.package, clean_xml, window_after, device_locked=device_locked)
    status, reason, lock_terms, forbidden_terms, positive_anchors = status_from_texts(
        texts,
        package_seen,
        screenshot_bytes=screenshot_path.stat().st_size if screenshot_path.exists() else 0,
        device_locked=device_locked,
    )
    write_json(
        output_dir / "08-home-cleanliness.json",
        verdict_json(
            status,
            reason,
            texts,
            lock_terms,
            forbidden_terms,
            package_seen,
            screenshot_path.stat().st_size if screenshot_path.exists() else 0,
            positive_anchors=positive_anchors,
            device_locked=device_locked,
        ),
    )
    write_text(output_dir / "09-forbidden-report-copy-scan.txt", forbidden_scan_md(forbidden_terms, texts))
    write_text(output_dir / "10-conclusion.md", conclusion_md(status, reason, texts, lock_terms, forbidden_terms, positive_anchors))
    return output_dir


def collect_window_state(adb: Path, serial: str) -> str:
    pieces: list[str] = []
    for command in (
        ("shell", "dumpsys", "window"),
        ("shell", "dumpsys", "activity", "activities"),
    ):
        result = run(adb_cmd(adb, serial, *command), check=False, timeout=20)
        filtered = "\n".join(
            line
            for line in result.text.splitlines()
            if re.search(r"mCurrentFocus|mFocusedApp|mDreamingLockscreen|mShowingLockscreen|Keyguard|ResumedActivity|topResumedActivity|com\.zhihuiji\.app", line)
        )
        pieces.append(f"$ adb {' '.join(command)}\n{filtered or '(no matching lines)'}")
    return "\n\n".join(pieces)


def is_device_locked(window_state: str) -> bool:
    lock_patterns = (
        r"isKeyguardShowing=true",
        r"KeyguardShowing=true",
        r"mDreamingLockscreen=true",
        r"NotificationShade",
        r"MiuiKeyguard",
    )
    return any(re.search(pattern, window_state) for pattern in lock_patterns)


def package_visible(package_name: str, clean_xml: str, window_state: str, *, device_locked: bool = False) -> bool:
    # `am start` and `resolve-activity` only prove the app can launch. They do
    # not prove the app is visible because a locked device may still report the
    # app as resumed behind Keyguard.
    if device_locked:
        return False
    return package_name in clean_xml or package_name in window_state


def infer_single_device(devices_text: str) -> str | None:
    serials: list[str] = []
    for line in devices_text.splitlines():
        if "\tdevice" in line or re.search(r"\sdevice\s", line):
            serials.append(line.split()[0])
    return serials[0] if len(serials) == 1 else None


def write_json(path: Path, payload: object) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def verdict_json(
    status: str,
    reason: str,
    texts: list[str],
    lock_terms: list[str],
    forbidden_terms: list[str],
    package_seen: bool,
    screenshot_bytes: int,
    *,
    positive_anchors: list[str] | None = None,
    device_locked: bool = False,
) -> dict[str, object]:
    return {
        "status": status,
        "reason": reason,
        "package_seen": package_seen,
        "device_locked": device_locked,
        "screenshot_bytes": screenshot_bytes,
        "positive_anchors": positive_anchors or [],
        "required_title": AI_HOME_TITLE,
        "required_secondary_anchors_any": list(AI_HOME_SECONDARY_ANCHORS),
        "lockscreen_hits": lock_terms,
        "forbidden_hits": forbidden_terms,
        "ui_text_preview": texts[:80],
    }


def forbidden_scan_md(forbidden_terms: list[str], texts: list[str]) -> str:
    lines = [
        "# Forbidden Report Copy Scan",
        "",
        f"Strong forbidden hits: `{', '.join(forbidden_terms) if forbidden_terms else 'none'}`",
        "",
        "Strong forbidden terms are default dashboard/report content, not generic capability copy.",
        "Allowed entry copy may mention generating Markdown, charts, or evidence after the user asks a question.",
        "",
        "UI text preview:",
        "",
    ]
    lines.extend(f"- {item}" for item in texts[:120])
    return "\n".join(lines)


def conclusion_md(
    status: str,
    reason: str,
    texts: list[str],
    lock_terms: list[str],
    forbidden_terms: list[str],
    positive_anchors: list[str] | None = None,
) -> str:
    preview = texts[:80]
    return "\n".join(
        [
            "# Device AI Home Evidence Conclusion",
            "",
            f"Status: `{status}`",
            "",
            reason,
            "",
            "Captured checks:",
            "",
            f"- Lock-screen terms: `{', '.join(lock_terms) if lock_terms else 'none'}`",
            f"- Forbidden report/dashboard terms: `{', '.join(forbidden_terms) if forbidden_terms else 'none'}`",
            f"- Positive AI home anchors: `{', '.join(positive_anchors or []) if positive_anchors else 'none'}`",
            f"- Required AI home title: `{AI_HOME_TITLE}`",
            f"- Required secondary anchors: `{', '.join(AI_HOME_SECONDARY_ANCHORS)}`",
            "",
            "UI text preview:",
            "",
            *(f"- {item}" for item in preview),
            "",
            "If status is not `pass-for-ai-home-cleanliness`, keep this evidence as a failed/partial attempt only.",
            "Do not use it to claim AI home UI acceptance.",
        ]
    )


def self_test() -> None:
    locked = ["5:35", "手电筒", "拍照", "电池电量为百分之 89。"]
    assert status_from_texts(locked, package_seen=True)[0] == "blocked-by-locked-device"
    assert status_from_texts(["AI 助手", "主屏保持干净"], package_seen=True, device_locked=True)[0] == "blocked-by-locked-device"
    report_home = ["AI 助手", "主屏保持干净", "销售额", "今日经营"]
    assert status_from_texts(report_home, package_seen=True)[0] == "fail-report-dashboard-content-visible"
    clean_home = ["AI 助手", "主屏保持干净", "Markdown", "图表", "依据"]
    assert status_from_texts(clean_home, package_seen=True)[0] == "pass-for-ai-home-cleanliness"
    weak_home = ["AI 助手"]
    assert status_from_texts(weak_home, package_seen=True)[0] == "partial-ai-home-not-detected"
    not_app = ["AI 助手"]
    assert status_from_texts(not_app, package_seen=False)[0] == "partial-not-in-app"
    locked_window = "mFocusedApp=ActivityRecord{... com.zhihuiji.app/.MainActivity}\nisKeyguardShowing=true"
    assert is_device_locked(locked_window)
    assert not package_visible("com.zhihuiji.app", "", locked_window, device_locked=True)
    unlocked_window = "mCurrentFocus=Window{... com.zhihuiji.app/com.zhihuiji.app.MainActivity}"
    assert package_visible("com.zhihuiji.app", "", unlocked_window, device_locked=False)
    raw = "ignored\n<?xml version='1.0'?><hierarchy><node text='AI'/></hierarchy>\nUI hierchary dumped"
    assert clean_ui_xml(raw).startswith("<?xml")
    assert extract_ui_texts(clean_ui_xml(raw)) == ["AI"]
    print("capture_ai_home_device_evidence self-test passed")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", default=os.environ.get("ADB", str(DEFAULT_ADB)))
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--package", default=os.environ.get("PACKAGE_NAME", DEFAULT_PACKAGE))
    parser.add_argument("--activity", default=os.environ.get("ACTIVITY", DEFAULT_ACTIVITY))
    parser.add_argument("--backend-port", type=int, default=int(os.environ.get("BACKEND_PORT", DEFAULT_BACKEND_PORT)))
    parser.add_argument("--output-root", default=os.environ.get("EVIDENCE_ROOT", str(DEFAULT_OUTPUT_ROOT)))
    parser.add_argument("--settle-seconds", type=float, default=float(os.environ.get("SETTLE_SECONDS", "3")))
    parser.add_argument("--wake", action="store_true", default=os.environ.get("WAKE_DEVICE") == "1")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()

    if args.self_test:
        self_test()
        return 0
    output_dir = capture(args)
    print(f"Device AI home evidence written to: {output_dir}")
    conclusion = output_dir / "10-conclusion.md"
    if conclusion.exists():
        first_lines = "\n".join(conclusion.read_text(encoding="utf-8").splitlines()[:5])
        print(first_lines)
    return 0


if __name__ == "__main__":
    sys.exit(main())
