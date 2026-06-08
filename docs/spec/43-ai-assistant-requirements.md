# 43 AI 助手真实 Agentic 需求文档

> 状态：需求基线，供后续 AI 功能审查与修改使用
> 版本：2026-06-09 v19
> 维护范围：AI 助手需求基线与当前代码证据校准；不代表本轮已经完成所有后端或 Android 代码整改
> 覆盖范围：后端 `/v2/agent/*`、Android AI 助手页面、AI 首页干净入口、真实数据查询、真实工具事件、草稿执行、取消确认、运行审计、性能可观测性、后续审查 checklist

## 0. 当前证据快照

本节只记录截至 2026-06-09 当前工作树可由代码和测试证明的事实，不能替代第 17 节端到端验收证据包。任何未列为“已证明”的能力，在后续审查中都必须按未完成处理。

### 0.1 已由当前代码 / 测试证明

| 能力点 | 当前证据 | 审查结论 |
|---|---|---|
| 非流式 `/v2/agent/chat` Android 模型兼容 agent 审计字段 | `master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/AgentChatRequestResponse.kt` 已包含 `plan_summary`、`tool_calls`、`evidence_refs`、`result_blocks`、`performance_summary`；`AgentChatResponseSerializationTest.decodesNonStreamingAgentRunContract` 已通过 | 只能证明 Android 模型可解析这些字段，不证明真实接口端到端已返回完整字段 |
| 非流式后端服务响应具备可审计字段 | `V2AgentAiServiceTest.nonStreamingChatIncludesAuditableAgentRunContract` 强制重跑通过，断言 `planSummary`、`toolCalls`、`evidenceRefs`、`evidence_card`、`performanceSummary` | 证明服务单测路径具备合同雏形；仍需真实 HTTP 响应和 owner 数据证据 |
| SSE `answer_delta` 不再承载规则摘要假流式 | `V2AgentAiServiceTest.streamFallbackAnswerCompletesRuleSummaryWithoutFakeDeltas`、`streamDisabledModelAnswerCompletesRuleSummaryWithoutFakeDeltas` 通过，断言规则摘要路径 `answer_delta` 数量为 0，降级内容只在 `answer_completed` 返回 `mode`、`llm_status` 和规则摘要说明 | 证明降级摘要不会通过分块制造“吐字”体验；仍需真实抓包和 UI 展示截图 |
| SSE `server_notice` 可承载服务端真实补充说明 | `V2AgentAiServiceTest.streamModelAnswerEmitsServerNoticeTailBeforeCompletionWhenBackendAppendsBoundaries` 证明模型真实 delta 后，后端追加的查询边界说明会以 `answer_delta(delta_source=server_notice)` 在 `answer_completed` 前发出；`AgentResponseProvenanceTest.serverNoticeDeltaIsLabeledAsBackendNoticeNotModelStream` 证明 Android 不把它标成模型流 | 证明查询边界 / 部分失败等服务端事实说明不会在最终完成事件突然整段跳出，也不会伪装成模型 token；仍需真实 SSE 抓包和真机截图 |
| SSE 结构化结果块可随工具完成提前出现 | `V2AgentAiService.buildResponse()` 在每个工具返回后立即 `emitBlocks(...)`，最后单独发送 `evidence_card`；`V2AgentAiServiceTest.streamEmitsEachToolResultBlockBeforeNextToolCompletes` 断言第一个工具的 `result_block` 早于第二个工具完成，且证据卡早于 `answer_completed` | 证明服务端不再等最终回答完成后才统一发送全部图表 / 表格；仍需真实 SSE 抓包和真机截图证明端到端节奏 |
| Android 对话时间线按服务端事件顺序渲染 | `ChatMessage.parts` 增加 `Text` / `ResultBlock` 顺序片段；`AgentChatViewModelAnswerMergeTest` 覆盖 answer delta、result block、final answer 的合并顺序，且最终答案不会重排已有 `Text -> ResultBlock -> Text` 时间线；`AgentChatScreen` 以 `AssistantMessageTimeline` 渲染片段 | 证明 Android 不再把所有结构化结果固定堆到回答下方，也不会在完成态把流式时间线重新搬动；仍需真实模型流式 SSE 和真机截图证明端到端体验 |
| Android 工具提示只展示真实短状态并自动收敛 | `AgentChatScreenToolStatusTest.latestVisibleToolShowsRecentlyCompletedToolBriefly` 证明工具完成后可短暂显示完成态；`latestVisibleToolDoesNotKeepCompletedToolAsPersistentPill` 证明过期完成工具不会作为 inline pill 常驻；`AgentResponseProvenanceTest.streamingRunTracePanelCollapsesAfterVisibleTimelineArrives` 证明文本或 result block 到达后默认收起 RunTrace；`AgentResponseProvenanceTest.runTracePanelHidesForCompletedSuccessUnlessAttentionIsNeeded` 证明完成成功 / 规则摘要完成态不再默认展开过程面板 | 证明 UI 会像真实 agent 一样显示正在查询 / 刚完成 / 失败的短状态，但不会把完成工具或完整 RunTrace 长期贴在回答下方；错误、手动展开、首个可见事件前仍保留审计入口；仍需真实工具事件抓包和真机截图 |
| Android SSE 客户端支持标准多行 SSE | `AgentSseClientCancellationTest.chatStream_buffersStandardMultiLineSseDataUntilBlankLine` 和 `chatStream_flushesLastBufferedSseEventWhenStreamEndsWithoutBlankLine` 覆盖多行 `data:` 缓冲与 EOF flush | 证明客户端可正确接收标准 SSE 事件；仍需真实后端流和供应商模型流抓包 |
| AI workbench 是显式干净入口合同 | `V2AgentAiService.getWorkbench()` 返回 `status=clean_entry_ready`、`data_policy`、`capabilities` 和 `warnings`，并保持 KPI、风险、今日摘要、快捷报表问题为空；`V2AgentAiServiceTest.workbenchDoesNotExposeReportDashboardDefaults` 和 `AgentChatResponseSerializationTest.decodesCleanWorkbenchStatusContract` 覆盖服务端与 Android 模型；`tools/ai_agent_evidence_capture.sh self-test` 会拒绝缺少该状态合同的 workbench 证据 | 证明 workbench 不再只是空数组 placeholder，而是明确说明真实数据只在用户发起 chat run 后查询；仍需真机首页截图和 UI tree |
| Android result block 渲染有坏数据门禁 | `ResultBlockRendererContractTest` 覆盖图表缺 labels、空 labels、缺 series、series 长度不一致、NaN / Infinity、柱状负数、donut / pie 非正数或无效分段、已知图表缺字段解析失败、未知 block 原始摘要、结构化 table 行列不一致和 table 单元格 Markdown；`ResultBlockRenderer` 对这些情况显示错误 / 空态 / 忽略提示，不补模拟图表或模拟表格数据 | 证明 UI 层不会主动补模拟图表 / 表格数据；仍需真实后端 block、真机截图和坏块视觉验收 |
| Markdown 解析已有基础覆盖 | `AgentMarkdownTextParserTest` 覆盖表格 pipe、代码块尾部空白、链接文本旁可见 URL、`www.` 链接规范化和坏链接不丢正文 | 证明部分解析边界和链接 URL 不丢；仍需真机视觉截图、链接点击 / 复制、长表格、代码复制交互和流式半成品视觉验收 |
| 服务端 cancel run 代码路径已存在 | `V2AgentController` 暴露 `POST /v2/agent/runs/{runId}/cancel`；`V2AgentAiService.cancelRun` 会校验 owner、标记 active run cancelled、发送 `run_cancelled`，不再立即移除仍运行的 active run；`V2AgentAiServiceTest.cancelRunMarksActiveStreamCancelledAndEmitsRunCancelledEvent` 已证明 active stream 取消后会发出 `run_cancelled`、阻止 `answer_completed`、并把审计状态写成 `cancelled`；`cancelRunDoesNotPretendUnknownRunWasCancelled` 和 `cancelRunDoesNotCancelOtherOwnerActiveRun` 证明未知 run / 跨 owner active run 只返回 `not_found/cancelled=false`，不会发送 `run_cancelled` 或伪造取消审计；Android `AgentChatViewModel.stopGeneration` 会调用 `AgentV2Repository.cancelRun` 并把服务端取消确认 / 未确认 / 失败写入反馈 | 证明当前代码有更诚实的取消路径，并已覆盖“不能把取消失败伪造成成功”的服务端合同；仍需真实 Android 点击停止后的 HTTP/SSE 抓包、审计接口对账和 Android 取消反馈截图 |

### 0.2 当前仍未完成 / 不得误判为通过

| 门禁 | 当前缺口 | 必须补充的证据 |
|---|---|---|
| 一比一 UI 还原 | 本文档仅定义 AI 助手验收基线，不证明所有页面已按设计稿还原 | 每个界面与设计稿逐屏对照截图、差异清单、真机截图和可交互验证 |
| 真实端到端 agentic run | 当前测试主要是单元测试和模型解析，尚未归档真实 `/v2/agent/chat` 或 `/chat/stream` 证据包 | 按第 17.1 节生成每个真实问题的 HTTP、SSE、工具结果、审计、截图、耗时证据 |
| 真模型流式输出 | 当前已证明 `rule_summary` 诚实降级、Android 可渲染 `model_stream` delta，不等于证明供应商 `model_stream` 真流式 | 抓包证明 `delta_source=model_stream` 与模型供应商 streaming、`mode`、`llm_status`、审计一致 |
| AI 首页干净入口 | 文档规定不得展示报表型数据，但仍需真机 UI tree / 截图确认当前实现 | `05-ui-home.png` 和 `09-ui-tree.xml`，证明无销售额、KPI、报表图、风险列表默认展示 |
| RunTrace 展开与 UI 区分度 | 文档规定用户 / AI / 工具 / 结果 / 错误分层，但仍需视觉证据 | 真实对话截图，含展开 RunTrace、Markdown、result block、错误或降级态 |
| 草稿真实执行 | 当前 P0 允许不执行写操作；不能把 `archived` 当执行成功 | P1 前确认按钮禁用或诚实归档；P1 后需业务单据真实创建 / 更新证据 |
| 全链路性能优化 | 当前已做局部优化：SSE worker 使用专用 executor；Android answer_delta 48ms 批量刷新；流式中先轻量文本渲染，完成后再 Markdown；AI 聊天列表提供稳定 key / contentType，Markdown inline 解析结果按文本缓存以减少流式重组开销；部分工具查询改为 DB 分页；AI workbench 不再查最近会话 / 草稿；Reports 往来余额改走后端汇总；Dashboard 应收金额 / 应收客户数 / 净现金流优先走后端汇总；`docs/acceptance-evidence/performance/20260609-052957-backend-report-performance/` 已采集 7 个后端报表接口各 5 次样本且均为 HTTP 200 / `code=0`。仍不等于完整性能验收 | 性能基线、优化前后对比、首事件 / 工具 / 模型 / Android 首次可见耗时表 |
| 底部 tap 栏 BiliPay 参考对齐 | 当前仅对齐了玻璃态、横向扫动、底栏区域上滑转发首页滚动，以及跨 tab 距离感动画；尚未重构为 BiliPay 的 `HorizontalPager + MainBottomPagerState + indicatorProgress` 主架构 | 后续若要求完全一比一，需用真实 pager 承载顶级页面，提供切换录屏、帧率 / jank 证据和与 `/Users/sunyiyang/Desktop/Project/Bilipay UI` 的文件级对照表 |

### 0.3 证据快照一致性规则

后续每次更新本文档的“已证明 / 当前代码证据快照”时，必须同步记录：

- 当前提交：`git rev-parse --short HEAD`。
- 证明用测试名或验收包路径。
- 直接源码路径和关键行号。
- 若第 0 节与第 22 节结论冲突，以当前源码和最新测试为准，并在同一提交内修正文档旧结论。

截至 `ed4d630`，非流式 `AgentChatResponse` 已在后端 DTO 和 Android 模型中包含 `tool_calls`、`evidence_refs`、`performance_summary`、`result_blocks` 等审计字段；任何仍声称“同步响应尚未提供这些顶层字段”的旧结论必须视为过期。

截至 2026-06-09 当前待提交工作树，Android 已增加按事件顺序渲染的 `ChatMessage.parts` 时间线、真实工具状态短提示、规则摘要 / 模型流标签区分、完成态提示收敛，以及标准多行 SSE 缓冲解析；后端 workbench 已增加 `clean_entry_ready`、`data_policy`、`capabilities` 和 `warnings` 以替代空壳式入口响应；后端流式接口已改为每个工具完成后即时发送该工具的 `result_block`，并在最终回答前发送 `evidence_card`。这些只能证明代码和单测层面契约，不得替代真实端到端验收证据。

## 1. 背景和原则

用户当前只要求先给出 AI 助手需求文档，后续再按本文档审查和修改 AI 功能。本文档是后续实现、验收和代码审查的统一基线。

智慧记 AI 助手必须提供真实 agentic 体验：根据用户问题理解意图，规划需要查询的数据，调用当前账号可访问的真实业务工具，基于真实结果生成回复，并记录可追溯的运行事件。任何模拟数据、模拟过程、假流式、固定模板回答，都不能作为 AI 助手生产体验通过验收。

核心原则：

- 全链路去模拟优先：生产链路不得保留用于 AI 助手体验的 mock、demo、fake、sample、placeholder、固定延迟、固定步骤、假任务、假通知、假草稿或假流式。
- 真实数据优先：所有数字、列表、风险、任务、通知、草稿必须来自当前 owner 的真实后端数据或真实空态。
- 真实过程优先：运行轨迹、工具状态、SSE 分片、取消、失败必须来自后端真实 run lifecycle。
- 诚实降级优先：模型不可用时可以做真实查询摘要，但必须标明规则 / 数据查询模式，不得伪装为 LLM 智能推理。
- 用户确认优先：AI 不得直接执行写操作；高风险动作必须经过草稿、预览、确认、真实执行、审计。
- 可审计优先：每次运行必须能追踪 owner、安全、计划、工具、模型、草稿、错误和最终结果。

## 2. 目标

P0 目标：

- 去掉 AI 助手生产链路中的所有 mock、demo、fake、sample、placeholder、模拟过程和模拟数据。
- 用户提出经营问题时，后端根据问题真实查询商品、库存、客户、供应商、销售、采购、付款、财务等业务数据后回复。
- 每条真实回复必须能说明查询范围、使用工具、数据来源、截断状态、失败状态和回答限制；不能只给无来源的泛泛建议。
- `/v2/agent/chat` 提供非流式真实回答；`/v2/agent/chat/stream` 只在真实事件存在时提供 SSE 事件。
- Android 仅展示后端真实回答、真实 result blocks、真实 run trace、真实草稿、真实任务和真实通知。
- AI 首页必须是干净入口，不展示销售额、风险、今日摘要、经营图表等报表型数据；这些内容保留在报表页或按用户问题在对话中实时查询。
- AI 聊天界面必须能清楚区分用户消息、助手回复、工具 / 思考过程、结构化结果和错误状态，不能所有内容都使用同一种颜色和同一种卡片层级。
- `AGENT_LLM_ENABLED=false`、模型未配置、模型失败、工具失败、网络失败时都有诚实降级状态。
- 工具调用、运行审计、用户取消、草稿确认必须有清晰状态和用户可感知反馈。
- P0 验收必须记录端到端耗时、首事件耗时、工具耗时、模型耗时、错误率和审计写入结果。
- 运行审计能支持后续产品、安全、后端、Android 联合排查。
- Markdown 必须作为 AI 回复的一等渲染能力，标题、列表、表格、引用、代码、链接和行内强调必须美观可读；不得因为流式输出而退化成纯文本。
- Agent 生成的统计图必须来自后端真实 `ResultBlockDto` 和真实工具结果；Android 只能渲染，不得为了好看补示例序列、默认排行或假图表。

P1 目标：

- 支持 AI 生成真实落库草稿，并由用户确认后执行真实业务写入。
- 增强 run cancel 的持久化状态、跨进程恢复、长任务中断、任务 / 通知闭环、失败重试和更完整审计查询。
- 支持多工具计划、分页结果、截断续查和部分成功回答。

P2 目标：

- 支持图片 / 单据识别、语音、多轮长期记忆、后台自动化和多 agent 协作。

## 3. 非目标

本轮文档不要求立即实现代码，也不要求修改 UI、后端服务或数据库结构。

P0 非目标：

- 不要求 AI 直接创建销售单、采购单、付款单、库存调整或财务流水。
- 不要求展示未实现的长任务后台进度。
- 不要求为了演示好看而生成任务、通知、草稿或经营趋势。
- 不允许 Android 直连模型供应商或持有模型密钥。
- 不允许把 demo seed 数据作为生产 AI 任务 / 通知来源。
- 不允许把 `archived` 当成“草稿已确认并执行”的替代状态。

## 4. 当前已知问题和整改门禁

