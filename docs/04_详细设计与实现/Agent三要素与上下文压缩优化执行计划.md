# Agent 三要素与上下文压缩优化执行计划

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | Agent 跨端优化与执行计划 |
| 当前状态 | 计划，尚未执行 |
| 适用端 | 后端 / Agent / Web / Android / iOS / 测试 |
| 计划基线 | 931eefba fix(backend): harden payment validation and list pagination |
| 计划日期 | 2026-08-23 |
| 参考项目 | ChatWithChat，审查版本 ca302c5e45db1213c280096c7546dbf2976797bc |
| 当前变更范围 | 本文档；本轮不修改业务代码、配置、数据库和客户端 |

## 一、结论与范围

本计划加入 Agent 的三项核心能力：

1. **编排循环**：管理一次运行从安全检查、上下文准备、规划、工具执行、结果判断到正式回答的完整生命周期。
2. **工具调用判断**：判断当前任务是否需要工具、允许哪些工具、工具参数是否可执行、工具结果是否足以结束任务，并把权限、Schema、owner/store、确认和审计放到统一执行边界。
3. **上下文窗口与压缩**：按照实际模型窗口计算预算，保留当前任务所需信息，压缩已经完成的历史轮次，并通过可复用检查点减少长会话成本。

长期记忆和在线搜索属于建立在这三项能力之上的扩展能力：长期记忆需要经过上下文预算注入，在线搜索需要经过工具调用判断和结果边界处理。先把三项核心能力做稳定，再扩展记忆和搜索，能够减少相互影响并提高问题定位效率。

本计划覆盖以下能力：

| 能力 | 本计划处理方式 |
|---|---|
| Agent loop 循环 | 建立显式运行状态、完成条件、失败状态和轮次策略 |
| 工具调用判断 | 引入工具范围、工具依赖、完成策略和统一执行门 |
| 工具调用链 | 保留每轮 assistant tool call、call ID、原始参数和 tool result 配对 |
| 推理流展示 | 延续 plan_delta 和 tool_* 事件，只展示计划摘要和执行事实 |
| 上下文压缩 | 按模型窗口计算预算，生成会话级检查点，支持失败降级 |
| 聊天历史 | 将检查点与边界后的原始消息合并，继续支持分页和恢复 |
| 长期记忆 | 在上下文能力稳定后增加 owner/store 隔离的记忆仓储和召回 |
| 在线搜索 | 在工具边界稳定后增加搜索 Provider、网页摘要和引用结果块 |
| 多端展示 | Web、Android、iOS 对齐运行轨迹、压缩事件、来源和搜索结果模型 |

本轮只写计划文档。文档中的“完成”指未来阶段的验收条件，不代表当前代码已经具备对应能力。

## 二、当前实现与问题基线

### 2.1 当前已有骨架

当前后端主链为：

~~~text
V2AgentController
  -> V2AgentAiService
  -> SafetyGuard
  -> ToolPlanner
  -> LongCatAnthropicClient
  -> ToolRegistry
  -> AgentTool
  -> ToolResult
  -> AnswerSynthesizer
  -> RunAuditService / SseStreamEmitter
  -> agent_messages / agent_run_audits / agent_run_audit_events
~~~

现有能力包括：

- 会话创建、恢复、列表、消息读取、删除和草稿操作。
- POST /v2/agent/chat 非流式回答。
- POST /v2/agent/chat/stream 流式运行。
- plan_delta、tool_started、tool_progress、tool_completed、tool_failed、answer_delta、result_block、draft_created、run_cancelled 和 run_completed 等事件。
- READ_ONLY 和 CREATE_ONLY 两种工具类型。
- 工具 Schema 的 required、类型、maximum、maxItems 和非法字段校验。
- owner 作用域查询、requiredPermission 检查、草稿确认和运行审计。
- Web、Android 与 iOS 对计划、工具状态、结果块、草稿和审计的展示模型。

### 2.2 当前主要问题

| 优先级 | 问题 | 当前影响 | 处理阶段 |
|---|---|---|---|
| 高 | 创建类工具选择不稳定 | 查询完成后可能直接生成普通回答，未生成目标草稿 | 阶段 1、2 |
| 高 | 多轮 native tool transcript 可能不完整 | assistant tool call、call ID、参数和 tool result 可能无法逐一配对 | 阶段 1 |
| 高 | 工具结果后缺少统一完成判断 | HTTP 200 或有文本回答不能说明业务任务完成 | 阶段 1 |
| 高 | 创建类工具缺少明确的移动端二次授权表达 | 用户可能把草稿生成误解为正式写入，或确认边界不清 | 阶段 5 |
| 高 | iOS 创建类 Agent 草稿确认链尚未纳入统一移动端契约 | iOS 需要补齐待确认状态、覆盖式二次授权和正式写入结果的状态闭环 | 阶段 5 |
| 中 | 工具权限入口分散 | 新增调用入口时可能遗漏权限检查 | 阶段 2 |
| 中 | Schema 错误缺少统一结构 | 模型和客户端难以根据字段路径修正参数 | 阶段 2 |
| 中 | 审计和结果缺少统一总量边界 | 大 JSON、文本、元数据可能挤占上下文或日志空间 | 阶段 2 |
| 中 | 部分 Agent 查询全量读取后内存截断 | 数据量增大后查询耗时和内存占用上升 | 阶段 3 |
| 中 | Agent 轮次固定为 3 轮 | 查询依赖、补参数、生成草稿和回答可能耗尽轮次 | 阶段 1 |
| 中 | 当前会话只加载最近约 10 条消息 | 长会话依赖简单摘要，历史事实可能丢失 | 阶段 4 |
| 中 | latestSummary 只是最新消息截断 | 不能表达多轮事实、待办、决策和未完成动作 | 阶段 4 |
| 中 | context_compacted 只有客户端展示预留 | Web、Android、iOS 有事件模型，后端尚无真正压缩服务 | 阶段 4 |
| 中 | 未发现长期记忆闭环 | 没有记忆提取、召回、删除和 owner/store 隔离管理 | 阶段 5 |
| 中 | 未发现在线搜索闭环 | 没有搜索工具、网页抓取、摘要和引用来源 | 阶段 6 |
| 待验证 | 同店多成员并发和真实跨店测试不足 | 权限和运行上下文需要真实多账号验证 | 阶段 7 |

### 2.3 重要边界

- 当前客户端已有 context_compacted 事件模型和展示组件，后端没有对应的实际生成流程。计划中必须把“客户端已能展示”和“服务端真实压缩”分别验收。
- 创建类 Agent 工具的客户端交互必须把“生成草稿”和“正式写入”表达为两个独立动作。Android 与 iOS 使用覆盖式确认弹窗表达二次授权；该要求只规定交互语义和状态边界，不规定颜色、布局、圆角、字体、动效或组件样式，视觉实现继续遵循各端现有设计系统。
- 当前会话摘要由最新消息截取约 120 个字符得到。它只能作为列表预览和短提示，不能直接升级为长期记忆或可靠检查点。
- “思考过程”只展示服务端允许展示的计划摘要，不展示模型隐藏推理内容。计划不会把隐藏推理写入消息、SSE 或审计。
- 长期记忆与会话压缩分开管理。会话压缩服务只处理当前会话历史，长期记忆服务处理跨轮次、经过授权的用户或门店事实。
- 没有 PostgreSQL 时，生产查询计划只能保留 SQL 和执行脚本，并标记 Blocked 或 Deferred，不能用 H2 结果代替。

## 三、三要素总体架构

### 3.1 三要素关系

| 要素 | 核心问题 | 主要输入 | 主要输出 | 当前对应组件 |
|---|---|---|---|---|
| 编排循环 | 这次运行处于哪一步，是否继续，何时结束 | 用户问题、运行状态、工具结果 | 下一状态、最终回答或明确失败 | V2AgentAiService、ToolPlanner |
| 工具调用判断 | 是否应该调用工具，调用哪个，参数和权限是否成立 | 工具目录、任务意图、上下文、权限 | 已批准调用、拒绝原因、工具结果 | ToolPlanner、ToolRegistry |
| 上下文压缩 | 给模型哪些历史，哪些历史应压缩 | 模型窗口、消息、工具结果、检查点 | 上下文包、检查点、压缩事件 | 当前缺少独立服务 |

三要素共享以下运行约束：

1. 每次运行只有一个可信的 owner/store 上下文。
2. 每次工具调用都有唯一的运行内身份和可审计状态。
3. 每次模型请求都带有明确的上下文预算。
4. 每次终止都必须属于 COMPLETED、FAILED、CANCELLED 或 EXHAUSTED 之一。
5. 正式回答不能绕过工具结果和完成策略。

### 3.2 目标调用链

