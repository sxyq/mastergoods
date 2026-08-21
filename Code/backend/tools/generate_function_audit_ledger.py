#!/usr/bin/env python3
"""Generate a function-level audit ledger for backend, Android, and web code."""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import os
import sqlite3
from pathlib import Path


TRACKED_PREFIXES = (
    "Code/backend/src/main/java/",
    "Code/backend/src/main/resources/",
    "Code/backend/src/test/java/",
    "Code/backend/src/test/resources/",
    "Code/backend/src/main/resources/static/admin-console/",
    "Code/frontend/android/",
    "Code/frontend/web/src/",
)

TRACKED_EXACT = {
    "Code/backend/build.gradle.kts",
    "Code/backend/settings.gradle.kts",
    "Code/backend/gradle.properties",
    "Code/frontend/web/package.json",
    "Code/frontend/web/tsconfig.json",
    "Code/frontend/web/tsconfig.app.json",
    "Code/frontend/web/vite.config.ts",
    "Code/frontend/web/index.html",
    "Code/frontend/android/build.gradle.kts",
    "Code/frontend/android/settings.gradle.kts",
    "Code/frontend/android/gradle.properties",
}

CODE_AND_CONFIG_EXTENSIONS = {
    ".java",
    ".kt",
    ".kts",
    ".ts",
    ".tsx",
    ".vue",
    ".js",
    ".mjs",
    ".cjs",
    ".html",
    ".css",
    ".scss",
    ".xml",
    ".yml",
    ".yaml",
    ".json",
    ".properties",
    ".pro",
    ".sql",
}

EXCLUDED_PARTS = {
    ".git",
    ".gradle",
    "build",
    "dist",
    "node_modules",
    ".idea",
    ".codegraph",
}

SYMBOL_KINDS = {
    "class",
    "component",
    "enum",
    "function",
    "interface",
    "method",
    "route",
    "type_alias",
}


def subsystem_for(path: str) -> str:
    if path.startswith("Code/frontend/android/"):
        return "android"
    if path.startswith("Code/frontend/web/") or path.startswith("Code/backend/src/main/resources/static/admin-console/"):
        return "web"
    if path.startswith("Code/backend/"):
        return "backend"
    return "other"


def in_scope(path: str) -> bool:
    return (
        any(path.startswith(prefix) for prefix in TRACKED_PREFIXES)
        or path in TRACKED_EXACT
    )


def is_audit_file(path: Path) -> bool:
    parts = set(path.parts)
    if parts & EXCLUDED_PARTS:
        return False
    rel = path.as_posix()
    if not in_scope(rel):
        return False
    return path.suffix in CODE_AND_CONFIG_EXTENSIONS or path.name in {"Dockerfile", "package.json"}


def language_for(path: str, indexed_language: str | None = None) -> str:
    if indexed_language:
        return indexed_language
    suffix = Path(path).suffix
    return {
        ".java": "java",
        ".kt": "kotlin",
        ".kts": "kotlin",
        ".ts": "typescript",
        ".tsx": "typescript",
        ".vue": "vue",
        ".js": "javascript",
        ".mjs": "javascript",
        ".cjs": "javascript",
        ".html": "html",
        ".css": "css",
        ".scss": "css",
        ".xml": "xml",
        ".yml": "yaml",
        ".yaml": "yaml",
        ".json": "json",
        ".properties": "properties",
        ".pro": "proguard",
    }.get(suffix, "text")


def csv_safe(value: object | None) -> object:
    if value is None:
        return ""
    if isinstance(value, str):
        return value.replace("\n", "\\n").replace("\r", "")
    return value


