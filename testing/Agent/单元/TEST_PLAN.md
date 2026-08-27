# Agent 单元与组件测试规划（单元）

更新日期：2026-08-28。目标：把现有后端/Android 测试类映射到 Agent 组件，登记缺失项，明确每个组件的必测边界。测试数为现状盘点；未运行内容一律不得写 `Passed`。

## 一、后端组件映射（AG-U-001~012）

| 编号 | 组件 | 现有测试类 | 必测边界 | 待补充项 |
|---|---|---|---|---|
| AG-U-001 | ToolPlanner | `ToolPlannerTest`、`V2AgentToolSelectionRegressionTest` | 空计划、重复工具、额外工具、单工具澄清、多来源/展示续轮、目标写工具检测、候选白名单；Provider 未配置降级 (`llm_unavailable`) | `planNextIteration` 各路径（tool-result continuation、write-target retry、single-candidate retry）分支覆盖 |
| AG-U-002 | ToolRegistry / ToolInvocationIdentity | `ToolRegistryTest`、`ToolInvocationIdentityTest` | 重名冲突抛错、listReadOnly/listCreate 排序、`hasAllRequiredParameters`、语义 key 去重 | 同名不同参数执行两次的 key 区分 |
| AG-U-003 | ToolExecutor / ToolArgumentsValidator | `ToolExecutor` 相关（含于业务测试） | 四层门顺序、6 个稳定错误码、violation fieldPath、依赖缺省放行/拒绝 | 参数门对 `additionalProperties=false` 与数组元素校验的独立单测 |
| AG-U-004 | AgentPromptCatalog | `AgentPromptCatalogTest` | `targetWriteTool` 全关键词映射、`hasWriteIntent`/`requestsVisualization`/`requestsMultipleSources` 判定、WRITE_WORDS 否定语义 | 关键词矩阵回归（防误判） |
| AG-U-005 | AnswerSynthesizer | `AnswerSynthesizerTest` | 空回答、LLM 失败、事实校验、结果块与证据引用、流式增量合成 | 非流式重试降级路径 |
| AG-U-006 | ContextBuilder | `ContextBuilderTest` | 预算计算各字段、24 条消息阈值、70% 阈值、检查点边界后消息、降级安全余量 | 边界消息数 24/25 |
| AG-U-007 | ContextWindowResolver | `ContextWindowResolverTest` | 未知窗口 8192、最大窗口钳制、覆盖键、降级标记 | 覆盖配置解析 |
| AG-U-008 | ContextCompactionService | `ContextCompactionServiceTest` | 触发条件、确定性摘要、语义摘要校验、超时降级、检查点 revision 重试、并发冲突回退、invalidateAfterBoundary | 脱敏命中、MIN 轮次边界（1/2 轮） |
| AG-U-009 | SafetyGuard | `SafetyGuardTest` | 四层判定、破坏性/越权拦截、否定写入不误判、写频率 20/10min、语义审查降级 | 频率窗口滑动边界 |
| AG-U-010 | AgentRunState / AgentIterationPolicy | 相关测试 | 预算 1/2/3/4/5、封顶 6、transcript 配对 fullyPaired、missing_target | HARD_ITERATION_CAP 边界 |
| AG-U-011 | RunAuditService / SseStreamEmitter | 含于 `V2AgentAiServiceTest` | seq/event_id 单调唯一、audit 队列丢/失败计数、lossy 告警、取消中断 | prepareSend 并发与队列容量压力 |
| AG-U-012 | AgentDraftConfirmService | `AgentDraftConfirmServiceTest` | 状态机 active/confirming/confirmed/cancelled、乐观锁、重复确认幂等、dispatchCreate 路由 13 类、未知类型拒绝 | `create_inventory_count_draft` 复用 routing、并发确认 |
| AG-U-013 | AgentMemoryService | `AgentMemoryServiceTest` | 召回 owner/store 隔离、limit 钳制、异步提取去重、敏感脱敏、配置关闭 | 召回/提取回归与 TTL |
| AG-U-014 | TokenEstimator | `TokenEstimatorTest` | 估算非负、历史文本估算 | 长文本截断一致性 |
| AG-U-015 | 工具实现（代表性） | `CustomerProfileLookupToolTest`、`ProductCatalogLookupToolTest`、`GeneratePosterPromptToolTest`、`PurchaseTrackingLookupToolTest`、`ReportQueryToolTest`、`SaleOrderLookupToolTest`、`PurchaseOrderLookupToolTest`、`SupplierStatementLookupToolTest`、`CreateInventoryCountDraftToolTest`、`CreatePayOrderToolTest`、`CreateSaleOrderToolTest`、`CreateCustomerToolTest` | 参数 schema、owner 隔离、返回结构、query_audit 字段 | 其余 47 个工具的一致性/结构断言（无单元测试的按风险补） |
| AG-U-016 | WebSearch 安全 | `WebSearchUrlSafetyTest`、`WebSearchProviderContractTest` | URL 白名单、危险来源、Provider 契约 | 重定向链与超长域名 |

## 二、Android 组件映射（AG-U-AND-*）

| 编号 | 组件/测试 | 必测边界 |
|---|---|---|
| AG-U-AND-001 | `AgentRunTraceModelsTest`、`AgentStreamEventSerializationTest`、`AgentChatResponseSerializationTest` | snake_case 字段、未知字段容忍、事件模型解析 |
| AG-U-AND-002 | `AgentSseClientCancellationTest` | 流中取消、连接生命周期、异常 EOF 处理 |
| AG-U-AND-003 | `AgentV2RepositoryTest` | 请求组装、错误映射、ID 精度（Long） |
| AG-U-AND-004 | `AgentChatViewModelAnswerMergeTest` | 增量合并、去重、终态切换 |
| AG-U-AND-005 | `AgentChatScreenToolStatusTest` | 工具过程状态渲染、不把工具名当正文 |
| AG-U-AND-006 | `AgentChatNetworkGateTest` | 断网/重试/等待态 |
| AG-U-AND-007 | `AgentWorkbenchHistoryTest` | 最近会话/历史恢复 |
| AG-U-AND-008 | `AgentResponseProvenanceTest`、`AgentMarkdownTextParserTest`、`AgentStoredResultBlockParseTest` | 证据引用、Markdown 渲染、结果块解析 |

## 三、iOS 组件（AG-U-IOS-*）

`AgentModels.swift`、`AgentViewModel.swift`、`AgentAccessPolicy.swift` 对应模型解析、状态机与访问策略；有 SVG/Compose 等价条件时补充序列化测试；无现成单测的登记为待补。

## 四、执行与门槛

- 运行：`./Code/backend/gradlew -p Code/backend test` 与 Android 相关模块单测；iOS 侧仅在有本机 Xcode 工具时运行 `xcodebuild`。
- 门槛：目标模块测试全部 `Passed` 或明确登记 `Blocked`（依赖环境）；失败项按 `AG-U-*` 编号单独登记，不得并入相邻通过项。
- 证据：JUnit XML/HTML 报告 → `单元/reports/`；失败堆栈与日志 → `单元/logs/`；脚本 → `../脚本/单元/`。