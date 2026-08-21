# iOS 实现索引

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 实现说明 |
| 当前状态 | 待验证 |
| 适用端 | iOS |
| 依据源码 | `Code/frontend/ios/ZhihuijiIOS/` |
| 依据测试 | `Code/frontend/ios/ZhihuijiIOSTests/` |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、工程结构

| 目录 | 说明 | 关键文件 |
|---|---|---|
| App | 应用入口、路由、会话 | `ZhihuijiIOSApp.swift`、`AppRouter.swift`、`AppSession.swift`、`AppEnvironment.swift` |
| Core/API | API 客户端 | `APIClient.swift`、`APIEndpoint.swift`、`APIError.swift` |
| Core/Auth | 认证 | `AuthModels.swift`、`AuthTokenStore.swift`、`Permission.swift`、`PermissionPolicy.swift` |
| Core/Design | 设计系统 | `ZhihuijiTheme.swift`、`Components/`（PrimaryGlassButton、MetricCard、StatusChip、EmptyStateView、LoadingStateView、ViewStyles） |
| Core/Models | 领域模型 | `AgentModels.swift`、`ProductModels.swift`、`SalesModels.swift`、`PurchaseModels.swift`、`FinanceModels.swift`、`InventoryModels.swift`、`ReportModels.swift`、`StoreModels.swift`、`SyncModels.swift`、`MediaModels.swift`、`EntityID.swift`、`DisplayNames.swift` |
| Features | 业务页面 | `Agent/`、`Auth/`、`Dashboard/`、`Archives/`、`Sales/`、`Purchases/`、`Finance/`、`Inventory/`、`Reports/`、`Settings/` 等 |
| ZhihuijiIOSTests | 测试 | `APIClientTests.swift`、`AuthPermissionTests.swift`、`ModelDecodingTests.swift`、各 ViewModel/Policy 测试 |

## 二、Agent 实现索引

| 文件 | 职责 |
|---|---|
| `Features/Agent/AgentChatView.swift` | 对话页（工作台+消息+输入+草稿 sheet+审计 sheet） |
| `Features/Agent/AgentViewModel.swift` | 视图模型 |
| `Features/Agent/AgentWorkbenchView.swift` | 工作台 |
| `Features/Agent/AgentDraftsView.swift` | 草稿 |
| `Features/Agent/AgentTasksView.swift` | 任务 |
| `Features/Agent/AgentAccessPolicy.swift` | 访问策略 |
| `Core/Models/AgentModels.swift` | Agent 模型 |

## 三、页面地图

详见 `Code/frontend/ios/PAGE_MAP.md`（页面 → 接口映射、权限边界、导航对齐说明）。

## 四、测试

- `ZhihuijiIOSTests/`：APIClientSessionTests、APIClientTests、AgentAccessPolicyTests、AppSessionTests、AuthPermissionTests、ModelDecodingTests 及大量 ActionPolicy/ViewModel 测试。

## 对应实现

- iOS 代码：`Code/frontend/ios/ZhihuijiIOS/`
- 后端代码：`Code/backend/`
- Android 代码：不适用
- Web 代码：不适用
- Agent 代码：`Features/Agent/`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`Core/Models/`
- 响应模型：同上
- SSE 事件：未发现流式消费实现（待验证）

## 对应测试

- 单元测试：`ZhihuijiIOSTests/`
- 功能测试：`testing/ios/功能测试/TEST_PLAN.md`
- 性能测试：`testing/ios/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：SSE 流式、Agent 主流程验证
- Blocked 内容：无
- Deferred 内容：多模态
- historical-only 内容：无