def load_index(project_root: Path) -> tuple[dict[str, dict[str, object]], list[dict[str, object]]]:
    db_path = project_root / ".codegraph" / "codegraph.db"
    files: dict[str, dict[str, object]] = {}
    symbols: list[dict[str, object]] = []
    if not db_path.exists():
        return files, symbols

    with sqlite3.connect(db_path) as conn:
        conn.row_factory = sqlite3.Row
        for row in conn.execute(
            """
            select path, language, size, node_count
            from files
            where path like 'Code/backend/src/main/java/%'
               or path like 'Code/backend/src/main/resources/%'
               or path like 'Code/backend/src/test/java/%'
               or path like 'Code/backend/src/test/resources/%'
               or path like 'Code/backend/src/main/resources/static/admin-console/%'
               or path like 'Code/frontend/android/%'
               or path like 'Code/frontend/web/src/%'
               or path = 'Code/frontend/web/vite.config.ts'
               or path = 'Code/backend/build.gradle.kts'
               or path = 'Code/backend/settings.gradle.kts'
            order by path
            """
        ):
            files[row["path"]] = dict(row)

        placeholders = ",".join("?" for _ in SYMBOL_KINDS)
        for row in conn.execute(
            f"""
            select file_path, language, kind, name, qualified_name, start_line, end_line, signature
            from nodes
            where kind in ({placeholders})
              and (
                file_path like 'Code/backend/src/main/java/%'
                or file_path like 'Code/backend/src/main/resources/%'
                or file_path like 'Code/backend/src/test/java/%'
                or file_path like 'Code/backend/src/test/resources/%'
                or file_path like 'Code/backend/src/main/resources/static/admin-console/%'
                or file_path like 'Code/frontend/android/%'
                or file_path like 'Code/frontend/web/src/%'
                or file_path = 'Code/frontend/web/vite.config.ts'
                or file_path = 'Code/backend/build.gradle.kts'
                or file_path = 'Code/backend/settings.gradle.kts'
              )
            order by file_path, start_line, kind, name
            """,
            sorted(SYMBOL_KINDS),
        ):
            symbols.append(dict(row))
    return files, symbols


def load_existing_rows(output_csv: Path) -> dict[tuple[str, str, str, str, str], dict[str, object]]:
    if not output_csv.exists():
        return {}
    with output_csv.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        return {
            (
                row.get("file", ""),
                row.get("row_type", ""),
                row.get("kind", ""),
                row.get("symbol", ""),
                row.get("qualified_name", ""),
            ): row
            for row in reader
        }


def preserve_existing_status(
    row: dict[str, object],
    existing_rows: dict[tuple[str, str, str, str, str], dict[str, object]],
) -> dict[str, object]:
    key = (
        str(row.get("file", "")),
        str(row.get("row_type", "")),
        str(row.get("kind", "")),
        str(row.get("symbol", "")),
        str(row.get("qualified_name", "")),
    )
    existing = existing_rows.get(key)
    if not existing:
        return row
    for column in (
        "visit_status",
        "review_status",
        "visited_at",
        "reviewer",
        "performance_risk",
        "security_risk",
        "action",
        "validation",
        "notes",
    ):
        value = existing.get(column)
        if value:
            row[column] = value
    return row


def find_filesystem_files(project_root: Path) -> dict[str, dict[str, object]]:
    files: dict[str, dict[str, object]] = {}
    for path in project_root.rglob("*"):
        if not path.is_file():
            continue
        try:
            rel_path = path.relative_to(project_root)
        except ValueError:
            continue
        if not is_audit_file(rel_path):
            continue
        stat = path.stat()
        rel = rel_path.as_posix()
        files[rel] = {
            "path": rel,
            "language": language_for(rel),
            "size": stat.st_size,
            "node_count": "",
        }
    return files


def write_csv(output_csv: Path, files: dict[str, dict[str, object]], symbols: list[dict[str, object]]) -> dict[str, int]:
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    existing_rows = load_existing_rows(output_csv)
    symbol_counts_by_file: dict[str, int] = {}
    for symbol in symbols:
        symbol_counts_by_file[symbol["file_path"]] = symbol_counts_by_file.get(symbol["file_path"], 0) + 1

    rows: list[dict[str, object]] = []
    seq = 1
    for path in sorted(files):
        file_info = files[path]
        rows.append(preserve_existing_status(
            {
                "seq": seq,
                "subsystem": subsystem_for(path),
                "file": path,
                "language": language_for(path, file_info.get("language") or None),
                "row_type": "file",
                "kind": "file",
                "symbol": "__FILE__",
                "qualified_name": path,
                "start_line": 1,
                "end_line": "",
                "signature": "",
                "visit_status": "PENDING",
                "review_status": "PENDING",
                "visited_at": "",
                "reviewer": "",
                "performance_risk": "",
                "security_risk": "",
                "action": "",
                "validation": "",
                "notes": f"indexed_symbols={symbol_counts_by_file.get(path, 0)}; size={file_info.get('size', '')}",
            },
            existing_rows,
        ))
        seq += 1
        for symbol in [item for item in symbols if item["file_path"] == path]:
            rows.append(preserve_existing_status(
                {
                    "seq": seq,
                    "subsystem": subsystem_for(path),
                    "file": path,
                    "language": language_for(path, symbol.get("language") or None),
                    "row_type": "symbol",
                    "kind": symbol["kind"],
                    "symbol": symbol["name"],
                    "qualified_name": symbol["qualified_name"],
                    "start_line": symbol["start_line"],
                    "end_line": symbol["end_line"],
                    "signature": symbol.get("signature") or "",
                    "visit_status": "PENDING",
                    "review_status": "PENDING",
                    "visited_at": "",
                    "reviewer": "",
                    "performance_risk": "",
                    "security_risk": "",
                    "action": "",
                    "validation": "",
                    "notes": "",
                },
                existing_rows,
            ))
            seq += 1

    with output_csv.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        for row in rows:
            writer.writerow({key: csv_safe(value) for key, value in row.items()})

    summary: dict[str, int] = {"rows": len(rows), "files": len(files), "symbols": len(rows) - len(files)}
    for row in rows:
        summary[row["subsystem"]] = summary.get(row["subsystem"], 0) + 1
    return summary


