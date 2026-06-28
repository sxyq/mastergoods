# technical-analysis 文档变更记录

> 历史说明：本文件保留文档层的关键结构变更，不再承担“全部问题修复流水账”角色。  
> 新版范围与领域规范请以 [docs/spec](/Users/sunyiyang/Desktop/Project/master-goods/docs/spec) 为准。

## 2026-06-02

### AI 助手多轮上下文第一段收口

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V2AgentAiService` 上下文接入层 | 已验证 | 最近消息虽然已经进入 `buildFinalAnswer()` / `buildFinalAnswerForStream()` 的最终回答 prompt，但工具规划、关键词兜底和 create 草稿参数提取层仍基本按“单轮问题”工作，追问场景容易丢实体 | 让第四章 `C1 多轮对话上下文` 至少具备“最近消息 + latestSummary 进入规划层”的真实闭环，而不是只在回答润色阶段看上下文 | `chat()` / `runChatStream()` 现在会先加载最近 10 条消息，再把 `history + latestSummary` 透传给 `buildResponse()`、`planToolsWithLlm()`、`planToolsWithNativeFunctionCalling()`、`inferToolPlan()`，以及 `buildCustomerKeywordParams()`、`buildProductKeywordParams()`、`buildPurchaseKeywordParams()`、`buildAccountKeywordParams()` 和若干 `create_*` 兜底参数提取函数；服务层新增保守的 `findRecentEntityHint(...)`，可从最近消息/摘要中回填客户、供应商、商品和账户实体 | 这是 `C1` 的第一段，不等于完整长期记忆、复杂指代消解或跨多实体 disambiguation；当前窗口仍只有最近 10 条消息 + latestSummary |
| `V2AgentAiServiceTest` | 已验证 | 之前没有真实回归证明“刚才那个客户/商品”这类追问能在无模型路径下命中上一轮实体 | 为多轮上下文补最小但真实的追问护栏，并确认不会打坏现有 Agent 循环和审计合同 | 已新增 `keywordFallbackUsesRecentConversationContextForCustomerFollowUpQuestion()`，固定“帮我看下张三商贸的客户画像”后追问“刚才那个客户的欠款呢”时，无 LLM 也会为 `customer_receivable_lookup` 自动补出 `keyword=张三商贸`；`./gradlew test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 与 `./gradlew test --tests com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClientTest` 已通过 | 这轮还顺手拦截了 `keyword=的欠款呢` 这类代词残片，避免追问被错误提取成伪关键词 |

### AI 助手 Agent 循环第一版收口

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V2AgentAiService.buildResponse` | 已验证 | 代码里虽已有 `planNextIteration()` 迭代骨架，但工具层从不返回“结果不足”信号，实际运行仍接近单轮工具查询 | 让第四章 `C2 Agent 循环` 至少具备“首轮空结果后自动补查”的真实闭环，而且不破坏现有非流式合同、evidence 和 SSE 审计 | 已新增 `ToolResult.emptyInsufficient()`，并在 `sale_order_lookup`、`purchase_order_lookup`、`pay_order_lookup`、`finance_record_lookup` 命中“有筛选条件但结果为空”时返回 insufficient；`V2AgentAiService` 在首轮执行后先走 `deterministic_recovery` 放宽筛选，再在未触发确定性补查时保留最多 `MAX_AGENT_ITERATIONS` 轮的 LLM ReAct 扩展；同时对 `tool_results` 的展示层做“同一工具仅保留最后一次有效执行”的收口，避免 `tool_calls`、`evidence_refs`、最终摘要和查询边界被首轮空结果污染 | 这是第一版 Agent 循环，不等于完整长期记忆或复杂多工具 Agent；当前确定性补查只覆盖少数查询工具，`C1 多轮上下文` 仍未完成 |
| `V2AgentAiServiceTest` | 已验证 | 原先没有真实测试证明空结果后会自动补查，也没有覆盖补查后展示层口径是否被污染 | 为 Agent 循环补真实回归护栏，并固定非流式 / 流式合同不会被这轮改动打坏 | 已新增 `chatRetriesWithDeterministicRecoveryWhenFirstFilteredToolResultIsInsufficient()`，固定“查一下张三上个月的销售单”首轮空后，会自动放宽为关键词补查并把真实订单号带入最终回答；同时顺序修复并重新通过 `nonStreamingChatIncludesAuditableAgentRunContract()`、`nonStreamingChatExplainsLimitedQueryBoundaryAndFieldLevelEvidence()`、`streamToolCompletedIncludesAuditMetadata()`、`receivableAndPayableUseRepositoryAggregatesForTotalsInsteadOfTopPageSample()` 等回归测试；`./gradlew test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 已再次全绿 | 这轮还顺手修正了关键词兜底把“应收情况”“客户应收和供应商应付情况”误当实体名的问题，避免证据卡和 query boundary 统计继续失真 |

