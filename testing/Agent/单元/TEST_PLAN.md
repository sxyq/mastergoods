# Agent 单元与组件测试规划（单元）

更新日期：2026-08-28。目标：把现有后端、Android、iOS 测试类映射到 Agent 组件，登记缺失项，明确每个组件的必测边界。测试数为源码盘点；未运行内容一律不得写 `Passed`。本文件的组件行是父项，实际执行要把测试类、参数化分支和失败结果分别登记。

## 一、后端组件映射（AG-U-001~017）

| 编号 | 组件 | 现有测试类 | 必测边界 | 待补充项 |
|---|---|---|---|---|
| AG-U-001 | ToolPlanner | `ToolPlannerTest`、`V2AgentToolSelectionRegressionTest` | 空计划、重复工具、额外工具、单工具澄清、多来源/展示续轮、目标写工具检测、候选白名单；Provider 未配置降级 (`llm_unavailable`) | `planNextIteration` 各路径（tool-result continuation、write-target retry、single-candidate retry）分支覆盖 |
| AG-U-002 | ToolRegistry / ToolInvocationIdentity | `ToolRegistryTest`、`ToolInvocationIdentityTest` | 重名冲突抛错、listReadOnly/listCreate 排序、`hasAllRequiredParameters`、语义 key 去重 | 同名不同参数执行两次的 key 区分 |
| AG-U-003 | ToolExecutor / ToolArgumentsValidator | 当前主要由业务测试间接覆盖；需独立执行记录 | 四层门顺序、注册/范围/依赖/Schema/权限/上下文拒绝、violation fieldPath | 补充独立 `ToolExecutorTest`：每个拒绝都断言业务工具零调用；覆盖 `additionalProperties=false`、数组元素、`maximum`、`maxItems` |
| AG-U-004 | AgentPromptCatalog | `AgentPromptCatalogTest` | `targetWriteTool` 全关键词映射、`hasWriteIntent`/`requestsVisualization`/`requestsMultipleSources` 判定、WRITE_WORDS 否定语义 | 关键词矩阵回归（防误判） |
| AG-U-005 | AnswerSynthesizer | `AnswerSynthesizerTest` | 空回答、LLM 失败、事实校验、结果块与证据引用、流式增量合成 | 非流式重试降级路径 |
| AG-U-006 | ContextBuilder | `ContextBuilderTest` | 预算计算各字段、24 条消息阈值、70% 阈值、检查点边界后消息、降级安全余量 | 边界消息数 24/25 |
| AG-U-007 | ContextWindowResolver | `ContextWindowResolverTest` | 未知窗口 8192、最大窗口钳制、覆盖键、降级标记 | 覆盖配置解析 |
| AG-U-008 | ContextCompactionService | `ContextCompactionServiceTest` | 触发条件、确定性摘要、语义摘要校验、超时降级、检查点 revision 重试、并发冲突回退、invalidateAfterBoundary | 脱敏命中、MIN 轮次边界（1/2 轮） |
| AG-U-009 | SafetyGuard | `SafetyGuardTest` | 四层判定、破坏性/越权拦截、否定写入不误判、写频率 20/10min、语义审查降级 | 频率窗口滑动边界 |
| AG-U-010 | AgentRunState / AgentIterationPolicy | 相关测试 | 预算 1/2/3/4/5、封顶 6、transcript 配对 fullyPaired、missing_target | HARD_ITERATION_CAP 边界 |
| AG-U-011 | RunAuditService / SseStreamEmitter | 含于 `V2AgentAiServiceTest` | seq/event_id 单调唯一、audit 队列丢/失败计数、lossy 告警、取消中断 | prepareSend 并发与队列容量压力 |
| AG-U-012 | AgentDraftConfirmService | `AgentDraftConfirmServiceTest` | 状态机 active/confirming/confirmed/cancelled、乐观锁、重复确认幂等、14 个草稿类型分支、未知类型拒绝 | `create_inventory_count_draft`落库类型为`create_inventory_adjustment`；`media_upload_tool`落库类型为`media_upload`；`image_generate`确认调用 AgentImageService；并发确认 |
| AG-U-013 | AgentMemoryService | `AgentMemoryServiceTest` | 召回 owner/store 隔离、limit 钳制、异步提取去重、敏感脱敏、配置关闭 | 召回/提取回归与 TTL |
| AG-U-014 | TokenEstimator | `TokenEstimatorTest` | 估算非负、历史文本估算 | 长文本截断一致性 |
| AG-U-015 | 工具实现（代表性） | `CustomerProfileLookupToolTest`、`ProductCatalogLookupToolTest`、`GeneratePosterPromptToolTest`、`PurchaseTrackingLookupToolTest`、`ReportQueryToolTest`、`SaleOrderLookupToolTest`、`PurchaseOrderLookupToolTest`、`SupplierStatementLookupToolTest`、`CreateInventoryCountDraftToolTest`、`CreatePayOrderToolTest`、`CreateSaleOrderToolTest`、`CreateCustomerToolTest`、`ImageGenerateToolTest` | 参数 schema、owner 隔离、返回结构、query_audit 字段、只读/创建类型 | 当前 61 个工具中有 13 个专门工具测试文件，另 48 个工具需要至少补充统一注册、Schema、权限、owner/store 和执行结果断言；不能以代表性测试替代逐工具覆盖 |
| AG-U-016 | WebSearch 安全 | `WebSearchUrlSafetyTest`、`WebSearchProviderContractTest` | URL 白名单、危险来源、Provider 契约 | 重定向链与超长域名 |

