# Agent 实现索引

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 实现说明 |
| 当前状态 | 后端/Android 已完成；iOS/Web 待验证 |
| 适用端 | Agent |
| 依据源码 | 后端 `application/service/v2/agent/`、Android `feature/agent/`、iOS `Features/Agent/`、Web `pages/agent/` |
| 依据测试 | `testing/Agent/` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 一、Agent 各端实现索引

| 能力 | 后端 | Android | iOS | Web |
|---|---|---|---|---|
| 对话编排 | `V2AgentAiService.java` | `AgentChatViewModel.kt` | `AgentViewModel.swift` | `AgentPage.vue` |
| 会话管理 | `V2AgentConversationService.java` | `AgentV2Repository.kt`、`ConversationListPanel.kt` | `AgentViewModel.swift` | `AgentPage.vue` |
| SSE | `SseStreamEmitter.java` | `AgentSseClient.kt` | 未发现 | `agent-stream.ts` |
| 安全检查 | `SafetyGuard.java` | UI 展示 | 待验证 | 事件模型 |
| 工具规划 | `ToolPlanner.java` | 工具过程 UI | 待验证 | UiToolCall |
| 工具执行 | `ToolRegistry.java` + `tool/` | 工具状态 | 待验证 | 事件模型 |
| 正式回答 | `AnswerSynthesizer.java` | 增量合并 | assistantBubble | 回答渲染 |
| 结果块 | `selectVisibleResultBlocks` | `ResultBlockRenderer.kt` | `resultBlockView` | resultBlocks |
| 草稿 | `AgentDraftConfirmService.java` | `DraftListScreen.kt`（入口 Blocked） | `AgentDraftsView.swift` | 草稿表单 |
| 取消 | `cancelRun()` | `stopGeneration()` | 未发现 | cancelAgentRun |
| 审计 | `RunAuditService.java` | `AgentAuditRepository.kt`、`AgentResponseProvenance.kt` | 审计 sheet | fetchAgentRunAudit |

## 二、后端 Agent 组件清单

| 组件 | 位置 |
|---|---|
| component | `SafetyGuard`、`ToolPlanner`、`AnswerSynthesizer`、`RunAuditService`、`SseStreamEmitter`、`AgentPromptCatalog`、`AgentTypes`、`SafetyDecision`、`ToolInvocationIdentity` |
| tool | `AgentTool`、`ToolContext`、`ToolRegistry`、`ToolResult`、`readonly/`（40+ 工具）、`write/`（CREATE_ONLY 工具） |

## 三、工具注册机制

`ToolRegistry` 构造注入 `List<AgentTool>` 自动收集 `@Component` 工具；`READ_ONLY` 直接执行，`CREATE_ONLY` 走草稿确认。扩展新工具只需实现 `AgentTool` + `@Component`。

## 四、SSE 事件契约

事件清单见 `03_系统设计/Agent系统设计/Agent总体架构.md`（12 类事件表格）。

## 对应实现

- 后端代码：`application/service/v2/V2AgentAiService.java`、`application/service/v2/agent/`
- Android 代码：`feature/agent/`、`data/agent/`、`core/network/AgentSseClient.kt`
- iOS 代码：`Features/Agent/`
- Web 代码：`pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`
- Agent 代码：`application/service/v2/agent/`

## 对应接口

- 接口路径：`/v2/agent/*`
- 请求模型：`V2AgentDtos`
- 响应模型：`V2AgentDtos`
- SSE 事件：`SseStreamEmitter`

## 对应测试

- 单元测试：`Code/backend/src/test/java/.../application/service/v2/`（V2AgentAiServiceTest、ToolRegistryTest、ToolPlannerTest、AnswerSynthesizerTest、SafetyGuardTest 等）
- Android 测试：`feature/agent/src/test/`、`core/network/src/test/`、`core/model/src/test/`
- 功能测试：`testing/Agent/功能测试/TEST_PLAN.md`
- 性能测试：`testing/Agent/性能测试/TEST_PLAN.md`
- 审计：`testing/Agent/审计/`

## 当前限制

- 未完成内容：iOS / Web Agent 主流程测试
- Blocked 内容：8220 生产 Agent 链路；Android 真机
- Deferred 内容：多模态
- historical-only 内容：154 环境 Agent 证据
