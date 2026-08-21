#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Callable


REPO_ROOT = Path(__file__).resolve().parents[2]
TESTING_ROOT = REPO_ROOT / "testing"
CATEGORIES = ("单元测试", "功能测试", "性能测试", "审计")

EXCLUDED_DIR_NAMES = {
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    "build",
    "dist",
    "node_modules",
    "Pods",
    "DerivedData",
    "out",
    "tmp",
}

CONTROL_KEYWORDS = {
    "if",
    "for",
    "while",
    "switch",
    "catch",
    "return",
    "throw",
    "when",
    "try",
    "else",
}

JAVA_CLASS_RE = re.compile(r"\b(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)")
KOTLIN_CLASS_RE = re.compile(r"\b(class|interface|object|sealed class|data class)\s+([A-Za-z_][A-Za-z0-9_]*)")
SWIFT_CLASS_RE = re.compile(r"\b(class|struct|enum|actor|protocol|extension)\s+([A-Za-z_][A-Za-z0-9_]*)")
TS_CLASS_RE = re.compile(r"\b(class|interface|type|enum)\s+([A-Za-z_$][A-Za-z0-9_$]*)")

JAVA_METHOD_RE = re.compile(
    r"^\s*(?:public|protected|private|static|final|abstract|default|synchronized|native|strictfp|\s)+"
    r"(?:<[^>]+>\s*)?(?:[\w\[\]<>?,.@]+\s+)+(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\([^;{}]*\)\s*"
    r"(?:throws [^{;]+)?[;{]?\s*$"
)
JAVA_CTOR_RE = re.compile(
    r"^\s*(?:public|protected|private)\s+(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\([^;{}]*\)\s*(?:throws [^{;]+)?[;{]?\s*$"
)
KOTLIN_FUN_RE = re.compile(
    r"^\s*(?:public|private|internal|protected|suspend|inline|tailrec|operator|infix|external|override|abstract|open|final|actual|expect|\s)*"
    r"fun\s*(?:<[^>]+>\s*)?(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\("
)
SWIFT_FUNC_RE = re.compile(
    r"^\s*(?:public|private|fileprivate|internal|open|mutating|nonmutating|override|static|class|convenience|required|\s)*"
    r"func\s+(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*(?:<[^>]+>)?\s*\("
)
SWIFT_INIT_RE = re.compile(
    r"^\s*(?:public|private|fileprivate|internal|convenience|required|override|\s)*init\s*\("
)
TS_FUNCTION_RE = re.compile(
    r"^\s*(?:export\s+)?(?:async\s+)?function\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*(?:<[^>]+>)?\s*\("
)
TS_ARROW_RE = re.compile(
    r"^\s*(?:export\s+)?const\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*(?:async\s*)?(?:\([^)]*\)|[A-Za-z_$][A-Za-z0-9_$]*)\s*=>"
)
TS_METHOD_RE = re.compile(
    r"^\s*(?:public|private|protected|static|async|get|set|\s)*(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^=;]*\)\s*\{?\s*$"
)
SCRIPT_BLOCK_RE = re.compile(r"<script\b[^>]*>(?P<body>.*?)</script>", re.S | re.I)


@dataclass
class PlatformConfig:
    name: str
    extensions: tuple[str, ...]
    include_prefixes: tuple[str, ...]
    file_filter: Callable[[Path, str], bool] | None = None


@dataclass
class Symbol:
    platform: str
    module: str
    source_file: str
    class_or_object: str
    function_name: str
    line_number: int


def normalize_posix(path: Path) -> str:
    return path.relative_to(REPO_ROOT).as_posix()


def should_skip_path(path: Path) -> bool:
    return any(part in EXCLUDED_DIR_NAMES for part in path.parts)


def agent_file_filter(path: Path, text: str) -> bool:
    rel = normalize_posix(path)
    if "/agent/" in rel.lower() or "agent" in path.name.lower():
        return True
    explicit = {
        "Code/frontend/android/app/src/main/java/com/zhihuiji/app/MainActivity.kt",
        "Code/frontend/android/app/src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt",
        "Code/frontend/android/app/src/main/java/com/zhihuiji/app/navigation/MainNavGraph.kt",
        "Code/frontend/android/app/src/main/java/com/zhihuiji/app/navigation/MainScreen.kt",
        "Code/frontend/android/app/src/main/java/com/zhihuiji/app/navigation/MainAccessViewModel.kt",
        "Code/frontend/web/src/shared/api/agent-stream.ts",
        "Code/frontend/web/src/pages/agent/AgentPage.vue",
    }
    if rel in explicit:
        return True
    if rel.startswith("Code/backend/src/main/java/") and "agent" in text.lower():
        return True
    return False


