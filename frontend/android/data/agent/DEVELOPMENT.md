# data/agent 模块开发说明

- 当前状态：P0 已完成（旧 V1 已删除），P1 进行中；workbench/chat/tasks/notifications/drafts 已改走 V2 数据层，测试 fake 仅用于单元隔离。
- 实际源码目录：`data/agent/src/main/java/com/zhihuiji/data/agent`
- 目标：封装新版 AI 助手的数据访问层，包括会话、消息、草稿、流式聊天。

## 现有类

- `AgentV2Repository`（保留并扩展）：对接 V2 API，负责 conversation / message / draft CRUD。

## 已删除类

- `AgentRepository`（旧 V1，已物理删除）。

## 需要实现的关键函数（新版）

- `AgentV2Repository.chatStream(request: AgentChatRequest): Flow<AgentStreamEvent>` — 流式 SSE 聊天，始终按 `stream=true` 发送。
- `AgentV2Repository.getWorkbench(): Result<AgentWorkbenchV2Dto>` — 工作台聚合数据。
- `AgentV2Repository.listDrafts(conversationId: Long? = null): Result<List<AgentDraftDto>>` — 草稿列表。
- `AgentV2Repository.deleteDraft(id: Long): Result<Unit>` — 删除草稿。

## 验收标准

- 旧 V1 相关引用全部清理完毕，项目可编译。
- `AgentV2Repository` 能独立支撑新版 Agent 所有数据需求。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 当前视觉真源以 Stitch 导出、`docs/spec/42-android-liquid-glass-ui-refactor-plan.md` 与 `master-goods-android/UI-DESIGN-SPEC.md` 为准；`docs/design-mockups/` 仅作历史参考。