| 已知问题 | 风险 | P0 整改要求 | 验收方式 |
|---|---|---|---|
| `AGENT_LLM_ENABLED` 默认 false 时降级为规则 / 模板 | 用户以为是真 AI，实际只是固定规则 | 响应必须返回 `mode=rules_only` 或等价状态；UI 明确显示“模型暂不可用，当前仅基于实时数据查询和规则摘要” | 用 `AGENT_LLM_ENABLED=false` 启动后提问，截图 / 日志证明无“AI 正在思考”等误导文案 |
| `V2AgentAiService` 关键词路由、固定模板回答 | 用户问题没有真正被 planner 理解，复杂问题会错答 | 关键词只能作为兜底工具选择，不得作为最终答案模板；最终答案必须引用本次工具结果 | 提 3 类问题，检查每个回答都包含本次工具证据 |
| 工具查询只取前 10 条或最近列表 | 回答会把截断数据说成全量结论 | 后端 `tool_completed` 已补 `total_count`、`returned_count`、`limit`、`is_truncated`、`duration_ms`、`next_cursor`、`evidence` 和 `query_window`，Android 已接收并在 RunTrace 展示；仍需真实超限数据抓包证明回答和 UI 均标注截断 | 构造超过 10 条数据，确认 UI 和回答标注截断 |
| SSE 规则摘要或模型失败降级来源标记端到端未验收 | 用户无法区分模型真流式和后端规则摘要完成态 | 后端规则摘要路径不得发送 `answer_delta`；只有真实供应商 streaming 才能发送 `delta_source=model_stream`；Android 必须用 `answer_completed.mode/llm_status` 显示降级状态；仍需真实 SSE 抓包和真机截图 | 抓包确认 `rule_summary` 无 delta、`model_stream` 只来自供应商 streaming、mode、llm_status 与 UI 文案一致 |
| 工具事件字段仍需端到端验收 | RunTrace 若字段缺失会难以证明真实查询范围、截断、耗时和失败边界 | `tool_started` / `tool_completed` / `tool_failed` 已补 `input_summary`、`query_window`、`started_at`、`completed_at`、`evidence`、`next_cursor` 和追踪字段，Android `ToolCompleted` / `ToolCallRecord` / `ToolAuditRecord` 已接收并在 RunTrace 工具卡显示短摘要；服务端已新增 `agent_run_audits` run 摘要审计和 `agent_run_audit_events` 事件级 payload 审计；仍缺真实 SSE / DB 对账证据包 | SSE 日志必须包含工具事件序列和字段完整性证据 |
| 非流式 `/v2/agent/chat` 与流式 trace 合同割裂 | 审查同步接口时无法独立证明 plan、tool、evidence 和性能来源 | 非流式响应必须补 `plan` / `tool_calls` / `evidence_refs` / `performance_summary`，或提供兼容别名；不能只依赖流式 UI 层 RunTrace | 直接调用 `/v2/agent/chat`，响应体能独立审查本次计划、工具、证据、结构化结果、模式和耗时 |
| 后端尚未生成可渲染 `evidence_card` | Android 虽支持 evidence block，但用户看不到可核对依据 | 后端工具结果必须把关键证据聚合为 `evidence_card` 或等价 result block，并关联 tool result | 真实问题返回的 blocks 中包含依据卡，金额 / 数量 / 排行能追溯到工具和查询窗口 |
| `RunTrace` 展开 no-op | 用户无法查看真实过程 | Android 展开 / 折叠必须改变状态并展示真实事件；没有事件时显示“当前接口未返回运行轨迹” | 点击轨迹展开，验证状态变化和事件来源 |
| AI 首页展示 KPI、摘要、风险提醒等报表型数据 | AI 入口和报表页职责混乱，初始界面拥挤且重复 | AI 首页只保留问候、提问入口、快捷问题、任务 / 草稿入口和干净说明；报表型经营数据不得出现在初始屏 | 首次进入 AI 首页截图，确认无销售额、KPI、图表、风险列表、经营摘要 |
| AI 首页文案继续暗示今日报表、风险看板或统计图看板 | 用户仍会把 AI 首页理解成报表页副本，且容易诱导未来补假数据 | 初始屏文案可以说明“按问题生成 Markdown / 图表 / 依据”，但不得出现今日经营摘要、风险列表、报表页数据搬运、默认统计图看板等承诺 | 首次进入 AI 首页截图和 UI tree，确认没有报表型文案堆叠；快捷问题必须是提问入口而不是默认报表展示 |
| AI 工作台远端同步失败时继续强承诺真实查询 | 用户误以为工作台数据和远端 Agent 状态已同步成功 | 远端工作台失败时只能保留对话入口，并明确发送问题后仍需连接服务端；不得显示“已同步”或默认报表式能力承诺 | 断开后端或模拟失败，首屏状态文案显示“远端未同步 / 仅保留入口” |
| 聊天界面所有气泡和过程卡颜色接近 | 用户无法快速分辨用户、AI、工具过程、结构化结果和错误 | 用户消息、助手消息、RunTrace、result blocks、错误提示必须使用不同色调 / 层级 / 标识；Markdown 内容保持可读 | 用一轮真实查询截图，确认角色、过程、结果块和错误状态可区分 |
| Markdown 链接不可点击、URL 丢失或代码块不可复制 | AI 输出虽然像 Markdown，但用户无法核对来源或复用内容 | P0 必须保留链接 URL 并以可识别样式展示；P1 支持点击链接和复制代码块；任何 Markdown 解析失败必须降级为安全纯文本而不是丢内容 | 用包含链接、表格、代码块、引用、列表的真实回答截图 / UI tree 验收 |
| 已知 result block 类型解析失败后静默消失 | 后端真实图表 / 表格已经返回，但 Android 用户看不到，也无法知道数据丢失 | P0 不允许静默丢弃 result block；解析失败必须显示“结构化结果暂无法渲染”并保留标题、类型、错误码或原始摘要 | 构造已知类型字段缺失 / 类型错误，确认 UI 显示失败卡且日志可定位 |
| draft 确认只是把状态改为 `archived` | 假确认，业务未执行 | P1 前确认按钮禁用或标注“仅归档”；P1 必须新增确认执行接口和状态机 | 点击确认后检查业务单据是否真实创建 / 更新 |
| 草稿缺字段时合成本地业务号 / 金额 | 用户会把本地占位当作真实草稿字段 | 缺字段必须显示“后端未返回某字段”或结构化错误，不能合成 `草稿 #id`、默认金额或默认往来方 | 构造缺业务号 / 往来方 / 金额的草稿，列表显示字段缺失而不是假业务值 |
| cancel run 尚缺真机端到端验收 | 后端服务单测已证明 active stream 取消会返回 `cancelled`、发送 SSE `run_cancelled`、阻止后续 `answer_completed`，并把审计状态写成 `cancelled`；未知 run 和跨 owner active run 已固定为 `not_found/cancelled=false` 且不伪造 `run_cancelled` 或取消审计；但还缺 Android 真机点击停止后的 HTTP/SSE 抓包和 UI 反馈截图 | P0 必须真实调用 `/v2/agent/runs/{run_id}/cancel`；Android 若取消请求失败必须显示“已停止本机接收，服务端取消失败或仍在处理” | 取消后检查 HTTP 响应、SSE `run_cancelled`、后端 active run 收尾移除、审计状态和 Android 提示 |
| demo seed 产生 agent tasks / notifications | 生产任务 / 通知可能被演示数据污染 | demo seed 只能在 local/demo profile 下写入，生产 profile 不得产生 AI 任务 / 通知 | 生产 profile seed / 启动后任务通知为空或来自真实 run |
| 缺少性能和可观测性门禁 | 功能看似可用但线上无法定位慢请求、断流、失败或审计丢失 | 每个 run 必须输出可关联日志 / 指标 / 审计；验收记录首事件、总耗时、工具耗时、模型耗时、失败原因 | 对 3 个真实问题导出日志、SSE、审计和耗时表 |

## 5. 真实 Agent 架构

### 5.1 分层职责

| 层级 | 职责 | 禁止项 |
|---|---|---|
| Android UI | 展示问题、回答、结构化结果、RunTrace、草稿、任务、通知、错误和降级态 | 不得构造假回答、假事件、假通知、假草稿、假流式 |
| Android Repository / SSE Client | 调用 `/v2/agent/*`，解析真实响应和 SSE 事件，处理取消接收 | 不得本地 timer / delay / 字符拆分制造流式 |
| Agent API | 暴露 workbench、chat、stream、runs、drafts、tasks、notifications、audit | 不得绕过 owner，不得把规则结果包装成模型结果 |
| Run Orchestrator | 创建 run，驱动安全检查、planner、工具、模型、草稿、审计、SSE | 不得无 run 状态地一次性拼答案 |
| Planner | 基于用户问题、权限、LLM 状态和工具白名单生成计划 | 不得输出写操作工具，不得生成 SQL |
| Tool Registry | 管理只读工具 schema、权限、输入校验、执行、分页、错误 | 不得静默失败成空结果 |
| Model Client | 负责模型规划、总结、回答生成和真实 delta streaming | 不得向 Android 泄露密钥 / base URL |
| Draft Executor | 生成草稿、预校验、确认执行、状态流转 | 不得用 archived 代替执行成功 |
| Audit / Trace Store | 记录 run、事件、工具、模型、草稿、错误 | 不得记录密钥、token、完整内部 prompt 或跨 owner 数据 |

### 5.2 请求处理链路

1. Android 提交用户问题到 `/v2/agent/chat` 或 `/v2/agent/chat/stream`。
2. 后端创建 `runId`，绑定 `ownerUserId`、`conversationId`、用户问题摘要和模型状态。
3. 安全检查拦截越权、敏感、SQL、破坏性或未确认写操作。
4. Planner 生成只读工具计划；LLM 不可用或规划结果不可用时当前使用关键词兜底 planner，并必须记录可区分的 `plan_source=keyword_fallback`，不得把兜底规划伪装成模型规划。
5. Tool Registry 按计划执行真实 owner-aware 查询。
6. Orchestrator 聚合工具结果，生成 result blocks、证据摘要和回答所需事实。
7. LLM 可用时基于事实生成回答；LLM 不可用时只输出事实摘要和降级说明。
8. 如需写操作，仅生成草稿并返回真实 `draftId`；不直接执行。
9. 审计记录完整 run lifecycle；SSE 按真实事件增量推送。
10. Android 渲染后端事件和结果，不补造任何 agentic 过程。

### 5.3 最小响应合同

`/v2/agent/chat` 最低响应字段：

- `run_id`
- `conversation_id`
- `mode`：`llm`、`rules_only`、`tool_only`、`blocked`、`partial`
- `llm_status`：`available`、`streaming`、`disabled`、`not_configured`、`stream_not_supported`、`stream_failed_or_empty`、`stream_interrupted`、`failed_or_empty`、`not_requested`、`timeout`、`error`
- `answer`
- `blocks`
- `tool_results_summary`
- `plan_source`：`llm`、`keyword_fallback`、`keyword`（旧兼容值）、`rules`、`manual`、`unsupported`
- `plan_summary`：本次选择哪些工具、为什么选择、哪些工具未接入
- `tool_calls[]`：至少包含 `tool_call_id`、`tool_name`、`status`、`input_summary`、`query_window`、`returned_count`、`total_count`、`is_truncated`、`duration_ms`、`error_code`
- `evidence_refs[]`：回答中关键金额、数量、排行、风险对应的 `tool_call_id`、字段名、聚合窗口和截断状态
- `draft_id`
- `safety_passed`
- `safety_reason`
- `audit_id`
- `warnings`
- `performance_summary`：至少包含 `started_at`、`completed_at`、`duration_ms`、`tool_duration_ms`、`model_duration_ms`
- `observability`：至少包含 `request_id` 或 `correlation_id`、`trace_id`、`log_ref` 或可定位审计记录

已有字段可以渐进兼容，但新增实现必须能表达上述语义。Android 不得通过猜测字段自行判断模型状态。

非流式 `/v2/agent/chat` 不能因为没有 SSE 就丢失 agentic 证据：即使只返回一次 HTTP JSON，也必须能从响应体独立审查 plan、tool、evidence、blocks、mode、llm_status、performance 和 observability。后续验收不得用“只有 stream 才能看过程”为理由让非流式接口通过。

### 5.4 真实问题查询与真实回复合同

真实经营问题必须经过“分类、计划、工具查询、证据聚合、回复生成、审计落库”链路：

- 问题分类：识别 `inventory`、`customer_receivable`、`supplier_payable`、`sales`、`purchase`、`finance`、`product`、`mixed`、`unsupported`。
- 查询计划：列出需要调用的只读工具、查询窗口、过滤条件、分页策略和每个工具的使用理由。
- 工具执行：所有数据来自当前 owner 的真实数据库查询；无数据时返回真实空态，不补假趋势。
- 证据聚合：每个关键数字、排名、风险和建议动作都必须能关联到工具结果。
- 回复生成：回答必须写明数据范围、时间窗口、截断 / 失败 / 空态限制，并区分“已查到事实”和“建议动作”。
- 审计落库：保存问题摘要、计划、工具结果摘要、回复摘要、耗时、错误和 warnings。

P0 至少覆盖以下真实问题类型：

- “哪些商品库存不足，风险最高？”
- “哪些客户还有应收款，金额是多少？”
- “最近销售 / 采购 / 财务情况怎么样？”

回答不通过条件：

- 只根据关键词返回固定模板。
- 不查询真实工具就给出金额、数量、排名或风险。
- 把截断列表说成全量结论。
- 模型不可用时仍使用“我已智能分析全部数据”等误导措辞。

## 6. Planner 需求

Planner 输入：

- 当前 `ownerUserId` 和权限摘要。
- 用户原始问题和会话上下文摘要。
- LLM 可用状态。
- 只读工具白名单和每个工具 schema。
- 当前 run 的安全检查结果。

Planner 输出：

- `plan_id`
- `plan_source`：`llm`、`keyword_fallback`、`rules`、`manual`
- `steps`
- 每个 step 的 `tool_name`、`input`、`reason`、`evidence_required`
- `requires_draft` 和草稿类型候选。
- `unsupported_reason`，用于工具缺失或问题超范围。

Planner 规则：

- LLM planner 只能选择白名单工具，不能生成 SQL、HTTP 任意请求或写工具。
- 规则 planner 可以使用关键词，但只能决定“查哪些真实工具”，不得生成最终经营结论。
- 如果问题需要多个数据域，必须多工具组合，而不是只返回最近 10 条默认列表。
- 如果工具缺失，回答必须明确“当前版本尚未接入该查询工具”，不得假装查到了。
- Planner 决策必须写入审计和 run trace。

## 7. 工具系统需求

### 7.1 P0 只读工具白名单

P0 至少支持以下只读工具：

- `inventory_low_stock_lookup`
- `customer_receivable_lookup`
- `sales_overview_lookup`
- `product_catalog_lookup`
- `supplier_payable_lookup`
- `sale_order_lookup`
- `purchase_order_lookup`
- `pay_order_lookup`
- `finance_record_lookup`

### 7.2 工具执行合同

每个工具必须具备：

- `tool_name`
- `input_schema`
- `owner_scope`
- `permission_check`
- `query_window` 或 `filters`
- `limit`
- `cursor`
- `started_at`
- `completed_at`
- `duration_ms`
- `status`
- `result_summary`
- `evidence`
- `total_count`
- `returned_count`
- `is_truncated`
- `next_cursor`
- `error_code`
- `error_message`

工具执行规则：

- owner 必须来自服务端安全上下文，不能信任客户端传入 owner。
- 工具失败必须返回 `tool_failed`，不得静默返回空列表。
- 工具默认截断时必须说明“当前仅展示前 N 条”，不得说成全量结论。
- 金额、库存、状态、日期必须来自数据库字段或已定义聚合，不得由模型编造。
- 每个 result block 必须能追溯到一个或多个 tool result。

### 7.3 回答证据要求

回答中出现的每个关键数字都必须能定位来源：

- 金额：来源工具、字段、聚合窗口。
- 数量：来源工具、过滤条件、是否截断。
- 风险等级：触发规则、阈值、证据项。
- 建议动作：对应业务对象、建议理由、是否需要草稿。

## 8. Run Lifecycle

### 8.1 状态机

Run 状态：

- `created`
- `safety_checking`
- `blocked`
- `planning`
- `tool_running`
- `model_responding`
- `drafting`
- `completed`
- `partial_completed`
- `failed`
- `cancel_requested`
- `cancelled`

状态规则：

- 每次 chat 必须创建 run。
- `runId` 必须贯穿 HTTP 响应、SSE 事件、审计、Android UI。
- `blocked`、`failed`、`cancelled` 都是终态。
- `partial_completed` 表示部分工具成功、部分失败，回答必须带 warning。
- P0 若代码路径已存在，必须通过编译和端到端证据证明服务端 cancel 可用；如果某环境没有服务端取消，只能取消客户端接收并诚实提示。P1 必须补齐取消审计、长任务恢复和跨进程状态持久化。

### 8.2 取消接口

当前目标接口：

- `POST /v2/agent/runs/{run_id}/cancel`

取消语义：

- 如果 run 未完成，标记 `cancel_requested`。
- Orchestrator 停止后续工具和模型调用。
- 已完成工具结果保留审计。
- SSE 发送 `run_cancelled`。
- Android stop 按钮调用服务端取消；如果调用失败，明确显示“已停止本机接收，服务端取消失败或仍在处理”。
- 取消接口必须 owner-aware；跨 owner、未知 run、已完成 run 必须返回可审查状态，不能伪造成功。

### 8.3 用户可控机制矩阵