PLATFORMS = {
    "后端": PlatformConfig(
        name="后端",
        extensions=(".java",),
        include_prefixes=("Code/backend/src/main/java/com/zhihuiji/backend/",),
    ),
    "安卓": PlatformConfig(
        name="安卓",
        extensions=(".kt",),
        include_prefixes=(
            "Code/frontend/android/app/src/main/java/",
            "Code/frontend/android/core/common/src/main/java/",
            "Code/frontend/android/core/database/src/main/java/",
            "Code/frontend/android/core/datastore/src/main/java/",
            "Code/frontend/android/core/designsystem/src/main/java/",
            "Code/frontend/android/core/model/src/main/java/",
            "Code/frontend/android/core/network/src/main/java/",
            "Code/frontend/android/data/",
            "Code/frontend/android/feature/",
        ),
    ),
    "ios": PlatformConfig(
        name="ios",
        extensions=(".swift",),
        include_prefixes=("Code/frontend/ios/ZhihuijiIOS/",),
    ),
    "web": PlatformConfig(
        name="web",
        extensions=(".ts", ".vue"),
        include_prefixes=("Code/frontend/web/src/",),
    ),
    "Agent": PlatformConfig(
        name="Agent",
        extensions=(".java", ".kt", ".swift", ".ts", ".vue"),
        include_prefixes=(
            "Code/backend/src/main/java/com/zhihuiji/backend/",
            "Code/frontend/android/app/src/main/java/",
            "Code/frontend/android/core/database/src/main/java/",
            "Code/frontend/android/core/model/src/main/java/",
            "Code/frontend/android/core/network/src/main/java/",
            "Code/frontend/android/data/",
            "Code/frontend/android/feature/",
            "Code/frontend/ios/ZhihuijiIOS/",
            "Code/frontend/web/src/",
        ),
        file_filter=agent_file_filter,
    ),
}


def iter_source_files(config: PlatformConfig) -> list[Path]:
    files: list[Path] = []
    for prefix in config.include_prefixes:
        base = REPO_ROOT / prefix
        if not base.exists():
            continue
        if base.is_file():
            candidate_paths = [base]
        else:
            candidate_paths = [p for p in base.rglob("*") if p.is_file()]
        for path in candidate_paths:
            if should_skip_path(path) or path.suffix not in config.extensions:
                continue
            rel = normalize_posix(path)
            if "/src/test/" in rel or rel.endswith("Tests.swift"):
                continue
            if config.file_filter:
                text = path.read_text(encoding="utf-8", errors="ignore")
                if not config.file_filter(path, text):
                    continue
            files.append(path)
    unique = sorted({path.resolve() for path in files})
    return [Path(item) for item in unique]


def module_from_path(platform: str, rel: str) -> str:
    parts = rel.split("/")
    if platform == "后端":
        start = parts.index("backend") + 1 if "backend" in parts else 0
        if start >= len(parts) - 1:
            return "root"
        return "/".join(parts[start:start + 2]) or "root"
    if platform == "安卓":
        if "android" not in parts:
            return "root"
        module_index = parts.index("android") + 1
        if module_index >= len(parts):
            return "root"
        module = parts[module_index]
        if module in {"core", "data", "feature"} and module_index + 1 < len(parts):
            return "/".join(parts[module_index:module_index + 2])
        if module == "app":
            return "app"
        return module
    if platform == "ios":
        if "ios" not in parts:
            return "ios"
        module_index = parts.index("ios") + 1
        return "/".join(parts[module_index:module_index + 2]) or "ios"
    if platform == "web":
        if "web" not in parts:
            return "web"
        module_index = parts.index("web") + 1
        return "/".join(parts[module_index:module_index + 2]) or "web"
    if platform == "Agent":
        if rel.startswith("Code/backend/src/main/java/"):
            return f"backend:{module_from_path('后端', rel)}"
        if rel.startswith("Code/frontend/android/"):
            return f"android:{module_from_path('安卓', rel)}"
        if rel.startswith("Code/frontend/ios/"):
            return f"ios:{module_from_path('ios', rel)}"
        if rel.startswith("Code/frontend/web/"):
            return f"web:{module_from_path('web', rel)}"
    return "root"


