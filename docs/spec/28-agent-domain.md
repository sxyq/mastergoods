# 28 AI 助手域

> 状态：领域对象审查索引，配合 `docs/spec/43-ai-assistant-requirements.md` 使用
> 更新日期：2026-06-09
> 目的：让后续 AI 助手审查不只看旧的任务 / 通知 / 会话表，而能覆盖真实 run、SSE、工具、result block、审计和 Android 本地证据。

## 1. 阅读规则

`43-ai-assistant-requirements.md` 是 AI 助手体验和验收基线；本文只负责说明当前代码里的领域对象、接口和证据归属。若本文与 `43` 冲突，以 `43`、当前源码和最新验收证据为准。

本文不能作为通过证明。每个对象都必须通过真实 owner-scoped 接口、SSE、审计、Android UI tree / 截图和测试证据才能升级为通过。

## 2. 核心领域对象

| 对象 / 合同 | 当前状态 | 代码 / 接口归属 | 审查重点 |
|---|---|---|---|
| `agent_conversations` | 已接入，仍需端到端联调证据 | `AgentConversationEntity/Repository`、`V2AgentConversationService`、`/v2/agent/conversations/*` | owner 隔离、标题 / 状态更新、删除时消息与草稿级联、closed / archived 会话拒绝继续写入 |
| `agent_messages` | 已接入，仍需真实问答链路证据 | `AgentMessageEntity/Repository`、`/v2/agent/conversations/{conversationId}/messages` | user / assistant 消息保存、结构化 result block JSON 持久化、会话摘要刷新、owner 校验 |
| `agent_drafts` | 已接入，P0 已支持真实落库草稿，P1 继续补状态机 | `AgentDraftEntity/Repository`、`/v2/agent/drafts/*`、`V2AgentAiService keyword fallback/native_tool_use create-only planner` | `archived` 不代表执行成功；当前无 LLM 时已可将自然语言兜底提取为 `create_*` 草稿参数并落库，Anthropic Messages / `chat_completions` / `responses` 已可通过 `plan_source=native_tool_use` 直接选中 `CREATE_ONLY` 工具，且流式链路会发送真实 `draft_created` 事件；当前剩余 blocker 已收敛为真实 HTTP/SSE 抓包、复杂单据/多工具规划覆盖，以及 P1 状态机；P1 前后续仍需补 confirm / reject / executing / executed / failed 状态机 |
| `agent_tasks` | 已接入，仍需真实来源证据 | `AgentTaskEntity/Repository`、`/v2/agent/tasks` | 任务不得来自生产 demo seed；接口失败不能伪装成“暂无任务”；任务结果需关联真实 run 或真实后台任务 |
| `agent_notifications` | 已接入，仍需真实来源证据 | `AgentNotificationEntity/Repository`、`/v2/agent/notifications`、mark read 接口 | 通知不得来自生产 demo seed；任务 / 通知分 tab 失败要诚实展示；已读状态 owner-aware |
| `agent_run_audits` | 已接入 run 摘要审计，仍需真实 DB / SSE 对账 | `AgentRunAuditEntity/Repository`、`GET /v2/agent/runs/{runId}/audit` | run 状态、mode、llmStatus、planSource、toolCount、eventCount、auditId、traceId、错误码和耗时必须能对上 HTTP / SSE |
| `agent_run_audit_events` | 已接入事件级审计，仍需真实对账 | `AgentRunAuditEventEntity/Repository`、`V2AgentAiService.sendEvent` | `event_id`、`seq`、`event_type`、payload JSON 不得缺失；不得记录密钥、token、完整内部 prompt 或跨 owner 数据 |
| `ActiveAgentRun` | 内存态 active run，P0 取消路径已存在 | `V2AgentAiService.ActiveAgentRun`、`POST /v2/agent/runs/{runId}/cancel` | active run 必须 owner-aware；取消成功发 `run_cancelled` 并阻止后续完成；未知 / 跨 owner run 不能伪造取消成功 |
| `AgentWorkbenchResponse` | 已改为干净入口合同 | `/v2/agent/workbench`、Android `AgentWorkbenchScreen` / model | 初始屏不得默认展示 KPI、风险、今日摘要、图表或排行；远端失败只能显示入口和同步失败 |
| Android `agent_audit_records` | 本地审计缓存已存在 | `AgentAuditEntity/Dao/Repository` | 仅作本地辅助证据；不能替代后端 run audit；保存失败不得影响主流程，但验收要记录 |

## 3. 运行域对象补充

### 3.1 Run

Run 是一次用户问题到回答完成 / 失败 / 取消的最小审计单位。

必须贯穿：

- HTTP 响应：`run_id`、`audit_id`、`trace_id`、`observability`。
- SSE：`run_started`、安全检查、plan、tool、answer、result block、draft、cancel、completed / failed。
- 后端审计：`agent_run_audits` 摘要和 `agent_run_audit_events` 事件 payload。
- Android UI：`RunTrace`、工具短提示、降级 / 中断标签、result block 时间线。