~~~mermaid
flowchart TD
    U[用户问题] --> C[V2AgentController]
    C --> R[V2AgentAiService]
    R --> S[安全检查]
    S --> CTX[ContextWindowResolver]
    CTX --> CB[ContextBuilder]
    CB --> CP{需要压缩?}
    CP -->|否| P[ToolPlanner]
    CP -->|是| CC[ContextCompactionService]
    CC --> CK[(agent_context_checkpoints)]
    CC --> P
    P --> TS[ToolScopePlanner]
    TS --> TD[工具调用判断]
    TD --> TE[统一 ToolExecutor]
    TE --> SV[Schema / 权限 / owner-store / 确认 / 超时]
    SV --> DB[AgentTool 与业务 Repository]
    DB --> TR[ToolResult 边界处理]
    TR --> EV[完成条件判断]
    EV -->|依赖未满足| P
    EV -->|需要压缩后继续| CC
    EV -->|目标草稿已生成| D[等待用户确认]
    EV -->|已完成或明确失败| A[AnswerSynthesizer]
    A --> OUT[SSE / REST 正式结果]
    R --> AUD[(RunAuditService)]
    OUT --> MSG[(agent_messages)]
~~~

### 3.3 运行状态机

~~~mermaid
stateDiagram-v2
    [*] --> RUN_STARTED
    RUN_STARTED --> SAFETY_CHECK
    SAFETY_CHECK --> CONTEXT_PREPARED
    SAFETY_CHECK --> BLOCKED
    CONTEXT_PREPARED --> PLAN_REQUESTED
    PLAN_REQUESTED --> TOOL_CALL_VALIDATING
    PLAN_REQUESTED --> FINAL_ANSWER: 无工具且任务允许文本回答
    TOOL_CALL_VALIDATING --> TOOL_EXECUTING: 调用通过
    TOOL_CALL_VALIDATING --> TOOL_FAILED: Schema/权限/上下文拒绝
    TOOL_EXECUTING --> TOOL_RESULTS_READY
    TOOL_EXECUTING --> TOOL_FAILED: 业务错误/超时/取消
    TOOL_RESULTS_READY --> DEPENDENCY_PENDING: 缺少真实 ID 或前置查询
    TOOL_RESULTS_READY --> CONFIRMATION_PENDING: CREATE_ONLY 草稿完成
    TOOL_RESULTS_READY --> PLAN_REQUESTED: 需要补充查询
    TOOL_RESULTS_READY --> FINAL_ANSWER: 完成策略满足
    DEPENDENCY_PENDING --> PLAN_REQUESTED
    PLAN_REQUESTED --> CONTEXT_COMPACTION: 预算不足
    CONTEXT_COMPACTION --> PLAN_REQUESTED
    PLAN_REQUESTED --> ITERATION_EXHAUSTED: 达到预算
    FINAL_ANSWER --> COMPLETED
    CONFIRMATION_PENDING --> COMPLETED: 运行只生成草稿
    RUN_STARTED --> CANCELLED: 用户取消
    TOOL_EXECUTING --> CANCELLED: 用户取消
    TOOL_FAILED --> FAILED
    BLOCKED --> COMPLETED
    ITERATION_EXHAUSTED --> FAILED
~~~

状态机的重点是让“模型返回了文本”与“业务任务已经完成”分开判断。创建、付款、入库、退货、转账和登记等任务必须满足目标工具或草稿完成条件，才能进入正式完成状态。

## 四、要素一：编排循环优化

### 4.1 目标

将当前 V2AgentAiService 中分散的首轮规划、ReAct 续轮、工具执行、答案生成和审计收尾，整理为可观察的运行状态。循环本身负责过程控制，工具负责业务动作，回答组件负责表达结果。

### 4.2 每轮循环的固定步骤

~~~text
1. 读取运行状态和当前上下文包
2. 判断本轮是否允许继续调用模型
3. 生成工具范围和完成策略
4. 请求模型返回结构化计划或最终回答
5. 校验工具调用的名称、参数、权限和上下文
6. 执行通过校验的工具
7. 记录工具结果、失败、跳过和审计事件
8. 判断任务是否完成、是否缺依赖、是否需要确认
9. 需要继续时进入下一轮，需要回答时进入正式回答
10. 发送终态事件并持久化消息和审计
~~~

### 4.3 循环状态对象

建议在服务内部引入一个运行状态对象，名称可按现有代码风格确定，至少包含：

| 字段 | 作用 |
|---|---|
| runId | 关联一次 Agent 运行 |
| conversationId | 关联会话 |
| ownerUserId | 当前 owner 隔离 |
| storeId | 当前门店上下文 |
| iteration | 当前循环轮次 |
| toolCallCount | 当前工具调用数 |
| plannedTools | 当前轮允许的工具 |
| requiredTargetTools | 完成任务所需的目标工具 |
| completedTools | 已完成工具及其状态 |
| pendingDependencies | 尚缺的实体 ID 或查询结果 |
| nativeTranscript | assistant tool call 与 tool result 配对记录 |
| contextState | 当前上下文预算和检查点状态 |
| completionState | 当前完成条件判定 |
| cancelled | 取消标志 |
| terminalStatus | 最终终态 |

该对象只存在于单次运行中。需要跨请求复用的历史内容进入消息、审计和上下文检查点表，不把完整运行状态长期放入会话摘要字段。

### 4.4 轮次策略

当前 MAX_AGENT_ITERATIONS = 3 不能继续作为所有任务的唯一上限。计划改为两个边界：

- 默认轮次预算：控制普通问题的响应时间和成本。
- 任务复杂度预算：依据依赖工具数量、创建目标和工具失败次数调整，但仍有硬上限。

建议初始策略：

| 场景 | 默认轮次 | 说明 |
|---|---:|---|
| 纯文本问题 | 1 | 没有业务事实查询时直接回答 |
| 单个只读查询 | 2 | 规划、查询、回答 |
| 多个只读查询 | 3 | 并行或串行查询后回答 |
| 查询后生成草稿 | 4 | 查询、补参数、生成草稿、回答 |
| 多依赖创建任务 | 5 | 仍受单 run 工具数和时间预算约束 |

轮次耗尽时必须返回结构化失败：

~~~json
{
  "status": "exhausted",
  "code": "AGENT_ITERATION_EXHAUSTED",
  "completed_tools": ["purchase_order_lookup"],
  "missing_target_tools": ["create_purchase_receipt"],
  "safe_message": "已完成采购单查询，但未完成入库草稿生成。"
}
~~~

### 4.5 终态规则

| 终态 | 进入条件 | 正式回答要求 |
|---|---|---|
| COMPLETED | 查询或目标动作满足完成策略 | 只陈述已验证结果 |
| CONFIRMATION_PENDING | CREATE_ONLY 已生成草稿 | 明确等待用户确认，不能声称已写入正式表 |
| FAILED | 工具、Provider、上下文或系统错误 | 说明已完成部分和安全错误信息 |
| BLOCKED | 安全或权限策略拒绝 | 不执行业务工具，说明拒绝原因 |
| CANCELLED | 用户或系统取消 | 不覆盖为普通失败 |
| EXHAUSTED | 预算耗尽但完成策略未满足 | 明确列出缺失目标，不返回成功语义 |

### 4.6 终态跨端契约

终态字段是 Agent 运行结果的唯一业务判断依据。HTTP 状态码只表示传输、认证、参数或服务层处理结果，不能用 HTTP 200 单独推导业务成功。REST、SSE、审计和客户端必须使用同一组大写终态值，并保留稳定错误码。

| 终态 | REST | SSE | Run audit | Web / Android |
|---|---|---|---|---|
| COMPLETED | `terminal_status=COMPLETED`、`code=0`，回答只引用已验证事实 | `run_completed` 携带 `terminal_status=COMPLETED`，每个 run 只出现一次终态事件 | `status=completed`，终态事件和正式回答可关联 | 成功状态，允许展示正式结果 |
| CONFIRMATION_PENDING | `terminal_status=CONFIRMATION_PENDING`，返回草稿引用和确认入口 | 先发送 `draft_created`，再发送携带该终态的终止事件 | `status=confirmation_pending`，记录草稿 ID 和目标动作 | 待确认状态，不能显示为正式写入成功 |
| FAILED | `terminal_status=FAILED` 和稳定错误码，按根因映射 HTTP 4xx/5xx | `run_failed` 或兼容终止事件中明确携带 `terminal_status=FAILED` | `status=failed`，记录已完成部分和安全错误 | 错误状态，保留重试条件和清理结果 |
| BLOCKED | `terminal_status=BLOCKED` 和权限或安全错误码 | `run_blocked`，拒绝后不能出现业务工具执行事件 | `status=blocked`，记录拒绝原因，不记录敏感参数 | 阻止状态，不能展示工具成功或正式回答 |
| CANCELLED | 取消接口返回 `terminal_status=CANCELLED` | `run_cancelled` 携带取消原因，之后不能再发送成功终态 | `status=cancelled`，记录取消时间和已停止的调用 | 已取消状态，忽略迟到的非终态事件 |
| EXHAUSTED | `terminal_status=EXHAUSTED`、`code=AGENT_ITERATION_EXHAUSTED`，列出已完成和缺失目标 | `run_exhausted` 或兼容终止事件中明确携带该终态 | `status=exhausted`，记录预算、轮次和缺失目标 | 非成功状态，展示部分结果和未完成目标 |