def feature_domain_from_path(rel: str) -> str:
    parts = rel.split("/")
    if "controller" in parts:
        return "api"
    if "service" in parts:
        return "service"
    if "repository" in parts or "dao" in parts:
        return "data-access"
    if "feature" in parts:
        idx = parts.index("feature")
        return parts[idx + 1] if idx + 1 < len(parts) else "feature"
    if "pages" in parts:
        idx = parts.index("pages")
        return parts[idx + 1] if idx + 1 < len(parts) else "page"
    if "Features" in parts:
        idx = parts.index("Features")
        return parts[idx + 1] if idx + 1 < len(parts) else "feature"
    if "shared" in parts:
        return "shared"
    return parts[-2] if len(parts) >= 2 else "root"


def parse_lines_for_symbols(path: Path, text: str) -> list[Symbol]:
    rel = normalize_posix(path)
    if path.suffix == ".java":
        return parse_java_symbols(rel, text)
    if path.suffix == ".kt":
        return parse_kotlin_symbols(rel, text)
    if path.suffix == ".swift":
        return parse_swift_symbols(rel, text)
    if path.suffix == ".ts":
        return parse_ts_symbols(rel, text, platform="web")
    if path.suffix == ".vue":
        return parse_vue_symbols(rel, text)
    return []


def parse_java_symbols(rel: str, text: str) -> list[Symbol]:
    current_class = Path(rel).stem
    symbols: list[Symbol] = []
    for idx, line in enumerate(text.splitlines(), start=1):
        class_match = JAVA_CLASS_RE.search(line)
        if class_match:
            current_class = class_match.group(2)
        ctor_match = JAVA_CTOR_RE.match(line)
        if ctor_match and ctor_match.group("name") == current_class:
            symbols.append(Symbol("", "", rel, current_class, current_class, idx))
            continue
        method_match = JAVA_METHOD_RE.match(line)
        if method_match:
            name = method_match.group("name")
            if name not in CONTROL_KEYWORDS:
                symbols.append(Symbol("", "", rel, current_class, name, idx))
    return dedupe_symbols(symbols)


def parse_kotlin_symbols(rel: str, text: str) -> list[Symbol]:
    current_class = Path(rel).stem
    symbols: list[Symbol] = []
    for idx, line in enumerate(text.splitlines(), start=1):
        class_match = KOTLIN_CLASS_RE.search(line)
        if class_match:
            current_class = class_match.group(2)
        fun_match = KOTLIN_FUN_RE.match(line)
        if fun_match:
            name = fun_match.group("name")
            if name not in CONTROL_KEYWORDS:
                symbols.append(Symbol("", "", rel, current_class, name, idx))
    return dedupe_symbols(symbols)


def parse_swift_symbols(rel: str, text: str) -> list[Symbol]:
    current_class = Path(rel).stem
    symbols: list[Symbol] = []
    for idx, line in enumerate(text.splitlines(), start=1):
        class_match = SWIFT_CLASS_RE.search(line)
        if class_match:
            current_class = class_match.group(2)
        if SWIFT_INIT_RE.match(line):
            symbols.append(Symbol("", "", rel, current_class, "init", idx))
            continue
        func_match = SWIFT_FUNC_RE.match(line)
        if func_match:
            symbols.append(Symbol("", "", rel, current_class, func_match.group("name"), idx))
    return dedupe_symbols(symbols)


def parse_ts_symbols(rel: str, text: str, platform: str) -> list[Symbol]:
    current_class = Path(rel).stem
    symbols: list[Symbol] = []
    for idx, line in enumerate(text.splitlines(), start=1):
        class_match = TS_CLASS_RE.search(line)
        if class_match:
            current_class = class_match.group(2)
        for matcher in (TS_FUNCTION_RE, TS_ARROW_RE, TS_METHOD_RE):
            method_match = matcher.match(line)
            if method_match:
                name = method_match.group("name")
                if name not in CONTROL_KEYWORDS:
                    symbols.append(Symbol("", "", rel, current_class, name, idx))
                break
    return dedupe_symbols(symbols)


def parse_vue_symbols(rel: str, text: str) -> list[Symbol]:
    symbols: list[Symbol] = []
    for match in SCRIPT_BLOCK_RE.finditer(text):
        body = match.group("body")
        start_line = text[: match.start("body")].count("\n") + 1
        current_class = Path(rel).stem
        for offset, line in enumerate(body.splitlines(), start=0):
            real_line = start_line + offset
            class_match = TS_CLASS_RE.search(line)
            if class_match:
                current_class = class_match.group(2)
            for matcher in (TS_FUNCTION_RE, TS_ARROW_RE, TS_METHOD_RE):
                method_match = matcher.match(line)
                if method_match:
                    name = method_match.group("name")
                    if name not in CONTROL_KEYWORDS:
                        symbols.append(Symbol("", "", rel, current_class, name, real_line))
                    break
    return dedupe_symbols(symbols)


