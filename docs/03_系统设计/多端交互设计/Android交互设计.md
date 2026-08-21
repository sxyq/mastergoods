# Android 交互设计

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 已完成（本地）/ Blocked（真机） |
| 适用端 | Android |
| 依据源码 | `Code/frontend/android/feature/agent/`、`core/network/AgentSseClient.kt`、`core/model/v2/agent/` |
| 依据测试 | `Code/frontend/android/feature/agent/src/test/`、`core/network/src/test/` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`（assembleDebug 通过） |
| 最后核对 | 2026-08-20 |

## 一、页面结构

| 页面 | 源码 | 说明 |
|---|---|---|
| Agent 工作台 | `AgentWorkbenchScreen.kt` + `AgentWorkbenchViewModel.kt` | 快捷词、最近会话、任务、通知（空态 `clean_entry_ready`） |
| Agent 对话 | `AgentChatScreen.kt` + `AgentChatViewModel.kt` | 消息列表、输入区、会话抽屉、结果块 |
| 会话列表 | `ConversationListPanel.kt` | 历史会话抽屉 |
| 草稿列表 | `DraftListScreen.kt` + `DraftListViewModel.kt` | 草稿管理（路由已注册，真机无可达入口，已知问题 #2） |
| 任务通知 | `TaskNotificationScreen.kt` + `TaskNotificationViewModel.kt` | 任务与通知 |
| 结果块 | `result/ResultBlockRenderer.kt` | KPI/表格/图表渲染 |
| 思考/追踪 | `trace/AgentMarkdownText.kt`、`AgentResponseProvenance.kt` | 思考折叠、回答出处 |

## 二、消息列表

- 用户消息与助手消息按会话展示；助手消息含思考过程（默认折叠）、工具执行过程、正式回答、结果块。
- 8220 基线确认：结果块按消息 part 顺序渲染；思考完成后自动折叠；历史分页恢复首个可见消息位置。
- 已知问题 #12（历史）：run 完成后可能停留在旧会话并显示断连错误（154 环境证据，当前待复测）。

## 三、输入区与发送

- 发送区位于底部（`AgentChatScreen`）。
- 输入：`onInputChange(text)`、`sendMessage()`、`editAndResend()`、`regenerateMessage()`。
- 图片：`uploadImage(uri)` / `removeImageAttachment()` / `generateImage()`（Deferred 相关 UI 存在，多模态链路未启用）。
- 快捷词：工作台快捷问题进入对话。

## 四、键盘与滚动

- 已知问题 #19（历史）：IME 打开后顶部栏零尺寸与状态栏重叠（`WindowInsets`/`imePadding` 待修正）。
- 长历史滚动性能：已知问题 #18（32 消息 jank 16.76%、PSS 增长 32688KB）。

## 五、历史会话与分页

- `loadConversations()` / `loadMoreConversations()` / `loadMoreMessages()` 分页加载。
- `switchConversation(id)` 切换会话；`deleteConversation(id)` 删除。
- `restoreMessagesWithRunTraces()` 恢复消息与运行轨迹（RunTrace 恢复缺失见已知问题 #3）。

## 六、取消与重试

- `stopGeneration()`：调用 `repository.cancelRun(runId)` 取消；取消消息按服务端返回展示。
- `AgentSseClient.retryState`：重试状态（attempt/maxAttempts），UI 展示"重连中... (n/m)"。
- 断网：`queueOfflineMessage()` 离线消息排队；`isQueueableNetworkFailure()` 判定可排队网络失败。

## 七、权限差异

- Android 由后端 `agent:view` / `agent:write` 注解控制；无本地角色矩阵。
- 工作台空态：`clean_entry_ready`（`AgentWorkbenchResponse`）。

## 八、参考图与规范

- 参考图真实位置：`docs/03_系统设计/UI设计/android-agent-reference-collapsed.png`（原 `docs/design/android-agent-reference-collapsed.png` 已移动，旧路径不再有效）。
- UI 规范真源：`Code/frontend/android/UI-DESIGN-SPEC.md`、`Code/frontend/android/DEVELOPMENT-PLAN.md`。

## 对应实现

- Android 代码：`feature/agent/`（conversation/result/task/trace）
- 后端代码：`api/controller/v2/V2AgentController.java`
- iOS 代码：不适用
- Web 代码：不适用
- Agent 代码：`V2AgentAiService`

## 对应接口

- 接口路径：`/v2/agent/*`
- 请求模型：`V2AgentDtos`
- 响应模型：`V2AgentDtos`
- SSE 事件：`SseStreamEmitter`（Android 通过 `AgentSseClient` 消费）

## 对应测试

- 单元测试：`AgentChatViewModelAnswerMergeTest.kt`、`AgentChatScreenToolStatusTest.kt`、`AgentChatNetworkGateTest.kt`、`AgentWorkbenchHistoryTest.kt`、`ResultBlockRendererContractTest.kt`、`AgentSseClientCancellationTest.kt`
- 功能测试：`testing/安卓/功能测试/TEST_PLAN.md`
- 性能测试：`testing/安卓/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：IME 顶部栏修复（#19）、长历史滚动优化（#18）、草稿入口（#2）
- Blocked 内容：真机验证（无 adb）
- Deferred 内容：多模态、图片输入
- historical-only 内容：154 环境 Android 证据