兼容旧客户端时，缺少 `terminal_status` 的终止事件不能作为新回归用例的成功依据。服务端必须保证终止事件、REST 结果和审计终态一致，客户端不能把 `llm_status=completed`、HTTP 200 或存在文本回答当作业务完成条件。

## 五、要素二：工具调用判断与执行边界

### 5.1 参考项目映射

参考项目的 ToolLoopOrchestrator、ToolExecutor、ToolScopePlanner 和 ToolArgumentsValidator 对本项目的借鉴重点如下：

| 参考职责 | 本项目目标职责 |
|---|---|
| Loop orchestrator | 统一管理计划、工具结果、失败、继续和终态 |
| Tool executor | 统一执行前检查、执行、超时、结果约束和审计 |
| Tool scope planner | 按任务意图缩小工具集合，并包含依赖工具 |
| Arguments validator | 返回字段路径、约束类型和稳定错误码 |
| Tool result bounds | 分别限制文本、结构化 JSON、来源、元数据和总大小 |

### 5.2 工具元数据

每个 AgentTool 需要提供或由注册层补齐以下元数据：

~~~text
name
type: READ_ONLY | CREATE_ONLY
capabilities
requiredPermission
requiredParameters
optionalParameters
parameterSchema
dependsOn
completionRole
requiresConfirmation
maxResultItems
timeoutSeconds
~~~

示例：

~~~text
purchase_order_lookup
  type: READ_ONLY
  capabilities: [purchase_order, supplier, product]
  completionRole: dependency_query

create_purchase_receipt
  type: CREATE_ONLY
  capabilities: [purchase_receipt]
  dependsOn: [purchase_order_lookup]
  completionRole: target_action
  requiresConfirmation: true
~~~

工具依赖由元数据表达。模型可以提出候选调用，服务端根据依赖和当前状态决定可用范围，减少相邻工具之间的误选。计划不使用固定中文关键词作为唯一判定依据。

实际工具依赖矩阵以注册表中的工具名为准，失败用例和回归测试不得使用文档自造名称：

| 目标工具 | 必需依赖 | 依赖结果要求 |
|---|---|---|
| `generate_poster_prompt` | `product_catalog_lookup` | 使用真实商品 ID、名称或规格，且已通过当前 owner/store 校验 |
| `create_inventory_count_draft` | `product_catalog_lookup`、`inventory_snapshot_lookup` | 商品和当前库存快照均查询成功后才能生成盘点草稿 |
| `create_pay_order` | `supplier_directory_lookup` | 收款对象必须来自当前作用域的供应商查询 |
| `create_purchase_order` | `supplier_directory_lookup`、`product_catalog_lookup` | 供应商和采购商品都必须有真实 ID |
| `create_purchase_receipt` | `purchase_order_lookup` | 入库草稿必须绑定真实采购单及其可入库明细 |
| `create_purchase_return` | `purchase_order_lookup` | 退货草稿必须绑定真实采购单及可退数量 |
| `create_sale_order` | `customer_directory_lookup`、`product_catalog_lookup` | 客户和销售商品均需通过 owner/store 校验 |

目标工具只有在所有必需依赖完成、参数由依赖结果构建、权限检查通过后才进入执行范围。依赖缺失时可以继续查询或进入 `DEPENDENCY_PENDING`，不能直接生成目标草稿或正式回答。

### 5.3 工具调用判断的四层门

~~~mermaid
flowchart LR
    A[模型候选调用] --> B[范围门]
    B --> C[参数门]
    C --> D[权限与上下文门]
    D --> E[任务完成门]
    E --> F[执行工具]
    B --> X[tool_skipped]
    C --> Y[TOOL_ARGUMENTS_INVALID]
    D --> Z[TOOL_PERMISSION_DENIED]
    E --> W[继续查询或等待依赖]
~~~

#### 范围门

- 工具名称必须在当前任务的允许范围内。
- result_visualization 等展示决策工具必须依赖真实数据结果。
- 重复的语义调用使用运行内身份去重。
- 已完成且结果未变化的调用不能无限重复。

#### 参数门

- 工具必须已注册。
- 必填字段、类型、枚举、数值边界、数组数量和非法字段全部在业务执行前检查。
- 错误返回字段路径，例如 $.items[0].quantity。
- 参数错误不能进入业务 Repository。

#### 权限与上下文门

- 从认证会话读取 currentUserId、ownerUserId、storeId、成员角色和权限集合。
- 模型参数中的 owner/store 只作为业务筛选输入，不能作为可信租户身份。
- 所有 Repository 查询保留 owner/store 条件。
- requiredPermission 在统一执行门检查，工具内部可做业务级二次检查。

`requiredPermission` 每次调用都从当前认证主体、成员关系和当前 store 上下文计算，不能读取模型参数、旧会话摘要或客户端自报权限。正向测试需要证明有权限的真实调用者可以执行；反向测试需要证明权限不足、跨 owner、跨 store 和成员关系失效时在业务 Repository 前被拒绝，并留下可审计的稳定错误。

#### 候选规划与执行判断

`ToolPlanner` 的关键词规则只能产生候选提示，不能直接决定工具执行、目标完成或草稿创建。执行判断按以下顺序完成：

1. 从用户请求和当前运行状态生成结构化意图，至少包含任务类型、目标动作、实体类型、已知实体 ID、是否需要确认和完成条件。
2. 合并模型 native tool call、结构化意图和工具元数据，形成候选工具集合。
3. 将关键词匹配结果作为低可信提示，仅用于补充候选或发现可能缺失的依赖，不能绕过 native call、Schema、权限和完成门。
4. 根据 `dependsOn`、`completionRole`、`requiresConfirmation` 和当前已完成工具结果计算本轮允许范围。
5. 通过四层门后才调用 `AgentTool`；目标工具缺失时继续查询、返回结构化失败或进入 `EXHAUSTED`。

重试只适用于明确的临时故障，例如 Provider 连接中断、超时或可重试的 5xx，并且必须受次数、总时长和相同调用身份限制。Schema 错误、权限拒绝、owner/store 不匹配和业务校验错误不能自动重复调用；需要修正参数时，必须产生新的结构化候选并重新经过全部执行门。

#### 任务完成门

- 查询类任务：至少有一个真实成功查询结果，正式回答只能引用已返回事实。
- 创建类任务：目标 CREATE_ONLY 工具必须成功生成草稿。
- 付款、入库、退货、转账等任务：草稿确认前不能声称正式业务写入完成。
- 依赖查询完成但目标工具未完成时，进入 DEPENDENCY_PENDING 或下一轮计划。
- 模型提前返回文本时，服务端依据完成策略判断是否允许终止。

### 5.4 原生 tool transcript

每个模型轮次都保存以下内存结构，直到本次运行收尾：

~~~json
{
  "round": 2,
  "assistant_tool_calls": [
    {
      "call_id": "call-123",
      "tool_name": "product_catalog_lookup",
      "raw_arguments": "{\"keyword\":\"...\"}"
    }
  ],
  "tool_messages": [
    {
      "call_id": "call-123",
      "tool_name": "product_catalog_lookup",
      "status": "completed",
      "output": "{...}"
    }
  ]
}
~~~

要求：

- 保留所有轮次，不只保留最新轮次。
- call_id 必须在 assistant tool call 和 tool message 之间一一对应。
- 原始参数解析失败时记录结构化失败，不能使用 {} 掩盖参数丢失。
- 工具结果需要被下一轮模型读取时，使用受限的结果 JSON；审计只保存脱敏摘要。
- Provider 续轮失败时，保留 transcript 以便审计和失败重试判断。

不同 wire API 的原生配对规则必须分别保留，不能只把工具调用转换成没有来源的普通文本。

#### Chat Completions

每轮消息必须保留 assistant 消息中的 `tool_calls[].id`、`tool_calls[].function.name` 和未经改写的 `tool_calls[].function.arguments`。每个调用随后生成一个 `role=tool` 消息，包含同一个 `tool_call_id`、工具名、工具结果或结构化失败。多工具调用按 assistant 给出的顺序配对，缺少结果的调用必须记录为失败或取消，不能静默丢弃。

#### Responses

每个 `function_call` 输出项必须保留 `call_id`、`name` 和原始 `arguments`；工具执行后使用同一 `call_id` 生成 `function_call_output`，输出内容为受限工具结果或结构化失败。Responses 的 item 顺序、调用 ID 和输出 ID 必须进入统一 transcript，不能按文本内容猜测对应关系。

两种协议都必须保留 `raw_arguments`。JSON 解析失败时记录 `TOOL_ARGUMENTS_INVALID`、字段路径和安全错误信息；不得用 `{}` 替代原始参数，也不得在参数解析失败后访问业务 Repository。