| 机制 | P0 要求 | P1 要求 | 审计要求 |
|---|---|---|---|
| 工具调用 | 后端真实发出 `tool_started` / `tool_completed` / `tool_failed`；Android 只展示后端事件 | 支持多工具并发 / 串行、分页续查、部分成功汇总 | 记录工具名、输入摘要、输出摘要、耗时、状态、错误码 |
| 取消 | 调用 `POST /v2/agent/runs/{run_id}/cancel`；失败时只能诚实显示本机停止接收 | 取消状态持久化、跨进程恢复、长任务中断和失败重试 | 记录 `cancel_requested_by`、时间、最终状态、已完成工具 |
| 确认 | 写操作只生成建议或草稿；未实现执行前不得显示成功 | 草稿 confirm / reject / execution 形成事务写入和状态机 | 记录确认用户、二次确认、业务对象 ID、执行结果 |
| 高风险动作 | 必须拦截直接执行，要求草稿和预览 | 支持权限校验、影响预览、失败恢复 | 记录安全检查、权限摘要、风险原因 |
| 审计查看 | 至少能通过 `run_id` 定位服务端日志 / 审计记录 | 提供按 run / conversation / draft 查询的审计接口 | 审计缺失必须作为 warning 或失败信号 |

## 9. SSE 事件协议

### 9.1 事件 envelope

所有 SSE payload 必须使用统一 envelope：

```json
{
  "event_id": "evt_...",
  "run_id": "run_...",
  "conversation_id": 1,
  "seq": 1,
  "event_type": "tool_started",
  "timestamp": 1710000000000,
  "data": {}
}
```

规则：

- `seq` 在单个 run 内单调递增。
- `event_id` 可用于审计和客户端去重。
- `data` 不得包含密钥、token、完整内部 prompt、SQL 堆栈或跨 owner 数据。
- 后端可以保持向后兼容，但 Android 新实现必须优先消费统一 envelope。

### 9.2 P0 事件类型

P0 必须支持：

- `run_started`
- `safety_check_started`
- `safety_check_passed`
- `safety_check_blocked`
- `plan_created`
- `tool_started`
- `tool_progress`
- `tool_completed`
- `tool_failed`
- `result_block`
- `answer_started`
- `answer_delta`
- `answer_completed`
- `run_completed`
- `run_failed`
- `error`

P0 取消语义要求优先调用服务端 cancel；如果当前环境调用失败或接口不可用，Android 必须显示“已停止本机接收，服务端取消失败或仍在处理”，并记录本地 `client_receive_stopped` 状态；不得伪造 `run_cancelled`。`run_cancelled` 只在服务端确实返回取消终态时出现。

P0 取消事件：

- `run_cancelled`

P1 增强事件：

- `draft_created`
- `draft_confirm_requested`
- `draft_execution_started`
- `draft_execution_completed`
- `draft_execution_failed`
- `context_compacted`
- `task_created`
- `notification_created`

### 9.3 流式来源和禁止假流式

`answer_delta` 只能来自可审计来源，且必须携带 `delta_source`：

- `model_stream`：模型供应商真实 streaming token / delta。
- `server_notice`：后端在真实模型 delta 之后补充的查询边界、部分失败、权限范围等服务端事实说明；不得用于拆分完整规则摘要或伪装模型 token。

规则摘要不是流式来源。后端已经完成真实工具查询后，如果只能基于工具事实输出规则摘要，必须通过 `answer_completed` 一次性返回完整降级答案，不得拆成 `answer_delta`。

规则：

- `model_stream` 必须配合 `mode=tool_query_llm_streamed` 或等价模型成功状态。
- `server_notice` 必须出现在真实 `model_stream` delta 之后、`answer_completed` 之前；Android 必须显示为“服务端说明”或等价文案，不得显示为模型正在生成。
- `tool_query_rule_summary` 必须只出现在 `answer_completed` / `run_completed`，并返回 `plan_source`、`llm_status=disabled`、`stream_failed_or_empty` 或其它明确降级原因。
- Android 可以把规则摘要作为完成态回答展示，但文案必须是“数据查询 / 规则摘要模式”，不得显示为“模型正在思考”或“正在吐字”。
- `result_block` 必须按服务端真实事件顺序渲染：工具完成后可以先出现对应结构化结果；模型真流式时 `answer_delta(model_stream)` 按供应商回调到达；规则摘要降级时完整回答只能由 `answer_completed` 给出，不得用增量伪造。

禁止以下做法：

- 后端先得到模型完整答案，再用固定 chunk size 切字符串，并伪装成模型 streaming。
- Android 收到完整答案后用 timer / delay / animation 拆字显示。
- 模型不可用时伪造“正在思考”和 token 流。
- 真实 streaming 失败后再同步调用模型拿完整答案，并把完整答案展示成“流式体验”。

如果当前模型 API 不支持 streaming，`/chat/stream` 可以发送工具事件和 `answer_completed`，但不能发送规则摘要增量，也不能伪造 `model_stream`。如果模型 streaming 中途失败，允许保留已收到的真实 partial `model_stream` delta，并在 completed / failed / warning 中标明 `stream_interrupted`；如果完全没有模型 delta，则只能通过 `answer_completed` 降级为工具 / 规则摘要或明确错误。

### 9.4 工具事件要求

每个被执行工具必须至少发送：

- `tool_started`：`tool_name`、`input_summary`、`started_at`
- `tool_completed`：`tool_name`、`result_summary`、`returned_count`、`is_truncated`、`duration_ms`

失败时发送：

- `tool_failed`：`tool_name`、`error_code`、`safe_message`、`duration_ms`

空实现的 `emitToolStarted` / `emitToolCompleted` 不满足 P0。

## 10. 草稿和执行需求

### 10.1 草稿状态机

草稿状态：

- `proposed`：AI 建议但尚未落库。
- `active`：已落库，等待用户确认。
- `confirming`：用户已确认，后端正在预校验或执行。
- `executed`：真实业务写入成功。
- `failed`：执行失败，保留草稿和错误。
- `rejected`：用户拒绝。
- `archived`：仅归档，不代表执行成功。

### 10.2 P0 边界

P0 可以生成建议型结果块，也可以列出已落库草稿，但：

- `draft_id=null` 的建议不得显示为可确认真实草稿。
- 如果没有确认执行接口，确认按钮必须禁用或标明“当前仅归档，不会执行业务写入”。
- Android 不得把 `updateDraft(status="archived")` 展示成“已提交 / 已执行”。

### 10.3 P1 执行接口

P1 必须新增或等价提供：

- `POST /v2/agent/drafts/{id}/confirm`
- `POST /v2/agent/drafts/{id}/reject`
- `GET /v2/agent/drafts/{id}/execution`

确认执行流程：

1. 读取当前 owner 的草稿。
2. 校验草稿状态为 `active`。
3. 校验用户权限和业务对象。
4. 预览变更摘要、金额、库存影响、关联客户 / 供应商 / 商品。
5. 用户二次确认。
6. 后端在事务中执行真实写入。
7. 写入成功后草稿进入 `executed`，返回业务对象 ID。
8. 写入失败后草稿进入 `failed`，保留可恢复错误。
9. 审计记录 `user_confirmed=true`、执行结果、错误码和业务对象 ID。

## 11. 任务和通知需求

P0 任务 / 通知必须来自真实接口：

- `GET /v2/agent/tasks`
- `GET /v2/agent/notifications`
- `POST /v2/agent/notifications/{id}/read`

规则：

- 没有任务或通知时展示真实空态。
- 已读必须写回服务端，失败不得本地假成功。
- demo seed 不得在生产 profile 产生任务或通知。
- 任务进度必须来自后端任务状态，不得 Android 本地递增。
- 如果任务来自 AI run，必须能关联 `runId` 和审计记录。

## 12. 审计和可追溯性

### 12.1 服务端审计为准

服务端必须记录：

- `audit_id`
- `run_id`
- `conversation_id`
- owner 安全摘要
- 用户问题摘要
- `mode`
- `llm_status`
- 安全检查结果和拒绝原因
- planner 来源、计划、工具列表
- 每个工具的输入摘要、状态、结果摘要、错误码、耗时
- result block 摘要
- 是否发生真实 streaming
- 是否发生上下文压缩
- 草稿 ID、状态、确认用户、执行结果
- 取消状态
- 最终回答摘要
- 错误信息
- 创建和更新时间

Android 本地审计可以作为离线排查补充，但不得替代服务端审计。

### 12.2 敏感信息规则

审计和日志不得记录：

- 模型密钥、token、session、密码。
- 完整内部 prompt 或 hidden reasoning。
- SQL 堆栈、数据库连接串。
- 跨 owner 数据。
- 超出用户权限的业务明细。

### 12.3 审计失败策略

- 审计写入失败不得导致用户问答假成功。
- 如果主问答成功但审计失败，响应必须带 `warnings=["audit_write_failed"]` 或等价可观测信号。
- 安全拒绝、工具失败、模型失败、用户取消、草稿确认必须尽最大可能落审计。

### 12.4 可观测性字段

每个 run 至少提供以下可观测性字段，供日志、指标、审计和前端排查关联：

- `request_id` 或 `correlation_id`
- `trace_id`
- `run_id`
- `conversation_id`
- `owner_user_id_hash` 或安全摘要
- `mode`
- `llm_status`
- `plan_source`
- `tool_count`
- `tool_success_count`
- `tool_failed_count`
- `started_at`
- `first_event_at`
- `completed_at`
- `duration_ms`
- `first_event_latency_ms`
- `tool_duration_ms`
- `model_duration_ms`
- `audit_write_status`
- `warning_codes`
- `error_code`

日志和指标规则：

- 日志必须能通过 `run_id` 或 `trace_id` 串起 HTTP 请求、SSE 事件、工具调用、模型调用和审计写入。
- 指标至少覆盖请求量、成功率、失败率、取消率、审计写入失败率、工具耗时、模型耗时、SSE 首事件耗时和端到端耗时。
- Android 错误上报或本地日志不得包含密钥、token、完整 prompt 或跨 owner 数据。

## 13. 错误处理和降级

| 场景 | 后端行为 | Android 行为 |
|---|---|---|
| LLM disabled / not configured | 真实工具可执行；返回 `mode=rules_only` 或 `tool_only`，附降级说明 | 显示“模型暂不可用，当前为数据查询 / 规则摘要模式” |
| LLM timeout / error | 工具成功则返回部分结果；工具失败则返回错误 | 展示部分成功或错误，不展示模型思考 |
| 工具失败 | 返回 `tool_failed` 和安全错误摘要 | 在结果区标明该工具失败，不给确定结论 |
| 无数据 | 返回真实空态 result block | 显示空态，不填假数据 |
| 安全拦截 | 返回 `blocked`，记录拒绝原因 | 显示拒绝原因和可改写建议 |
| SSE 断开 | 标记 run 未知或失败，保留已收到事件 | 提供重试或查看非流式结果 |
| 用户取消 | P0 优先调用服务端 cancel；失败或接口不可用时只能诚实显示本机停止接收 | 明确是否服务端已取消 |
| 草稿确认失败 | 草稿保留 `failed` 或 `active`，返回可恢复错误 | 展示失败原因，不移除草稿 |

错误消息要求：

- 面向用户的错误必须安全、简洁、可行动。
- 不得泄露 SQL、堆栈、密钥、内部 prompt、供应商原始错误详情。
- 同时在审计或安全日志中保留排查所需错误码。

## 14. Android 验收要求

Android 必须满足：

- `AgentWorkbenchScreen` 必须是干净的 AI 入口页，只展示问候、新建对话、快捷问题、任务 / 草稿入口和说明文案。
- `AgentWorkbenchScreen` 初始屏不得展示销售额、KPI 四宫格、今日经营摘要、风险提醒、经营图表、客户 / 商品 / 供应商排行、最近单据等报表型经营数据。
- `AgentWorkbenchScreen` 初始屏文案也必须保持干净：不得把“今日”“风险”“经营图表”“报表页指标”包装成默认入口内容；快捷问题可以问“帮我查哪些商品可能需要补货”，但不能预先展示补货风险列表或默认图表。
- 如需展示最近对话、待处理草稿或任务数量，必须来自 `/v2/agent/workbench` 或对应真实接口；若当前初始屏选择不展示这些真实数据，应保持空白入口，不得用静态数字补位。
- `AgentChatScreen` 只展示后端回答、后端 blocks、后端 SSE 事件。
- `AgentChatScreen` 必须用清晰的视觉层级区分用户消息、AI 回复、RunTrace、result blocks、错误和降级状态；不能全部使用同一种白色玻璃卡。
- AI 回复 Markdown 必须正确渲染标题、段落、无序列表、有序列表、表格、引用、分隔线、代码块、粗体、斜体、行内代码和链接，不得退化为纯文本。
- Markdown 链接不得丢失 URL。P0 至少展示链接文本和可复制 / 可见 URL；P1 支持点击打开、长按复制和安全域名提示。
- 代码块 P0 必须保留语言标识、等宽排版、横向滚动、复制能力和尾部空白；P1 支持长代码折叠和基础语法高亮。
- 流式输出中的 Markdown 渲染必须稳定：未闭合代码块、表格半行、列表半项等中间状态不得导致整条消息闪烁、崩溃或内容丢失。
- Markdown 视觉验收必须看截图而不只看解析分支：标题需有清晰层级和段前 / 段后间距，段落行高不得拥挤，列表缩进和 bullet / 序号需对齐，引用块需有左侧强调线或独立底色，代码块需深色或高对比背景、等宽字体、语言标签和复制入口，表格需横向滚动且 header / body 层级可辨，链接需有下划线 / 色彩区分并保留 URL，可读性必须覆盖长文本、长表格、长代码和流式半成品。
- `RunTracePanel` 展开 / 折叠真实可用；没有事件时不得补固定步骤。
- `ResultBlockRenderer` 的数字和列表来自 `ResultBlockDto`，不得 UI 层补假图表数据。
- `ResultBlockRenderer` 支持真实 `line_chart`、`bar_chart`、`donut_chart` / `pie_chart`；图表数据必须来自后端 result block，不得 Android 生成示例图。
- `ResultBlockRenderer` 必须对 `text`、`kpi_grid`、`table`、`rank_list`、`line_chart`、`bar_chart`、`donut_chart` / `pie_chart`、`risk_card`、`evidence_card`、`draft_card` 提供明确分支或明确兜底；已知类型解析失败不能静默不显示。
- 图表空态必须诚实：真实数据为空时显示“暂无可绘制数据”；字段缺失时显示“结果结构不完整”；工具失败时显示工具失败原因，不得自动生成 0 值图或示例图。验收必须覆盖空 labels、series 长度与 labels 不一致、NaN / Infinity、donut / pie 非正数 segment、未知 block、已知 block 字段缺失和坏 SSE 帧。
- `DraftListScreen` 列表来自 `/v2/agent/drafts`；确认能力未实现前不得显示为真实执行。
- `TaskNotificationScreen` 列表和已读来自真实接口。
- `AgentSseClient` 负责解析真实 SSE，不负责生成任何事件。
- stop 按钮在 P0 必须调用服务端 cancel；若请求失败或 run 不存在，必须诚实显示本机已停止接收但服务端取消未确认。
- Android 不含模型密钥、模型 base URL、provider 私密配置。

## 15. 后端验收要求

后端必须满足：

- `/v2/agent/*` 全部 owner-aware。
- `V2AgentAiService` 或后续拆分服务不再用固定模板充当真实 AI。
- Planner、Tool Registry、Run Orchestrator、Audit 至少在代码结构或职责上可审查。
- LLM disabled 返回明确状态，不伪装模型成功。
- `tool_started` / `tool_completed` / `tool_failed` 真实发出。
- `/chat/stream` 不得把完整模型答案或规则摘要固定切块伪装成流式；`answer_delta` 只允许真实 `delta_source=model_stream`。
- 工具结果返回截断元数据。
- 任务、通知、草稿不依赖生产 demo seed。
- 草稿确认不等价于 `archived`。
- 服务端 cancel run 必须可编译、可调用、owner-aware，并通过 SSE `run_cancelled` 与审计证据证明真实停止后续处理；P1 继续补齐持久化 run 状态和跨进程恢复。

## 16. 禁止项扫描门禁

每次审查必须扫描生产链路：

- `mock`
- `sample`
- `demo`
- `fake`
- `placeholder`
- `模拟`
- `演示`
- `假数据`
- `delay`
- `timer`
- `substring`
- `chunkSize`

命中生产 UI、Repository、网络层、后端服务、资源或配置时，必须证明它不在 AI 助手生产链路中；否则验收不通过。`substring` / `chunkSize` 命中如果用于回答拆字、规则摘要分块、模型外本地 reveal 或伪造 `model_stream`，一律不通过；只有与 AI 回复流式无关的普通字符串处理才可逐项解释通过。

## 17. P0 验收标准

### 17.1 AI 助手端到端证据包模板

每次 P0 审查必须按“一个真实问题一个证据包”的方式归档。证据包必须能独立证明该回答来自真实 owner-scoped 数据、真实工具调用、真实 SSE / HTTP 响应和真实 UI 渲染；不能只提交截图或口头说明。

证据包固定归档到：

```text
docs/acceptance-evidence/ai-agent/{yyyyMMdd-HHmm}-{run_id}/
```

建议文件名：

