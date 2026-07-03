#!/usr/bin/env python3
"""Scan AI assistant production paths for mock/demo/fake-streaming risk terms."""

from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

DEFAULT_PATHS = [
    "src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java",
    "src/main/java/com/zhihuiji/backend/application/service/v2",
    "src/main/java/com/zhihuiji/backend/infrastructure/ai",
    "src/main/java/com/zhihuiji/backend/api/dto/v2/agent",
    "src/main/java/com/zhihuiji/backend/infrastructure/repository",
    "master-goods-android/feature/agent",
    "master-goods-android/data/agent",
    "master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent",
    "master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/AgentSseClient.kt",
]

SKIP_PARTS = {
    ".gradle",
    ".idea",
    ".kotlin",
    "build",
    "generated",
    "intermediates",
    "test-results",
}

TERMS = [
    "mock",
    "demo",
    "fake",
    "sample",
    "placeholder",
    "模拟",
    "演示",
    "假数据",
    "delay",
    "timer",
    "substring",
    "chunkSize",
]

ALLOW_PATTERNS = [
    (re.compile(r"AgentChatScreen\.kt:.*placeholder", re.IGNORECASE), "input placeholder copy only"),
    (re.compile(r"AgentChatScreen\.kt:.*delay", re.IGNORECASE), "tool-status clock refresh only; does not create answer text"),
    (re.compile(r"AgentChatViewModel\.kt:.*delay", re.IGNORECASE), "UI coalescing delay for server answer_delta only"),
    (re.compile(r"AgentSseClient\.kt:.*take\(", re.IGNORECASE), "error snippet truncation only"),
    (re.compile(r"ResultBlockRenderer\.kt:.*take\(", re.IGNORECASE), "visible raw-summary truncation only"),
    (re.compile(r"ResultBlockRenderer\.kt:.*模拟", re.IGNORECASE), "user-facing error says rendering stopped to avoid simulated chart labels"),
    (re.compile(r"AgentMarkdownText.*substring", re.IGNORECASE), "markdown parser string slicing, not answer reveal"),
    (re.compile(r"V2AgentAiService\.java:.*模拟", re.IGNORECASE), "user-facing/model instruction text explicitly forbids simulated data"),
    (re.compile(r"V2AgentAiService\.java:.*substring", re.IGNORECASE), "summary truncation, JSON extraction, server_notice tail, or title clipping; not fake streaming"),
    (re.compile(r"V2AgentConversationService\.java:.*substring", re.IGNORECASE), "conversation summary/title clipping only"),
    (re.compile(r"LongCatAnthropicClient\.java:.*substring", re.IGNORECASE), "SSE data-prefix parsing only"),
    (re.compile(r"/test/|src/test/", re.IGNORECASE), "test-only path"),
]


@dataclass(frozen=True)
class Hit:
    path: str
    line_number: int
    term: str
    text: str
    verdict: str
    reason: str


def iter_files(paths: list[str]) -> list[Path]:
    files: list[Path] = []
    for raw in paths:
        path = (ROOT / raw).resolve()
        if not path.exists():
            continue
        if path.is_file():
            files.append(path)
            continue
        for child in path.rglob("*"):
            if any(part in SKIP_PARTS for part in child.relative_to(ROOT).parts):
                continue
            if child.is_file() and child.suffix in {".java", ".kt", ".xml", ".yml", ".yaml", ".json"}:
                files.append(child)
    return sorted(set(files))


def classify(relative_path: str, line: str) -> tuple[str, str]:
    key = f"{relative_path}:{line}"
    for pattern, reason in ALLOW_PATTERNS:
        if pattern.search(key):
            return "pass", reason
    return "needs_evidence", "manual review required: prove this hit does not create mock data, fake streaming, placeholder results, or simulated agent behavior"


def scan(paths: list[str]) -> list[Hit]:
    pattern = re.compile("|".join(re.escape(term) for term in TERMS), re.IGNORECASE)
    hits: list[Hit] = []
    for file_path in iter_files(paths):
        relative_path = file_path.relative_to(ROOT).as_posix()
        try:
            lines = file_path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue
        for index, line in enumerate(lines, start=1):
            match = pattern.search(line)
            if not match:
                continue
            verdict, reason = classify(relative_path, line)
            hits.append(Hit(relative_path, index, match.group(0), line.strip(), verdict, reason))
    return hits


def render_markdown(hits: list[Hit]) -> str:
    needs = sum(1 for hit in hits if hit.verdict == "needs_evidence")
    status = "pass-for-static-scan" if needs == 0 else "fail-needs-review"
    lines = [
        "# AI Agent Forbidden Scan",
        "",
        f"Status: `{status}`",
        f"Needs evidence hits: `{needs}`",
        "",
        "This scan covers static production-path AI files only. A pass here does not replace HTTP, SSE, audit, Android screenshot, UI tree, or provider model_stream evidence.",
        "",
        "| File | Line | Term | Verdict | Reason | Snippet |",
        "|---|---:|---|---|---|---|",
    ]
    if not hits:
        lines.append("| - | - | - | `pass` | No forbidden terms found. | - |")
    for hit in hits:
        snippet = hit.text.replace("|", "\\|")
        lines.append(
            f"| `{hit.path}` | {hit.line_number} | `{hit.term}` | `{hit.verdict}` | {hit.reason} | `{snippet}` |"
        )
    lines.append("")
    return "\n".join(lines)


def self_test() -> None:
    sample = classify("master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt", 'placeholder = "输入经营问题"')
    assert sample[0] == "pass", sample
    sample = classify("src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java", "fake stream")
    assert sample[0] == "needs_evidence", sample
    rendered = render_markdown([
        Hit("a.kt", 1, "fake", "fake stream", "needs_evidence", "manual"),
    ])
    assert "fail-needs-review" in rendered
    rendered = render_markdown([])
    assert "pass-for-static-scan" in rendered


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", help="Write markdown report to this path.")
    parser.add_argument("--path", action="append", dest="paths", help="Additional or replacement path to scan. Can be passed multiple times.")
    parser.add_argument("--self-test", action="store_true", help="Run script self-test.")
    args = parser.parse_args()

    if args.self_test:
        self_test()
        print("ai_agent_forbidden_scan self-test passed")
        return 0

    paths = args.paths if args.paths else DEFAULT_PATHS
    report = render_markdown(scan(paths))
    if args.output:
        output_path = Path(args.output)
        if not output_path.is_absolute():
            output_path = ROOT / output_path
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(report, encoding="utf-8")
    else:
        print(report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