### 5.5 创建工具的业务边界

~~~text
用户意图
  -> 查询真实实体
  -> 校验实体 owner/store
  -> 生成 Agent Draft
  -> draft_created
  -> 用户确认
  -> AgentDraftConfirmService
  -> 正式业务 Service
  -> 正式业务表
~~~

Agent 工具运行阶段不直接写正式业务表。草稿确认使用既有业务 Service 和事务边界，确保 Agent 与普通 Web、Android、iOS 操作使用同一业务规则。

### 5.6 创建草稿与二次授权交互

创建类 Agent 工具的生命周期必须明确分成两个服务端动作和一个客户端授权节点：

~~~text
Agent 依赖查询
  -> CREATE_ONLY 工具生成草稿
  -> 返回 draft_created / CONFIRMATION_PENDING
  -> Android / iOS 覆盖式确认弹窗展示待写入内容和影响范围
  -> 用户确认
  -> 独立 confirm draft 请求
  -> 正式业务 Service 与事务写入
~~~

Android 与 iOS 交互要求：

- 草稿生成后进入待确认状态，覆盖式确认弹窗必须明确这是“即将写入正式业务数据”的二次授权，不得把草稿状态显示成已完成或已入账。
- 弹窗至少展示业务动作、关键对象、数量/金额、目标 owner/store、草稿状态和确认/拒绝操作；敏感字段按现有脱敏规则展示。
- “确认”只调用草稿确认接口，并携带草稿 ID、会话/运行关联和幂等信息；客户端不能直接调用正式业务创建接口，也不能自行拼装正式业务 payload。
- “拒绝”、关闭弹窗、返回页面、超时、网络中断或进程重启都不得触发正式写入。草稿应保持可查询的待确认或已取消状态，并允许按既有规则清理。
- 确认按钮重复点击必须只产生一次正式确认结果；客户端显示加载和结果状态，服务端仍以权限、草稿 owner/store、状态转换和事务唯一性为准。
- 确认前权限、门店上下文或草稿状态发生变化时，服务端必须重新校验并拒绝失效确认，不能沿用生成草稿时的旧授权。
- iOS 在 AgentChatView、AgentViewModel 和 AgentDraftsView 中表达待确认、确认中、确认成功、确认拒绝和确认失败状态；APIClient 或其仓储层只能通过独立草稿确认接口触发正式写入。
- iOS 确认弹窗遵循现有 SwiftUI 设计系统和 Apple 平台可访问性要求，支持安全区、Dynamic Type、VoiceOver 标签/提示、明确的取消语义和 Reduce Motion；关闭弹窗不能等同于确认。

该要求借鉴的是覆盖式确认弹窗的表达形式和“草稿 -> 确认 -> 正式写入”语义，不复制任何附件或可视化参考中的具体视觉风格。Web、Android 和 iOS 均使用各自现有设计系统表达同一契约；所有端都必须以服务端状态和正式写入结果作为最终依据。

## 六、要素三：上下文窗口与压缩方案

### 6.1 当前问题

当前 Agent 在部分路径使用最近约 10 条消息，并附加会话的 latestSummary。latestSummary 当前是最新消息截断文本，无法稳定表达：

- 早期用户目标和已确认决策。
- 多轮查询得到的真实事实。
- 尚未完成的创建动作和缺失实体 ID。
- 工具失败、用户取消和草稿确认状态。
- 当前会话中哪些内容可以被模型引用。

因此计划引入“预算驱动的上下文构建 + 会话级检查点 + 运行时降级摘要”。

### 6.2 上下文包组成

每次模型请求都由 ContextBuilder 生成上下文包，按以下顺序组织：

~~~text
A. 系统规则和安全约束
B. 当前 owner/store 作用域说明
C. 最新有效会话检查点
D. 检查点边界之后的原始完整轮次
E. 当前轮工具调用和工具结果
F. 当前用户问题
G. 输出格式和完成策略
~~~

各部分职责：

| 部分 | 是否可压缩 | 处理规则 |
|---|---|---|
| 系统规则 | 否 | 固定版本配置，超预算时先减少历史内容 |
| owner/store 作用域 | 否 | 每次请求重新从认证上下文构建 |
| 会话检查点 | 可合并 | 只使用当前会话、当前 owner 的有效检查点 |
| 较早原始轮次 | 是 | 仅压缩已完成轮次 |
| 当前轮工具结果 | 受限 | 保留完成判断所需字段，限制 JSON 和文本大小 |
| 当前用户问题 | 否 | 完整保留 |
| 输出规则 | 否 | 保留正式回答和工具调用要求 |

### 6.3 预算计算

引入 ContextWindowResolver 和 TokenEstimator 接口。预算公式为：

~~~text
providerWindow = resolve(provider, model, wireApi)
usableWindow = min(providerWindow, configuredMaximum)
historyBudget = usableWindow
  - systemBudget
  - scopeBudget
  - currentQuestionBudget
  - toolResultBudget
  - reservedOutputBudget
  - safetyMargin
~~~

初始默认比例建议：

| 预算项 | 初始比例或上限 | 说明 |
|---|---:|---|
| 系统规则 | 10% | 统一安全、工具和回答规则 |
| owner/store 作用域 | 3% | 作用域和权限摘要 |
| 当前问题 | 8% | 超长输入单独按请求上限拒绝或裁剪 |
| 当前轮工具结果 | 20% | 结构化结果优先，原始明细受限 |
| 正式回答预留 | 15% | 防止工具结果挤掉回答空间 |
| 安全余量 | 10% | 应对估算误差和 Provider 差异 |
| 历史消息与检查点 | 34% | 由上下文服务动态分配 |

这些比例属于起始配置，真实 Provider 使用量、超时和回答质量验证后再调节。配置必须可按模型覆盖，不能把某个模型的窗口写成全局固定值。

#### 窗口未知与估算失败的保守策略

- Provider、model 或 wire API 的窗口无法确认时，使用配置中的较小安全上限，并提高安全余量；在确认真实窗口前不得按最大可能窗口发送完整历史。
- `TokenEstimator` 无法估算单条消息、工具结果或结构化摘要时，按更保守的字符/字节估算加消息固定开销，并把该请求标记为估算降级。
- 估算仍超过安全上限时，先压缩已完成历史；当前问题、owner/store 约束、未完成工具调用和待确认草稿不得被静默截断。无法满足预算时返回稳定的 4xx 或 `EXHAUSTED` 结果。
- Provider 返回真实 usage 后只用于后续预算指标和配置校准，不能改变已经发出的请求，也不能把敏感原文写入性能日志。

### 6.4 压缩触发条件

满足任意条件时进入压缩评估：

1. 预计上下文占用超过可用窗口的 70%。
2. 历史消息超过安全消息数上限，且存在至少两个已完成轮次。
3. 工具结果加入后，历史与当前轮合计超过工具预算。
4. Provider 返回上下文超限错误。
5. 检查点失效后，重建上下文仍超过预算。

压缩不应在每轮固定发生。预算足够时直接复用原始消息，避免无意义的摘要损失。

### 6.5 压缩对象选择

压缩按“完整用户轮次”选择最早的历史段：

~~~text
可压缩：已完成的 user -> assistant -> tool evidence 轮次
保留：当前 user 问题、未完成工具调用、待确认草稿、最近完整轮次
保留：安全拦截、取消、失败和用户明确的关键决定
跳过：重复的工具进度事件、重复结果行、过期的临时状态
~~~

选择规则：

- 一次至少压缩一个完整轮次，不能只删除半个 user/assistant 对。
- 当前运行产生的未完成工具调用不进入持久化检查点。
- CREATE_ONLY 草稿处于待确认状态时，草稿摘要和草稿 ID必须保留。
- 工具返回的业务明细只保留完成回答所需的事实、数量、时间窗口和来源摘要。
- 原始消息仍保存在 agent_messages，检查点只承担模型输入压缩职责。

### 6.6 两级压缩策略

#### 一级：确定性抽取摘要

在任何模型压缩请求前，先生成当前请求可用的确定性摘要，确保 Provider 不可用时仍能构建请求。摘要提取：

- 用户问题的短标题。
- 已确认的业务事实。
- 已完成工具及结果数量。
- 未完成动作和原因。
- 待用户确认的草稿。
- 错误、取消和安全状态。
- 最后一个有效消息时间。

该摘要只用于本次请求的降级路径。通过确定性规则生成的摘要需要标记来源和质量等级，不能伪装成模型生成的语义检查点。

#### 二级：隔离的语义压缩

当上下文需要长期复用时，调用独立的压缩请求：

- 只接收历史轮次和已有检查点。
- 不进入工具循环。
- 不允许调用业务工具、搜索工具或写入工具。
- 使用结构化 JSON 输出。
- 设定独立超时和输出上限。
- 压缩请求本身不再次触发上下文压缩。