def dedupe_symbols(symbols: list[Symbol]) -> list[Symbol]:
    seen: set[tuple[str, str, int]] = set()
    unique: list[Symbol] = []
    for symbol in symbols:
        key = (symbol.source_file, symbol.function_name, symbol.line_number)
        if key in seen:
            continue
        seen.add(key)
        unique.append(symbol)
    return unique


def audit_focus_for(symbol: Symbol) -> tuple[str, str, str, str]:
    rel = symbol.source_file.lower()
    name = symbol.function_name.lower()
    security = "通用输入校验、权限、租户隔离待核"
    performance = "通用热点待核"
    reuse = "检查是否复用现有公共组件/工具"
    simplify = "检查是否可减少重复分支与重复映射"
    if any(token in rel for token in ("auth", "security", "permission", "session", "token", "agent")):
        security = "重点核权限边界、敏感数据、提示词/工具调用安全"
    if any(token in rel for token in ("repository", "dao", "query", "database", "sync", "import")) or any(
        token in name for token in ("list", "search", "load", "sync", "import", "stream")
    ):
        performance = "重点核查询次数、批处理、I/O 与分页"
    if any(token in rel for token in ("common", "shared", "utils", "formatter", "designsystem", "business.ts")):
        reuse = "重点核是否仍有本地重复实现可收口"
    if any(token in rel for token in ("screen", "view", "page", "controller", "service", "viewmodel")):
        simplify = "重点核状态分支、重复 UI 拼装、重复错误处理"
    return security, performance, reuse, simplify


def metric_for(symbol: Symbol) -> tuple[str, str, str]:
    rel = symbol.source_file.lower()
    name = symbol.function_name.lower()
    if any(token in rel for token in ("controller", "api", "repository", "dao", "service", "network", "sync", "import", "stream", "agent")):
        metric = "latency|throughput|allocation"
        target = "建立基线并记录 p95/吞吐/内存"
        priority = "高"
    elif any(token in rel for token in ("screen", "view", "page", "compose")):
        metric = "startup|frame_time|jank"
        target = "建立首帧/切页/滚动基线"
        priority = "高"
    else:
        metric = "cpu|memory"
        target = "建立函数级基线"
        priority = "中"
    if any(token in name for token in ("render", "stream", "list", "search", "load", "refresh", "query", "save", "submit")):
        priority = "高"
    return metric, target, priority


def scenario_name_for(symbol: Symbol) -> str:
    file_stem = Path(symbol.source_file).stem
    if any(token in file_stem.lower() for token in ("screen", "view", "page")):
        return f"验证 {file_stem}.{symbol.function_name} 对应界面与交互行为"
    if any(token in file_stem.lower() for token in ("viewmodel", "service", "controller", "repository", "dao")):
        return f"验证 {file_stem}.{symbol.function_name} 的输入、状态流转与输出"
    return f"验证 {file_stem}.{symbol.function_name} 业务逻辑"


