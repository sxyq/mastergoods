# Agent backend integration evidence: 20260830-luna

本批集成实测为 `Blocked`。服务和隔离 H2 可用，但没有授权会话、测试 owner/store、可控 Provider 或完整业务夹具。`AG-I-001..013` 均未进入真实 Agent 集成链路。

| 统计项 | 数值 |
|---|---:|
| 计划父卡/专项用例 | 13 |
| 独立集成记录应执行 | 13 |
| 独立集成记录已生成 | 0 |
| Passed | 0 |
| Failed | 0 |
| Blocked | 13 |
| Deferred | 0 |
| 实际集成请求 | 0 |

独立集成派生记录尚未生成。证据完整率为：共享预检目录 `1/1`（100%，含 00-10 文件），独立集成证据 `0/13`（0%）；共享目录不能替代逐条集成证据。

服务启动和匿名路由观测不等于集成通过。24 个匿名 HTTP 请求均在 Agent 执行前返回 403；没有 ToolPlanner/ToolExecutor 调用、Provider 调用、run/audit、SSE 终态或业务数据变化。Agent 表 before/after 计数保持为 0。

证据：`testing/Agent/集成/artifacts/20260830-luna-agent-live-01-AG-I-001/`、`testing/Agent/集成/reports/20260830-luna-integration-ledger.csv`。`AG-I-001..013` 的独立请求、工具链、Provider、run/audit 和数据库断言仍未覆盖。