- `00-env.md`：账号、profile、LLM 配置状态和安全摘要。
- `01-http-response.json`：非流式响应或 stream 初始响应关键字段。
- `02-raw-sse.log`：`curl -N` 保存的原始 SSE。
- `03-run-audit.json`：`GET /v2/agent/runs/{run_id}/audit` 保存的服务端 run 摘要和事件 payload。
- `04-tool-results.json`：每个工具输入摘要、查询窗口、结果摘要、截断和耗时。
- `05-ui-home.png`、`06-ui-chat.png`、`07-ui-runtrace.png`、`08-ui-blocks.png`：真机截图。
- `09-ui-tree.xml`：`uiautomator` dump。
- `10-forbidden-scan.txt`：禁止项扫描原始命中。
- `11-latency.md`：首事件、首 token / 首摘要、工具、模型、端到端耗时。
- `12-conclusion.md`：按 `pass` / `fail` / `partial` 写结论。
- `13-sse-audit-ui-reconciliation.md`：SSE 与服务端审计按 `seq` / `event_id` / `event_type` 自动对账；UI 列只能作为预期映射，仍需真机截图验证。
- `14-agent-run-summary.json`：从 run audit 和 tool results 派生的 run 摘要、工具耗时、截断状态和事件列表。
- `15-forbidden-scan-review.md`：禁止项命中逐项解释；任何 `needs evidence` 行必须在 P0 通过前处理。
- `16-workbench-response.json`：`/v2/agent/workbench` 真实响应；无 auth / 无后端时必须写 `skipped` 或 `failed`，不得伪造。
- `17-workbench-cleanliness.md`：自动检查 workbench 是否返回默认 KPI、风险、今日摘要或报表型快捷问题；`pass-for-interface` 仍不能替代真机首屏截图。

命令模板：

```bash
RUN_ID_DIR="$(
  TOKEN="<redacted>" \
  BASE_URL="http://localhost:8080" \
  MESSAGE="哪些商品库存不足，风险最高？" \
  MODE="stream" \
  ./tools/ai_agent_evidence_capture.sh |
  awk -F': ' '/AI agent evidence package written to/ {print $2}'
)"

adb -s <serial> exec-out screencap -p > "$RUN_ID_DIR/05-ui-chat.png"
adb -s <serial> exec-out uiautomator dump /dev/tty > "$RUN_ID_DIR/09-ui-tree.xml"
```

AI 首页初始屏可先用独立设备脚本采集，脚本会自动拦截锁屏、弱锚点和默认报表内容，不能把锁屏 / 黑屏误判为通过：

```bash
ANDROID_SERIAL="<serial>" \
BASE_URL="http://localhost:18080" \
python3 tools/capture_ai_home_device_evidence.py --wake
```

该脚本输出 `docs/acceptance-evidence/ai-agent/{yyyyMMdd-HHmmss}-device-ai-home/`，其中 `08-home-cleanliness.json` 和 `10-conclusion.md` 必须为 `pass-for-ai-home-cleanliness` 才能作为 AI 首页干净入口证据。通过条件必须同时看到 `AI 助手` 标题和至少一个 Hero 锚点（如“主屏保持干净”或“开始一次真实 Agent 对话”），且不得命中 `销售额`、`KPI`、`今日经营摘要`、`风险列表`、`销售趋势`、`净现金流`、`库存预警` 等默认报表 / 看板内容。若状态为 `blocked-by-locked-device`、`partial-not-in-app` 或 `partial-ai-home-not-detected`，只能作为失败 / 部分尝试保存，不能支撑 P0 通过。注意：`mFocusedApp` / `ResumedActivity` 可能在锁屏时仍显示 `com.zhihuiji.app`，不得把它当成可见 App 证据；必须同时确认 `device_locked=false`、无 Keyguard / NotificationShade 遮挡且 UI tree 来自真实 app 内容。

`tools/ai_agent_evidence_capture.sh` 会自动生成 `00-env.md`、`00-request.json`、`01-http-response.json`、`02-raw-sse.log`、`03-run-audit.json`、`04-tool-results.json`、`10-forbidden-scan.txt`、`11-latency.md`、`12-conclusion.md`、`13-sse-audit-ui-reconciliation.md`、`14-agent-run-summary.json`、`15-forbidden-scan-review.md`、`16-workbench-response.json` 和 `17-workbench-cleanliness.md`。脚本默认结论为 `partial`，因为它只采集接口 / SSE / 审计证据，不会伪造真机截图或 UI tree。截图、UI tree 和 Android 首次可见耗时必须从真实设备补充。`12-conclusion.md` 必须列出不可替代证据清单，明确接口 / 审计对账不能证明 Android 渲染。`13-sse-audit-ui-reconciliation.md` 的 `pass-for-interface` 只能证明接口和服务端审计一致，不能替代 Android RunTrace 截图。`17-workbench-cleanliness.md` 的 `pass-for-interface` 只能证明后端 workbench 响应干净，不能替代 Android 首屏截图和 UI tree。`15-forbidden-scan-review.md` 是自动审查草案，不是自动通过证明；任何 `needs evidence` 行必须人工复核并给出源码 / 运行证据后才能 P0 通过。

已有证据包需要按最新脚本刷新派生产物时，使用：

```bash
./tools/ai_agent_evidence_capture.sh refresh-existing docs/acceptance-evidence/ai-agent/<run-dir>
```

如果没有现成 `TOKEN`，可以改用 `LOGIN_PHONE` / `LOGIN_PASSWORD` 让脚本先调用 `/v1/auth/login` 获取临时 token。脚本不得把密码、token 或模型密钥写入证据包；`00-env.md` 只能记录 token 来源和脱敏手机号尾号。

| 字段 | 必填 | 取证要求 | 不通过示例 |
|---|---|---|---|
| `account/profile` | 是 | 记录测试账号、owner 安全摘要、后端 profile、`AGENT_LLM_ENABLED`、模型配置状态；不得记录密码、token、模型密钥 | 只写“本地环境”或无法区分 demo / prod profile |
| `question` | 是 | 保存用户原始问题、会话 ID、提交时间、是否多轮上下文 | 只保存后端归一化后的关键词 |
| `HTTP response` | 是 | 保存 `/v2/agent/chat` 或 `/v2/agent/chat/stream` 的状态码、关键 header、响应 envelope、`mode`、`llm_status`、warnings | 只保存 Android 展示文本 |
| `raw SSE` | 流式场景必填 | 保存原始 SSE 事件序列，至少覆盖 `run_started`、`plan_created`、`tool_*`、`answer_*`、终态事件；必须保留 `delta_source` | 只贴最终答案，无法证明是否真流式 |
| `run_id/trace_id/audit_id` | 是 | 三者必须能互相定位；如果暂缺某字段，证据包必须标记为 P0 缺口而不是通过 | 只有 `conversation_id` |
| `tool result` | 是 | 保存每个工具的 `tool_name`、输入摘要、查询窗口、`returned_count`、`total_count`、`is_truncated`、`duration_ms`、失败码、关键结果摘要 | 工具返回被 Android 本地改写或截断状态缺失 |
| `screenshot` | 是 | 真机截图覆盖 AI 首页初始屏、聊天回答、RunTrace 展开、result block / 图表 / 空态；截图文件名包含 run_id 或时间戳 | 只截最终聊天气泡，未展开 RunTrace |
| `latency` | 是 | 记录端到端耗时、首事件耗时、首 token / 首摘要耗时、工具耗时、模型耗时、Android 首次可见耗时 | 只写“响应较快” |
| `forbidden-item scan` | 是 | 保存对 Android AI 生产链路和后端 agent 生产链路的禁用词扫描结果，并逐项解释合法命中 | 命中 `fake` / `demo` / `delay` 后未解释 |
| `conclusion` | 是 | 明确写 `pass`、`fail`、`partial`，并列出阻塞项、证据缺口和下一步修复路径 | 只写“基本可用” |

证据包结论规则：

- `mode`、`llm_status`、`delta_source`、RunTrace UI、审计记录必须互相一致；任一处矛盾即失败。
- 如果答案包含金额、数量、排行、风险等级或图表，证据包必须能定位到具体 tool result 字段；定位不到即失败。
- 如果模型不可用或流式失败，证据包必须证明 UI 明确显示降级，不得以“用户看不出来”为通过理由。
- 如果真实数据为空，证据包必须保存真实空态的工具结果和 UI 空态，不得用默认 0 值图、示例排行或补位文案通过。

### 17.2 真实数据与图表来源矩阵

Android 只能渲染后端 `ResultBlockDto` 和工具结果，不得为美观补示例序列、默认排行、默认 segment 或假图表。后端生成每个 block 时必须写入来源工具、聚合窗口和截断信息；Android 渲染失败时必须显示失败卡。

#### 17.2.1 ResultBlock canonical schema

当前 canonical schema 必须以 Android `AgentResultBlockModels.kt` 可直接解析的字段为准：

- `line_chart` / `bar_chart`：`data.labels: string[]`、`data.series[].name: string`、`data.series[].data: number[]`、可选 `data.series[].color`。
- `donut_chart` / `pie_chart`：`data.segments[].name: string`、`data.segments[].value: number`、可选 `data.segments[].color`。
- `kpi_grid`：`data.kpis[].label`、`data.kpis[].value`、可选 `unit`、`trend_direction`、`trend_value`。
- `table`：当前 Android canonical 字段为 `headers: string[]`、`rows: string[][]`、可选 `row_count`；若后端返回 `columns/rows object` 形态，必须先补兼容解析和测试。
- `rank_list`：当前 Android canonical 字段为 `items[].rank`、`items[].name`、`items[].value`、可选 `change_direction`。
- `evidence_card`：当前 Android canonical 字段为 `items[].label`、`items[].value`、可选 `source`。

兼容别名规则：

- 文档历史样例中的 `series[].values`、`segments[].label` 只能作为兼容别名；没有 Android / 后端双侧测试证明前，不得作为 P0 通过证据。
- 后端若采用更丰富 schema，如 `columns[key,label,type]`、`source_tools`、`query_window`、`is_truncated`，Android 必须显示或至少保留可定位摘要；字段被丢弃时不能标记为完整通过。
- 所有 result block 验收必须保存原始 JSON，不能只看 Android 渲染截图。

| Block type | Backend tool / source | Required fields | Aggregation window | Truncation fields | Android renderer | Empty-state copy |
|---|---|---|---|---|---|---|
| `text` | Orchestrator 基于真实 tool summary 或安全拒绝生成 | `title`、`content`、`source_tools`、`warnings` | 继承所用工具窗口 | `is_truncated`、`source_tools` | Markdown / safe text renderer | “暂无可总结的真实数据” |
| `kpi_grid` | `sales_overview_lookup`、`customer_receivable_lookup`、`supplier_payable_lookup`、`finance_record_lookup` | canonical: `kpis[label,value,unit?,trend_direction?,trend_value?]`；来源扩展可带 `source_field` | 必填，如 `last_7_days`、`last_30_days`、`single_day` | `is_truncated=false` 或逐项说明 | `ResultBlockRenderer` KPI 分支 | “暂无可展示指标” |
| `table` | 任意列表型工具，如 `sale_order_lookup`、`purchase_order_lookup`、`finance_record_lookup` | canonical: `headers`、`rows`、`row_count?`；`columns[key,label,type]` 需兼容测试后才可作为 P0 | 查询过滤条件和时间范围 | `total_count`、`returned_count`、`limit`、`next_cursor`、`is_truncated` | table 分支，支持横向滚动 | “暂无符合条件的记录” |
| `rank_list` | `inventory_low_stock_lookup`、`customer_receivable_lookup`、`supplier_payable_lookup`、`product_catalog_lookup` | canonical: `items[rank,name,value,change_direction?]`；来源扩展可带 `id`、`reason`、`source_fields` | 查询窗口或库存快照时间 | `total_count`、`returned_count`、`limit`、`is_truncated` | rank list 分支 | “暂无可排序的真实记录” |
| `line_chart` | `sales_overview_lookup`、`finance_record_lookup` 的时间序列聚合 | canonical: `labels`、`series[name,data,color?]`；来源扩展可带 `y_unit`、`source_fields` | 必填且与 labels 对齐 | `bucket_count`、`is_truncated`、`window_start`、`window_end` | line chart 分支 | “暂无可绘制趋势数据” |
| `bar_chart` | 销售 / 采购 / 商品 / 客户分组聚合工具 | canonical: `labels`、`series[name,data,color?]`；来源扩展可带 `y_unit`、`group_by` | 必填 | `total_groups`、`returned_groups`、`limit`、`is_truncated` | bar chart 分支 | “暂无可绘制柱状数据” |
| `donut_chart` / `pie_chart` | 分类占比聚合，如收款状态、费用类型、库存风险等级 | canonical: `segments[name,value,color?]`；来源扩展可带 `value_unit`、`source_fields` | 必填 | `total_segments`、`returned_segments`、`other_merged`、`is_truncated` | donut / pie chart 分支 | “暂无可绘制占比数据” |
| `risk_card` | Orchestrator 基于工具证据和固定阈值生成 | canonical: `level`、`title`、`description`、`affected_items?`、`suggested_action?`；来源扩展可带 `threshold`、`evidence_items` | 继承证据工具窗口 | `evidence_truncated`、`source_tools` | risk card 分支 | “当前没有可确认风险” |
| `evidence_card` | Tool Registry / Audit | canonical: `items[label,value,source?]`；来源扩展可带 `query_window`、`filters`、`counts`、`warnings` | 必填 | `total_count`、`returned_count`、`is_truncated` | evidence card 分支 | “暂无可展示证据” |
| `draft_card` | Draft service | canonical: `draft_id`、`draft_type`、`title`、`summary`；可选 `item_count`、`total_amount`、`partner_name`、`warnings` | 不适用 | 不适用 | draft card 分支 | “暂无待确认草稿” |

图表字段门禁：

- `labels.size` 必须与每个 `series.data.size` 一致；不一致时 Android 显示“结果结构不完整”，不得自行裁剪补齐。
- `NaN`、`Infinity`、负数 donut / pie segment、空 label、空 series 必须视为坏数据并展示失败 / 空态。
- `0` 值可以展示，但必须来自真实工具结果；不得用一组 0 值替代缺失数据。
- 任何未知 block type 必须显示“暂不支持的结构化结果：{type}”，并保留标题和来源摘要。

### 17.3 流式 / 降级判定样例

| 样例 | 后端证据要求 | Android 展示要求 | 验收结论 |
|---|---|---|---|
| `model_stream` 成功 | SSE 存在真实 `answer_delta(delta_source=model_stream)`；`mode` 表示模型流式成功；`llm_status=enabled`；审计记录模型 streaming 开始和结束 | 可以展示“模型生成中”；RunTrace 标记“模型流式回复”；最终气泡不显示降级 | 通过真流式验收 |
| `rule_summary` 降级 | 工具真实完成后，SSE 不发送 `answer_delta`；`answer_completed` 返回 `mode=tool_query_rule_summary` 或等价；`llm_status=disabled/not_configured/stream_not_supported/stream_failed_or_empty/failed_or_empty` | 必须显示“数据查询 / 规则摘要模式”；不得显示“模型正在思考”“模型流式成功”或吐字动画 | 只通过诚实降级，不通过真模型流式 |
| `stream failed` 部分模型 delta 后失败 | 原始 SSE 有部分 `model_stream` delta，随后 `run_failed` 或 `answer_completed(warnings includes stream_interrupted)`；审计记录供应商错误摘要 | 保留已收到内容，显示“模型流式中断”；如有规则补充，必须分段标明来源 | 通过部分成功或失败，不能标为完全成功 |
| `LLM disabled` | HTTP / SSE 返回 `llm_status=disabled`，无 `model_stream` delta；工具仍可真实执行 | UI 显示模型不可用，当前基于真实数据查询和规则摘要 | 通过降级；若显示 AI 智能推理则失败 |
| `SSE disconnect` | Android 记录连接断开时间、最后 `seq`；后端 run 最终状态可通过 audit / retry 查询 | UI 显示“连接中断，可重试或查看非流式结果”；不得把不完整回答当最终成功 | 需二次查询 run 终态；缺终态证据为 partial |

流式验收硬规则：

- `delta_source`、`mode`、`llm_status`、UI 文案、RunTrace、审计必须一致。
- 规则摘要不能伪装成成功 `model_stream`，也不能用服务端分块或 Android 动画拆字伪装模型吐字。
- Android 不得对完整 answer 使用 timer / delay / substring 制造“打字机”流式。
- 如果抓包无法证明 `model_stream` delta 来自供应商真实 streaming，则只能按非流式或规则摘要降级验收。

### 17.4 ResultBlock fixtures

后端、Android 和验收脚本必须复用同一组最小 fixtures，避免“实现能跑但验收另写一套样例”。以下 JSON 仅展示 `block_type`、`title` 和 `data` 的最小形状；实际接口还必须带 `source_tools`、查询窗口、截断和 evidence。

合法折线图：

```json
{
  "block_type": "line_chart",
  "title": "近7天销售趋势",
  "data": {
    "labels": ["06-02", "06-03", "06-04"],
    "series": [
      {"name": "销售额", "data": [1200.0, 800.0, 1560.0]}
    ],
    "y_unit": "元"
  }
}
```

合法柱状图：

```json
{
  "block_type": "bar_chart",
  "title": "客户应收排行",
  "data": {
    "labels": ["客户A", "客户B"],
    "series": [
      {"name": "应收款", "data": [3200.0, 1800.0]}
    ],
    "y_unit": "元",
    "group_by": "customer"
  }
}
```

合法环形图：