### Android Agent 第三章 P1 收口

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `AgentMarkdownText.kt` 代码语法高亮 | 已验证 | Android Agent 代码块只有深色背景和等宽字体，没有 token 着色 | 至少补轻量语法高亮，让代码回复可读性不再明显落后于规划口径 | 已为代码块补关键词 / 字符串 / 注释 / 数字着色，覆盖 Kotlin/Java、JS/TS、JSON、SQL、Shell；`./gradlew :feature:agent:compileDebugKotlin` 与 `./gradlew :feature:agent:testDebugUnitTest --tests "com.zhihuiji.feature.agent.AgentMarkdownTextParserTest"` 已通过 | 当前是轻量 lexer 方案，不是 Prism4j 级完整能力；仍需真机截图验证长代码块视觉和滚动体验 |
| Android Agent 第三章 P1 对标状态 | 已同步 | 原详细计划里 4 个 P1 项容易继续被视为“整体未做” | 让文档明确区分：哪些已落地、哪些已验证、哪些只差设备证据 | 已在 `docs/technical-analysis/android/feature/agent/README.md` 回写 P1-1~P1-4 真实状态：编辑用户消息 / SSE 重连提示 / 工作台增强链路已存在，语法高亮已补齐并通过本地编译 + 单测 | 后续继续补真机交互、断网重连和工作台联调截图证据 |

