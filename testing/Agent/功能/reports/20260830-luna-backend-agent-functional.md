# Agent backend functional evidence: 20260830-luna

## Result

本批功能 live 结果为 `Blocked`。`local` 服务在 18080 启动成功，24 个 Agent 路由匿名请求全部返回 HTTP 403；没有合法会话、测试 owner/store、可控 Provider 或授权测试数据，因此没有开始 chat、stream、草稿确认、run/audit 或工具执行。

## Counts

| 范围 | 计划量 | 实际授权执行 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|---:|
| READ_ONLY tools | 46 | 0 | 0 | 0 | 0 | 46 |
| CREATE_ONLY tools | 15 | 0 | 0 | 0 | 0 | 15 |
| tool branches (61 x 11) | 671 | 0 | 0 | 0 | 0 | 671 |
| functional live environment | 1 | 24 anonymous HTTP requests | 0 | 0 | 1 | 0 |

`0` 个工具调用、`0` 个 Provider 请求、`0` 个授权 chat/stream case。61 工具数量由源码注册文件盘点得到（46 + 15），不代表真实运行通过。

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

ToolPlanner/ToolExecutor selection and order, all 61 tools, requiredPermission, owner/store isolation, draft confirmation and replay, Loop states, compression, cancellation/disconnect/retry, image_generate Provider chain, formal answer and SSE terminal contract remain unconfirmed. The targeted Agent unit/component suite passed 220 tests, but it is not live HTTP/DB/Provider evidence.