建议输出结构：

~~~json
{
  "summary_version": 1,
  "conversation_goal": "用户正在处理的目标",
  "confirmed_facts": [
    "已经由业务工具验证的事实"
  ],
  "decisions": [
    "用户明确确认的选择"
  ],
  "pending_actions": [
    "尚未完成的动作和原因"
  ],
  "entity_references": [
    {
      "type": "product",
      "id": "业务实体 ID",
      "label": "脱敏展示名"
    }
  ],
  "tool_evidence": [
    {
      "tool_name": "工具名",
      "status": "completed",
      "returned_count": 3,
      "summary": "工具事实摘要"
    }
  ],
  "open_questions": [
    "等待用户补充的内容"
  ],
  "source_boundary_message_id": 100,
  "source_message_count": 8
}
~~~

压缩结果验证规则：

- source_boundary_message_id 必须属于当前 owner/store 的当前会话。
- confirmed_facts 只能来自消息或工具证据，禁止凭空补充业务结论。
- entity_references 必须经过当前作用域检查。
- 字段数量、文本长度、JSON 深度和总字节数均有限制。
- 摘要作为历史资料注入，使用明确的历史标记，不能被模型当作新的系统指令。
- 输出缺字段、格式错误、超限或 Provider 失败时，当前请求使用确定性摘要；无效语义摘要不覆盖旧检查点。

### 6.7 检查点持久化

建议新增迁移，例如 V32__agent_context_checkpoints.sql，具体版本号以执行时当前迁移目录为准。表结构建议：

~~~text
agent_context_checkpoints
  id BIGINT PRIMARY KEY
  owner_user_id BIGINT NOT NULL
  conversation_id BIGINT NOT NULL
  source_boundary_message_id BIGINT NOT NULL
  source_message_count INTEGER NOT NULL
  summary_body TEXT NOT NULL
  summary_version INTEGER NOT NULL
  context_policy_version INTEGER NOT NULL
  tool_schema_version INTEGER NOT NULL
  revision INTEGER NOT NULL
  quality VARCHAR(32) NOT NULL
  status VARCHAR(32) NOT NULL
  model_name VARCHAR(128)
  estimated_input_tokens INTEGER
  estimated_output_tokens INTEGER
  created_at BIGINT NOT NULL
  updated_at BIGINT NOT NULL
  invalidated_at BIGINT
  invalidation_reason VARCHAR(128)
~~~

建议索引和约束：

~~~text
INDEX(owner_user_id, conversation_id, source_boundary_message_id)
INDEX(owner_user_id, conversation_id, status, updated_at)
UNIQUE(owner_user_id, conversation_id, source_boundary_message_id, context_policy_version, revision)
FOREIGN KEY(conversation_id) REFERENCES agent_conversations(id)
~~~

实现要求：

- 所有查询和写入带 owner_user_id。
- 检查点只对当前会话有效。
- 会话删除时一并删除检查点。
- 消息编辑、删除或重新生成影响边界之后的内容时，使相关检查点变为 invalidated。
- 并发压缩同一边界时，使用事务和唯一约束避免产生重复有效检查点。
- 读取时选择距离当前消息最近且状态有效的检查点，再读取边界之后的原始消息。
- 检查点版本至少区分摘要结构、上下文策略、工具 Schema 和 Provider/model；版本变化或依赖证据变化时，旧检查点必须失效，不能继续当作当前契约使用。
- 唯一约束竞争时捕获唯一键冲突并读取已提交的有效检查点；失败请求不能创建第二个有效副本，也不能把并发异常返回为未处理的 500。
- 检查点保留策略按 owner、会话和边界执行，保留恢复所需的最近有效版本，过期的失效记录和失败记录按明确 TTL 清理；清理任务不能删除仍被活动运行引用的检查点。
- 检查点只保存完成判断所需的最小字段。手机号、地址、凭据、完整认证载荷和无关客户资料不得进入摘要、SSE 或性能日志；实体显示名需要脱敏或使用最小展示字段。
- 新的压缩请求只有在结果通过结构、边界、作用域和大小校验后才能切换有效版本；超时、格式错误、唯一约束冲突处理失败或 Provider 错误都不能覆盖已有有效检查点。

### 6.8 检查点复用和失效

~~~mermaid
flowchart TD
    A[当前会话消息] --> B[查找当前 owner 的有效检查点]
    B --> C{存在可复用检查点?}
    C -->|是| D[加载检查点]
    C -->|否| E[读取原始历史]
    D --> F[加载边界之后的原始完整轮次]
    E --> G{预算足够?}
    F --> G
    G -->|是| H[构建上下文包]
    G -->|否| I[选择更早完整轮次压缩]
    I --> J[确定性摘要]
    J --> K[语义压缩请求]
    K --> L{输出有效?}
    L -->|是| M[保存检查点]
    L -->|否| N[当前请求使用降级摘要]
    M --> H
    N --> H
~~~

消息删除、编辑、会话分支或草稿状态变化时，检查点处理规则：

| 变化 | 处理 |
|---|---|
| 删除检查点边界之前的消息 | 使受影响检查点失效，重新选择历史输入 |
| 编辑检查点范围内的用户消息 | 使该消息之后的检查点失效 |
| 删除最近消息 | 保留不受影响的旧检查点，重新读取边界后的消息 |
| 新增普通消息 | 检查点继续可用，只追加边界后的消息 |
| 草稿确认或取消 | 更新当前运行上下文摘要，旧检查点按边界判断是否继续有效 |
| 会话删除 | 级联清理检查点和相关运行轨迹 |

### 6.9 上下文压缩与长期记忆的边界

| 项目 | 会话上下文压缩 | 长期记忆 |
|---|---|---|
| 作用范围 | 单个会话 | owner/store 范围内的跨会话事实 |
| 触发时机 | 当前请求预算不足 | 一轮对话完成后的异步候选提取 |
| 内容 | 历史轮次和工具证据 | 用户明确偏好、长期业务事实或授权记忆 |
| 来源 | 当前会话消息和审计摘要 | 明确来源会话、消息和用户授权 |
| 删除 | 消息变化导致检查点失效 | 用户删除记忆后停止召回 |
| 注入方式 | 作为会话历史块 | 作为有限的记忆参考块 |
| 可信边界 | 历史资料 | 历史资料 |

两者不能共用一张“万能摘要”表。上下文压缩优先解决当前会话可持续运行，长期记忆需要单独的敏感信息、来源、删除和权限策略。

## 七、推理流、审计与多端展示

### 7.1 事件原则

现有 plan_delta 作为计划摘要事件继续保留。事件名称表达“计划”或“执行事实”，不表达模型隐藏推理。

新增或扩展上下文事件建议：

~~~json
{
  "event_type": "context_compacted",
  "run_id": "run-123",
  "checkpoint_id": 12,
  "source_boundary_message_id": 100,
  "compacted_count": 8,
  "summary_preview": "已保留当前业务目标、已确认事实和未完成动作",
  "input_token_estimate": 9200,
  "output_token_estimate": 1800,
  "reason": "context_budget_threshold",
  "reused": false,
  "timestamp": 0
}
~~~

事件不携带完整客户资料、完整工具参数、完整搜索页面或认证载荷。完整检查点只由服务端内部按 owner/store 权限读取。

### 7.2 Web

Web 端计划：

- 继续展示计划摘要、工具状态、结果块、草稿和审计。
- 增加上下文压缩事件的边界、原因、压缩条数和摘要预览。
- 增加搜索结果专用卡片，不把网页来源混入业务数据表格。
- 历史消息分页加载时，显示检查点和原始消息的时间顺序。
- 工具循环耗尽时显示“已完成部分”和“未完成目标”，禁止使用成功状态样式。
- 创建类工具收到 draft_created 后显示待确认状态；正式业务结果只能在确认接口成功并收到服务端终态后展示。

### 7.3 Android

Android 端计划：

- 保留 AgentRunTraceReducer 的去重、状态合并和折叠展示。
- 扩展 ContextCompacted 模型以支持检查点 ID、边界消息 ID、预算和原因。
- 增加历史消息恢复时的运行轨迹关联。
- 增加长期记忆和在线搜索的纯 Kotlin 模型、Repository、ViewModel 测试。
- 创建类工具的待确认状态使用覆盖式确认弹窗表达二次授权，弹窗确认和拒绝都必须映射到明确的 ViewModel 状态；关闭、返回和取消不得触发确认接口。
- 确认弹窗复用项目现有设计系统和无障碍规范，计划不规定颜色、布局、圆角、字体、动效或具体组件样式；需要保证焦点顺序、读屏文案、重复点击防护和错误可重试。
- 确认成功后刷新草稿、运行轨迹和正式业务结果；确认失败、权限变化、草稿过期或网络中断时保留可解释的错误状态，不乐观更新正式业务数据。
- 真机、模拟器、adb、APK 和 UI 点击属于独立验证范围，不能由纯单元测试代替。

