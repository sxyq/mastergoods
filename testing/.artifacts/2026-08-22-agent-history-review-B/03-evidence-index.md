# 原始证据映射

本文件只列路径和用途，不复制原始 JSON/SSE，也不包含认证载荷。

## 当前 8220 Agent 全量

- 汇总：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/full-tools-evidence/summary.json`
- 状态表：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/full-tools-evidence/case-status.tsv`
- 逐 case：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/full-tools-evidence/cases/`
- provider/runner：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/full-tools-evidence/provider.log`、`runner.log`
- 当前环境与 provenance：`testing/.artifacts/2026-08-20-8220-agent-retest/environment/host-runtime.txt`、`provider/runtime-config.txt`、`tool-registry/summary.txt`

## 当前历史与 owner

- 8220 认证、门店、会话、workbench、messages、run-traces：`testing/.artifacts/2026-08-20-8220-agent-retest/auth-history/wave1-readonly-api.txt`
- 显式 conversation_id 400：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/case-product-catalog.txt`
- 不带显式会话的成功控制：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/case-product-catalog-retry.txt`
- 旧 owner 7/8 隔离对照：`testing/.artifacts/2026-08-03-server-sync-scope/remote-sync-scope.jsonl`、`remote-sync-scope.raw.tsv`

## 当前 SSE、cancel、audit、并发

- cancel 原文和响应：`testing/.artifacts/2026-08-20-8220-agent-retest/stream-cancel/mg-8220-sse-retest-20260820/`
- 10 路 SSE：`testing/.artifacts/2026-08-20-8220-agent-retest/performance/mg-8220-sse-concurrency-20260820/`
- 30 轮非流式：`testing/.artifacts/2026-08-20-8220-agent-retest/performance/nonstream-30-summary.txt`
- 旧客户端终态缺失：`testing/.artifacts/2026-07-31-server-agent-predeploy/predeploy-20260731T151657Z-server-config/03-raw.sse`、`03-audit.json`
- 旧 Android interruption：`testing/.artifacts/2026-07-19-agent-llm-live-recheck/02-stream-raw.sse`、`03-audit.json`

## 清理与媒体

- 全量 case 清理字段：`testing/.artifacts/2026-08-20-8220-agent-retest/readonly-agent/full-tools-evidence/cases/057-media_upload_tool.json` 及 `summary.json`
- 媒体 API：`testing/.artifacts/2026-08-20-8220-agent-retest/database/media-api-case.txt`
- 最终状态：`testing/.artifacts/2026-08-20-8220-agent-retest/database/final-state.txt`
- 旧 DeepSeek draft lifecycle：`testing/.artifacts/2026-08-02-server-agent-eval/wave1-deepseek-functional-live-20260802T195600+0800/`

## 旧环境和过期边界

- 154 soak：`testing/.artifacts/2026-08-03-production-agent-recheck-154-deployed/soak-20260803-approved-final-v2/`
- 154 performance：`testing/.artifacts/2026-08-02-server-agent-eval/wave1-deepseek-performance-complete-20260802T201300+0800/`
- 154 deployment gate：`testing/.artifacts/2026-08-01-server-agent-eval/`
- 2026-07-19 Android history：`testing/.artifacts/2026-07-19-agent-llm-live-recheck/30-turn/android/`
- image provider 422：`testing/.artifacts/2026-07-19-deployment-config-audit/image-precheck-body.json`
