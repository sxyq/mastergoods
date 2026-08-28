# Agent 对话系统需求

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 业务系统需求 |
| 当前状态 | 已完成 |
| 适用端 | Agent（后端 / Android / iOS / Web） |
| 依据源码 | `application/service/v2/V2AgentAiService.java`、`application/service/v2/agent/component/`、`application/service/v2/agent/tool/`、`infrastructure/ai/LongCatAnthropicClient.java` |
| 依据测试 | `testing/Agent/Agent综合功能与性能测试方案.md`、`V2AgentAiServiceTest.java`、`V2AgentConversationServiceTest.java`、`AgentDraftConfirmServiceTest.java` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` |
| 最后核对 | 2026-08-20 |

## 系统需求

### SR-AGT-001 Agent 对话（REST）

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-001 |
| 需求名称 | Agent 对话（REST） |
| 业务目标 | 一次性返回 Agent 回答 |
| 参与角色 | 有 agent:write 权限的成员 |
| 前置条件 | 已登录、Provider 已配置 |
| 输入 | 消息、可选 conversationId、可选 imageAssetIds |
| 处理规则 | `V2AgentAiService.chat()`：保存用户消息 → 安全检查 → Provider 请求 → 工具执行 → 正式回答 → 审计落库 |
| 输出 | AgentChatResponse（回答、块、工具调用、性能摘要、auditId/traceId） |
| 异常情况 | 安全检查拦截（blocked）、LLM 失败（llm_answer_unavailable） |
| 权限要求 | `agent:write` |
| 涉及端 | 后端 / Android / iOS / Web |
| 对应接口 | `POST /v2/agent/chat` |
| 对应源码 | `V2AgentAiService.chat()`（`application/service/v2/V2AgentAiService.java`） |
| 对应测试 | `V2AgentAiServiceTest.java` |
| 验收标准 | 回答与审计落库一致 |
| 当前状态 | 已完成（源码+测试）；8220 生产链路 Blocked |

### SR-AGT-002 Agent 对话（SSE 流式）

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-002 |
| 需求名称 | Agent 对话（SSE 流式） |
| 业务目标 | 流式输出运行事件与回答增量 |
| 参与角色 | 有 agent:write 权限的成员 |
| 前置条件 | 已登录、Provider 已配置 |
| 输入 | 消息、conversationId |
| 处理规则 | `V2AgentAiService.chatStream()` → `runChatStream()`：发送 `run_started`、工具事件、`answer_delta`、`answer_completed`、`result_block`、`run_completed` |
| 输出 | SSE 事件流 |
| 异常情况 | `error` 事件（STREAM_ERROR 等）、`run_cancelled` |
| 权限要求 | `agent:write` |
| 涉及端 | 后端 / Android / Web（iOS 待验证） |
| 对应接口 | `POST /v2/agent/chat/stream`（`produces = "text/event-stream"`） |
| 对应源码 | `V2AgentAiService.chatStream()`、`SseStreamEmitter.java` |
| 对应测试 | `testing/Agent/Agent综合功能与性能测试方案.md`（流式对话）；`AgentSseClientCancellationTest.kt`（Android） |
| 验收标准 | 事件顺序与终态正确（`answer_completed` 后 `run_completed`） |
| 当前状态 | 后端已完成（8220 SSE 直连探针 Passed）；Android 本地已完成；iOS/Web 待验证 |

### SR-AGT-003 历史会话

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-003 |
| 需求名称 | 历史会话 |
| 业务目标 | 会话列表、消息历史、分页恢复 |
| 参与角色 | 有 agent:view 权限的成员 |
| 前置条件 | 会话存在 |
| 输入 | conversationId、分页参数 |
| 处理规则 | `V2AgentConversationService.listConversations/listMessages` 按 owner 分页查询 |
| 输出 | 会话列表、消息历史 |
| 异常情况 | 会话不存在 |
| 权限要求 | `agent:view` |
| 涉及端 | 多端 |
| 对应接口 | `GET /v2/agent/conversations`、`GET /v2/agent/conversations/{id}/messages` |
| 对应源码 | `application/service/v2/V2AgentConversationService.java` |
| 对应测试 | `testing/Agent/Agent综合功能与性能测试方案.md`（Wave 1 history reload）；`AgentWorkbenchHistoryTest.kt`（Android） |
| 验收标准 | 历史消息与分页位置正确恢复 |
| 当前状态 | Android 已完成（历史分页恢复首个可见消息位置）；iOS/Web 待验证；8220 无会话数据 |

### SR-AGT-004 工具选择与执行

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-004 |
| 需求名称 | 工具选择与执行 |
| 业务目标 | 模型选择真实业务工具并执行 |
| 参与角色 | Agent |
| 前置条件 | Provider 可用、工具已注册 |
| 输入 | 工具计划（AgentToolPlan） |
| 处理规则 | `ToolPlanner` 规划 → `ToolRegistry` 校验参数并执行（READ_ONLY 直接执行；CREATE_ONLY 生成草稿） |
| 输出 | 工具结果、结果块、审计事件 |
| 异常情况 | 参数缺失 → `tool_failed`（TOOL_QUERY_FAILED） |
| 权限要求 | 工具 owner-scoped |
| 涉及端 | 后端 / Agent |
| 对应接口 | Agent 链路（无独立 REST） |
| 对应源码 | `application/service/v2/agent/tool/ToolRegistry.java`、`tool/AgentTool.java`、`component/ToolPlanner.java` |
| 对应测试 | `ToolRegistryTest.java`、`ToolPlannerTest.java`、`ToolInvocationIdentityTest.java`、各 LookupToolTest |
| 验收标准 | 工具执行与审计一致；参数 schema 完整 |
| 当前状态 | 已完成（源码+测试；schema/去重已修复） |

### SR-AGT-005 思考过程默认折叠

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-005 |
| 需求名称 | 思考过程默认折叠 |
| 业务目标 | 思考内容默认收起，点击后展开 |
| 参与角色 | 所有用户 |
| 前置条件 | 有思考/计划事件（plan_delta） |
| 输入 | 计划事件 |
| 处理规则 | Android 渲染时思考过程默认 collapsed，点击可展开（8220 基线：思考完成后自动折叠） |
| 输出 | 折叠/展开状态 |
| 异常情况 | 无 |
| 权限要求 | 无 |
| 涉及端 | Android（iOS/Web 待验证） |
| 对应接口 | SSE `plan_delta`（Android 模型 `AgentStreamModels.kt` 有 `PlanDelta`） |
| 对应源码 | `feature/agent/conversation/AgentChatScreen.kt` |
| 对应测试 | `AgentChatScreenToolStatusTest.kt`、`AgentChatViewModelAnswerMergeTest.kt` |
| 验收标准 | 默认折叠且可展开 |
| 当前状态 | Android 已完成（本地验证） |

### SR-AGT-006 结果块与图表

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-006 |
| 需求名称 | 结果块与图表 |
| 业务目标 | KPI、表格、图表结果块按消息 part 顺序渲染 |
| 参与角色 | 所有用户 |
| 前置条件 | 工具产生结果块 |
| 输入 | `result_block` 事件 / 消息 structured_data |
| 处理规则 | `selectVisibleResultBlocks` 过滤可见块（draft/draft_card 恒显，图表需请求可视化且有真实数据）；Android `ResultBlockRenderer` 渲染 |
| 输出 | KPI、表格、图表 |
| 异常情况 | 空数据图表不显示（`hasMeaningfulChartData`） |
| 权限要求 | 无 |
| 涉及端 | 多端 |
| 对应接口 | SSE `result_block` |
| 对应源码 | 后端 `V2AgentAiService.selectVisibleResultBlocks()`；Android `feature/agent/result/ResultBlockRenderer.kt`；iOS `AgentChatView.resultBlockView()`；Web `AgentPage.vue` |
| 对应测试 | `ResultBlockRendererContractTest.kt`、`AgentStoredResultBlockParseTest.kt`（Android）；`testing/Agent/Agent综合功能与性能测试方案.md` |
| 验收标准 | 结果块按 part 顺序渲染且图表数据真实 |
| 当前状态 | Android 已完成（本地验证）；iOS/Web 待验证 |

### SR-AGT-007 草稿创建 / 确认 / 取消 / 删除

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-007 |
| 需求名称 | 草稿操作 |
| 业务目标 | 写操作草稿化，用户确认后执行 |
| 参与角色 | 有 agent:write 权限的成员 |
| 前置条件 | CREATE_ONLY 工具执行 |
| 输入 | 草稿创建/更新/确认/取消/删除 |
| 处理规则 | `AgentDraftConfirmService.confirmDraft()` 执行写操作；`cancelDraft()` 取消；`V2AgentConversationService` 提供 CRUD |
| 输出 | 草稿状态变化、`draft_created` 事件 |
| 异常情况 | 草稿确认失败（如明细为空，已知问题 #5） |
| 权限要求 | `agent:write` |
| 涉及端 | 后端 / Android / iOS / Web |
| 对应接口 | `/v2/agent/drafts`、`/v2/agent/drafts/{id}/confirm`、`/v2/agent/drafts/{id}/cancel` |
| 对应源码 | `application/service/v2/agent/AgentDraftConfirmService.java` |
| 对应测试 | `AgentDraftConfirmServiceTest.java`；`testing/.artifacts/2026-07-28-draft-closedloop/evidence-summary.md` |
| 验收标准 | 确认后写操作生效且草稿终态正确 |
| 当前状态 | 后端已完成；Android 草稿列表无可达 UI 入口（已知问题 #2） |

### SR-AGT-008 取消运行

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-008 |
| 需求名称 | 取消运行 |
| 业务目标 | 用户可取消正在进行的运行 |
| 参与角色 | 发起用户 |
| 前置条件 | 运行进行中 |
| 输入 | runId |
| 处理规则 | `V2AgentAiService.cancelRun()`：标记取消 → 取消 Provider 流 → 发送 `run_cancelled` → 审计 `cancelled` → 持久化取消消息 |
| 输出 | `run_cancelled` 事件、审计终态 |
| 异常情况 | 极早取消时 SSE 收尾失败（已知问题 #7：STREAM_ERROR） |
| 权限要求 | 仅本人 run |
| 涉及端 | 后端 / Android / Web |
| 对应接口 | `POST /v2/agent/runs/{runId}/cancel` |
| 对应源码 | `V2AgentAiService.cancelRun()`、`RunAuditService.ActiveAgentRun` |
| 对应测试 | `testing/.artifacts/2026-07-31-continuation-6/17-backend-cancel-test/`、`AgentSseClientCancellationTest.kt` |
| 验收标准 | 审计终态为 cancelled 且无 STREAM_ERROR |
| 当前状态 | 进行中（已知问题 #7 待完整解除） |

### SR-AGT-009 审计与运行轨迹

| 字段 | 内容 |
|---|---|
| 需求编号 | SR-AGT-009 |
| 需求名称 | 审计与运行轨迹 |
| 业务目标 | 每次运行可审计、可追溯 |
| 参与角色 | 系统 |
| 前置条件 | 运行开始 |
| 输入 | runId |
| 处理规则 | `RunAuditService` 创建 agent_run_audits + agent_run_audit_events；`getRunAudit` 返回审计与事件 |
| 输出 | 审计记录、事件序列 |
| 异常情况 | 审计写失败计数（auditWriteFailedCount） |
| 权限要求 | `agent:view`（仅本人） |
| 涉及端 | 后端 / Android / iOS / Web |
| 对应接口 | `GET /v2/agent/runs/{runId}/audit` |
| 对应源码 | `application/service/v2/agent/component/RunAuditService.java`、`AgentRunAuditRepository` |
| 对应测试 | `testing/Agent/Agent执行台账.csv`、`V2AgentAiServiceTest.java` |
| 验收标准 | 审计事件与 SSE 事件一致 |
| 当前状态 | 已完成（源码+测试）；Android 历史消息未恢复 RunTrace（已知问题 #3） |

## 对应实现

- 后端代码：`application/service/v2/V2AgentAiService.java`、`application/service/v2/agent/component/`、`application/service/v2/agent/tool/`
- Android 代码：`feature/agent/`、`data/agent/`、`core/network/AgentSseClient.kt`、`core/model/v2/agent/`
- iOS 代码：`Features/Agent/`
- Web 代码：`pages/agent/AgentPage.vue`、`shared/api/agent-stream.ts`
- Agent 代码：`application/service/v2/agent/` 全套

## 对应接口

- 接口路径：`/v2/agent/*` 全部
- 请求模型：`api/dto/v2/agent/V2AgentDtos.java`
- 响应模型：同上
- SSE 事件：`run_started`、`plan`、`safety_check_*`、`tool_started`、`tool_progress`、`tool_completed`、`tool_failed`、`tool_skipped`、`answer_delta`、`answer_completed`、`result_block`、`draft_created`、`run_cancelled`、`run_completed`、`error`

## 对应测试

- 单元测试：`V2AgentAiServiceTest.java`、`V2AgentConversationServiceTest.java`、`AgentDraftConfirmServiceTest.java`、`component/*`、`tool/*`
- 功能测试：`testing/Agent/Agent综合功能与性能测试方案.md`
- 性能测试：`testing/Agent/Agent综合功能与性能测试方案.md`
- 审计：`testing/Agent/Agent执行台账.csv`

## 当前限制

- 未完成内容：iOS / Web Agent 主流程测试
- Blocked 内容：8220 生产 Agent 链路（无会话数据）；Android 真机
- Deferred 内容：多模态、图片输入、图片结果展示
- historical-only 内容：154 环境 Agent 历史证据