```json
{
  "block_type": "donut_chart",
  "title": "费用类型占比",
  "data": {
    "segments": [
      {"name": "物流", "value": 600.0},
      {"name": "人工", "value": 400.0}
    ],
    "value_unit": "元"
  }
}
```

坏例必须验收为错误 / 空态卡，而不是静默消失或补假图：

```json
[
  {"block_type": "line_chart", "title": "空标签", "data": {"labels": [], "series": [{"name": "销售额", "data": [1.0]}]}},
  {"block_type": "bar_chart", "title": "长度不一致", "data": {"labels": ["A", "B"], "series": [{"name": "金额", "data": [1.0]}]}},
  {"block_type": "donut_chart", "title": "非正数 segment", "data": {"segments": [{"name": "坏值", "value": -1.0}]}},
  {"block_type": "unknown_chart", "title": "未知类型", "data": {"raw": true}},
  {"block_type": "table", "title": "字段缺失", "data": {"headers": ["名称"]}}
]
```

Fixture 验收规则：

- 合法图表必须能截图证明渲染为图形，并显示标题、单位和真实 labels。
- 坏例必须显示“结构化结果暂无法渲染”或具体空态 / 结构不完整提示，并保留 block title、type 和可定位摘要。
- Android 不得为了让坏例“好看”补 labels、补 0 值、裁剪 series 或合并 segment。
- 后端测试、Android 单测和手工证据包应尽量直接引用本节 fixture，而不是各写各的样例。

### 17.5 P0 验收证据清单

P0 通过前必须提供以下证据：

- 真实账号登录信息和后端 profile。
- 至少 3 个真实经营问题：
  - 库存风险。
  - 客户应收或供应商应付。
  - 最近经营 / 销售 / 财务概览。
- 每个问题的工具调用事件、工具结果摘要、最终回答和 result blocks。
- 回答中关键数字可追溯到工具结果。
- `AGENT_LLM_ENABLED=false` 的诚实降级截图或日志。
- 模型 enabled 时的真实模型规划 / 总结证据。
- SSE 事件序列中包含真实 run、safety、plan、tool、answer、completed / failed 事件。
- 无假流式代码或抓包证据。
- RunTrace 可展开并展示真实事件。
- AI 首页初始屏干净，不展示销售额、KPI、图表、风险列表、经营摘要等报表型数据。
- 聊天界面能清楚区分用户消息、AI 回复、RunTrace、结构化结果块、错误和降级状态。
- owner 隔离测试或人工证据。
- 任务 / 通知真实空态或真实数据证据。
- 草稿确认能力状态说明：未实现则禁用或标注；实现后必须真实写入。
- 审计记录样本。
- 性能和可观测性证据：耗时表、日志关联 ID、错误 / warning 统计、审计写入状态。

### 17.6 Markdown fixture 验收包

每次 Markdown 渲染审查至少要保存一轮包含以下内容的真实或测试回答截图，不能只看 parser 单测：

- 标题层级：`#`、`##`。
- 无序列表和有序列表，至少包含一层缩进。
- 表格：包含转义 `\|`、行内代码 `` `a|b` `` 和长单元格。
- 引用块：至少 2 行连续引用。
- 代码块：带语言标识、长行、尾部空白、复制入口。
- 链接：链接文本旁必须可见 URL，或 UI tree 能证明 URL 未丢失。
- 流式半成品：未闭合代码块、未完成表格行、列表半项都不得崩溃、闪烁清空或丢正文。

Markdown 验收结论必须同时列出：解析测试名、截图路径、UI tree 路径、失败兜底文案。没有截图只能标 `partial`。

### 17.7 SSE / 审计 / UI 三方对账表

每个真实 run 必须补一张对账表，证明同一 `run_id` 下事件没有串台：

| seq | event_id | raw SSE event_type | audit event_type | Android RunTrace 行 | 结论 |
|---|---|---|---|---|---|
| 1 | evt_xxx | `run_started` | `run_started` | 运行开始 | pass / fail |
| 2 | evt_xxx | `tool_started` | `tool_started` | 工具开始 | pass / fail |
| 3 | evt_xxx | `tool_completed` | `tool_completed` | 工具完成 + 耗时 / 截断 | pass / fail |
| n | evt_xxx | `answer_completed` / `run_completed` | 同名事件 | 最终回答 / 完成态 | pass / fail |

对账规则：

- raw SSE、`03-run-audit.json`、Android RunTrace 必须使用同一 `run_id`。
- `seq` 必须单调递增；缺号、重复、event_id 不一致都必须标 `fail` 或解释为兼容旧事件。
- UI 没有展示的审计事件不能静默忽略；必须说明是不需要展示、未实现展示，还是解析失败。
- `delta_source=model_stream` 的每个 `answer_delta` 必须能关联到同一 run 的模型调用审计或 trace。

### 17.8 AI Workbench 兼容字段清洁验收

`AgentWorkbenchResponse` 为兼容旧合同仍可能包含 `kpi_cards`、`risk_alerts`、`today_summary` 等报表型字段，但 AI 首页 P0 清洁验收规则如下：

- 后端 `/v2/agent/workbench` 初始响应必须返回 `kpi_cards=[]`、`risk_alerts=[]`、`today_summary=null` 或等价空值。
- Android 初始屏不得消费这些字段展示 KPI、风险、今日摘要、图表或排行。
- 如果未来某业务入口需要使用这些字段，必须放在用户主动提问后的对话 / result block 或报表页，不得回到 AI 首页默认看板。
- 验收包必须保存 `/v2/agent/workbench` 响应、AI 首屏截图和 UI tree；只看 DTO 是否有字段不能判定失败。

### 17.9 必跑测试和脚本矩阵

AI 助手 P0 修改后的最小验证矩阵：

| 范围 | 命令 / 证据 | 目的 |
|---|---|---|
| 后端 agent 单测 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew -p /Users/sunyiyang/Desktop/Project/master-goods test --tests 'com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest' --tests 'com.zhihuiji.backend.api.controller.V2AgentMediaControllerTest' --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home` | 固定 run 审计、SSE 合同、非流式合同和 audit API |
| Android agent 合同 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./gradlew :core:model:testDebugUnitTest :feature:agent:compileDebugKotlin --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home` | 固定模型解析、Markdown / stream 合同和 agent UI 编译 |
| 证据脚本离线自测 | `./tools/ai_agent_evidence_capture.sh self-test` | 无需后端或 token，验证 SSE run_id 提取、audit tool result 展开、SSE/audit 对账和 run summary 派生逻辑 |
| AI 首页设备脚本自测 | `python3 tools/capture_ai_home_device_evidence.py --self-test` | 无需设备，验证锁屏、弱锚点、默认报表内容和干净首页判定规则 |
| 真实接口证据 | `TOKEN=<redacted> ./tools/ai_agent_evidence_capture.sh` 或 `LOGIN_PHONE=<phone> LOGIN_PASSWORD=<password> ./tools/ai_agent_evidence_capture.sh` | 生成 HTTP / SSE / run audit / workbench / 对账 / summary / 禁止项扫描 / latency 初稿；不得保存密码或 token |
| AI 首页真机证据 | `ANDROID_SERIAL=<serial> python3 tools/capture_ai_home_device_evidence.py --wake`，且 `10-conclusion.md` 为 `pass-for-ai-home-cleanliness` | 证明 AI 初始屏是干净入口；锁屏、非 app、弱锚点或报表默认内容必须保持非通过 |
| 聊天真机证据 | ADB 截图、UI tree、logcat 或录屏 | 证明聊天、RunTrace、Markdown、图表真实渲染 |
| 禁止项扫描 | `10-forbidden-scan.txt` + `15-forbidden-scan-review.md`；所有 `needs evidence` 行必须人工处理 | 防止 mock、fake、demo、假流式、占位数据回流 |

P0 不通过条件：

- 任意生产回答依赖 mock / demo / fake / sample 数据。
- 任意生产流式体验通过完整答案切块伪造。
- 任意工具事件为空实现或本地伪造。
- LLM disabled 时仍宣传“AI 智能分析完成”。
- draft 确认只 archived 却显示为业务已执行。
- 任务 / 通知由 demo seed 污染生产体验。
- 无法用 `run_id` 或 `trace_id` 定位日志、SSE、工具事件和审计记录。
- AI 首页继续堆放报表页已有的经营 KPI、图表、风险和摘要。
- 聊天气泡、过程卡、结果块和错误状态无法通过视觉层级区分。

## 18. 性能和可观测性验收

P0 验收必须在同一环境、同一账号、同一后端 profile 下记录真实测量值。默认门槛如下；如果业务数据量或模型供应商导致超时，必须给出原因、影响范围和后续优化项，不能只写“偶发慢”。

| 指标 | P0 默认门槛 | 证据 |
|---|---|---|
| 非流式 `/v2/agent/chat` 首响应 | 工具 / 规则模式 2 秒内返回；LLM 模式 8 秒内返回或明确超时降级 | HTTP 日志、响应体、耗时 |
| SSE 首事件 | 1 秒内收到 `run_started` 或错误事件 | 抓包 / 日志中的 `first_event_latency_ms` |
| 单工具耗时 | 常规查询 3 秒内完成；超过时返回 slow warning | `tool_started` / `tool_completed` 时间差 |
| 多工具总耗时 | P0 三类真实问题 15 秒内完成或部分成功返回 | run `duration_ms`、工具耗时表 |
| 取消生效 | P0 服务端 cancel 请求后 2 秒内进入 `cancelled`、返回已完成不可取消状态，或明确不可取消原因 | cancel 接口、SSE、审计 |
| 审计写入 | 成功问答必须有审计；审计失败必须有 warning | audit 记录或 `audit_write_failed` |
| 错误可定位 | 用户可见错误有安全文案，服务端有错误码和 trace | UI / 响应 / 日志 |

性能采样方法：

- 每个场景至少重复 3 次，分别记录原始证据；汇总时给出 P50、P95 或在样本不足时明确写 `n=3, max/min/median`。
- 同一轮对比必须固定账号、数据规模、后端 profile、网络环境、模型 provider 状态、设备型号和 app build。
- 分组记录 `AGENT_LLM_ENABLED=false`、模型 enabled 且配置完整、工具失败、无数据、取消、SSE 断开。
- 冷启动和热路径要分开：冷启动记录首次请求，热路径记录同一进程后续请求。
- Android 首次可见耗时必须来自真实设备证据，如录屏时间轴、logcat 埋点或 frame timing；接口耗时不能替代 UI 可见耗时。
- 如果 provider 或网络异常导致不能采样，必须在 `12-conclusion.md` 标 `partial/fail` 并写明不可测原因。

### 18.1 当前剩余性能债台账

本台账只记录不改变 UI 也能继续优化的链路问题；每项修复后必须补测试和真实耗时证据，不能只用“代码看起来更快”作为通过依据。

| 性能债 | 当前证据 | 下一步最小修复 | 风险 |
|---|---|---|---|
| Dashboard 净现金流聚合仍需 Android 性能证据 | 已新增 owner-scoped `cashflow-summary` 聚合并保持资金流水 `type=收入 - 支出` 口径；Dashboard 不再刷新 / 读取资金流水列表求和；`docs/acceptance-evidence/performance/20260609-052957-backend-report-performance/03-summary.md` 记录 `dashboard_cashflow_summary` 5/5 HTTP OK、5/5 logical OK、p95 1.87ms，并用 `finance_records_page_for_cashflow_reconcile` 作为分页对账锚点 | 补 Android 首页刷新首个可见时间、帧统计和真机截图；后端侧继续用真实账号扩大数据量复测 | 不能把后端接口很快直接等同于 Android 首屏性能通过 |
| 报表利润汇总聚合仍需 Android / 大数据量证据 | 已新增 `SaleOrderItemRepository.profitSummary()` LEFT JOIN / COALESCE scalar 聚合；`ReportService.profitSummary()` 不再拉订单、明细和商品实体后本地聚合；同一性能包记录 `report_profit_summary` 5/5 HTTP OK、5/5 logical OK、p95 1.29ms | 用更大真实账号对比新 SQL 聚合值与旧本地聚合值，并记录 Android 报表首次可见耗时 | 缺商品价格时成本按 0 处理，真实对账必须覆盖缺商品 / 异常商品 id |
| 库存出库 / 库存流水报表仍需 Android / 大数据量证据 | 已新增 repository 级按时间倒序分页查询；`stockOutRecords()` 不再拉整段订单 / 明细后排序，`inventoryFlow()` 改为销售出库、取消入库、库存调整三路各取前 N 后合并截断；同一性能包记录 `report_stock_out_records` p95 1.68ms、`report_inventory_flow` p95 2.65ms，均 5/5 HTTP OK、5/5 logical OK | 用真实账号对比新旧响应顺序和值，并补 Android 报表首次可见耗时 / frame timing | 库存流水混合销售出库、取消入库、调整单，真实对账必须覆盖三类来源 |
| V2 销售单列表仍需 Android / 大数据量证据 | `V2SaleOrderController` 已将 page / size 下传 service；`V2SaleOrderService.list()` 使用 repository `Pageable` 查询订单，并按当前页 orderId 批量查询明细；同一性能包记录 `v2_sale_orders_page` p95 4.06ms、`v2_sale_orders_filtered_page` p95 2.47ms，均 5/5 HTTP OK、5/5 logical OK | 用真实账号对比分页结果、过滤条件和接口耗时；如果 UI 需要 total / hasMore 再扩展合同 | 当前响应仍为 `List`，不能从响应直接判断总数或下一页 |
| AI evidence 与 Top N 截断仍需端到端证据 | 后端已补字段级 `evidence_refs` 和查询边界提示；部分工具默认 limit=10，刚好返回 10 条时会提示不能视为全量结论 | 生成真实超限数据证据包，证明 HTTP、SSE、RunTrace 和最终回答都一致展示字段来源与查询边界 | 不能把单测通过当成 P0 端到端通过 |

后端接口性能证据包模板：

```bash
TOKEN="<redacted>" \
BASE_URL="http://localhost:8080" \
ACCOUNT_LABEL="local-owner-5" \
BACKEND_PROFILE="local-h2" \
python3 tools/report_performance_evidence.py \
  --samples 5 \
  --warmup 1 \
  --window-days 30 \
  --limit 20 \
  --sale-order-status 1 \
  --size 20
```

`tools/report_performance_evidence.py` 会采集 `cashflow-summary`、对应 `finance-records` 分页对账锚点、`profit-summary`、`stock-out-records`、`inventory-flow`、`/v2/sale-orders?page=&size=` 和带 `status/created_after/created_before` 的销售单过滤分页路径的 HTTP 状态、业务 `code`、p50 / p95 / max / mean 耗时，输出到 `docs/acceptance-evidence/performance/{yyyyMMdd-HHmmss}-backend-report-performance/`。该包只证明后端接口侧，结论必须保持 `partial`，不能替代 Android 首次可见耗时、截图、UI tree 或 frame timing。

可观测性验收包必须包含：

- 3 个真实问题的请求 / 响应样本。
- 对应 SSE 事件序列。
- 对应 `run_id` / `trace_id` / `audit_id`。
- 工具耗时、模型耗时、端到端耗时。
- LLM disabled、工具失败、无数据、SSE 断开、取消、草稿确认失败至少各 1 条证据或明确说明暂不可测原因。
- 禁止项扫描结果和命中解释。

## 19. 迁移步骤

后续修改 AI 功能时按以下顺序推进：

1. 锁定本文档为审查基线，所有实现 PR 对照本文档列验收证据。
2. 清理生产链路 mock / demo / fake / sample 数据，隔离 demo seed 到 local/demo profile。
3. 为 chat 引入 run 状态和服务端审计记录，保证每次请求都有 `runId`。
4. 拆分 `V2AgentAiService` 职责：Run Orchestrator、Planner、Tool Registry、Model Client、Draft Service、Audit Service。
5. 实现工具执行合同，为每个工具补齐 owner 校验、分页 / 截断元数据、错误码和工具事件。
6. 替换关键词模板回答：关键词规则仅保留为 planner 兜底，最终回答必须基于工具结果和模型 / 规则摘要。
7. 改造 SSE：统一 envelope，真实发送 tool events，禁止完整答案固定切块。
8. 改造 Android RunTrace：展开 / 折叠有效，只显示真实事件；没有事件显示真实缺失态。
9. 改造 LLM 降级：后端响应 `mode` 和 `llm_status`，Android 展示诚实文案。
10. 改造 draft：确认按钮先禁用或标注；P1 新增确认执行接口和状态机。
11. 验证 P0 服务端 cancel run 可调用并让 Android stop 调用该接口；P1 再补持久化、跨进程恢复和长任务中断。
12. 补齐 owner 隔离、LLM disabled、工具失败、SSE 断开、草稿失败、任务通知真实来源的测试。
13. 形成验收证据包，包含接口响应、SSE 日志、Android 截图、审计记录和禁止项扫描结果。
14. 补齐性能和可观测性验收包，记录 `run_id`、`trace_id`、首事件耗时、工具耗时、模型耗时、总耗时、warning 和错误码。

## 20. 审查结论模板