### AI 助手一句话创建兜底补齐

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V2AgentAiService.inferToolPlan` | 待验证 | LLM 不可用时只能做只读关键词兜底，`一句话创建` 无法真实落草稿 | 至少在无 LLM / 无 native tool_use 时，把常见创建意图兜底提取成真实 `create_*` 工具参数 | 已补 `create_customer`、`create_supplier`、`create_product`、`create_sale_order`、`create_purchase_order`、`create_pay_order`、`create_finance_record` 的轻量参数提取，并复用现有 CREATE_ONLY 工具落库真实草稿 | 属于过渡实现，仍不是原生 function calling |
| `V2AgentAiServiceTest` | 待验证 | 原先只有只读关键词兜底覆盖 | 为自然语言创建链路补真实回归护栏 | 已新增客户 / 商品 / 资金流水参数提取测试、“帮张三开销售单”无 LLM 直落草稿测试，以及 `native_tool_use` 流式 `draft_created` 事件测试；`./gradlew test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 已通过 | 后续继续补复杂单据、多工具规划和真实抓包证据 |
| `web/src/pages/agent/AgentPage.vue` | 待验证 | Web 主对话区流式时只有“正在生成...”，工具实时进度需要展开轨迹才可见 | 让主对话区直接暴露当前工具执行状态，降低黑盒感 | 已新增 assistant 气泡顶部的轻量工具进度条，跟随 `tool_started/progress/completed/failed` 更新工具名、状态与摘要；`cd web && npm run build` 已通过 | 仍未完成移动端布局和更完整的工具阶段化设计 |
| `web/src/pages/agent/AgentPage.vue` 草稿编辑器 | 待验证 | 右侧草稿编辑只能直接改 `draftType + contentJson`，大多数真实草稿需要人工改原始 JSON | 为已接入 CREATE_ONLY 草稿补结构化编辑，同时保留未知类型的原始 JSON 兜底 | 已为客户、供应商、商品、销售单、采购单、付款单、资金流水草稿补结构化表单和 JSON 预览；草稿摘要解析也补齐 snake_case / camelCase 键兼容；`cd web && npm run build` 已通过 | 仍未接入选项型下拉、复杂业务校验和移动端布局 |
| `web/src/pages/agent/AgentPage.vue` / `web/src/style.css` 移动端适配 | 待验证 | 之前只有移动端入口按钮显隐，三栏页面缺少真正的抽屉、遮罩和关闭逻辑 | 把 Agent 页补成手机可用的单栏 + 抽屉模式 | 已补会话列表/工作台抽屉切换、互斥开关、遮罩关闭、body 滚动锁，以及 `<768px` 下左右侧抽屉布局；`cd web && npm run build` 已通过 | 仍需截图证据、触屏交互验证和审计面板的移动端细节收口 |
| `receivable_payable_lookup` | 已验证 | 原详细计划第四章 I6“应收应付对账”已在关键词兜底和白名单中挂名，但此前没有真实注册工具实现 | 让 Agent 能基于真实 owner-scoped 数据输出应收 / 应付 / 净敞口 / 重点往来方对账结果 | 已新增 `ReceivablePayableLookupTool`，复用 `CustomerRepository.sumPositiveBalance/countByOwnerUserIdAndBalanceGreaterThan` 与 `SupplierRepository.sumPositiveBalance/countByOwnerUserIdAndBalanceGreaterThan` 生成 KPI、排行、风险卡和 `toolFacts(net_exposure)`；同时在 `V2AgentAiService.toEvidenceRefs()` 补 `receivable_payable_lookup` 的 evidence 映射，`V2AgentAiServiceTest.receivablePayableLookupProvidesCombinedReconciliationSummary()` 与 `./gradlew test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 已通过 | 这是原计划 I6 的真实落地；第四章这组智能经营工具现已全部完成真实接入 |
| `customer_profile_lookup` | 已验证 | 原详细计划第四章 I4“客户画像与催收建议”已在关键词兜底和工具白名单中挂名，但此前没有真实注册工具实现，且组合触发 `customer_receivable_lookup` 时没有稳定透传客户关键词 | 让 Agent 能基于真实 owner-scoped 客户、销售单、收款、退货数据输出客户画像、付款习惯和催收建议 | 已新增 `CustomerProfileLookupTool`，复用 `CustomerRepository.search`、`SaleOrderRepository.search`、`PaymentRepository.findByOwnerUserIdAndOrderIdOrderByCreatedAtAsc`、`SalesReturnRepository.findByOwnerUserIdAndOriginalOrderIdOrderByCreatedAtDesc` 生成 KPI、画像表、风险卡和 `toolFacts(customer_name/total_sales_amount/balance/payment_habit/collection_suggestion)`；同时在 `V2AgentAiService` 为 `customer_profile_lookup` / `customer_receivable_lookup` 共用客户关键词提取，并在 `synthesizeAnswer()` / `toEvidenceRefs()` 补画像字段级摘要与 evidence 映射；`V2AgentAiServiceTest.customerProfileLookupProvidesCustomerInsightAndCollectionSuggestion()` 与 `inferToolPlanBuildsCustomerKeywordParamsForProfileAndReceivableTools()` 已通过 | 这是原计划 I4 的真实落地；第四章这组智能经营工具现已全部完成真实接入，整组回归测试仍有既有失败项需单独收口 |
| `inventory_panorama_lookup` | 已验证 | 原详细计划第四章 I3“商品库存全景”已在关键词兜底和工具白名单中挂名，但此前没有真实注册工具实现 | 让 Agent 能基于真实 owner-scoped 商品、月度库存统计和近 30 天销量输出单商品库存健康度 | 已新增 `InventoryPanoramaLookupTool`，复用 `ProductRepository`、`InventoryMonthlyStatsRepository.findByOwnerUserIdAndProductIdAndYearAndMonth` 与 `SaleOrderItemRepository.recentStockOutRows` 生成当前库存、安全库存、近 30 天销量、周转天数、建议补货量、健康度表格与风险卡；同时在 `V2AgentAiService` 补 `inventory_panorama_lookup` 的商品关键词提取、规则摘要和 evidence 映射；`V2AgentAiServiceTest.inventoryPanoramaLookupProvidesInventoryHealthInsight()` 与 `inferToolPlanBuildsProductKeywordParamsForInventoryPanoramaTool()` 已通过 | 这是原计划 I3 的真实落地；第四章这组智能经营工具现已全部完成真实接入，整组回归测试仍有既有失败项需单独收口 |
| `purchase_tracking_lookup` | 已验证 | 原详细计划第四章 I2“采购到货跟踪”已在关键词兜底和工具白名单中挂名，但此前没有真实注册工具实现 | 让 Agent 能基于真实 owner-scoped 采购单、采购入库单、采购退货单输出采购链路跟踪结果 | 已新增 `PurchaseTrackingLookupTool`，复用 `PurchaseOrderRepository.search`、`PurchaseReceiptRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc`、`PurchaseReturnRepository.findByOwnerUserIdAndPurchaseOrderIdOrderByCreatedAtDesc` 生成采购总额、已到货金额、待付款金额、关联入库单 / 退货单表格、风险卡和 `toolFacts(order_no/supplier_name/received_amount/outstanding_amount/receipt_count/return_count)`；同时在 `V2AgentAiService` 补 `purchase_tracking_lookup` 的采购关键词提取、规则摘要和 evidence 映射；`V2AgentAiServiceTest.purchaseTrackingLookupProvidesReceiptAndReturnChainInsight()` 与 `inferToolPlanBuildsPurchaseKeywordParamsForPurchaseTrackingTool()` 已通过 | 这是原计划 I2 的真实落地；第四章这组智能经营工具现已全部完成真实接入，整组回归测试仍有既有失败项需单独收口 |
| `account_health_lookup` | 已验证 | 原详细计划第四章 I5“资金账户健康度”已在关键词兜底和工具白名单中挂名，但此前没有真实注册工具实现 | 让 Agent 能基于真实 owner-scoped 账户、账户转账、资金流水和现金变动输出账户健康概览 | 已新增 `AccountHealthLookupTool`，复用 `AccountRepository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc`、`AccountTransferRepository.findAllByOwnerUserIdOrderByCreatedAtDesc`、`CashChangeRecordRepository.findAllByOwnerUserIdOrderByCreatedAtDesc` 与 `FinanceRecordRepository.cashflowSummary` 生成账户总余额、活跃账户数、低余额账户数、近窗口收支比、近期账户转账 / 资金变动明细、风险卡和 `toolFacts(total_balance/income_expense_ratio/low_balance_count/transfer_count/default_account_name)`；同时在 `V2AgentAiService` 补 `account_health_lookup` 的账户关键词 / 窗口提取、规则摘要和 evidence 映射；`V2AgentAiServiceTest.accountHealthLookupProvidesAccountBalanceFlowAndRiskInsight()` 与 `inferToolPlanBuildsAccountKeywordParamsForAccountHealthTool()` 已通过 | 这是原计划 I5 的真实落地；第四章这组智能经营工具现已全部完成真实接入，后续重点转向复杂多工具组合、真实 SSE 证据和端到端验收 |

### AI 助手原生 Function Calling 第一段收口

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `LongCatAnthropicClient.createMessageWithTools` | 已验证 | 只有 Anthropic Messages `tool_use` 路径，`chat_completions` 无法走原生 tools，`responses` 也没有原生工具调用能力 | 至少补齐 `chat_completions` 和 `responses` 原生 tools 请求/响应解析，让 provider 可直接返回工具选择结果 | 已新增 `chat_completions` `tools + tool_choice=auto` 请求体、`tool_calls`/`function.arguments` 解析，以及 `responses` `tools` 请求与 `output[type=function_call]` 解析；`./gradlew test --tests com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClientTest` 已通过 | 协议接入已打通，剩余是更高层的端到端证据和复杂规划覆盖 |
| `V2AgentAiService.planToolsWithNativeFunctionCalling` | 已验证 | 原生 function calling 只停留在客户端注释层，服务层没有真实 `native_tool_use` 回归护栏 | 让服务层真正消费 provider 返回的工具选择，并落到注册工具执行链 | 已验证 `plan_source=native_tool_use` 会驱动真实 `CreateCustomerTool` 生成草稿，且流式链路会发送 `draft_created` SSE 事件并写审计；`./gradlew test --tests com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest` 已通过 | 当前验证已覆盖“原生规划 + 后端执行 + 流式草稿事件”，剩余是复杂多工具场景、更多 create_* 类型和真实抓包证据 |
| Agent 文档 | 已同步 | 文档仍把 provider 原生 function calling 写成“未接入”或未来项，容易把当前完成度写偏 | 让规格明确区分：哪些路径已支持 `native_tool_use`，哪些仍是 blocker | 已同步 `docs/spec/28-agent-domain.md` 与 `docs/spec/43-ai-assistant-requirements.md`，标明 Anthropic Messages / `chat_completions` / `responses` 已支持，剩余 blocker 已改为端到端证据和复杂规划覆盖 | 后续继续跟进真实 SSE、复杂单据和多工具验收 |

### 后端 owner 底座第一批实现

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V7__owner_scope_foundation.sql` | 新版已做 | 旧版无统一 owner 回填机制 | 核心业务表统一补 owner 并回填历史数据 | 已新增迁移脚本与系统默认归属账号 | `SYSTEM-LEGACY-OWNER` 不对外暴露 |
| `CurrentOwnerService` | 新版已做 | 旧版 controller/service 无统一 owner 获取方式 | 统一从认证上下文读取当前 owner | 已新增基础服务 | 后续 repository/service 全面接入 |
| server entity 文档同步 | 新版已做 | 文档仍描述“owner 未落地” | 实时反映首批实体字段变化 | 已同步 `02-domain-model-overview`、`10-auth-and-tenant`、`server/entity` | 继续随代码原子更新 |
| owner-aware repository/service/controller | 新版已做 | 旧版单据链路主要按全局数据工作 | 核心单据、主数据、同步、报表、AI 改为默认 owner 过滤 | 已完成核心 repository/service 改造，并补 `/v2` 首批单据控制器 | 仍需补完整 JDK21 编译验证 |
| `/v2` 单据域首批 | 新版已做 | 旧版无 `/v2` 单据接口 | 先落地销售/采购/付款三条新契约 | 已新增 `controller/v2`、`dto/v2`、`service/v2` 三层 | 其他领域后续继续扩展 |
| Android 单据规划同步 | 新版已做 | 安卓文档仍只面向 `/v1` 首版 | 明确后端 `/v2` 已落地、安卓尚未切换 | 已同步 `31-android-impact`、`data/order`、`core/model`、`feature/sales|purchases|payments` | UI 代码仍未变更 |
| `FinanceRecordRepository.search` 字段修正 | 新版已做 | 首版搜索条件存在 `remark` 命名偏差 | 关键字搜索与实体字段严格一致 | 已改为 `recordNo/category/notes` 且保持 owner 过滤 | 财务域文档已同步 |
| `HttpClientConfig` | 新版已做 | 首版未显式提供 `RestClient.Builder` Bean | 稳定支撑 AI 基础设施与后续外部 HTTP 客户端 | 已新增配置类并完成装配 | 配置层文档已同步 |
| `AgentTaskConfig` 执行器契约收口 | 新版已做 | 首版执行器类型与 service 注入期望存在偏差 | 统一返回 `ExecutorService` 并声明关闭策略 | 已完成 Bean 契约修正 | 避免 agent 任务服务启动失败 |
| 本地 Java 21 后端全量验证 | 新版已做 | 之前仅有规划口径 | 完成编译与测试双验证 | 已通过 `compileJava`、`testClasses`、`test` | `32-rollout-and-compatibility` 已同步 |

