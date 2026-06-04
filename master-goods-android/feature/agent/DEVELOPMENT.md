# feature/agent 模块开发说明

- 当前状态：AI 工作台、AI 问答、操作草稿、任务与通知首版页面已完成，已接入主导航子路由；本轮继续把工作台/任务/通知/草稿页的假数据占位收口为明确的待联调空态，避免把缺失端点误看成已接入能力。当前真实接通的是 conversation/message/draft 流程，workbench/task/notification 聚合端点仍未完成。
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

- 已完成：
  - AI 工作台首页
  - 非流式 AI 问答页
  - 操作草稿页（生成草稿 / 提交草稿）
  - 任务与通知中心页
- 待完善：
  - 工作台聚合总览的真实后端数据
  - 任务详情页 / 通知详情页
  - 任务与通知列表的真实后端数据
  - SSE 实时更新

## UI 设计规范

- 对照设计图 `01.png` 和 `08.png` 的 AI 工作台、AI 问答、操作草稿、任务与通知实现（来源见 `docs/design-mockups`）。
- AI 工作台顶部使用机器人头像、问候语和 KPI 四宫格，下面是经营洞察、快捷操作和大家都在问。
- AI 问答页使用聊天气泡布局，用户消息靠右，助手消息靠左；助手回答中可嵌入报表卡片、趋势图和建议行动。
- 操作草稿页使用分类 Tab、仅看我创建复选框、排序入口和草稿卡片；卡片底部有“编辑/提交”。
- 任务与通知页使用“任务/通知”Tab 和状态筛选 Chip，任务卡片展示进度条、状态、时间和结果。
- 助手入口在底部导航激活态使用蓝色圆形强调。
- 当 `/v2/agent` 缺少 workbench/task/notification 聚合端点时，页面必须回落为诚实的空态或待联调态，不再伪造 KPI、任务进度、通知结果或示例草稿编号。
- 问答与草稿页允许展示真实 conversation/message/draft 返回值，但结构化结果只按已返回字段渲染，不追加假图表或假业务结论。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
