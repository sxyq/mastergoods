# feature/agent 模块开发说明

- 当前状态：新版 Agent 页面、V2 Repository、真实 SSE 聊天链路、任务/通知接口和草稿归档链路已接入；P0 真实性、审计、性能与端到端证据仍以 `docs/spec/43-ai-assistant-requirements.md` 为准继续验收，P1 草稿真实执行闭环待继续完善。
- 实际源码目录：`feature/agent/src/main/java/com/zhihuiji/feature/agent`
- 目标：从头重建新版 AI 助手模块，包括工作台、聊天、草稿、任务通知。

## 文件清单与状态

| 文件 | 状态 | 说明 |
|---|---|---|
| `AgentWorkbenchScreen.kt` | 已收敛为干净入口 | 仅保留 AI 问候、真实对话入口、远端同步状态和任务 / 通知入口；不展示 KPI、今日摘要、风险提醒、默认排行或报表型图表 |
| `AgentChatScreen.kt` | 已完成 | 消息列表（用户靠右/助手靠左）+ 输入栏 + 停止生成 + 空态 |
| `AgentChatViewModel.kt` | 已接入 V2 SSE | 构造 `AgentChatRequest(stream = true)`，收集 `AgentV2Repository.chatStream()` 的真实后端事件，RunTrace 展开状态已生效 |
| `AgentWorkbenchViewModel.kt` | 已收敛为入口状态 | 只管理问候语、同步状态和错误状态；不消费 workbench 中的报表型字段 |
| `DraftListScreen.kt` | 已保留 | 已迁移到 V2 Repository，UI 明确“仅归档 AI 草稿，不执行业务写入” |
| `DraftListViewModel.kt` | 已保留 | 已迁移到 `AgentV2Repository`，接入真实草稿列表 API；当前 archive 只写 `status = "archived"` |
| `TaskNotificationScreen.kt` | 已保留 | 任务与通知中心接入真实任务/通知接口，网络失败展示错误或真实空态 |
| `TaskNotificationViewModel.kt` | 已保留 | 已移除旧 V1 依赖，调用后端任务/通知列表并回写通知已读状态 |

## 已删除文件

- `AgentViewModel.kt`（旧 V1，mock 数据）
- 旧 V1 `AgentWorkbenchScreen.kt` 实现已被当前同名 V2 工作台重建替换，不再作为 mock 工作台保留。

## 新版关键组件（P1-P4）

- `AgentWorkbenchScreen` + `AgentWorkbenchViewModel`（干净入口，不做报表页副本）
- `AgentChatScreen` + `AgentChatViewModel`
- 富结果组件：`KpiGridCard`、`TableCard`、`RankListCard`、`RiskCard`、`ChartCard`、`DraftCard`
- 过程轨迹组件：`RunTracePanel`、`ToolCallItem`、`SafetyBlockCard`、`ContextCompactedCard`

## 验收标准

- P0：旧 V1 全部物理删除，项目可编译。
- P1：工作台干净入口 + 聊天壳可运行，导航正常。
- P2：真实 SSE 事件 + 过程轨迹 + 富结果展示落地。
- P3：草稿生成与归档语义落地；真实确认执行闭环待 P1 后续实现。
- P4：安全拦截 + 上下文压缩 + 审计记录落地。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