### 商品域与伙伴域第二阶段第一批实体扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V8__product_and_partner_expansion.sql` | 新版已做 | 旧版这些能力在本地库中已存在更厚模型 | 第二阶段先补商品分类/单位、伙伴分组/联系人 | 已新增迁移脚本 | `/v2` 接口与服务下一批继续补 |
| `ProductCategoryEntity` / `ProductUnitEntity` | 新版已做 | 旧版有分类与单位体系 | 第二阶段先落商品主数据扩域表 | 已新增实体 | 多价格后续补 |
| `PartnerGroupEntity` / `PartnerContactEntity` | 新版已做 | 旧版有分组与联系人能力 | 第二阶段先落伙伴主数据扩域表 | 已新增实体 | tags 和价格策略后续补 |
| `ProductEntity` 扩域位 | 新版已做 | 旧版商品域比当前更厚 | 为 `/v2/products` 预留分类/单位引用 | 已新增 `categoryId/unitId` | `/v1` 通过 `@JsonIgnore` 保持冻结 |
| `CustomerEntity` / `SupplierEntity` 扩域位 | 新版已做 | 旧版客户/供应商画像更厚 | 为 `/v2/customers`、`/v2/suppliers` 预留分组与主联系人摘要 | 已新增 `groupId/contactName/contactPhone` | `/v1` 继续不暴露扩域字段 |
| `/v2` 商品域接口与服务 | 新版已做 | 旧版无 `/v2` 商品契约 | 第二阶段先落商品、分类、单位首批接口 | 已新增 `V2Product*` controller/service/dto | `/v1/products` 保持冻结兼容 |
| `/v2` 伙伴域接口与服务 | 新版已做 | 旧版无 `/v2` 伙伴契约 | 第二阶段先落客户、供应商、分组、联系人首批接口 | 已新增 `V2Customer*`、`V2Supplier*`、`V2Partner*` controller/service/dto | `/v1/customers|suppliers` 保持冻结兼容 |
| 第二阶段测试回归补齐 | 新版已做 | 新增扩域代码初次落地后仍需补迁移、service、controller、兼容回归验证 | 让商品/伙伴 `/v2` 首批能力与 `/v1` 冻结兼容同时具备可回归证据 | 已新增 `V8ProductAndPartnerExpansionSqlTest`、`V2ProductCategoryServiceTest`、`V2PartnerContactServiceTest`、`V2ProductControllerTest`、`V2PartnerControllerTest`、`V1CatalogCompatibilityControllerTest`，并修正联系人摘要测试桩顺序 | 当前以本地 JDK 21 全量测试通过为验收 |
| 安卓商品/伙伴规划同步 | 新版已做 | 安卓文档原先只面向 `/v1` 基础档案 | 明确后端商品/伙伴 `/v2` 已具备首批能力 | 已同步 `31-android-impact`、`data/product|customer|supplier`、`feature/products|customers|suppliers`、`core/model` | 本阶段不改安卓代码 |