| 检查项 | 结论 | 证据 | 风险 | 后续动作 |
|---|---|---|---|---|
| 真实数据 | 通过 / 不通过 / 部分通过 | 接口、截图、日志 | 高 / 中 / 低 | 修复项 |
| 真实 planner | 通过 / 不通过 / 部分通过 | plan 记录、代码路径 | 高 / 中 / 低 | 修复项 |
| 真实工具事件 | 通过 / 不通过 / 部分通过 | SSE、审计、日志 | 高 / 中 / 低 | 修复项 |
| 禁止假流式 | 通过 / 不通过 / 部分通过 | 代码扫描、抓包 | 高 / 中 / 低 | 修复项 |
| LLM disabled 诚实降级 | 通过 / 不通过 / 部分通过 | 配置、响应、UI | 高 / 中 / 低 | 修复项 |
| RunTrace 展开 | 通过 / 不通过 / 部分通过 | Android 操作证据 | 高 / 中 / 低 | 修复项 |
| AI 首页干净入口 | 通过 / 不通过 / 部分通过 | 初始屏截图、代码路径 | 高 / 中 / 低 | 修复项 |
| 聊天视觉层级 | 通过 / 不通过 / 部分通过 | 对话截图、Compose 代码 | 高 / 中 / 低 | 修复项 |
| 草稿执行 | 通过 / 不通过 / 暂缓 | 接口、DB、审计 | 高 / 中 / 低 | 修复项 |
| cancel run | 通过 / 不通过 / 暂缓 | 接口、SSE、审计 | 高 / 中 / 低 | 修复项 |
| 任务 / 通知真实来源 | 通过 / 不通过 / 部分通过 | DB、接口、profile | 高 / 中 / 低 | 修复项 |
| owner 隔离 | 通过 / 不通过 / 部分通过 | 测试、日志 | 高 / 中 / 低 | 修复项 |
| 审计记录 | 通过 / 不通过 / 部分通过 | DB / 日志样本 | 高 / 中 / 低 | 修复项 |
| 性能和可观测性 | 通过 / 不通过 / 部分通过 | 耗时、trace、metrics | 高 / 中 / 低 | 修复项 |

审查结论必须包含文件路径、接口路径、日志片段、SSE 事件或数据库证据，不能只写“看起来正常”。

## 21. 需求 ID 和审查门禁

后续所有 AI 助手修改必须按需求 ID 提交证据。每个 ID 的结论只能是：`pass`、`fail`、`partial`、`not_applicable`。没有证据时一律算 `fail`。

| ID | 需求 | P0/P1 | 代码审查证据 | 运行验收证据 |
|---|---|---|---|---|
| AGT-P0-001 | 生产链路不得生成 mock / demo / fake / sample 任务、通知、草稿、回答或流式事件 | P0 | 禁止项扫描结果；生产 profile 配置；seed 路径检查 | 新账号任务 / 通知真实空态；真实 run 后才出现任务 / 通知 |
| AGT-P0-002 | 每次聊天必须创建真实 `run_id` 并贯穿响应、SSE、RunTrace、审计 | P0 | chat / stream / audit 代码路径 | 同一 `run_id` 出现在接口、SSE、Android、审计记录 |
| AGT-P0-003 | Planner 只能选择白名单只读工具，关键词只能用于兜底选工具 | P0 | Planner 输出结构、工具白名单、关键词路径 | 提问后能看到 plan、tool reason、工具结果来源 |
| AGT-P0-004 | 工具调用必须 owner-aware，失败必须产生 `tool_failed` | P0 | repository 查询条件、异常处理、tool event 代码 | 构造工具失败后 UI 显示部分失败，不给确定结论 |
| AGT-P0-005 | SSE 不得把完整答案或规则摘要固定切块伪造成流式 | P0 | `answer_delta.delta_source=model_stream` 只来自模型 streaming 回调；`server_notice` 只承载模型流之后的服务端事实说明；规则摘要只通过 `answer_completed` 降级返回 | 抓包证明 `model_stream` 对应模型真实 streaming；`server_notice` 不被 UI 标为模型流；`tool_query_rule_summary` 无 `answer_delta`，并带 `llm_status=disabled/stream_failed_or_empty` |
| AGT-P0-006 | RunTrace 展开必须真实改变状态并展示后端事件 | P0 | `toggleRunTrace` 有状态更新；事件映射来自 SSE | 点击展开后显示真实 safety / plan / tool / answer 事件 |
| AGT-P0-007 | LLM 不可用必须诚实降级，不伪装智能推理 | P0 | 响应包含 `mode` / `llm_status` 或等价字段 | `AGENT_LLM_ENABLED=false` 时 UI 显示规则 / 数据查询模式 |
| AGT-P0-008 | 草稿确认未实现真实执行前不得展示为提交成功 | P0 | Android 不再把 `archived` 当执行成功 | 点击确认前有禁用 / 提示；无业务单据假写入 |
| AGT-P0-009 | 任务 / 通知只能来自真实 run 或真实后台任务 | P0 | seed、scheduler、service 路径不写演示 AI artifact | 生产 / local 新 seed 不出现“演示通知链路”类数据 |
| AGT-P0-010 | 关键数字必须能追溯到工具结果和查询窗口 | P0 | block schema 包含来源 / 窗口 / 截断字段 | 回答中金额、数量、排名能定位工具结果 |
| AGT-P0-011 | 真实回复必须说明查询范围、工具来源、截断 / 失败限制和建议动作边界 | P0 | answer builder、block schema、warnings 映射 | 3 个真实问题的回答均可追溯且不夸大 |
| AGT-P0-012 | 每个 run 必须可观测：`run_id` / `trace_id` 贯穿日志、SSE、工具、审计和响应 | P0 | 日志 MDC / trace 代码、audit schema、响应字段 | 用同一 ID 定位请求、事件、工具耗时和审计 |
| AGT-P0-013 | P0 验收必须记录首事件、工具、模型和端到端耗时 | P0 | metrics / log 字段、测试脚本或手工验收表 | 3 个真实问题的耗时表和 slow warning 证据 |
| AGT-P0-014 | AI 首页必须是干净入口，不展示报表页已有的 KPI、图表、风险、摘要或排行 | P0 | `AgentWorkbenchScreen` 代码无报表型组件；ViewModel 不触发工作台统计查询 | 首次进入 AI 首页截图无经营数据堆叠 |
| AGT-P0-015 | AI 聊天必须有清晰角色和过程层级，Markdown 与图表保持美观可读 | P0 | 用户 / AI 气泡、RunTrace、ResultBlockRenderer、AgentMarkdownText 代码路径 | 一轮真实查询截图能区分消息、过程、结果块和错误 / 降级状态 |
| AGT-P0-016 | Markdown 渲染不得丢内容，链接 URL、表格、代码块和行内强调必须可读 | P0 | `AgentMarkdownText` 解析与渲染分支、失败兜底 | 用真实回答覆盖标题、列表、表格、引用、代码块、链接并截图 |
| AGT-P0-017 | 已知 result block 类型解析失败不得静默消失，图表不得用示例数据补位 | P0 | `ResultBlockRenderer` 每个已知类型都有分支或失败卡；无 Android 示例图数据 | 构造缺字段 block 和空数据 block，确认 UI 显示真实空态 / 错误态 |
| AGT-P0-018 | AI 初始屏文案不得把报表页能力伪装成默认看板 | P0 | quick questions、hero、notice 文案扫描 | 首屏截图 / UI tree 无默认“今日报表、风险看板、统计图看板” |
| AGT-P0-019 | 服务端 cancel 已存在时必须可调用、owner-aware，并通过 `run_cancelled` 或明确不可取消状态与审计对齐 | P0 | `/v2/agent/runs/{run_id}/cancel` 路由、owner 校验和状态机 | 取消后服务端 run 终止或返回已终态 / 不可取消原因，Android 显示已取消或取消未确认 |
| AGT-P1-001 | run cancel 支持持久化状态、跨进程恢复、长任务中断和失败重试 | P1 | run 状态持久化、恢复任务和中断点 | 重启 / 长任务场景下仍能查询取消终态 |
| AGT-P1-002 | 草稿确认执行必须真实事务写入并落审计 | P1 | confirm / reject / execution 接口和状态机 | 确认后业务对象真实创建 / 更新，审计有执行结果 |

## 22. 当前代码证据快照

本节记录 2026-06-08 当前工作树中可直接审查的证据。后续修复时应逐项更新结论，而不是删除问题。

