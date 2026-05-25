# feature/agent 模块开发说明

- 当前状态：脚手架已创建，页面未开始。
- 实际源码目录：`feature/agent/src/main/java/com/zhihuiji/feature/agent`
- 目标：实现 AI 工作台、问答、操作草稿、任务、通知。

## 需要创建的类

- `AgentWorkbenchScreen`
- `AgentChatScreen`
- `OperationDraftScreen`
- `AgentTaskScreen`
- `NotificationScreen`
- `AgentViewModel`

## 需要实现的关键函数

- `AgentViewModel.loadWorkbench()`
- `AgentViewModel.ask(question: String)`
- `AgentViewModel.createOperationDraft(input: String)`
- `AgentViewModel.submitDraft()`
- `AgentViewModel.loadTasks()`
- `AgentViewModel.loadTaskDetail(taskId: Long)`
- `AgentViewModel.loadNotifications(unreadOnly: Boolean = false)`
- `AgentViewModel.markRead(notificationId: Long)`
- `AgentViewModel.markDelivered(notificationId: Long)`
- `AgentViewModel.connectSseIfNeeded()`

## 验收标准

- 第一阶段至少先完成非流式问答和通知列表。

## UI 设计规范

- 对照设计图 `01.png` 和 `08.png` 的 AI 工作台、AI 问答、操作草稿、任务与通知实现。
- AI 工作台顶部使用机器人头像、问候语和 KPI 四宫格，下面是经营洞察、快捷操作和大家都在问。
- AI 问答页使用聊天气泡布局，用户消息靠右，助手消息靠左；助手回答中可嵌入报表卡片、趋势图和建议行动。
- 操作草稿页使用分类 Tab、仅看我创建复选框、排序入口和草稿卡片；卡片底部有“编辑/提交”。
- 任务与通知页使用“任务/通知”Tab 和状态筛选 Chip，任务卡片展示进度条、状态、时间和结果。
- 助手入口在底部导航激活态使用蓝色圆形强调。