def csv_rows_for_platform(platform: str) -> tuple[list[dict[str, str]], list[dict[str, str]], list[dict[str, str]], list[dict[str, str]]]:
    config = PLATFORMS[platform]
    unit_rows: list[dict[str, str]] = []
    functional_rows: list[dict[str, str]] = []
    performance_rows: list[dict[str, str]] = []
    audit_rows: list[dict[str, str]] = []

    for path in iter_source_files(config):
        text = path.read_text(encoding="utf-8", errors="ignore")
        rel = normalize_posix(path)
        module = module_from_path(platform, rel)
        feature_domain = feature_domain_from_path(rel)
        symbols = parse_lines_for_symbols(path, text)
        for symbol in symbols:
            symbol.platform = platform
            symbol.module = module
            unit_rows.append(
                {
                    "platform": platform,
                    "module": module,
                    "source_file": rel,
                    "class_or_object": symbol.class_or_object,
                    "function_name": symbol.function_name,
                    "line_number": str(symbol.line_number),
                    "test_status": "未测试",
                    "test_file": "",
                    "test_case": "",
                    "evidence_path": "",
                    "notes": "",
                }
            )
            functional_rows.append(
                {
                    "platform": platform,
                    "module": module,
                    "feature_domain": feature_domain,
                    "source_file": rel,
                    "source_symbol": f"{symbol.class_or_object}.{symbol.function_name}",
                    "line_number": str(symbol.line_number),
                    "scenario_id": build_scenario_id(platform, rel, symbol.function_name, symbol.line_number),
                    "scenario_name": scenario_name_for(symbol),
                    "test_status": "未测试",
                    "evidence_path": "",
                    "notes": "",
                }
            )
            metric, target, priority = metric_for(symbol)
            performance_rows.append(
                {
                    "platform": platform,
                    "module": module,
                    "source_file": rel,
                    "class_or_object": symbol.class_or_object,
                    "function_name": symbol.function_name,
                    "line_number": str(symbol.line_number),
                    "scenario_name": scenario_name_for(symbol),
                    "metric_family": metric,
                    "target_or_threshold": target,
                    "priority": priority,
                    "status": "未测试",
                    "notes": "",
                }
            )
            security, performance, reuse, simplify = audit_focus_for(symbol)
            audit_rows.append(
                {
                    "platform": platform,
                    "module": module,
                    "source_file": rel,
                    "class_or_object": symbol.class_or_object,
                    "function_name": symbol.function_name,
                    "line_number": str(symbol.line_number),
                    "audit_status": "未审计",
                    "security_focus": security,
                    "performance_focus": performance,
                    "reuse_focus": reuse,
                    "simplification_focus": simplify,
                    "notes": "",
                }
            )
    return unit_rows, functional_rows, performance_rows, audit_rows


def build_scenario_id(platform: str, rel: str, function_name: str, line_number: int) -> str:
    safe_path = rel.replace("/", "_").replace(".", "_")
    return f"{platform}_{safe_path}_{function_name}_{line_number}"


def write_csv(path: Path, headers: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=headers)
        writer.writeheader()
        writer.writerows(rows)


def ensure_scripts(platform: str) -> None:
    platform_dir = TESTING_ROOT / platform
    platform_dir.mkdir(parents=True, exist_ok=True)
    runner_names = {
        "单元测试": "run_unit_tests.sh",
        "功能测试": "run_function_tests.sh",
        "性能测试": "run_performance_tests.sh",
        "审计": "run_audit_checks.sh",
    }
    for category in CATEGORIES:
        scripts_dir = platform_dir / category / "scripts"
        scripts_dir.mkdir(parents=True, exist_ok=True)
        readme_path = scripts_dir / "README.md"
        readme_path.write_text(
            "# Scripts\n\n"
            "本目录用于放置该端当前测试类型下的执行脚本、采样脚本与结果整理脚本。\n"
            "当前阶段已预置统一台账刷新入口，后续新增脚本直接放在这里。\n",
            encoding="utf-8",
        )
        refresh_path = scripts_dir / "refresh_tables.sh"
        refresh_path.write_text(
            "#!/usr/bin/env bash\n"
            "set -euo pipefail\n\n"
            "REPO_ROOT=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")/../../../..\" && pwd)\"\n"
            f"python3 \"$REPO_ROOT/testing/scripts/generate_testing_assets.py\" --platform \"{platform}\"\n",
            encoding="utf-8",
        )
        refresh_path.chmod(0o755)
        runner_path = scripts_dir / runner_names[category]
        runner_path.write_text(
            "#!/usr/bin/env bash\n"
            "set -euo pipefail\n\n"
            "echo \"占位脚本：后续请在本目录补充真实执行命令，并同步回填对应 CSV 台账。\"\n",
            encoding="utf-8",
        )
        runner_path.chmod(0o755)