| 证据 | 当前状态 | 需求影响 |
|---|---|---|
| `src/main/java/com/zhihuiji/backend/application/service/DemoDataService.java:109-129` 只 seed 用户、供应商、客户、商品、采购、销售、付款和库存异常；`src/main/java/com/zhihuiji/backend/infrastructure/config/LocalDemoDataInitializer.java:9-20` 限定在 `local` profile 自动 seed。 | warm AI artifact seed 已不在当前 seed 流程中；`clearAll()` 的 agent task / notification 清理已改为 demo owner 范围，不再全表删除非 demo owner 的真实 AI 历史。`AdminControllerTest.seedResetDoesNotDeleteNonDemoOwnerAgentArtifacts()` 覆盖 reset 后真实 owner task / notification 保留。 | 支撑 AGT-P0-001、AGT-P0-009；后续仍需用生产 profile 启动证据证明 demo seed 不污染生产任务 / 通知。 |
| `src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java:126-134` 暴露 `/v2/agent/chat` 和 `/v2/agent/chat/stream`，均进入 `V2AgentAiService`。 | 这是 AI 助手真实验收入口；后续验收不得用 legacy admin smoke 代替。 | 支撑 AGT-P0-002、AGT-P0-011。 |
| `src/main/java/com/zhihuiji/backend/infrastructure/ai/LongCatAnthropicClient.java:55-59` 要求 enabled、apiKey、model、baseUrl 全部存在才调用模型；`LongCatAnthropicClient.java:206-277` 使用 `stream=true` 请求 OpenAI-compatible `chat/completions` 并逐行解析 SSE delta。 | 模型调用和 streaming 都在服务端；Android 不得持有密钥或 provider 配置。 | 支撑 AGT-P0-005、AGT-P0-007 和安全门禁。 |
| `src/main/resources/application.yml:39-45`、`src/main/resources/application-prod.yml:43-48` 默认 `AGENT_LLM_ENABLED=false`；`src/main/resources/application-local.yml:17-22` local 默认 enabled 但 api-key 仍来自环境变量。 | P0 验收必须分别覆盖 disabled 降级和模型配置齐全后的真实 LLM 路径；不能把 local 默认 enabled 当作模型已可用证据。 | 支撑 AGT-P0-007、AGT-P0-013。 |
| `src/main/java/com/zhihuiji/backend/api/controller/AdminController.java:62-71` 暴露 `/v1/admin/demo/seed` 和 `/v1/admin/agent/smoke`；`src/main/java/com/zhihuiji/backend/application/service/AdminService.java:129-137` 的 smoke 固定返回 disabled，并提示使用 `/v2/agent`。 | admin smoke 不是 rebuilt agent 验收链路；demo seed 只能作为 local 数据准备，不得作为真实 agentic 能力证明。 | 支撑 AGT-P0-001、AGT-P0-009、AGT-P0-011。 |
| `src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java:268-352` 当前 stream 会创建真实 `runId` 并发送 `run_started`、安全检查、最终完成事件；`V2AgentAiService.java:359-393` 会按计划执行工具并捕获失败。 | 已具备 run lifecycle 雏形，`sendEvent` 会为带 `run_id` 的事件补 `event_id`、`seq`、`conversation_id` 和 `timestamp`，主要过程事件已有 `audit_id` / `trace_id` / `observability`；`agent_run_audits` 会持久化 run 摘要状态、mode、llm_status、plan_source、tool_count 和 event_count；`agent_run_audit_events` 会持久化每个 SSE 事件 payload JSON。 | 部分支撑 AGT-P0-002、AGT-P0-012；仍需补完整 `data` 包裹式 envelope、真实抓包与数据库审计对账证据。 |
| `V2AgentAiService.java:1361-1390` 已发送 `tool_started` / `tool_completed`；`V2AgentAiService.java:1393-1409` 已发送 `tool_failed`，且 `V2AgentAiService.java:370-380` 会在工具异常时进入失败事件路径。 | 工具失败事件路径已存在；started / completed / failed payload 已补 `input_summary`、`query_window`、`started_at`、`completed_at`、`duration_ms`、`returned_count`、`total_count`、`limit`、`is_truncated`、`next_cursor`、`evidence` 和 `trace_id`；Android `ToolCompleted` / `ToolCallRecord` / `ToolAuditRecord` 已接收并在 RunTrace 工具卡显示范围、依据、耗时和追踪摘要。 | 部分通过 AGT-P0-004、AGT-P0-006；下一步要补真实 SSE 抓包、超限数据验收和事件级审计。 |
| `V2AgentAiService.java` 仅在模型 streaming 回调内发送 `answer_delta(delta_source=model_stream)`；LLM disabled 或 stream failed / empty 时不再发送规则摘要分块，而是通过 `answer_completed` 返回 `tool_query_rule_summary`、`plan_source` 和对应 `llm_status`。后端追加查询边界 / 部分失败说明时，可在真实模型 delta 后、`answer_completed` 前发送 `answer_delta(delta_source=server_notice)`，Android 会标为服务端说明而不是模型流。`answer_delta` 已补 `audit_id`、`trace_id` 和 `observability`，Android `AgentStreamEvent.AnswerDelta` 可接收这些字段。`V2AgentAiServiceTest.streamFallbackAnswerCompletesRuleSummaryWithoutFakeDeltas`、`streamDisabledModelAnswerCompletesRuleSummaryWithoutFakeDeltas` 覆盖 fallback / disabled 的 `answer_delta` 数量为 0、`answer_completed` 含规则摘要、规划来源和降级状态；`streamModelAnswerEmitsOnlyModelStreamDeltasAndStreamedCompletion` 覆盖模型流式成功时模型 delta 均为 `delta_source=model_stream`、每个 delta 带 `audit_id` / `trace_id` / `log_ref`、最终 `mode=tool_query_llm_streamed` / `llm_status=streaming` / `plan_source=keyword_fallback`，且不带规则摘要提示；`streamModelAnswerEmitsServerNoticeTailBeforeCompletionWhenBackendAppendsBoundaries` 覆盖服务端说明增量在 `answer_completed` 前出现。 | 后端“固定切完整答案 / 规则摘要冒充流式”的代码边界已收口；成功流式、服务端说明、禁用降级和空流失败路径都有单元门禁，且 delta 能被抓包归因到同一 run / trace。证据脚本需同步允许 `server_notice` 但仍拦截非白名单 delta；当前仍不能把它视为端到端通过，因为还缺真实模型 `model_stream` 抓包和真机 UI 证据。 | AGT-P0-005 后端和 Android 展示边界已改善；部分支撑 AGT-P0-012 可观测性；仍需 SSE 抓包、真机 UI、真实模型 delta、server_notice 展示和 rule_summary 完成态截图验收。 |
| `V2AgentAiService.java` 优先尝试 LLM 工具规划；LLM 不可用或规划不可解析时使用 `plan_source=keyword_fallback`，`plan_delta`、run summary 和审计 payload 均可带出该来源；Android `RunTracePanel` 显示为“关键词兜底规划”。最终回答不可用时返回 `tool_query_rule_summary` / 对应 `llm_status`。 | 兜底规划和规则摘要不再被模糊标成普通关键词规划或模型规划；但这仍是 P0 过渡方案，不等同于 provider function-calling 或真模型规划。 | 部分支撑 AGT-P0-003、AGT-P0-007、AGT-P0-011；仍需真实 SSE / DB 对账和真机 UI 证据。 |
| `src/main/java/com/zhihuiji/backend/api/dto/v2/agent/V2AgentDtos.java` 和 `master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/AgentChatRequestResponse.kt` 当前非流式 `AgentChatResponse` 已包含 `tool_calls`、`evidence_refs`、`performance_summary`、`result_blocks`、`audit_id`、`trace_id` 和 `observability`。`AgentChatResponseSerializationTest.decodesNonStreamingAgentRunContract`、`V2AgentAiServiceTest.nonStreamingChatIncludesAuditableAgentRunContract` 已覆盖模型解析和服务单测合同。 | 同步接口合同已经具备独立审查 plan / tool / evidence / performance 的字段基础；仍不能替代真实 HTTP 响应、owner 数据、审计对账和真机 UI 证据。 | 部分支撑 AGT-P0-002、AGT-P0-003、AGT-P0-011、AGT-P0-012；运行验收仍需 `/v2/agent/chat` 真实响应包。 |
| `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:163-230` 将服务端安全和 plan 事件写入 `RunTrace`；`AgentChatViewModel.kt:497-501` 的 `toggleRunTrace()` 已真实切换展开状态。 | Android 不再是 no-op 展开；仍需真机验证真实 SSE 事件能完整显示。 | 支撑 AGT-P0-006；运行验收仍需截图 / UI tree / SSE 日志。 |
| `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:543-568` 和 `DraftListViewModel.kt:70-99` 仍通过 `status = "archived"` 处理当前草稿动作；`DraftListScreen.kt:51-54`、`DraftListScreen.kt:197-200` 明确写成“仅归档”，`DraftListScreen.kt:234-239` 将 archived 映射为 `StatusType.ARCHIVED`，`DraftListViewModel.kt:152-156` 将 archived 展示为“已归档（未执行）”。 | P0 代码文案边界已收口为“不执行业务写入 / 仅归档”，不再把 archived 显示为业务执行成功；P1 仍必须新增 confirm / reject / execution 接口和真实状态机。 | AGT-P0-008 代码边界已改善，仍需真机运行复核；AGT-P1-002 未通过。 |
| `master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/AgentSseClient.kt` 只连接 `/v2/agent/chat/stream` 并解析服务端 SSE / ndjson；当前已用 cancellable execute 将 Flow 取消传递到 OkHttp `Call.cancel()`，并由 `AgentSseClientCancellationTest` 覆盖“停止接收会取消底层网络调用”。`AgentSseClient` 在实例生命周期内复用 60s read timeout 的 SSE OkHttp client，避免每次聊天重新构造派生 client。`AgentStreamModels.kt` 定义工具、失败、answer_completed / answer_delta 等事件模型，其中 `AnswerDelta` 已接收服务端 `audit_id`、`trace_id`、`observability`、`event_id`、`seq` 和 `conversation_id`，`AnswerCompleted` 已接收 `mode`、`llm_status` 和 `plan_source`。 | Android 网络层方向正确，不主动生成事件；P0 停止接收不再只改 UI 状态，也会释放本地长连接资源。事件模型已跟进模型 delta 和完成态的可观测字段与兼容 envelope；服务端 run cancel 已可调用，P1 继续补持久化 run 状态和跨进程恢复。 | 支撑 AGT-P0-005、AGT-P0-006 和 P0 取消诚实态；部分支撑 AGT-P0-012；依赖后续真机和真实 SSE 抓包验收。 |
| `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatViewModel.kt:132-159` 构造 `AgentChatRequest(stream = true)` 并收集 `repository.chatStream(request)`；`master-goods-android/data/agent/src/main/java/com/zhihuiji/data/agent/AgentV2Repository.kt:99` 将请求交给 SSE client。 | Android 主聊天链路已指向真实 `/v2/agent/chat/stream`，不是本地 mock chat；后续验收要补真机端到端证据。 | 支撑 AGT-P0-002、AGT-P0-006、AGT-P0-011。 |
| `V2AgentAiService.java` 的 `/v2/agent/workbench` 当前只做 owner 鉴权并返回 greeting、空 `kpi_cards`、空 `quick_questions`、空 `recent_conversations`、空 `pending_drafts`、空 `risk_alerts`、`today_summary=null`；不再为 AI 初始屏额外查询最近会话、active 草稿或逐条 message count。`V2AgentAiServiceTest.workbenchDoesNotExposeReportDashboardDefaults()` 固定该合同，并验证不会触发 conversation / draft / message repository。`AgentWorkbenchViewModel.kt` 只消费 greeting / 同步状态；`AgentWorkbenchScreen.kt` 只展示 Hero、任务 / 通知入口和干净说明。 | AI 首页从后端合同、Android 消费和防回归测试三层收敛为轻量干净入口；DTO 为兼容仍保留字段，但当前不展示且返回为空。这同时减少初始屏 DB 查询，属于不改 UI 的链路性能优化。P0 仍需真机首屏截图 / UI tree 证明无 KPI / 图表 / 风险 / 今日摘要 / 默认报表看板。 | AGT-P0-014 / AGT-P0-018 代码边界已改善；部分支撑性能目标；仍需接口抓取和真机首屏证据。 |
| `master-goods-android/feature/reports/src/main/java/com/zhihuiji/feature/reports/ReportViewModel.kt` 的往来余额改为 `ReportRepository.reconciliationSummary()`，不再为了应收 / 应付金额全量拉取客户和供应商列表；`feature/reports/build.gradle.kts` 同步清理为只依赖 `data:report`。`ReportDto.ReconciliationSummaryReportDto` / Android `ReconciliationSummaryReportDto` 已补 `total_receivable_customer_count` 和 `total_payable_supplier_count`；`DashboardViewModel.kt` 的应收金额和应收客户数优先使用后端 SUM / COUNT，仅在汇总失败或旧后端缺 count 时兜底客户列表。净现金流保留原资金流水口径，不用回款 / 付款单口径替代。 | 在不改 UI 的情况下减少报表页和首页部分全量列表聚合；同时避免为了性能优化改变 Dashboard “资金流水净额”的业务语义。仍未解决销售趋势需要销售单明细等更深层性能问题。 | 部分支撑全链路性能目标；仍需真实数据量下的接口耗时、Android 首屏耗时和帧统计证据。 |
| `V2AgentAiService.buildSalesTrendBlock()` 已改为复用 `SaleOrderRepository.salesTrendBuckets()` 的数据库 bucket 聚合；`salesTrendBuckets()` 同时返回销售额、订单数和回款金额，AI 近 7 天销售趋势继续输出原 `line_chart` 的“销售额 / 回款”双序列，不再拉取近 7 天销售单实体后在 Java 内存分桶。`V2AgentAiServiceTest.nonStreamingChatPlansEnglishBusinessKeywordsForDeviceQa()` 固定 `sales_overview_lookup` 会调用 `salesTrendBuckets()` 且不会调用 `findByOwnerUserIdAndCreatedAtBetween()`；`SaleOrderRepositoryTest.salesTrendBucketsAggregatesByOwnerBucketAndExcludesCancelledOrders()` 固定 SQL 聚合会排除取消单并返回回款列。 | AI 经营概览图表的性能路径已从实体扫描收敛为数据库聚合，且不改变 Android result block / UI 合同。仍需真实大数据量账号下的接口耗时、SSE 工具耗时和 UI 首次可见证据。 | 部分支撑 AGT-P0-010、AGT-P0-011、AGT-P0-013 和全链路性能目标；不能替代端到端证据包。 |
| `V2AgentAiService.buildFinalAnswer()` 与 `buildFinalAnswerForStream()` 已统一调用查询边界提示：工具带 `limit`、`is_truncated` 或 `window_days` 时，最终回答会说明最多查询 / 仅返回前 N 条 / 不能视为全量结论 / 窗口为近 N 天。`toEvidenceRefs()` 已从各工具 facts 拆出字段级证据，例如 `customer_count`、`top10_receivable_total`、`sales_amount`、`recent_total_amount` 等。`V2AgentAiServiceTest.nonStreamingChatExplainsLimitedQueryBoundaryAndFieldLevelEvidence()` 固定 10 条应收客户场景必须提示边界，并要求 evidence refs 包含具体字段和值。 | AI 回复从“工具摘要”推进到“结论 + 查询边界 + 字段级依据”，降低 Top N / 最近 N 条被误解为全量结论的风险；LLM 合成路径和规则摘要路径都受同一事实初稿约束。仍需真实超限数据、真实模型流式回答、RunTrace 截图和 HTTP/SSE 证据包证明端到端一致。 | 部分支撑 AGT-P0-004、AGT-P0-006、AGT-P0-011、AGT-P0-012；运行验收仍未完成。 |
| `ReportService.cashflowSummary()` 与 `/v1/reports/cashflow-summary` 已按 `finance_records` 的 `TYPE_INCOME - TYPE_EXPENSE` 聚合收入、支出、净现金流和记录数；Android `CashflowSummaryReportDto` / `ZhihuijiApi.cashflowSummary()` / `ReportRepository.cashflowSummary()` 已接入，`DashboardViewModel` 改为消费该聚合结果，并移除 `data:finance` 依赖。`ReportServiceTest.cashflowSummaryUsesFinanceRecordAggregatesWithoutChangingPaymentLedgerSemantics()`、`SerializationContractTest.cashflowSummary_usesSnakeCaseForFinanceRecordAggregateContract()`、`ZhihuijiApiContractTest` 固定口径、snake_case 和接口路径。`docs/acceptance-evidence/performance/20260609-052957-backend-report-performance/03-summary.md` 记录 local H2 / LLM disabled 下 `dashboard_cashflow_summary` 5 次样本全部 HTTP 200 / `code=0`，p95 1.87ms。 | 首页净现金流从“刷新资金流水列表 + Room 本地求和”推进到后端 owner-scoped 聚合，且没有把 `reconciliationSummary.netCashFlow` 的回款 / 付款单口径误用于 Dashboard。后端接口侧已有第一份真实采样证据，但仍需真实账号值对账、Android 首页刷新首个可见耗时和 frame timing。 | 部分支撑全链路性能目标；后端接口证据已补一轮，Android 运行验收仍未完成。 |
| `ReportService.profitSummary()` 已改为调用 `SaleOrderItemRepository.profitSummary()`，由数据库按 owner、订单创建时间、非取消状态聚合销售额和估算成本；SQL 使用 LEFT JOIN 商品和 `COALESCE(p.purchasePrice, 0)`，保持“缺商品不丢销售额、成本按 0”的旧口径。`ReportServiceTest.profitSummaryIgnoresInvalidProductIdsInsteadOfFailingReport()` 固定 service 不再调用销售单列表、明细批量拉取和商品全量拉取；`SaleOrderItemRepositoryTest.profitSummaryAggregatesSalesAndTreatsMissingProductCostAsZero()` 固定取消单排除、owner 隔离、缺商品成本为 0 和商品成本命中。 | 利润汇总从三类实体列表本地聚合推进到数据库 scalar 聚合，降低大区间报表内存和查询压力，同时不改变响应 DTO / UI 合同。仍需真实账号与旧口径对账、接口耗时和大数据量回归证据。 | 部分支撑全链路性能目标；运行验收仍未完成。 |
| `ReportService.stockOutRecords()` 已改为调用 `SaleOrderItemRepository.recentStockOutRows()`，在 repository 层按 owner、订单创建时间窗口、非取消状态过滤，并按明细 `createdAt DESC` 分页；`ReportService.inventoryFlow()` 已改为三路分页：`recentSaleInventoryFlowRows()` 取销售出库、`recentCancelledSaleInventoryFlowRows()` 取取消入库、`InventoryAdjustmentRepository.findByOwnerUserIdAndCreatedAtBetweenOrderByCreatedAtDesc()` 取库存调整，再在 service 层合并为原 `InventoryFlowRecordDto` 合同并按 `flowTime DESC` 截断。`ReportServiceTest.stockOutRecordsUsesPagedItemJoinInsteadOfLoadingFullOrderRange()` 和 `inventoryFlowLoadsOnlyPagedSourcesThenMergesByFlowTime()` 固定 service 不再调用旧全量订单 / 明细回填路径；`SaleOrderItemRepositoryTest` 与 `InventoryAdjustmentRepositoryTest` 固定 owner、状态、时间、排序和 limit。 | 库存报表从“整段实体扫描 + 内存排序 limit”推进到 repository 分页候选集，响应 DTO 和 Android UI 合同不变。三路各取 N 后合并能覆盖全局 Top N 候选，但仍需真实数据对账和接口耗时证据。 | 部分支撑全链路性能目标；运行验收仍未完成。 |
| `V2SaleOrderController.list()` 已移除 controller 级 `PaginationUtils.slice(v2SaleOrderService.list(...))` 全量切片，改为将 `page` / `size` 传入 `V2SaleOrderService.list(...)`；`V2SaleOrderService` 使用 `saleOrderRepository.search(..., PageRequest)` 只取当前页订单，并通过 `saleOrderItemRepository.findByOwnerUserIdAndOrderIdIn()` 批量组装明细，避免每个订单再 `listItems()`。`V2SaleOrderServiceTest.listUsesRepositoryPaginationAndBatchLoadsItems()` 固定分页查询与批量明细路径，`V2BillDomainControllerTest.saleOrderListPassesPaginationToServiceInsteadOfControllerSlice()` 固定 controller 不再吞掉分页参数。 | V2 销售单列表从服务端全量列表 + controller 截断 + 明细 N+1 推进到 repository 分页 + 一次批量明细查询；响应仍保持原 `List<SaleOrderResponse>` 合同，不改 Android UI。仍需真实分页数据对账和接口耗时证据。 | 部分支撑全链路性能目标；运行验收仍未完成。 |
| `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentWorkbenchViewModel.kt` 已记录 `isRemoteSynced`，远端工作台失败时错误文案改为“工作台状态同步失败，仍可打开对话入口”；`AgentWorkbenchScreen.kt` 仅在远端同步成功时承诺真实查询状态，失败时显示“远端工作台未同步，仅保留对话入口”。`DraftListViewModel.kt` 对缺失业务号、往来方和金额显示“后端未返回...”而不是合成 `草稿 #id` 或默认金额。 | AI 入口和草稿列表的静态兜底更加诚实，降低用户把本地占位理解为真实后端数据的风险；仍需真机截图覆盖远端失败和缺字段草稿。 | 支撑 AI 首页诚实态和草稿禁止假字段门禁。 |
| `TaskNotificationViewModel.kt` 已拆分 `tasksError` / `notificationsError`，任务和通知接口分别失败时不再用 `emptyList()` 冒充真实空态；`TaskNotificationScreen.kt` 在对应 tab 显示“同步失败，本页不会把失败伪装成暂无任务 / 通知”，成功同步但为空时才显示“当前账号没有真实任务 / 通知”。 | 任务 / 通知入口从“单一 error 或空列表”推进到按 tab 诚实显示局部失败；仍需断网 / 单接口失败真机截图和 UI tree 验收。 | 支撑 AGT-P0-001、AGT-P0-009、AGT-P0-018 的诚实空态要求。 |
| `AgentWorkbenchViewModel.kt:59-87` 默认 quick questions 偏审查说明，不展示经营 KPI，并新增报表型关键词过滤；`AgentWorkbenchScreen.kt:237-240` 明确“Markdown、图表和依据会在对话里按问题生成”；`AgentWorkbenchScreen.kt:292-312` 显示工作台同步状态。 | 当前文案方向是入口型而非默认报表型；如果后端未来误回“今天、风险、补货、图表”等默认看板式内容，Android 会回退到干净入口问题。 | AGT-P0-018 代码边界已改善；后续以首屏截图、UI tree 和 `/v2/agent/workbench` 响应验收。 |
| `master-goods-android/feature/agent/src/main/java/com/zhihuiji/feature/agent/AgentChatScreen.kt:267` 用不同渐变区分用户 / AI 气泡；`AgentChatScreen.kt:619-634` 定义 AI / 用户气泡色；`RunTracePanel.kt:59-62` 为思考过程使用独立暖色底。 | 聊天角色和过程层级已比同色玻璃卡更清晰；后续仍需真机截图验证 Markdown、图表、过程卡同屏可读。 | 支撑 AGT-P0-015。 |
| `AgentMarkdownText.kt` 当前已经有标题、列表、代码块、引用、分隔线、表格、粗体、斜体、行内代码和链接样式的轻量解析；表格解析已改为扫描器，跳过转义 `\|` 和行内代码 span 内的 `|`；代码块显示和复制不再 `trimEnd()` 吃掉尾部空白。`AgentMarkdownTextParserTest` 覆盖转义竖线、代码 span 竖线和代码块尾部空白保真。 | Markdown 方向继续收敛；后续审查仍必须确认链接 URL 不丢失、长代码可读、未闭合 Markdown 不崩溃；点击链接和复制代码属于 P1。 | 部分支撑 AGT-P0-016；已有单测覆盖关键表格 / 代码块边界，仍需运行截图和真实回答边界输入验证。 |
| `ResultBlockRenderer.kt` 当前支持 KPI、表格、排行、折线 / 面积 / 趋势图、柱状图、环形 / 饼图、风险、依据和草稿等 result blocks；已知类型解析失败会显示错误卡并保留原始数据摘要；未知 block 会显示标题、类型和原始数据摘要；空 KPI / 表格 / 排行 / 依据会显示真实空态；图表标签、序列数量、无效数值不合规时停止绘制并说明原因，柱状图保留负数并按零基线绘制，donut / pie 对无效或非正数分段显示可见忽略提示，结构化 table 行列不一致时停止渲染且单元格复用 inline Markdown。`ResultBlockRendererContractTest` 已覆盖图表坏合同、table 行列错位、table Markdown、donut / pie 无效分段和已知图表缺字段解析失败；`AgentSseClient.kt` 对本地无法解析的 SSE 坏帧返回 `STREAM_PARSE_ERROR`，不再静默吞掉。 | 结构化结果兜底边界已有单元门禁：真实后端坏块、未知块、空块和坏帧不会完全消失，也不会为了好看补示例数据。仍需真实后端 block、真机截图和坏块视觉验收。 | AGT-P0-017 代码边界已改善；运行验收仍需覆盖真实空数据、字段缺失、未知 block、坏 SSE 帧、工具失败和真实 chart block。 |
| `ChatMessage.hasServerAnswerDelta` 记录是否收到过服务端 `answer_delta`；`AgentChatViewModel.kt` 只在 `AnswerDelta` 到达时标记 `hasServerAnswerDelta=true` 并保存 `answerDeltaSource`；`AnswerCompleted` / `RunCompleted` 只补全最终文本，不再触发本地 `animateReveal`。`AgentChatScreen.kt` 已移除 `StreamingMarkdownText` 的 `chunkSize`、`substring(0, nextLength)` 和 `delay(16)` 拆字循环；规则摘要完成态按 `mode/llm_status` 显示降级，不再当作流式吐字。`AgentResponseProvenanceTest` 固定“没有服务端 delta 的完成回答不能标成流式”、“只有 `model_stream` 显示模型流”、“`rule_summary` 显示服务端摘要”。 | Android 生产聊天 UI 不再用本地拆字制造完整回答过程；回答过程只能由服务端 `answer_delta(model_stream)` 驱动，规则摘要不会被显示成模型成功或吐字过程。`AgentChatViewModel` 的 `delay(48)` 已明确为 UI 合帧节流，只合并服务端 delta，不拆分完整回答伪造 token。后续必须用抓包证明 `model_stream` 来源与 UI 文案一致。 | AGT-P0-005 Android 假流式和来源展示边界已改善；AGT-P0-015、AGT-P0-016 仍需真机截图、真实 SSE 抓包和 Markdown 边界验证。 |
| `master-goods-android/feature/agent/DEVELOPMENT.md` 已同步说明 AgentChat 走真实 SSE、RunTrace 来自后端事件、草稿当前仅归档；`master-goods-android/core/database/DEVELOPMENT.md` 已同步 Room、Agent 通知和 Agent 审计表状态。 | 后续审查以源码和本文档为正本；Agent 与 database 模块说明不再把已接入能力误写成未开始或非流式 fallback。 | 降低后续审查误判风险。 |