### 安卓剩余基础模块文档深化

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/technical-analysis/android/core/common/README.md` | 新版已做 | 仍偏工具类清单 | 改成金额/状态/错误三类跨域语义文档 | 已补 `BigDecimal/Double` 并存、状态集扩展、废弃兼容逻辑说明 | 更利于后续模型精度治理 |
| `docs/technical-analysis/android/core/designsystem/README.md` | 新版已做 | 偏现有 UI 组件说明 | 改成设计系统分层、领域组件规划与统一 UI 基线文档 | 已补 token 层、容器层、领域组件方向、废弃兼容组件说明，并明确设计稿真源与 `core/designsystem` 实现真源关系 | 不追逐像素细节，但必须约束新增业务保持同一视觉语言 |
| `docs/technical-analysis/android/backdrop/README.md` | 新版已做 | 偏效果说明 | 改成底层渲染模块定位文档 | 已补构建信息、渲染职责、边界要求 | 明确不承载业务语义 |
| 基于 git 日志的漏文档补记 | 新版已做 | 部分代码已落地但文档只覆盖主线目标，没有补到辅助类和安全收口细节 | 按真实改动回补遗漏文档，避免技术分析与当前代码脱节 | 已补 `SessionAccessService`、`PaginationUtils`、`PartnerTypes`、`SyncCursorId`、`V7/V8` 迁移、Android 本地订单图与安全收口等文档说明 | 作为本轮“代码先于文档”的回补收口 |

### 商品域第三阶段第一批结构扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V9__product_price_levels_and_supplier_relations.sql` | 新版已做 | 旧版商品域在多价格与供应关系上更厚 | 第三阶段先补价格层级、商品-供应商关系与商品多价格值快照列 | 已新增迁移脚本 | 后续 `/v2` 服务与接口继续补 |
| `ProductPriceLevelEntity` | 新版已做 | 旧版有多价格体系 | 建立 owner 私有价格层级定义 | 已新增实体 | `code/name` 走 owner 内唯一 |
| `ProductSupplierRelationEntity` | 新版已做 | 旧版有商品-供应商关系 | 建立 owner 私有商品-供应商关系与采购偏好模型 | 已新增实体 | 包含默认供应商与采购优先级 |
| `ProductEntity.priceLevelValuesJson` | 新版已做 | 旧版多价格值维度明显更厚 | 让商品主档能挂接多价格值结构，同时不污染 `/v1` | 已新增 JSON 快照列并保持 `@JsonIgnore` | `/v2/products` 会消费该字段 |
| 商品价格/供应关系仓储 | 新版已做 | 旧版商品目录厚度更高 | 为第三阶段 `/v2/products` 扩域读写打底 | 已新增 `ProductPriceLevelRepository`、`ProductSupplierRelationRepository` | 服务与控制器下一批补上 |

