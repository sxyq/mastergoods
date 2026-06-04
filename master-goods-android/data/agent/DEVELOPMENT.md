# data/agent 模块开发说明

- 当前状态：脚手架已创建，仓储未开始。
- 实际源码目录：`data/agent/src/main/java/com/zhihuiji/data/agent`
- 目标：封装 AI 工作台、问答、操作草稿、任务、通知。

## 需要创建的类

- `AgentRepository`
- `NotificationStreamRepository`

## 需要实现的关键函数

- `getWorkbench(windowDays: Int, limit: Int, agingDays: Int): AgentWorkbenchDto`
- `query(question: String, conversationId: String?): AgentAnswerDto`
- `generateOperationDraft(input: String): OperationDraftDto`
- `submitOperationDraft(draftId: String?, payload: OperationSubmitRequest): OperationSubmitResultDto`
- `createTask(request: CreateAgentTaskRequest): AgentTaskSummaryDto`
- `listTasks(): List<AgentTaskSummaryDto>`
- `getTask(taskId: Long): AgentTaskDetailDto`
- `listNotifications(unreadOnly: Boolean, undeliveredOnly: Boolean): List<AgentNotificationDto>`
- `markNotificationRead(id: Long): AgentNotificationDto`
- `markNotificationDelivered(id: Long): AgentNotificationDto`
- `connectNotificationStream()`
  - 第二阶段接入 SSE。

## 验收标准

- 第一阶段至少要让工作台、问答、草稿、任务、通知列表跑通。

## UI 设计规范支撑

- 工作台需要返回 KPI、经营洞察、回款提醒、库存预警和快捷操作所需数据。
- 问答结果需要保留结构化分析块，支撑聊天页中的报表卡片和建议行动卡。
- 任务需要提供进度、状态、开始时间、完成时间和错误信息。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