def update_platform_readme(platform: str) -> None:
    readme_path = TESTING_ROOT / platform / "README.md"
    lines = [f"# {platform}测试方案索引", ""]
    if platform in {"安卓", "后端", "Agent"}:
        lines.append("- 测试种类总台账：`测试分类总台账.csv`")
    lines.extend(
        [
            "- 单元测试：`单元测试/TEST_PLAN.md` + `单元测试/unit_function_coverage.csv`",
            "- 功能测试：`功能测试/TEST_PLAN.md` + `功能测试/functional_feature_matrix.csv`",
            "- 性能测试：`性能测试/TEST_PLAN.md` + `性能测试/performance_scope_matrix.csv`",
            "- 审计：`审计/audit_function_ledger.csv`",
            "- 破坏性逆向安全测试：`破坏性逆向安全测试/TEST_PLAN.md` + `破坏性逆向安全测试/reverse_attack_matrix.csv`",
            "",
            "常规测试与破坏性逆向安全测试分开维护，避免混账。",
        ]
    )
    readme_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def update_root_readme() -> None:
    readme_path = TESTING_ROOT / "README.md"
    text = (
        "# Master-Goods Full Coverage Test Plan Index\n\n"
        "## Scope\n\n"
        "This directory contains execution-oriented plans and machine-trackable ledgers for every platform:\n\n"
        "- Backend\n"
        "- Android\n"
        "- iOS\n"
        "- Web\n"
        "- Agent\n\n"
        "Each platform now includes five lanes:\n\n"
        "- Unit tests\n"
        "- Functional tests\n"
        "- Performance tests\n"
        "- Audit\n"
        "- Destructive reverse security tests\n\n"
        "## Ledger Files\n\n"
        "- `测试分类总台账.csv`: 测试分类总台账，负责定义分类并统计模块/文件/函数覆盖\n"
        "- `单元测试/unit_function_coverage.csv`: 函数级单元测试覆盖台账，带 `category_id` / `category_name`\n"
        "- `功能测试/functional_feature_matrix.csv`: 对照源码建立的功能测试台账，带 `category_id` / `category_name`\n"
        "- `性能测试/performance_scope_matrix.csv`: 需要建立基线的性能测试台账，带 `category_id` / `category_name`\n"
        "- `审计/audit_function_ledger.csv`: 安全/性能/复用/简化四维审计台账\n"
        "- `破坏性逆向安全测试/reverse_attack_matrix.csv`: 攻击式逆向安全测试台账\n\n"
        "## Usage\n\n"
        "1. 先看 `测试分类总台账.csv`，确认分类是否齐全以及已经覆盖多少文件/函数。\n"
        "2. 再进入单元/功能/性能台账，按 `category_id` 回填测试状态、脚本、证据。\n"
        "3. 常规质量测试与破坏性逆向安全测试分开维护，不混用状态字段。\n"
        "4. `generate_testing_assets.py` 会刷新函数级台账，并自动调用 `sync_testing_taxonomy.py` 维持分类映射。\n"
    )
    readme_path.write_text(text, encoding="utf-8")


def generate(platforms: list[str]) -> None:
    (TESTING_ROOT / "scripts").mkdir(parents=True, exist_ok=True)
    shared_readme = TESTING_ROOT / "scripts" / "README.md"
    shared_readme.write_text(
        "# Shared Generators\n\n"
        "- `generate_testing_assets.py`: 根据当前源码刷新各端测试与审计台账。\n",
        encoding="utf-8",
    )
    update_root_readme()
    for platform in platforms:
        ensure_scripts(platform)
        update_platform_readme(platform)
        unit_rows, functional_rows, performance_rows, audit_rows = csv_rows_for_platform(platform)
        base = TESTING_ROOT / platform
        write_csv(
            base / "单元测试" / "unit_function_coverage.csv",
            ["platform", "module", "source_file", "class_or_object", "function_name", "line_number", "test_status", "test_file", "test_case", "evidence_path", "notes"],
            unit_rows,
        )
        write_csv(
            base / "功能测试" / "functional_feature_matrix.csv",
            ["platform", "module", "feature_domain", "source_file", "source_symbol", "line_number", "scenario_id", "scenario_name", "test_status", "evidence_path", "notes"],
            functional_rows,
        )
        write_csv(
            base / "性能测试" / "performance_scope_matrix.csv",
            ["platform", "module", "source_file", "class_or_object", "function_name", "line_number", "scenario_name", "metric_family", "target_or_threshold", "priority", "status", "notes"],
            performance_rows,
        )
        write_csv(
            base / "审计" / "audit_function_ledger.csv",
            ["platform", "module", "source_file", "class_or_object", "function_name", "line_number", "audit_status", "security_focus", "performance_focus", "reuse_focus", "simplification_focus", "notes"],
            audit_rows,
        )
    sync_targets = [platform for platform in platforms if platform in {"安卓", "后端", "Agent"}]
    if sync_targets:
        command = ["python3", str(TESTING_ROOT / "scripts" / "sync_testing_taxonomy.py")]
        for platform in sync_targets:
            command.extend(["--platform", platform])
        subprocess.run(command, check=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate testing and audit ledgers.")
    parser.add_argument("--platform", action="append", choices=sorted(PLATFORMS.keys()), help="Generate only the selected platform(s).")
    args = parser.parse_args()
    selected = args.platform or list(PLATFORMS.keys())
    generate(selected)


if __name__ == "__main__":
    main()