## 23. 下一轮实现优先级

建议按以下顺序改造，避免先做 UI 演示而继续保留假 agent 行为：

1. 后端先补统一 run event envelope：`event_id`、`run_id`、`conversation_id`、`seq`、`event_type`、`timestamp`、`trace_id`，并把现有 `run_started`、安全检查、plan、tool、answer、completed / failed 事件全部纳入同一格式。
2. 为所有工具事件和工具结果补齐 `input_summary`、`started_at`、`returned_count`、`is_truncated`、`duration_ms`、`query_window`、`evidence`、`error_code`，保证回答中的数字能追溯。
3. 建立服务端审计存储和可观测性字段，让 `run_id` / `trace_id` 能串起 HTTP、SSE、工具、模型、草稿、错误和审计记录。
4. 保持禁止假流式门禁：模型支持 streaming 时只把供应商回调转发为 `delta_source=model_stream`；服务端查询边界 / 部分失败说明只能作为 `delta_source=server_notice` 且不得标成模型流；不支持或 fallback / rules 路径不得发送规则摘要分块，只能通过 `answer_completed` 标明 `tool_query_rule_summary` 和 `disabled` / `stream_failed_or_empty`；每次修改后运行 `streamFallbackAnswerCompletesRuleSummaryWithoutFakeDeltas`、`streamDisabledModelAnswerCompletesRuleSummaryWithoutFakeDeltas`、`streamModelAnswerEmitsServerNoticeTailBeforeCompletionWhenBackendAppendsBoundaries` 或等价测试。
5. Android 继续只展示服务端真实事件；补真机验收，确认 RunTrace 展开后能看到 safety、plan、tool、answer、completed / failed 事件。
6. 草稿确认按钮在 P1 前继续保持“仅归档，不会执行业务写入”的诚实文案；P1 再实现 confirm / reject / execution 接口。
7. 建立验收脚本：禁止项扫描、LLM disabled 降级、SSE 事件序列、owner 隔离、任务通知真实来源。
8. 收紧 AI 初始屏文案：去掉默认报表 / 风险 / 今日看板暗示，只保留“按问题查询后生成 Markdown、表格、图表和依据”的入口说明。
9. 完成 Markdown 和 ResultBlock 兜底：链接 URL 不丢、已知 block 解析失败不静默、真实空态和工具失败态可见。
10. 建立草稿 P1 执行状态机：区分 `pending`、`confirmed`、`executing`、`executed`、`failed`、`archived`，并提供真实事务写入证据。
11. 持续保持 DEVELOPMENT 文档与源码同步，避免后续审查把真实 SSE、Agent 审计或通知表误判为未接入。
12. 建立性能 / 可观测性验收表：真实问题、`run_id`、`trace_id`、首事件耗时、工具耗时、模型耗时、总耗时、错误码、warning、审计状态。

## 24. 后续审查修改 checklist

每次后续 AI 助手审查或修改前，必须逐项勾选并留下证据；未勾选项不能口头通过。

- [ ] 写入范围确认：列出本轮允许修改的文件；如本轮不允许动 Android，不得修改 `master-goods-android/**`。
- [ ] 工作树保护：记录开始前 `git status --short`，识别他人并行改动，不 revert 不相关文件。
- [ ] 禁止项扫描：扫描 `mock`、`demo`、`fake`、`sample`、`placeholder`、`模拟`、`演示`、`delay`、`timer`、`substring`、`chunkSize`，逐项解释命中是否在生产 AI 链路；`substring` / `chunkSize` 若用于回答拆字、规则摘要分块或模型外本地 reveal，验收不通过。
- [ ] 真实数据链路：验证 3 个真实经营问题均调用 owner-aware 工具，回答中的金额、数量、排名、风险可追溯。
- [ ] Agentic 过程：验证每个 run 有 `run_started`、安全检查、plan、tool、answer、completed / failed 事件；无事件不得由 Android 补造。
- [ ] 工具合同：检查每个工具有输入摘要、权限校验、查询窗口、分页 / 截断、耗时、状态、错误码和 evidence。
- [ ] 审计合同：用同一 `run_id` 或 `trace_id` 定位 HTTP、SSE、工具、模型、草稿、取消、错误和审计记录。
- [ ] 取消机制：P0 验证服务端 cancel 可调用、owner-aware、SSE `run_cancelled` 或明确不可取消状态和审计一致；若当前环境调用失败，Android 必须显示“本机已停止接收但服务端取消未确认”。
- [ ] 确认机制：P0 不把 `archived` 当执行成功；P1 验证 confirm / reject / execution 接口真实事务写入。
- [ ] 降级机制：验证 `AGENT_LLM_ENABLED=false`、模型错误、工具失败、无数据、SSE 断开都有诚实 UI / 响应。
- [ ] 性能验收：记录 3 个真实问题的首事件耗时、工具耗时、模型耗时、端到端耗时、slow warning 和错误率。
- [ ] 可观测性验收：确认响应、日志、指标、审计包含 `run_id`、`trace_id`、`audit_id` 或可定位替代字段。
- [ ] 安全验收：确认日志、审计、SSE、Android 本地状态不包含密钥、token、完整内部 prompt、SQL 堆栈或跨 owner 数据。
- [ ] Markdown 验收：真实回答覆盖标题、列表、表格、引用、代码块、链接、粗体、斜体、行内代码；确认链接文本旁可见 URL，代码块可复制并保留尾部空白，解析失败不丢正文。
- [ ] 图表验收：真实 result block 覆盖折线、柱状、环形 / 饼图；空 labels、series 长度与 labels 不一致、NaN / Infinity、donut / pie 非正数 segment、未知 block、已知 block 字段缺失、坏 SSE 帧、工具失败均显示真实空态 / 错误态，不生成示例图。
- [ ] AI 首页文案验收：首屏截图和 UI tree 无默认报表看板、今日经营摘要、风险列表或预置统计图暗示。
- [ ] 多 agent 审查：至少一个 Android 视角、一个后端 / agent 视角并行复核；若代理额度不足，记录阻塞原因并在资源释放后补跑。
- [ ] 结论更新：按第 20 节模板和第 21 节需求 ID 更新 pass / fail / partial / not_applicable，不删除未解决问题。

## 25. 多 Agent 审查分工

后续进入 AI 助手实现审查或 UI 一比一还原时，必须优先使用多 agent 并行复核，但每个 agent 的写入范围必须互不重叠。

推荐分工：

- Android Agent：只审查 / 修改 `master-goods-android/feature/agent/**`、`master-goods-android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/**`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/AgentSseClient.kt`，负责 Markdown、流式 UI、RunTrace、ResultBlock、AI 初始屏干净度和真机截图。
- Backend Agent：只审查 / 修改 `src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java`、`src/main/java/com/zhihuiji/backend/application/service/v2/**`、`src/main/java/com/zhihuiji/backend/infrastructure/ai/**`、agent repository / audit 相关文件，负责真实 planner、工具事件、SSE、审计、降级和禁止假流式。
- Design Agent：只读 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 和 Bilipay UI 参考项目，产出 UI 差距表，不直接改生产代码，避免和实现 agent 冲突。
- Verification Agent：只运行测试、ADB、禁止项扫描、接口 / SSE 抓包和截图采集，不改文件。

多 agent 合并规则：

- 主线程负责最终集成和冲突处理。
- 子 agent 不得 revert 其他人改动。
- 每个子 agent 最终必须列出证据、文件路径、运行命令和未验证风险。
- 如果当前线程代理数量达到上限，主线程先吸收已完成代理结果并关闭不再需要的代理，再继续并行审查。

## 26. 本轮多 Agent 证据吸收

本节记录 2026-06-08 已完成的并行 agent 审查结论，作为后续修改的输入。若后续源码变化，应重新复核，不得把本节当成永久通过证明。

| Agent 视角 | 已吸收结论 | 对需求的影响 |
|---|---|---|
| 后端 / Agent 服务审查 | `/v2/agent/chat` 和 `/v2/agent/chat/stream` 当前业务结果主要来自当前 owner 范围内的 repository 查询；工具白名单覆盖低库存、商品、客户应收、供应商应付、销售概览、销售单、采购单、付款单和财务流水。 | 支撑“真实查询优先”的方向，但仍要求补齐工具合同字段、统一 event envelope、服务端 run 状态、审计和可观测性。 |
| 后端 / Agent 服务审查 | 当前过程事件由服务端编排生成，不是模型原生 function-calling；这可以作为 P0 过渡，但 UI 和文档不得把它包装成“模型正在真实调用工具”。 | `plan_source`、`tool_source`、`mode`、`llm_status` 必须显式返回并展示；P1 再升级 provider tool / function calling。 |
| 后端 / Agent 服务审查 | `AdminService.runAgentSmoke()` 是 admin/local smoke 固定响应，不是 `/v2/agent` 验收链路；`DemoDataService` 只可作为 local 数据准备，不能作为生产 AI 任务 / 通知证明。 | 后续验收必须使用 `/v2/agent`、真实账号和真实数据，不能用 admin smoke 或 demo seed 冒充 agent 能力。 |
| Android / Agent 前端审查 | Android chat 主链路已走 `AgentV2Repository.chatStream()` 和 `AgentSseClient`；后端 fallback / rules 不再分块发送规则摘要，只通过 `answer_completed` 标明 `tool_query_rule_summary` + `disabled` / `stream_failed_or_empty`；Android 新增 `hasServerAnswerDelta` 区分服务端真实 delta 和本地等待查询，本地完整回答 reveal 已移除。 | AGT-P0-005 后端与 Android 假流式边界已改善；模型真流仍只能由服务端 `delta_source=model_stream` 抓包证明，后续要补端到端 SSE / 真机证据。 |
| 多 agent 并行审查 2026-06-08 | 后端审查确认 `/v2/agent/chat/stream` 的 `answer_delta` 发送点只在 `LongCatAnthropicClient.streamTextMessage` 回调内，Android 审查确认本地 `delay(48)` 只做服务端 delta 合帧，不生成内容；本轮据此补 `streamModelAnswerEmitsOnlyModelStreamDeltasAndStreamedCompletion`、`AgentResponseProvenanceTest`，并为 `answer_delta` 补 `audit_id` / `trace_id` / `observability`。 | 禁止假流式从“代码意图”推进到后端 / Android 双侧单测门禁，且模型 delta 可归因到同一 run trace；仍缺真实 provider SSE 抓包和真机 UI 证据。 |
| Android / 任务通知诚实态审查 | 子代理指出 `TaskNotificationViewModel` 原先在任务或通知接口失败时可能把该类 `getOrDefault(emptyList())` 显示成空列表；本轮已拆分每类错误并在 UI 中明确“同步失败，不伪装成暂无”。 | 局部失败不会再被误判为真实空态；仍需模拟单接口失败的真机或集成证据。 |
| 多 agent 并行审查 2026-06-08 / 工具合同 | Android 子代理确认 `ToolStarted` / `ToolCompleted` / `ToolFailed -> AgentChatViewModel -> RunTracePanel` 是流式工具证据链；本轮据此补后端工具事件合同字段、Android 流式模型字段、`ToolCallRecord` / `ToolAuditRecord` 保存和 RunTrace 短摘要展示，并用 `V2AgentAiServiceTest.streamToolCompletedIncludesAuditMetadata`、`AgentStreamEventSerializationTest.decodesBackendToolCompletedEventWithSnakeCaseFields` 固定字段解析。 | 工具事件从“只有数量 / 耗时”推进到可审查查询范围、证据、游标和时间边界；仍缺真实 SSE 日志、真机 RunTrace 截图和事件级审计。 |
| 多 agent 并行审查 2026-06-08 / 服务端审计 | 后端子代理确认生产环境是 Flyway 建表、JPA `ddl-auto=validate`，新增 run 审计必须带 migration；本轮新增 `agent_run_audits`、`agent_run_audit_events`、`AgentRunAuditEntity`、`AgentRunAuditEventEntity` 及仓库，并在同步 / 流式 chat 的 running、blocked、completed、failed、cancelled 路径写入 run 摘要审计，在 `sendEvent` 写入事件 payload 审计；新增 `GET /v2/agent/runs/{runId}/audit` 供 owner-scoped 验收读取。`V2AgentAiServiceTest.nonStreamingChatIncludesAuditableAgentRunContract`、`streamEventsIncludeCompatibleEnvelopeMetadata`、`getRunAuditReturnsOwnerScopedSummaryAndEvents` 和 `V2AgentMediaControllerTest.getRunAuditReturnsSnakeCaseFieldsAndEvents` 验证状态、mode、llm_status、plan_source、tool_count、event_count、事件 payload 和 HTTP 输出。 | 服务端审计从“只有前端本地记录 / 响应字段”推进到后端 run 摘要、事件级 payload 持久化和可读取验收接口；仍缺真实数据库迁移验收、真实 SSE / DB 对账和接口证据包。 |
| Android / Agent 前端审查 | `AgentMarkdownText` 和 `ResultBlockRenderer` 已有 Markdown、表格、KPI、排行、折线、柱状、环形 / 饼图等渲染分支。 | 方向可保留，但仍需验证链接 URL、未闭合 Markdown、字段缺失、未知 block 和空数据不丢内容、不补假图。 |
| Android / ResultBlock / Markdown 只读复核 | 子代理指出 `ResultBlockRenderer` 未知 block 原先只显示类型、空 table 可能只剩空卡、SSE 坏帧在 `AgentSseClient` 解析失败时会返回 null 被吞；Markdown 表格简单 split 对转义 `|` 和代码区间有风险。 | 已补 Unknown / parse failed 原始摘要、空结构化结果提示、SSE parse error 事件；Markdown 表格 scanner 和代码块空白保真已补单测，仍需真机渲染截图。 |
| Android / Agent 前端审查 | `AgentWorkbenchScreen` 视觉上已收敛为入口页；后端 workbench 当前返回空 KPI / 风险 / today summary，Android quick questions 已加报表型文案过滤。 | AI 初始屏干净度已有代码和测试门禁；仍需接口抓取、真机首屏 UI tree 和截图证明。 |
| Workbench 并行只读审查 | 子代理复核 `/v2/agent/workbench` schema 仍包含 `kpi_cards`、`risk_alerts`、`today_summary` 兼容字段，但当前实际返回为空 / null，Android 首屏不消费这些字段；唯一风险是 greeting / quick questions 文案污染。 | 已据此增加后端 workbench 干净合同测试和 Android quick question 过滤；后续可考虑收窄 DTO schema 或标注 deprecated。 |
| AI 首页设备证据脚本审查 2026-06-09 | 新增 `tools/capture_ai_home_device_evidence.py`，独立采集 `screencap`、`uiautomator`、window state、logcat 和首页清洁度 JSON；判定必须命中 `AI 助手` + Hero 锚点，且自动拦截锁屏、非 app、弱锚点和默认报表内容。脚本已修正锁屏下 `mFocusedApp` / `ResumedActivity` 仍可能指向 app 的误判风险，`docs/acceptance-evidence/ai-agent/20260609-054441-device-ai-home/` 试跑在锁屏设备上正确输出 `device_locked=true`、`package_seen=false` 和 `blocked-by-locked-device`。 | 这改善了后续验收工具链，防止黑屏 / 锁屏 / 后台 resumed app 被误判为通过；但当前仍未获得解锁后的 AI 首页真机通过截图，P0 首页干净入口运行验收仍未完成。 |
| 设计 / UI 审查 | 全局 UI 一比一重构仍未完成；首页、报表、单据等页面还有设计稿字段与真实后端合同不匹配的地方，不能用假同比、假趋势、假审核数补齐。 | AI 文档之外的全界面重构仍是 active goal，后续必须逐页按 Stitch / 真实字段边界整改和验收。 |
| 底部 tap 栏 / Bilipay 参考审查 | 参考项目使用连续 pager 进度、slot 等宽 indicator、玻璃模糊 / 折射和弹簧参数；当前项目已做部分玻璃栏和安全避让，但还不是完整 Bilipay 式连续 pager 体验。 | tap 栏动画仍需后续专项实现和真机性能验证；不得因为当前可用就标记一比一完成。 |

多 agent 结论当前状态：

- 已吸收：后端真实查询审查、Android / Agent 前端审查、设计稿差距审查、底部 tap 栏参考审查。
- 未完成：逐页一比一 UI 重构验收、真实 SSE 抓包、真实账号三问端到端证据、生产 profile demo 隔离证据、性能 / 可观测性证据包。
- 工具限制：本轮尝试新建额外 agent 时遇到线程 agent 数量上限；已优先复用和吸收现有 completed agent 输出，后续资源释放后再补 Verification Agent。
