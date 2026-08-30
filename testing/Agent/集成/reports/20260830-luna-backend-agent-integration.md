# Agent backend integration evidence: 20260830-luna

本批集成实测为 `Blocked`。服务和隔离 H2 可用，但没有授权会话、测试 owner/store、可控 Provider 或完整业务夹具。`AG-I-001..013` 均未进入真实 Agent 集成链路。

| 范围 | 计划量 | 实际集成请求 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|---:|
| AG-I-001..013 | 13 | 0 | 0 | 0 | 13 | 0 |

服务启动和匿名路由观测不等于集成通过。24 个匿名 HTTP 请求均在 Agent 执行前返回 403；没有 ToolPlanner/ToolExecutor 调用、Provider 调用、run/audit、SSE 终态或业务数据变化。Agent 表 before/after 计数保持为 0。

证据：`testing/Agent/集成/artifacts/20260830-luna-agent-live-01-AG-I-001/`、`testing/Agent/集成/reports/20260830-luna-integration-ledger.csv`。
