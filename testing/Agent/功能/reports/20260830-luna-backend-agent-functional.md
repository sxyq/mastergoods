# Agent backend functional evidence: 20260830-luna

## Result

本批功能 live 结果为 `Blocked`。`local` 服务在 18080 启动成功，24 个 Agent 路由匿名请求全部返回 HTTP 403；没有合法会话、测试 owner/store、可控 Provider 或授权测试数据，因此没有开始 chat、stream、草稿确认、run/audit 或工具执行。

## Counts

| 统计项 | 数值 |
|---|---:|
| 计划父卡 | 111（工具 61 + 其他功能卡 50） |
| 工具派生应执行 | 671（61 x 11） |
| 独立派生记录已生成 | 0 |
| 台账父/分组行 | 17 |
| Passed | 0 |
| Failed | 0 |
| Blocked | 1 |
| Deferred | 16 |
| 匿名 HTTP 请求 | 24 |
| 授权 Agent 请求 | 0 |

`0` 个工具调用、`0` 个 Provider 请求、`0` 个授权 chat/stream case。61 工具数量由源码注册文件盘点得到（46 + 15），不代表真实运行通过。671 个独立派生记录尚未生成，台账中的父/分组行只引用共享预检证据。

证据完整率分开统计：共享预检目录为 `1/1`（100%，含 00-10 文件）；独立工具派生证据为 `0/671`（0%）。

## Observed boundary

- Version: application `0.1.0`, Spring Boot `3.2.6`, Java `21.0.11`.
- Configuration: default model `glm-5.3-flash`; existing OneAPI default remained configured. Secret values were not read or stored.
- HTTP: all 24 Controller mappings responded `403 application/json` with a standard `Forbidden` response shape.
- Agent database tables: before and after counts were zero for conversations, messages, drafts, memories, tasks, notifications, checkpoints, run audits, and audit events.
- SSE: no stream was opened; no event, terminal state, formal answer, tool fact, run_id, or audit was produced.
- Cleanup: service stopped; port 18080 was clear; no test data existed to delete.

## Evidence

- `testing/Agent/功能/artifacts/20260830-luna-agent-live-01-AG-F-ENV-001/`
- `testing/Agent/功能/reports/20260830-luna-functional-ledger.csv`

## Not confirmed

ToolPlanner/ToolExecutor selection and order, all 61 tools, requiredPermission, owner/store isolation, draft confirmation and replay, Loop states, compression, cancellation/disconnect/retry, image_generate Provider chain, formal answer and SSE terminal contract remain unconfirmed. The targeted Agent unit/component suite passed 220 tests, but it is not live HTTP/DB/Provider evidence. The 671 independent derived cases remain ungenerated.