### 商品域第三阶段第二批 `/v2` 契约落地

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `V2ProductPriceLevelService` | 新版已做 | 旧版没有当前 `/v2` 服务层 | 为 owner 私有价格层级提供 CRUD、唯一性校验、引用校验 | 已新增 service | 已补单测 |
| `V2ProductSupplierRelationService` | 新版已做 | 旧版没有当前 `/v2` 服务层 | 为商品-供应商关系提供 CRUD、默认供应商约束与 owner 校验 | 已新增 service | 已补单测 |
| `V2ProductPriceLevelController` | 新版已做 | 旧版无该接口 | 暴露 `/v2/product-price-levels/*` | 已新增 controller | snake_case 契约已锁定 |
| `V2ProductSupplierRelationController` | 新版已做 | 旧版无该接口 | 暴露 `/v2/product-supplier-relations/*` | 已新增 controller | 按 `product_id` 列表查询 |
| `V2ProductService` 扩域读写 | 新版已做 | 旧版商品读模型较薄 | 让 `/v2/products` 返回多价格、默认供应商、供应关系列表，并支持回写 | 已升级 service 与 `V2ProductDtos` | `/v1/products` 继续冻结 |
| 第三阶段测试补齐 | 新版已做 | 新扩域代码刚落地时仍缺第三阶段回归证据 | 为迁移、service、controller、`/v1` 兼容提供测试护栏 | 已新增 `V9ProductPriceLevelAndSupplierRelationSqlTest`、`V2ProductPriceLevelServiceTest`、`V2ProductSupplierRelationServiceTest`，并升级 `V2ProductControllerTest` 与 `V1CatalogCompatibilityControllerTest` | 当前已通过本地 JDK 21 全量测试 |
| 商品域与安卓规划文档同步 | 新版已做 | 文档仍停留在第二阶段口径 | 把多价格与供应关系写入 spec、server technical-analysis、android planning | 已同步 `20-product-domain`、`30-api-contracts`、`31-android-impact`、`32-rollout-and-compatibility` 以及 product 相关技术分析文档 | 本阶段不改安卓 UI 代码 |

