# iOS 交互设计

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 系统设计 |
| 当前状态 | 待验证 |
| 适用端 | iOS |
| 依据源码 | `Code/frontend/ios/ZhihuijiIOS/Features/Agent/`、`Core/Models/AgentModels.swift`、`Core/API/`、`App/AppRouter.swift` |
| 依据测试 | `Code/frontend/ios/ZhihuijiIOSTests/`、`testing/ios/功能测试/TEST_PLAN.md` |
| 依据证据 | 无 8220 运行证据（Agent 主流程本轮未展开） |
| 最后核对 | 2026-08-20 |

## 一、页面结构（真实源码）

| 页面 | 源码 | 说明 |
|---|---|---|
| Agent 对话 | `Features/Agent/AgentChatView.swift` | 工作台区 + 消息区 + 输入区 + 草稿 sheet + 审计 sheet |
| Agent 视图模型 | `Features/Agent/AgentViewModel.swift` | 会话/消息/草稿/任务/通知状态 |
| Agent 工作台 | `Features/Agent/AgentWorkbenchView.swift` | 快捷问题、KPI、风险提示 |
| 草稿 | `Features/Agent/AgentDraftsView.swift` | 草稿列表（`pendingDraftSection` 前缀 4 条） |
| 任务 | `Features/Agent/AgentTasksView.swift` | 任务列表（前缀 4 条） |
| 权限 | `Features/Agent/AgentAccessPolicy.swift` | Agent 访问策略 |

## 二、消息区与气泡

- `AgentChatView.swift`：`ScrollView` + `ForEach(viewModel.messages)`。
- `userBubble(_ message:)`：用户消息气泡。
- `assistantBubble(...)`：助手消息，`structuredData.prefix(2)` 展示前 2 个结果块（`resultBlockCompactView`）。
- `resultBlockView(_ block:)`：KPI（kpis 枚举）、表格（headers/rows）、列表（items.prefix(6)）、`draft_card`（AgentDraftCardBlock）。

## 三、导航

- `AppRouter.swift` 管理路由；`AgentChatView` 通过 `.navigationTitle("AI 助手")` 与 `NavigationLink`（运行审计、编辑草稿 sheet）导航。
- 底部导航严格对齐 Android：首页/单据/档案/报表/助手（`Code/frontend/ios/PAGE_MAP.md`）。

## 四、输入区

- `AgentChatView.swift` 输入区：`TextField("输入问题", axis: .vertical)`。
- 发送按钮：`disabled(viewModel.isSending || draftQuestion.nilIfBlank == nil)`。
- 存草稿按钮：`saveQuestionAsDraft`。

## 五、历史会话

- `viewModel.conversations` 横向滚动（`ScrollView(.horizontal)`）。
- 消息历史恢复：`AgentViewModel` 加载消息（源码存在）。
- **与 Exyte/Chat 类似的历史会话交互（会话列表 + 消息流 + 输入区组合）**：当前以原生 ScrollView + 自定义气泡实现，未发现引入 Exyte/Chat 库的证据；`PAGE_MAP.md` 只作为设计参考，不改变源码事实。

## 六、SSE 流式

- **未发现 iOS SSE 流式消费（`text/event-stream` / EventSource / URLSession streaming）实现证据**——标记待验证。
- 会话恢复、工具过程、思考折叠、流式增量均需在源码中确认后才可标记完成。

## 七、键盘与滚动

- 未发现 `keyboard` 相关适配证据；`ScrollView` 常规滚动。

## 八、权限与角色

- `Core/Auth/PermissionPolicy.swift`：角色权限矩阵（对齐后端 StoreAccessPolicy）。
- `AgentAccessPolicy.swift`：Agent 页面访问策略。
- `ASSISTANT` 展示为"AI/只读助理"（PAGE_MAP.md）。

## 对应实现

- iOS 代码：`Features/Agent/`、`Core/Models/AgentModels.swift`
- 后端代码：`api/controller/v2/V2AgentController.java`
- Android 代码：不适用
- Web 代码：不适用
- Agent 代码：`V2AgentAiService`

## 对应接口

- 接口路径：`/v2/agent/*`
- 请求模型：`V2AgentDtos`
- 响应模型：`V2AgentDtos`
- SSE 事件：待验证（未发现流式消费实现）

## 对应测试

- 单元测试：`ZhihuijiIOSTests/AgentAccessPolicyTests.swift`、`ModelDecodingTests.swift`
- 功能测试：`testing/ios/功能测试/TEST_PLAN.md`
- 性能测试：`testing/ios/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：SSE 流式消费、思考折叠、工具过程展示的实现验证；Agent 主流程测试
- Blocked 内容：无（缺运行环境与证据）
- Deferred 内容：多模态
- historical-only 内容：无（iOS 无 154 证据）
