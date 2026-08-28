# Agent 实现索引

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 实现说明 |
| 当前状态 | 后端（上下文压缩/工具选择/分页/记忆/搜索）已完成并验证；Web/Android 已完成并验证；iOS 实现完成、xcodebuild 因无完整 Xcode 标记 Blocked |
| 适用端 | Agent |
| 依据源码 | 后端 `application/service/v2/agent/`、Android `feature/agent/`、iOS `Features/Agent/`、Web `pages/agent/` |
| 依据测试 | `testing/Agent/` |
| 依据证据 | `testing/.artifacts/2026-08-23-agent-revive-main-impl/`（01-07） |
| 最后核对 | 2026-08-24 |

## 一、Agent 各端实现索引

| 能力 | 后端 | Android | iOS | Web |
|---|---|---|---|---|
| 对话编排 | `V2AgentAiService.java` | `AgentChatViewModel.kt` | `AgentViewModel.swift` | `AgentPage.vue` |
| 会话管理 | `V2AgentConversationService.java` | `AgentV2Repository.kt`、`ConversationListPanel.kt` | `AgentViewModel.swift` | `AgentPage.vue` |
| SSE | `SseStreamEmitter.java` | `AgentSseClient.kt` | 经 APIClient 流式 | `agent-stream.ts` |
| 安全检查 | `SafetyGuard.java` | UI 展示 | `SafetyDecision` 展示 | 事件模型 |
| 工具规划 | `ToolPlanner.java`（候选范围/续轮/写目标重试） | 工具过程 UI | 工具状态 | UiToolCall |
| 工具执行 | `ToolRegistry.java` + `tool/` + `ToolExecutor.java`（范围/参数/权限门） | 工具状态 | 待验证 | 事件模型 |
| 生图工具 | `ImageGenerateTool.java` → `agent_drafts` → `AgentDraftConfirmService` → `AgentImageService` → Provider | 覆盖式确认 | 待验证 | `image_url` / `revised_prompt` |
| 上下文预算/构建/压缩 | `agent/context/`（ContextWindowResolver/TokenEstimator/ContextBuilder/ContextCompactionService） | — | — | 压缩展示 |
| 检查点 | `AgentContextCheckpointEntity` + `AgentContextCheckpointRepository` + V32 迁移 | — | — | — |
| 长期记忆 | `agent/memory/AgentMemoryService`（已接入请求链）+ V33 迁移 | — | — | — |
| 在线搜索 | `agent/search/`（Provider/URL 安全）+ `WebSearchTool` | — | — | 搜索结果块 |
| 正式回答 | `AnswerSynthesizer.java` | 增量合并 | assistantBubble | 回答渲染 |
| 结果块 | `selectVisibleResultBlocks` | `ResultBlockRenderer.kt` | `resultBlockView` | resultBlocks |
| 草稿 | `AgentDraftConfirmService.java` | `DraftListScreen.kt` | `AgentDraftsView.swift` | 草稿表单 |
| 取消 | `cancelRun()` | `stopGeneration()` | deinit 取消流 | cancelAgentRun |
| 审计 | `RunAuditService.java` | `AgentAuditRepository.kt`、`AgentResponseProvenance.kt` | 审计 sheet | fetchAgentRunAudit |

## 二、后端 Agent 组件清单

| 组件 | 位置 |
|---|---|
| component | `SafetyGuard`、`ToolPlanner`、`AnswerSynthesizer`、`RunAuditService`、`SseStreamEmitter`、`AgentPromptCatalog`、`AgentTypes`、`SafetyDecision`、`ToolInvocationIdentity` |
| context | `ContextWindowResolver`、`TokenEstimator`、`ContextBuilder`、`ContextCompactionService` |
| memory | `AgentMemoryService`（召回/异步提取/脱敏/隔离） |
| search | `WebSearchProvider`、`DisabledWebSearchProvider`、`WebSearchUrlSafety`、`WebSearchRequest/Result` |
| tool | `AgentTool`、`ToolContext`、`ToolRegistry`、`ToolExecutor`、`ToolResult`、`readonly/`（46 个）、`write/`（15 个 CREATE_ONLY 工具，含 `ImageGenerateTool`） |

## 三、工具注册机制

`ToolRegistry` 构造注入 `List<AgentTool>` 自动收集 `@Component` 工具；`READ_ONLY` 直接执行，`CREATE_ONLY` 走草稿确认。扩展新工具只需实现 `AgentTool` + `@Component`。

## 四、上下文压缩与检查点

- 预算：`ContextWindowResolver`（known model 覆盖 / unknown 保守 8192）+ `TokenEstimator`（chars/3 估算）。
- 构建：`ContextBuilder`（系统/作用域/历史/当前问题/工具结果/输出/安全余量预算；检查点存在时按边界加载）。
- 压缩：`ContextCompactionService`（预算超限 → 确定性降级摘要 / 语义摘要校验；敏感信息脱敏；并发唯一约束读已提交；失效后同一边界 revision 重建）。
- 检查点：V32 迁移（owner+conversation+boundary+policy+revision 唯一约束、ON DELETE CASCADE）；消息编辑/策略版本变化时失效。

## 五、SSE 事件契约

事件清单见 `03_系统设计/Agent系统设计/Agent总体架构.md`（12 类事件表格）；本轮新增 `context_compacted` 事件（携带 boundary/compacted_count/quality）。

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

- 单元测试：`Code/backend/src/test/java/.../application/service/v2/`（V2AgentAiServiceTest、V2AgentToolSelectionRegressionTest、ToolRegistryTest、ToolPlannerTest、AnswerSynthesizerTest、SafetyGuardTest、context/Context*Test、memory/AgentMemoryServiceTest、search/WebSearch*Test）
- Android 测试：`feature/agent/src/test/`、`core/network/src/test/`、`core/model/src/test/`
- 功能测试：`testing/Agent/功能/TEST_PLAN.md`
- 性能测试：`testing/Agent/性能/TEST_PLAN.md`
- 单元/契约/集成/安全/可靠性/客户端/数据：对应 `testing/Agent/` 分类 `TEST_PLAN.md`
- 映射与证据：`testing/Agent/README.md`、`testing/Agent/映射台账.md`

## 当前限制

- Blocked 内容：iOS xcodebuild/test（本机仅 Command Line Tools 无完整 Xcode）；PostgreSQL EXPLAIN（无环境，仅 H2 验证语义）；SQLite 执行 V32 迁移（IDENTITY 语法不兼容）；真实 Provider 语义压缩/在线搜索（未配置 Provider）；生产 Agent 链路；Android 真机
- Deferred 内容：多模态；真实跨会话记忆端到端；真实并发幂等
- historical-only 内容：154 环境 Agent 证据