## 二、Android 组件映射（AG-U-AND-*）

Android 每个组件行必须分别记录序列化、网络错误、状态合并、取消和展示边界；纯 JVM 测试不能证明真机、网络栈或 UI 点击通过。

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

`Code/frontend/ios/ZhihuijiIOS/` 中的 `AgentModels.swift`、`AgentViewModel.swift`、`AgentAccessPolicy.swift` 对应模型解析、状态机与访问策略。应为 snake_case 解码、未知字段、SSE 增量合并、取消/断线、草稿确认和权限状态分别建立纯 Swift 测试；没有 Xcode 或目标环境时记 `Blocked`，不能用 Android 测试结果代替。

## 四、当前直接工具测试覆盖

| 统计项 | 当前值 | 解释 |
|---|---:|---|
| 源码工具 | 61 | 46 个只读 + 15 个创建类 |
| 有专门工具测试文件 | 13 | 8 个只读 + 5 个创建类 |
| 尚无专门工具测试文件 | 48 | 需要补统一结构测试，并按高风险工具补业务分支 |
| 直接工具文件覆盖率 | 20% | 只代表测试文件存在，不代表测试断言通过 |

统一工具结构测试至少检查：工具名唯一、类型正确、`requiredPermission`、`requiresConfirmation`、Schema 必填/类型/边界/未知字段、依赖名已注册、owner/store 从 `ToolContext` 获取、成功/失败结果和审计字段。对创建类工具还要断言执行阶段只增加 `agent_drafts`，确认阶段才允许进入正式业务 Service。

### AG-U-017 `image_generate` 单元记录

| 输入 | 预期结果 | 证据位置 | 初始状态 |
|---|---|---|---|
| `ImageGenerateToolTest` 的注册、Schema、非法参数、合法草稿、真实调用者权限分支；`AgentImageServiceTest` 的 `url`、`b64_json`、HTTP 失败、超时、取消和非法参考图分支 | 工具名/类型/权限为 `image_generate`/`CREATE_ONLY`/`agent:write`；Schema 约束准确；非法输入不保存草稿；合法输入只生成 owner-scoped active 草稿；Provider 结果和错误安全映射；未确认不调用 Provider | `单元/reports/junit-<wave>.xml`、`单元/logs/AG-U-017.log`、源码测试路径 `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/agent/tool/write/ImageGenerateToolTest.java` 与 `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/AgentImageServiceTest.java` | `Deferred`；提交 `180bb7be` 仅为源码事实，未在本轮执行测试 |

## 五、执行与门槛

- 运行：`./Code/backend/gradlew -p Code/backend test` 与 Android 相关模块单测；iOS 侧仅在有本机 Xcode 工具时运行 `xcodebuild`。
- 门槛：目标模块测试全部 `Passed` 或明确登记 `Blocked`（依赖环境）；失败项按 `AG-U-*` 编号单独登记，不得并入相邻通过项。
- 证据：JUnit XML/HTML 报告 → `单元/reports/`；失败堆栈与日志 → `单元/logs/`；脚本 → `../脚本/单元/`。每个测试类或参数化分支要带 `test_id` 与 `wave_id`，不得只提交一份全量汇总。