### 7.4 iOS

iOS 端计划：

- 在 AgentChatView 和 AgentDraftsView 中展示 draft_created、CONFIRMATION_PENDING、确认中、确认成功、拒绝和失败状态；草稿卡片不能显示成正式业务已完成。
- 使用 SwiftUI 原生覆盖式确认弹窗表达二次授权，确认操作和拒绝操作必须有清晰的 VoiceOver 标签、提示和状态反馈；弹窗关闭、返回、系统中断和取消都不能调用正式确认接口。
- AgentViewModel 通过 APIClient 的独立草稿确认请求完成正式写入，确认成功后重新读取草稿、运行审计和业务结果；客户端不直接调用业务创建接口。
- 适配安全区、紧凑/常规宽度、横竖屏、Dynamic Type、深色模式、Increase Contrast、Reduce Motion 和 VoiceOver；弹窗内容过长时仍需保证确认与拒绝操作可见且顺序明确。
- iOS 真机、模拟器和 UI 自动化属于独立验证范围，不能用 Swift 单元测试替代；未运行时按台账记录 Blocked 或 Deferred。

## 八、长期记忆执行计划

长期记忆放在上下文压缩之后实施，第一版使用数据库文本检索，保留后续增加向量检索的接口。

### 8.1 数据字段

~~~text
agent_memories
  id
  owner_user_id
  store_id
  source_conversation_id
  source_message_id
  memory_type
  summary
  details
  recall_text
  sensitivity
  confidence
  status
  created_at
  updated_at
  expires_at
  last_accessed_at
~~~

### 8.2 记忆流程

~~~text
一轮回答完成
  -> 异步提取候选记忆
  -> 检查敏感信息和作用域
  -> 与已有记忆去重或合并
  -> 保存有效记忆
  -> 新问题到来时按当前 owner/store 召回
  -> 限量注入上下文
  -> 记录召回来源
~~~

要求：

- 记忆提取不阻塞当前回答。
- 用户可以关闭自动学习。
- 用户可以查看来源、编辑、删除和导出。
- 记忆删除后不能继续被召回。
- 召回结果必须标记为历史记忆，不能与当前实时业务查询混合。
- 第一版不引入 Milvus、独立 Python 服务或 Provider 托管记忆。

## 九、在线搜索与摘要展示执行计划

### 9.1 搜索工具边界

新增能力建议拆为：

~~~text
WebSearchProvider
  -> WebSearchRepository
  -> WebSearchTool
  -> ToolExecutor 统一权限、超时和结果限制
  -> web_search_results 结果块
  -> 正式回答引用
~~~

搜索参数：

~~~text
query
result_limit
recency
domains
language
~~~

搜索结果：

~~~json
{
  "citation_id": "[1]",
  "title": "网页标题",
  "url": "https://example.com/article",
  "snippet": "网页摘要",
  "source_name": "example.com",
  "published_at": "...",
  "retrieved_at": "..."
}
~~~

### 9.2 网络安全要求

- 只允许 HTTP/HTTPS。
- 拒绝环回地址、私有网段、云元数据地址和本机服务地址。
- 限制 DNS 解析结果、重定向次数、响应大小和抓取时间。
- 搜索请求、网页抓取和模型请求使用独立超时。
- 只在结果结构通过校验后交给模型和客户端。
- 审计记录 URL、域名、状态和摘要，不保存外部服务凭据。
- 搜索结果、网页正文、标题、摘要和页面中的指令全部视为不可信数据，不能修改系统规则、工具范围、权限、owner/store 或完成条件。
- 抓取前清理 HTML、脚本、样式、表单、嵌入资源和超长正文；网页中的提示词注入只作为页面内容保留或丢弃，不能进入系统消息或工具参数。
- 每次重定向都重新校验 URL、解析结果和 DNS；禁止环回地址、私有网段、云元数据地址、本机服务地址和 DNS 重绑定。
- 限制允许的 MIME 类型、响应大小、解压后大小、压缩包处理、单页超时、总抓取超时和并发数；不支持的内容类型直接返回结构化失败。
- 搜索缓存按规范化查询、语言、时间范围和安全域名隔离，并按 owner 或调用主体执行限流；缓存命中不能绕过当前 URL 安全校验。
- 来源可信度只能用于排序和展示提示，不能替代事实验证。引用编号必须与实际返回结果一一对应，失效、被清理或未进入正式上下文的来源不得出现在正式回答中。

### 9.3 展示要求

Web 和 Android 增加 web_search_results 结果块，显示：

- 引用编号。
- 网页标题。
- 来源域名。
- 搜索摘要。
- 发布时间和检索时间。
- 安全校验后的打开链接。

正式回答可以使用 [1]、[2] 引用编号。业务数据库结果继续使用当前业务结果块，在线网页结果单独展示。

## 十、分阶段执行顺序

### 阶段 0：基线与契约确认

**目标**：确定当前源码、运行服务、Provider 和测试证据不混淆。

**工作项**：

- 检查 Git、服务版本、数据库类型和部署版本。
- 确认 OneAPI 中实际模型 ID，密钥继续只通过环境变量注入。
- 固化工具事件、终态、错误码和审计字段清单。
- 记录当前 10 条 Agent 重点失败用例作为回归基线。
- 校准文档和台账状态：区分代码已实现、单元测试已验证、真实环境已验证、Blocked、Deferred 和 historical-only，不把历史记录的结论迁移为当前源码结论。

**验收**：

- 旧环境证据与当前源码分别记录。
- Provider 不可用时标记 Blocked 或 Deferred。
- 不产生业务代码变更。

### 阶段 1：编排循环与 native transcript

**目标**：修复创建类工具提前结束和多轮调用记录缺失。

**工作项**：

- 引入运行状态对象和终态枚举。
- 将工具结果、失败、继续、草稿等待和正式回答分开。
- 保存所有轮次的 assistant tool call 与 tool result。
- 依赖查询和目标创建工具使用完成策略。
- 按任务复杂度调整轮次预算，同时保留硬上限。

**重点回归**：009、012、016、041、048、049、051、052、053、054。

**验收**：

- 查询完成但目标工具未完成时不能返回成功语义。
- 每个 native tool call 都有对应结果或结构化失败。
- 草稿生成后不直接写正式业务表。
- 失败测试能核对目标工具、正式回答、审计、消息和清理。

### 阶段 2：统一工具执行边界

**目标**：把 Schema、权限、上下文、确认、超时和审计集中到一个可复用边界。

**工作项**：

- 统一工具执行器。
- 结构化参数错误和字段路径。
- requiredPermission 的真实调用者校验。
- 结果文本、结构化 JSON、来源和总 payload 限制。
- 工具超时、取消、重复调用和审计失败处理。

**验收**：

- 参数非法时不调用业务工具。
- 权限不足时不访问业务 Repository。
- owner/store 条件贯穿工具和 Repository。
- 审计不保存完整敏感参数。

### 阶段 3：Agent 查询数据库分页

**目标**：将过滤、排序、分页和数量统计下推到 Repository。

**优先工具**：

- PaymentLookupTool
- ProductSupplierRelationLookupTool
- InventoryAdjustmentLookupTool
- CrossAnalysisLookupTool
- CustomerProfileLookupTool
- ProductCatalogLookupTool
- CustomerReceivableLookupTool
- SupplierPayableLookupTool

#### 全量工具分页盘点

除上列优先工具外，以下工具也必须逐个检查 Repository 查询是否全量读取后再截断：

~~~text
AccountBalanceLookupTool
AccountHealthLookupTool
AccountTransferLookupTool
CashChangeLookupTool
ImportJobLookupTool
InventoryLedgerLookupTool
InventorySnapshotLookupTool
PartnerGroupLookupTool
PaymentLookupTool
ProductCategoryLookupTool
ProductPriceLevelLookupTool
ProductSupplierRelationLookupTool
CrossAnalysisLookupTool
CustomerProfileLookupTool
InventoryAdjustmentLookupTool
~~~

每个工具最终只能归类为以下三类之一：

| 归类 | 判定要求 |
|---|---|
| 数据库分页 | 过滤、稳定排序、`page`、`size` 和 `count` 均下推到 Repository；查询必须带 owner/store 条件 |
| 固定小结果集 | 只有明确有限的枚举或配置集合才允许保留固定结果，并在代码和测试中证明上限 |
| 明确豁免 | 记录具体原因、最大数据边界和审查人；无法证明固定上限时不得使用豁免 |

阶段验收条件为所有未豁免工具的全量扫描清零。H2 只验证查询语义和分页行为，生产 PostgreSQL 查询计划单独记录为 `Blocked` 或 `Deferred`。

**验收**：

- 空页、负 page、超大 size、过滤和稳定排序测试通过。
- H2 只验证语义和 Repository 行为。
- PostgreSQL EXPLAIN 单独标记 Blocked 或 Deferred。

### 阶段 4：上下文窗口、压缩和检查点