P0 状态至少覆盖 `running`、`completed`、`blocked`、`failed`、`cancelled`。如果环境无法证明其中某个状态，不得把该状态标记为验收通过。

### 3.2 Tool Call

Tool call 是真实查询的最小证据单位。当前实现里没有独立 `agent_tool_calls` 表，工具调用主要通过 SSE payload、`AgentToolCallDto`、`ToolCallRecord`、`ToolAuditRecord`、`evidence_refs` 和 run audit event 承载。

每个工具必须可审查：

- `tool_call_id`
- `tool_name`
- `status`
- `input_summary`
- `query_window`
- `returned_count`
- `total_count`
- `limit`
- `is_truncated`
- `duration_ms`
- `error_code` / `safe_message`
- `evidence`
- `next_cursor`

后续若新增 `agent_tool_calls` 表，必须与 `agent_run_audit_events` 和 `AgentToolCallDto` 保持同一 `tool_call_id`。

补充边界：

- `CREATE_ONLY` 工具同样属于真实 tool call，但结果是“已落库草稿”而不是直接写业务单据。
- 当前 `keyword_fallback` 已支持对 `create_customer`、`create_supplier`、`create_product`、`create_sale_order`、`create_purchase_order`、`create_pay_order`、`create_finance_record` 生成参数并执行落草稿。
- 当前 `receivable_payable_lookup` 已作为真实只读工具接入注册表，返回应收总额、应付总额、净敞口和重点往来方，不再只是白名单占位名。
- 当前 `customer_profile_lookup` 已作为真实只读工具接入注册表，可按客户关键词汇总订单、收款、退货、欠款、付款习惯和催收建议，并在 `evidence_refs` 暴露 `customer_name`、`total_sales_amount`、`balance`、`payment_habit` 等字段级依据。
- 当前 `plan_source=native_tool_use` 已支持 Anthropic Messages、`chat_completions` 和 `responses` 路径直接选中注册工具；剩余缺口主要在真实 SSE 证据、复杂多工具规划和更完整的端到端验收。
- 当前 `V2AgentAiService` 已把最近 10 条 `agent_messages` 和 `conversation.latestSummary` 真正接入到工具规划链路：它们不仅进入最终回答 prompt，也进入 `planToolsWithLlm()`、`planToolsWithNativeFunctionCalling()`、`inferToolPlan()` 以及若干 `keyword_fallback` 参数提取函数。无模型场景下，追问“刚才那个客户/那个商品/那家供应商”时，服务层现在可以优先从最近消息中回填客户、商品、供应商、账户实体；这属于 `C1 多轮对话上下文` 的第一段真实接入，不等于完整长期记忆或复杂指代消解。
- 当前 `V2AgentAiService` 已具备第一版真实 Agent 循环：当 `sale_order_lookup`、`purchase_order_lookup`、`pay_order_lookup`、`finance_record_lookup` 在“有筛选条件但结果为空”时返回 insufficient，服务层会先生成 `plan_source=deterministic_recovery` 的放宽筛选补查；若未触发这条确定性补查且 LLM 可用，仍可继续进入 `plan_source=react_iterated` 的后续补充规划。当前这解决的是“首轮空结果自动补查”的最小闭环，不等于 `C1 多轮上下文` 已完成。
- 当前 `inventory_panorama_lookup` 已作为真实只读工具接入注册表，可按商品关键词输出当前库存、安全库存、近 30 天销量、周转天数和建议补货量，并在 `evidence_refs` 暴露 `product_name`、`current_stock`、`recent_sales_quantity`、`turnover_days`、`suggested_restock` 等字段级依据。
- 当前 `purchase_tracking_lookup` 已作为真实只读工具接入注册表，可按采购单或供应商关键词汇总采购总额、已到货金额、待付款金额、关联入库单、关联退货单与跟踪建议，并在 `evidence_refs` 暴露 `order_no`、`supplier_name`、`received_amount`、`outstanding_amount`、`receipt_count`、`return_count` 等字段级依据。
- 当前 `account_health_lookup` 已作为真实只读工具接入注册表，可按账户关键词汇总账户总余额、近窗口收支比、活跃账户、低余额账户、近期账户转账与资金变动，并在 `evidence_refs` 暴露 `total_balance`、`income_expense_ratio`、`low_balance_count`、`transfer_count`、`default_account_name` 等字段级依据。
- 原计划第四章这一组智能经营工具（I2~I6）现在都已经从“关键词白名单挂名”收口为真实注册工具；剩余缺口主要转到复杂多工具规划、真实 SSE 证据和端到端验收。
- 流式链路在 `CREATE_ONLY` 工具成功后会发送真实 `draft_created` 事件；这不等价于业务单据已执行成功。

### 3.3 Result Block

Result block 是 AI 可视化结果的最小渲染单位。当前实现里没有独立 `agent_result_blocks` 表，主要通过：

- 后端 `V2AgentDtos.ResultBlockDto`
- SSE `result_block`
- 非流式 `result_blocks`
- Android `ResultBlockDto`
- 历史消息 `structuredDataJson`

P0 已知类型：