def write_markdown(output_md: Path, output_csv: Path, summary: dict[str, int]) -> None:
    today = dt.date.today().isoformat()
    content = f"""# 三端函数级性能与安全审计台账

- 创建日期：`{today}`
- 覆盖范围：后端 `Code/backend/src/main/java` / `Code/backend/src/test/java` 与 `Code/backend/` 构建配置，Android `Code/frontend/android`，Web `Code/frontend/web/src` 与关键前端入口配置。
- 明细台账：[`{output_csv.name}`](./{output_csv.name})
- 生成方式：读取 `.codegraph/codegraph.db` 的文件/符号索引，并补充当前文件系统中未被索引但属于三端范围的代码/配置文件。

## 审计目标

1. 访问并标记三端每个文件与每个函数/方法/组件/路由。
2. 在不改变功能、不改变 UI 语义的前提下精简代码。
3. 提升后端查询性能、Android 手机运行与滑动流畅度、Web 交互性能。
4. 检查全链路安全风险，包括鉴权、权限、数据隔离、配置、上传、AI/SSE、缓存与日志。

## 状态字段

- `PENDING`：尚未访问。
- `VISITED`：已经阅读源代码，确认职责和调用边界。
- `REVIEWED`：已经记录性能/安全结论。
- `OPTIMIZED`：已经实施低风险优化。
- `VERIFIED`：优化或结论已有测试、构建、运行或静态证据支撑。
- `BLOCKED`：需要设备、环境、账号、线上配置或用户决策才能继续。

## 当前基线

| 项目 | 数量 |
|---|---:|
| 台账总行数 | {summary["rows"]} |
| 文件行 | {summary["files"]} |
| 函数/方法/组件/路由等符号行 | {summary["symbols"]} |
| backend 行 | {summary.get("backend", 0)} |
| android 行 | {summary.get("android", 0)} |
| web 行 | {summary.get("web", 0)} |

## 执行规则

1. 每次开始前先看 `git status --short`，保护已有未提交改动。
2. 每访问一个文件或函数，就在 CSV 中更新 `visit_status` 与 `visited_at`。
3. 每发现风险，写入 `performance_risk` 或 `security_risk`，并标注优先级。
4. 只做能证明功能/UI 不变的低风险优化；涉及合同或 UI 行为变化时先暂停说明。
5. 后端优化优先用测试或定向请求证明；Android 优先编译、单测、必要时用 adb/gfxinfo/Perfetto；Web 优先 `npm run build` 与页面运行检查。

## 首轮优先访问顺序

1. 后端安全与 owner/RBAC：`SecurityConfig`、`TokenAuthenticationFilter`、`CurrentOwnerService`、`StorePermissionInterceptor`。
2. 后端高查询成本路径：报表、库存、同步、导入、AI/SSE。
3. Android 滑动/重组热点：主导航、Dashboard、Reports、Agent、长列表与 Markdown/result block 渲染。
4. Web API/client/RBAC 与大页面：`client.ts`、session store、AppLayout、Planning/Agent/Stitch 页面。

## 进度日志

| 时间 | 范围 | 结果 |
|---|---|---|
| {today} | 建立台账 | 已生成函数级 CSV 基线，所有条目默认 `PENDING`。 |
"""
    output_md.write_text(content, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", default=".")
    parser.add_argument("--output-prefix", default="docs/performance-security-function-audit-2026-06-23")
    args = parser.parse_args()

    project_root = Path(args.project_root).resolve()
    output_prefix = project_root / args.output_prefix
    output_csv = output_prefix.with_suffix(".csv")
    output_md = output_prefix.with_suffix(".md")

    indexed_files, symbols = load_index(project_root)
    filesystem_files = find_filesystem_files(project_root)
    files = {**filesystem_files, **indexed_files}
    summary = write_csv(output_csv, files, symbols)
    write_markdown(output_md, output_csv, summary)

    print(f"wrote {output_md.relative_to(project_root)}")
    print(f"wrote {output_csv.relative_to(project_root)}")
    print(summary)


if __name__ == "__main__":
    main()
