# Android Data 层 - Agent 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 agent 子模块全部 1 个 Kotlin 源文件

---

## 1. AgentRepository

- **文件路径**: `data/agent/src/main/java/com/zhihuiji/data/agent/AgentRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: AI 助手相关功能，包括工作台数据获取、自然语言查询、操作草稿生成与提交、任务管理和通知管理
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用 AI 助手相关 API
- 建议：无

### 函数/方法

##### getWorkbench(windowDays: Int = 7, limit: Int = 6, agingDays: Int = 15): Result<AgentWorkbenchDto>
- 参数：`windowDays: Int = 7` - 时间窗口天数；`limit: Int = 6` - 返回条数限制；`agingDays: Int = 15` - 账龄天数
- 返回值：`Result<AgentWorkbenchDto>` - 工作台数据
- 实现逻辑：委托给 `safeApiCall { api.agentWorkbench(windowDays, limit, agingDays) }`
- 调用关系：调用了 `safeApiCall()`、`api.agentWorkbench()`，被 `DashboardViewModel.loadDashboard()`、`AgentViewModel.loadWorkbench()` 调用
- 建议：无

##### query(question: String): Result<AgentAnswerDto>
- 参数：`question: String` - 用户提问
- 返回值：`Result<AgentAnswerDto>` - AI 回答
- 实现逻辑：委托给 `safeApiCall { api.agentQuery(AgentQueryRequest(question)) }`
- 调用关系：调用了 `safeApiCall()`、`api.agentQuery()`，被 `AgentViewModel.ask()` 调用
- 建议：无

##### generateOperationDraft(instruction: String): Result<OperationDraftDto>
- 参数：`instruction: String` - 操作指令
- 返回值：`Result<OperationDraftDto>` - 操作草稿
- 实现逻辑：委托给 `safeApiCall { api.operationDraft(OperationDraftRequest(instruction)) }`
- 调用关系：调用了 `safeApiCall()`、`api.operationDraft()`
- 建议：当前未被任何 ViewModel 调用，UI 层的快捷操作按钮尚未接入此功能

##### submitOperationDraft(draft: OperationDraftDto): Result<OperationSubmitResultDto>
- 参数：`draft: OperationDraftDto` - 操作草稿
- 返回值：`Result<OperationSubmitResultDto>` - 提交结果
- 实现逻辑：委托给 `safeApiCall { api.operationSubmit(OperationSubmitRequest(draft)) }`
- 调用关系：调用了 `safeApiCall()`、`api.operationSubmit()`
- 建议：当前未被任何 ViewModel 调用

##### createTask(request: CreateAgentTaskRequest): Result<AgentTaskDto>
- 参数：`request: CreateAgentTaskRequest` - 创建任务请求
- 返回值：`Result<AgentTaskDto>` - 创建的任务
- 实现逻辑：委托给 `safeApiCall { api.createAgentTask(request) }`
- 调用关系：调用了 `safeApiCall()`、`api.createAgentTask()`
- 建议：当前未被任何 ViewModel 调用

##### listTasks(): Result<List<AgentTaskSummaryDto>>
- 参数：无
- 返回值：`Result<List<AgentTaskSummaryDto>>` - 任务列表
- 实现逻辑：委托给 `safeApiCall { api.agentTasks() }`
- 调用关系：调用了 `safeApiCall()`、`api.agentTasks()`，被 `AgentViewModel.loadTasks()` 调用
- 建议：无

##### getTask(taskId: Long): Result<AgentTaskDto>
- 参数：`taskId: Long` - 任务 ID
- 返回值：`Result<AgentTaskDto>` - 任务详情
- 实现逻辑：委托给 `safeApiCall { api.agentTask(taskId) }`
- 调用关系：调用了 `safeApiCall()`、`api.agentTask()`
- 建议：当前未被任何 ViewModel 调用

##### listNotifications(unreadOnly: Boolean = false, undeliveredOnly: Boolean = false): Result<List<AgentNotificationDto>>
- 参数：`unreadOnly: Boolean = false` - 仅未读；`undeliveredOnly: Boolean = false` - 仅未送达
- 返回值：`Result<List<AgentNotificationDto>>` - 通知列表
- 实现逻辑：委托给 `safeApiCall { api.notifications(unreadOnly, undeliveredOnly) }`
- 调用关系：调用了 `safeApiCall()`、`api.notifications()`，被 `AgentViewModel.loadNotifications()` 调用
- 建议：无

##### markNotificationRead(id: Long): Result<Unit>
- 参数：`id: Long` - 通知 ID
- 返回值：`Result<Unit>` - 操作结果
- 实现逻辑：委托给 `safeApiCall { api.markNotificationRead(id) }`
- 调用关系：调用了 `safeApiCall()`、`api.markNotificationRead()`
- 建议：当前未被任何 ViewModel 调用

##### markNotificationDelivered(id: Long): Result<Unit>
- 参数：`id: Long` - 通知 ID
- 返回值：`Result<Unit>` - 操作结果
- 实现逻辑：委托给 `safeApiCall { api.markNotificationDelivered(id) }`
- 调用关系：调用了 `safeApiCall()`、`api.markNotificationDelivered()`
- 建议：当前未被任何 ViewModel 调用