- `text`
- `kpi_grid`
- `table`
- `rank_list`
- `line_chart` / `area_chart` / `trend_chart`
- `bar_chart` / `column_chart` / `horizontal_bar_chart`
- `donut_chart` / `pie_chart`
- `risk_card`
- `evidence_card`
- `draft_card`

审查要求：

- Android 只能渲染后端返回的 block，不得补示例序列、默认排行或假图表。
- 已知类型解析失败要显示错误卡；未知类型要显示标题、类型和原始摘要；不能静默消失。
- `evidence_card` 必须能把关键字段、值、来源、`tool_call_id`、`query_window`、截断状态展示给用户或审查者。

### 3.4 SSE Event

SSE event 是 ChatGPT-like 体验的时序证据，不只是网络格式。

必须区分：

- `answer_delta(delta_source=model_stream)`：只允许来自供应商真实 streaming。
- `answer_delta(delta_source=server_notice)`：只能在真实模型 delta 之后补充服务端查询边界 / 部分失败说明。
- `answer_completed`：规则摘要、禁用模型、非流式 provider 或最终收束。
- `result_block`：真实结构化结果，用户可见时间线不得早于首段回答正文。
- `tool_started/tool_completed/tool_failed`：真实工具状态和短提示来源。

规则摘要不得拆分成 `answer_delta`；Android 不得本地 timer / delay / substring 制造吐字。

## 4. 与 `43` 需求基线映射

| 领域对象 / 合同 | 对应 `43` 需求 | 当前证据强度 | 仍缺证据 |
|---|---|---|---|
| conversations / messages | AGT-P0-002、AGT-P0-011、AGT-P0-012 | 代码和部分接口证据 | 当前提交后的真实端到端对话、消息持久化和 UI 回放截图 |
| drafts | AGT-P0-008 | 代码边界和文档说明 | 草稿 dialog 真机证据、归档不写业务单据证据；P1 执行状态机 |
| tasks / notifications | AGT-P0-001、AGT-P0-009、AGT-P0-018 | 代码路径和局部失败诚实态 | 生产 profile 下无 demo 污染证据、断网 / 单接口失败截图 |
| run audit / event audit | AGT-P0-002、AGT-P0-012、AGT-P0-013 | 单测和接口证据包 | 同一 run 的 HTTP、SSE、DB audit、Android UI reconciliation |
| active run / cancel | AGT-P0-019 | 服务端单测和 Android 调用路径 | Android stop 点击、cancel HTTP、SSE `run_cancelled`、审计状态和 UI 反馈 |
| tool calls | AGT-P0-003、AGT-P0-004、AGT-P0-006、AGT-P0-011 | 单测和 interface evidence | 工具失败、超限截断、多工具连续切换和真机 RunTrace |
| result blocks / evidence cards | AGT-P0-010、AGT-P0-015、AGT-P0-017 | 模型 / 渲染单测和 interface evidence | 真机截图证明 answer、evidence、query window、tool source 同屏可审查 |
| SSE answer delta / completed | AGT-P0-005、AGT-P0-007、AGT-P0-015、AGT-P0-016 | 单测门禁和规则摘要 evidence | 真实 provider `model_stream` 抓包、stream interrupted / empty / failed 真机证据 |
| workbench clean entry | AGT-P0-014、AGT-P0-018 | 代码和旧设备包；新锁屏包是 blocked | 解锁设备首屏截图、UI tree、断开后端状态 |
| Android local audit | AGT-P0-012、AGT-P0-015 | 本地模型和 DAO 存在 | 与后端 audit 对账，证明本地审计不漏关键状态 |

## 5. 后续领域演进建议

这些建议不是 P0 必须一次完成，但后续实现时应避免和现有合同冲突。

- 新增 `agent_runs` 持久化表：用于跨进程恢复 active run、取消、超时和重试状态。
- 新增 `agent_tool_calls` 表：减少只靠 event payload 追工具调用的审计成本。
- 新增 `agent_result_blocks` 表或消息 block 子表：让历史结构化结果可分页、可单独审计、可重渲染。
- 扩展 `agent_drafts` 状态：`pending`、`confirmed`、`executing`、`executed`、`failed`、`archived`。
- 为 workbench 兼容字段标注 deprecated 或收窄默认响应，避免后续重新塞入报表型默认数据。
- 建立 `run_id` 级证据包命名规范：一个真实问题一个目录，包含 HTTP、SSE、audit、tool result、Android screenshot/UI tree、latency 和 conclusion。

## 6. 审查禁区

- 不得用 `AdminService.runAgentSmoke()`、demo seed 或 local-only admin 接口证明生产 AI agent 能力。
- 不得把 Android 本地 `agent_audit_records` 当作后端 run audit 的替代。
- 不得把规则摘要、关键词 fallback 或服务端 orchestrator 包装成 provider 原生 function calling；具体文案边界见 `43-ai-assistant-requirements.md` 第 9.7 节。
- 不得把 `archived` 草稿说成“已执行”。
- 不得把锁屏、AI 首页、无 result block、无工具锚点的设备截图说成 AI 对话体验通过。