## 2026-06-01

### 文档结构重建

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/technical-analysis/android` | 新版已做 | 目录存在但说明不完整 | 与 `master-goods-android` 目录一一对应 | 已补齐目录级 README | 统一接入六态状态表 |
| `docs/technical-analysis/server` | 新版已做 | 目录存在但与真实服务端结构有偏差 | 对齐 `src/main/java/com/zhihuiji/backend` 与 `src/main/resources` | 已补齐目录级 README | 重点服务于后端先行重构 |
| 旧问题式 README | 新版需要去掉 | 大量文档仍按历史缺陷罗列 | 改成“当前实现 + spec 差距 + 下一阶段动作” | 已完成首轮切换 | 后续逐步补字段矩阵 |
| 会员体系 | 新版需要去掉 | 旧版可能存在会员扩展空间 | 当前新版不纳入范围 | 已在相关文档统一标记 | 后续若恢复需单独立项 |

### 安卓文档同步到新版规划

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `docs/spec/31-android-impact.md` | 新版已做 | 只有极简占位表 | 改成 Android `/v2`、owner、扩域影响总表 | 已重写为新版迁移规范 | 不涉及 UI 视觉细节 |
| `docs/technical-analysis/android/README.md` | 新版已做 | 仍偏首版说明 | 改成 Android 新版迁移总览 | 已同步到后端 entity/dto 方向 | 作为安卓文档入口 |
| `app/core/data/feature` 总文档 | 新版已做 | 仍偏“页面已做完”口径 | 改成职责、迁移、owner、导入视角 | 已统一重写 | 更利于后续重构 |
| Android 核心子模块 README | 新版已做 | 多数仍以 `/v1` 首版闭环为核心 | 改成面向 `/v2` 和扩域能力的规划文档 | 已同步 model/network/database/datastore/auth/order/sync 等关键模块 | 后续继续字段级深化 |
| Android 业务域子模块 README | 新版已做 | 更强调页面是否能跑通 | 改成领域职责与场景拆分说明 | 已同步商品、档案、销售、采购、付款、财务、报表、助手、设置等模块 | 本轮不改代码 |

### 后续维护规则

1. 先更新 `docs/spec/`
2. 再更新对应 `docs/technical-analysis/*/README.md`
3. 仅在文档结构或状态发生变化时更新本文件