**目标**：替代固定最近 10 条消息，建立预算驱动的会话上下文。

**工作项**：

- ContextWindowResolver。
- TokenEstimator。
- ContextBuilder。
- ContextCompactionService。
- agent_context_checkpoints 表和 Repository。
- context_compacted SSE 事件扩展。
- 检查点复用、失效和并发压缩处理。

**验收**：

- 窗口足够时不触发压缩。
- 超过阈值时只压缩已完成历史轮次。
- 当前问题、待确认草稿和未完成工具链保留。
- 语义压缩失败时当前请求仍能使用确定性摘要。
- 无效摘要不会覆盖有效检查点。
- owner/store 之间不能互相读取检查点。
- 同一边界并发压缩不会产生多个有效副本。

### 阶段 5：Web、Android、iOS 运行轨迹和上下文展示

**目标**：让客户端展示真实的计划、工具、压缩、终态和历史恢复状态。

**工作项**：

- Web 事件解析、轨迹、错误和压缩卡片。
- Android SSE 模型、Reducer、ViewModel 和历史恢复。
- iOS SSE/运行轨迹模型、AgentViewModel 和历史恢复。
- 结果块、工具来源和审计状态对齐。
- 在 AgentChatScreen、DraftListScreen、AgentChatView 和 AgentDraftsView 中复用或补齐草稿确认接口，补齐覆盖式二次确认表达、拒绝与错误状态，不新增绕过 Repository 的正式业务写入入口。
- 增加跨端契约测试。

**验收**：

- 事件重复、乱序、取消和断线有明确状态。
- EXHAUSTED、FAILED、CANCELLED 不显示成功样式。
- 创建类工具的草稿生成、确认、拒绝、关闭、重复确认、权限失效和确认失败均有 Web、Android、iOS 状态断言；只有确认成功后才展示正式业务写入结果。
- Web build、Android 纯单元测试和 iOS XCTest/SwiftUI 状态测试结果真实记录。
- 浏览器点击、adb、真机和模拟器另行标记。

### 阶段 6：长期记忆

**目标**：提供可删除、可审计、按 owner/store 隔离的跨会话记忆。

**依赖**：阶段 4 上下文预算完成，阶段 2 权限边界完成。

**验收**：

- 记忆提取不阻塞回答。
- 记忆来源和敏感级别可见。
- 删除后不能召回。
- 跨 owner/store 召回测试拒绝。
- 第一版使用数据库文本检索，不增加额外服务运行时。

### 阶段 7：在线搜索与摘要结果块

**目标**：增加安全的 Web Search 工具和引用展示。

**依赖**：阶段 2 工具执行边界、阶段 5 多端结果块完成。

**验收**：

- 搜索工具只在任务需要时进入工具范围。
- 搜索失败、超时、无结果和来源失效有结构化状态。
- URL 安全策略拒绝内网和本机地址。
- 搜索摘要、来源和引用在 Web、Android、iOS 中可追溯。
- 未配置搜索 Provider 时标记 Blocked 或 Deferred。

### 阶段 8：真实环境验收

**目标**：验证静态检查和单元测试之外的真实边界。

**项目**：

- 真实 Provider 和指定模型。
- 真实 SSE 长会话。
- 真实跨 owner/store 攻击测试。
- 同店多成员并发运行。
- PostgreSQL EXPLAIN / EXPLAIN ANALYZE。
- Web 真实登录和浏览器交互。
- Android 真机或模拟器。
- iOS 真机或模拟器。

**限制**：环境不可用的项目只能记录 Blocked 或 Deferred，不能用其他数据库、静态分析或 HTTP 200 替代。

## 十一、测试与证据计划

### 11.1 测试分层

| 层级 | 重点内容 | 结果要求 |
|---|---|---|
| 单元测试 | 状态机、完成策略、Schema、权限、Transcript、预算和摘要校验 | 每个分支有稳定断言 |
| Repository 测试 | owner/store、分页、检查点边界、失效、并发唯一约束 | 断言查询参数和数据变化 |
| Controller/API 测试 | 状态码、错误码、响应字段、会话和草稿操作 | 验证 GlobalExceptionHandler 映射 |
| SSE 契约测试 | 事件顺序、重复事件、终态、取消、断线 | 验证事件语义和去重 |
| Agent 真实测试 | 工具选择、参数、执行、回答、审计、消息、数据库和清理 | HTTP 200 不能单独判定通过 |
| 性能测试 | 大列表、工具结果大小、上下文预算、压缩耗时 | 记录查询次数、耗时和 payload |
| 真实环境测试 | Provider、PostgreSQL、跨 owner、同店成员、设备 | 无环境时记录 Blocked/Deferred |

### 11.2 上下文压缩测试用例

| 用例 | 场景 | 关键断言 |
|---|---|---|
| CTX-001 | 历史未超过窗口 | 不调用压缩 Provider，不生成检查点 |
| CTX-002 | 超过阈值且有完整旧轮次 | 只选择最早完整轮次进行压缩 |
| CTX-003 | 当前问题很长 | 当前问题完整保留，超限按 4xx 规则处理 |
| CTX-004 | 当前轮存在未完成工具 | 未完成调用和失败保留，不进入检查点 |
| CTX-005 | 待确认草稿 | 草稿 ID、类型、状态和目标动作保留 |
| CTX-006 | 语义压缩成功 | 保存检查点，记录边界、版本、预算和质量 |
| CTX-007 | Provider 超时 | 当前请求使用确定性摘要，旧检查点不被覆盖 |
| CTX-008 | 输出格式错误 | 标记压缩失败，不能把无效 JSON 注入模型 |
| CTX-009 | 检查点复用 | 读取检查点加边界后原始轮次，顺序正确 |
| CTX-010 | 消息编辑 | 边界之后受影响检查点失效 |
| CTX-011 | 同一边界并发压缩 | 只有一个有效检查点，其他请求复用或安全退出 |
| CTX-012 | 跨 owner 读取 | 无法读取其他 owner 的检查点 |
| CTX-013 | 压缩事件展示 | Web、Android、iOS 展示条数、边界和原因，不展示敏感原文 |
| CTX-014 | 压缩后工具续轮 | tool call 与 tool result 仍能完整配对 |

### 11.3 Agent 工具回归断言

每个创建类 case 至少验证：

~~~text
目标工具是否被选择
额外工具是否被选择
参数是否来自真实查询
工具是否实际执行
工具完成事件是否出现
草稿是否创建
正式业务表是否没有提前写入
正式回答是否符合终态
audit 与 SSE 是否能互相对应
失败、重试和清理是否记录
~~~

草稿确认链额外验证：

~~~text
draft_created 后终态为 CONFIRMATION_PENDING
草稿生成前后正式业务表均无变化
Android 与 iOS 覆盖式确认弹窗展示待写入动作和关键影响字段
确认只调用 drafts/{id}/confirm，不调用正式业务创建接口
确认成功后草稿、审计、业务表和正式结果一致
拒绝、关闭、返回、超时、断线和进程重启不会调用确认接口或写入正式业务表
重复确认、过期草稿、权限或 owner/store 变化均被服务端拒绝或幂等处理
确认和拒绝在 Web、Android、iOS 的原始 HTTP、SSE、审计、数据库前后状态和清理结果均有脱敏证据
~~~

### 11.4 证据目录

继续使用现有目录，不创建第二套测试体系：

~~~text
testing/.artifacts/<日期>-<范围>-<阶段>-<代理ID>/
testing/Agent/
testing/后端/
testing/Web/
testing/安卓/
~~~

每条记录包含：

~~~text
test_id
category_id
wave_id
environment
account/store
precondition
operation
expected
actual
evidence_path
cleanup
result
~~~

结果只使用：

~~~text
Passed
Failed
Blocked
Deferred
~~~

Token、Cookie、密码、私钥、完整认证载荷和模型密钥必须脱敏或完全不落盘。

### 11.5 测试编号与文档状态治理

- 新增后端 Agent 结果继续使用现有 `AG-UT-BE-*`、`AG-FT-BE-*` 和 `AG-PT-BE-*` 分类；不覆盖、不改写旧记录，也不把旧编号重新分配给不同场景。
- 新增记录必须带新的 `wave_id` 或唯一执行后缀，并保留源码版本、部署版本和证据目录，避免把不同服务版本的结果合并。
- 当前结果只允许使用 `Passed`、`Failed`、`Blocked`、`Deferred`。历史台账中已有的中文状态保留为历史记录，不能与当前结果直接混算。

| 文档状态 | 含义 | 允许的证据 |
|---|---|---|
| 代码已实现 | 源码中存在目标行为，但未证明测试覆盖 | 当前源码和提交 |
| 单元测试已验证 | 受控测试验证了目标分支 | 测试报告和测试输出 |
| 真实环境已验证 | 目标服务、Provider、数据库或设备实际运行通过 | 原始 HTTP/SSE/设备/数据库证据 |
| Blocked | 依赖环境或权限不可用，当前无法执行 | 阻塞原因和检查证据 |
| Deferred | 已明确延后到生产迁移、部署或后续阶段 | 延后边界和责任阶段 |
| historical-only | 只代表旧源码或旧部署版本 | 历史证据，不计入当前完成率 |

阶段报告、测试总台账和实现索引在每个阶段结束时完成状态校准；“代码已实现”不能自动升级为“单元测试已验证”或“真实环境已验证”。

## 十二、代码与文档落点

### 后端

| 目标 | 现有或计划位置 |
|---|---|
| 编排循环 | Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java 及新增 Agent component |
| 工具范围和元数据 | .../application/service/v2/agent/component/ToolPlanner.java、.../agent/tool/AgentTool.java |
| 统一执行边界 | .../application/service/v2/agent/tool/ToolRegistry.java 或新增 ToolExecutor |
| 上下文预算 | 新增 .../agent/context/ContextWindowResolver.java、TokenEstimator.java |
| 上下文构建 | 新增 .../agent/context/ContextBuilder.java |
| 上下文压缩 | 新增 .../agent/context/ContextCompactionService.java |
| 检查点仓储 | .../infrastructure/repository/AgentContextCheckpointRepository.java |
| SSE 扩展 | .../agent/component/SseStreamEmitter.java、V2AgentDtos |
| 长期记忆 | 后续新增 .../agent/memory/，按 owner/store 建立 Repository |
| 在线搜索 | 后续新增 .../agent/search/，通过统一工具边界执行 |

### Web

| 目标 | 位置 |
|---|---|
| SSE 事件模型 | Code/frontend/web/src/shared/api/agent-stream.ts |
| 运行轨迹和压缩展示 | Code/frontend/web/src/pages/agent/AgentPage.vue |
| 搜索结果块 | Agent 页面结果块渲染区域和共享契约 |
| 测试 | Code/frontend/web 现有静态测试或新增最小契约测试，不执行浏览器真实点击作为本计划的单元验证 |

### Android

| 目标 | 位置 |
|---|---|
| SSE 模型 | Code/frontend/android/core/model/src/main/java/com/zhihuiji/core/model/v2/agent/stream/AgentStreamModels.kt |
| 运行轨迹 | .../agent/stream/AgentRunTraceModels.kt、AgentRunTraceReducer |
| Repository/API | Code/frontend/android/data/agent/、Code/frontend/android/core/network/ZhihuijiV2Api.kt |
| Agent 页面 | Code/frontend/android/feature/agent/ |
| 草稿二次确认 | Code/frontend/android/feature/agent/src/main/java/com/zhihuiji/feature/agent/conversation/AgentChatScreen.kt、AgentChatViewModel.kt、task/DraftListScreen.kt、task/DraftListViewModel.kt |
| 纯单元测试 | core/model/src/test、core/network/src/test、data/agent/src/test、feature/agent/src/test |

### iOS

| 目标 | 位置 |
|---|---|
| Agent 模型 | Code/frontend/ios/ZhihuijiIOS/Core/Models/AgentModels.swift |
| API 与草稿确认 | Code/frontend/ios/ZhihuijiIOS/Core/API/APIClient.swift |
| Agent 会话与待确认状态 | Code/frontend/ios/ZhihuijiIOS/Features/Agent/AgentViewModel.swift、AgentChatView.swift |
| 草稿列表与确认入口 | Code/frontend/ios/ZhihuijiIOS/Features/Agent/AgentDraftsView.swift、AgentWorkbenchView.swift |
| 纯单元与状态测试 | Code/frontend/ios/ZhihuijiIOSTests/，补充 Agent 草稿确认、拒绝、重复确认和状态恢复测试 |

### 既有文档关系

- [Agent 总体架构](../03_系统设计/Agent系统设计/Agent总体架构.md)：保留现有主链和事件总表，本计划补充未来优化状态。
- [工具选择与执行设计](../03_系统设计/Agent系统设计/工具选择与执行设计.md)：本计划补充统一执行边界、完成策略和 transcript。
- [思考过程与执行过程设计](../03_系统设计/Agent系统设计/思考过程与执行过程设计.md)：本计划补充压缩事件和终态展示规则。
- [对话生命周期设计](../03_系统设计/Agent系统设计/对话生命周期设计.md)：本计划补充检查点复用、失效和长会话上下文。
- [Agent 数据模型](../03_系统设计/数据库设计/Agent数据模型.md)：后续补充检查点、记忆和搜索结果数据表。
- [Agent 测试说明](../05_测试与验收/Agent测试说明.md)：本计划新增上下文压缩、工具状态机和真实 Agent 回归矩阵。
- [Agent 实现索引](Agent实现索引.md)：阶段完成后更新各端实现状态。

## 十三、Git、发布和回滚边界

### 13.1 建议提交边界

1. fix(agent): stabilize orchestration loop and native transcript
2. fix(agent): centralize tool decision permission and result bounds
3. fix(agent): push agent pagination into repositories
4. feat(agent): add context window and compaction checkpoints
5. fix(client): render context events and restore run traces
6. feat(agent): add scoped long term memory
7. feat(agent): add web search result contract
8. test(agent): add end to end regression ledger

每次提交前检查：

~~~text
git diff --cached --name-only
git diff --cached --check
git show --stat --oneline HEAD
~~~

### 13.2 禁止进入提交的对象

- data/server-backups/
- data/server-exports/
- Token、Cookie、密码、私钥和模型密钥。
- web/dist/、node_modules/、APK、JAR 和 Gradle 缓存。
- 没有经过本阶段授权的生产配置、生产迁移和部署文件。

### 13.3 发布策略

- 先启用只读观测字段和上下文预算统计。
- 再灰度启用检查点读取，保留旧的最近消息策略作为明确的降级路径。
- 检查点验证稳定后，再开启语义压缩写入。
- 长期记忆和在线搜索使用独立开关，Provider 不可用时不阻塞普通 Agent 对话。
- 生产迁移、生产部署和 git push 不属于本计划自动执行范围。

## 十四、最终验收标准

### 三要素

- 编排循环有明确状态、完成条件、失败、取消和轮次耗尽处理。
- 工具调用判断经过范围、参数、权限、owner/store、确认和完成策略检查。
- 上下文窗口根据模型预算构建，压缩只处理已完成历史轮次，检查点可复用、可失效、可审计。

### Agent 工具链

- 10 条重点失败用例均有回归记录。
- 查询完成但目标动作未完成时，不返回成功语义。
- native tool call 与 tool result 一一配对。
- CREATE_ONLY 只创建草稿，确认动作才进入正式业务 Service。
- Android 与 iOS 创建类 Agent 工具在草稿生成后以覆盖式确认弹窗表达二次授权；拒绝、关闭或中断不产生正式业务写入，确认成功后才更新正式业务结果。
- 工具输入和输出存在统一边界。

### 上下文

- 长会话不会只依赖最新一条消息截断摘要。
- 压缩失败时当前请求仍有确定性降级路径。
- 无效检查点不会覆盖有效检查点。
- 消息变化、会话删除和 owner/store 边界都有对应处理。
- Web、Android 和 iOS 能展示实际发生的压缩事件。

### 扩展能力

- 长期记忆具备来源、敏感级别、删除和 owner/store 隔离。
- 在线搜索具备 Provider 错误、URL 安全、来源摘要和引用展示。
- 业务数据结果与在线搜索结果使用不同结果块类型。

### 不能替代的真实验证

以下项目必须保留单独状态：

| 项目 | 无环境时的状态 |
|---|---|
| 真实 Provider 和目标模型 | Blocked / Deferred |
| PostgreSQL EXPLAIN / EXPLAIN ANALYZE | Blocked / Deferred |
| 真实跨 owner/store 攻击 | Blocked / Deferred |
| 同店多成员并发 | Blocked / Deferred |
| Web 真实登录与浏览器点击 | Blocked / Deferred |
| Android/iOS 真机、模拟器和设备自动化 | Blocked / Deferred |
| 生产迁移与部署 | Deferred |

## 十五、待确认事项

| 事项 | 需要确认的内容 | 影响阶段 |
|---|---|---|
| Provider 模型 | OneAPI 中 deepseek flash 0731 的准确模型 ID | 阶段 0、8 |
| 在线搜索 Provider | 搜索 API、SearXNG 或其他已授权服务 | 阶段 7 |
| 记忆范围 | 当前 owner、当前 store、用户个人或组合范围 | 阶段 6 |
| 记忆保留 | 过期时间、敏感字段、默认是否自动学习 | 阶段 6 |
| 压缩预算 | Provider 实际上下文窗口和输出预留 | 阶段 4 |
| 历史编辑 | 是否开放消息编辑、删除和分支会话 | 阶段 4、5 |

本文档完成后，后续执行应以阶段顺序、测试台账和本文件的验收条件为准。当前文档只记录方案，未宣称任何新增能力已经通过实现或运行验证。
