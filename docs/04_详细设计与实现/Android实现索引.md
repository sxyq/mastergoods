# Android 实现索引

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 实现说明 |
| 当前状态 | 已完成（本地构建+测试）；真机 Blocked |
| 适用端 | Android |
| 依据源码 | `Code/frontend/android/` |
| 依据测试 | `Code/frontend/android/**/src/test/`、`testing/安卓/` |
| 依据证据 | `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md`（assembleDebug 通过） |
| 最后核对 | 2026-08-20 |

## 一、模块结构

| 模块 | 说明 | 关键文件 |
|---|---|---|
| app | 应用壳、导航、认证 | `MainActivity.kt`、`ZhihuijiApp.kt`、`navigation/AppNavGraph.kt`、`navigation/MainNavGraph.kt`、`security/RuntimeSecurityGuard.kt` |
| core/common | 格式化、状态、UI 消息 | `core/common/` |
| core/database | Room 数据库、DAO | `core/database/`（ProductDao、SaleOrderDao、Agent DAO 等） |
| core/datastore | 本地会话与偏好 | `core/datastore/` |
| core/designsystem | 颜色、形状、组件、主题 | `core/designsystem/` |
| core/model | API/领域模型与序列化 | `core/model/`（v2/agent/ 含 AgentChatModels、AgentStreamModels、AgentRunTraceModels、AgentResultBlockModels、AgentV2Models、AgentAuditModels） |
| core/network | 网络客户端、SSE、拦截器 | `core/network/`（AgentSseClient.kt、ZhihuijiV2Api.kt、AuthInterceptor.kt、BaseUrlInterceptor.kt、TokenAuthenticator.kt、SafeApiCall.kt） |
| data | Repository 层 | `data/agent/`、`data/auth/`、`data/product/`、`data/customer/`、`data/supplier/`、`data/order/`、`data/finance/`、`data/report/`、`data/sync/` |
| feature | UI 页面 | `feature/agent/`、`feature/auth/`、`feature/dashboard/`、`feature/products/`、`feature/sales/`、`feature/purchases/`、`feature/finance/`、`feature/payments/`、`feature/reports/`、`feature/settings/` 等 |
| backdrop | 毛玻璃效果（第三方） | `backdrop/src/main/java/com/kyant/backdrop/` |
| benchmark | Macrobenchmark | `benchmark/src/main/java/com/zhihuiji/benchmark/`（AppMacrobenchmark、BaselineProfileGenerator、BenchmarkFlows） |

## 二、Agent 模块索引

| 子模块 | 文件 | 职责 |
|---|---|---|
| conversation | `AgentChatScreen.kt`、`AgentChatViewModel.kt`、`AgentWorkbenchScreen.kt`、`AgentWorkbenchViewModel.kt`、`ConversationListPanel.kt` | 对话与工作台 |
| result | `ResultBlockRenderer.kt` | 结果块渲染 |
| task | `DraftListScreen.kt`、`DraftListViewModel.kt`、`TaskNotificationScreen.kt`、`TaskNotificationViewModel.kt` | 草稿与任务 |
| trace | `AgentMarkdownText.kt`、`AgentResponseProvenance.kt` | Markdown 与出处 |

## 三、数据与网络

- SSE：`AgentSseClient.kt`（`Flow<AgentStreamEvent>`）。
- API：`ZhihuijiV2Api.kt`（v2 接口契约）。
- 离线：`AgentPendingMessageRepository.kt`（离线消息队列）。

## 四、测试

- `feature/agent/src/test/`：AgentChatViewModelAnswerMergeTest、AgentChatScreenToolStatusTest、AgentChatNetworkGateTest、AgentWorkbenchHistoryTest、ResultBlockRendererContractTest、AgentStoredResultBlockParseTest、AgentMarkdownTextParserTest、AgentResponseProvenanceTest。
- `core/network/src/test/`：AgentSseClientCancellationTest、ZhihuijiV2ApiContractTest 等。
- `core/model/src/test/`：AgentStreamEventSerializationTest、AgentChatResponseSerializationTest 等。
- `data/agent/src/test/`：AgentV2RepositoryTest。

## 对应实现

- Android 代码：`Code/frontend/android/`
- 后端代码：`Code/backend/`
- iOS 代码：不适用
- Web 代码：不适用
- Agent 代码：`feature/agent/`、`data/agent/`、`core/network/AgentSseClient.kt`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`core/model/`（序列化模型）
- 响应模型：同上
- SSE 事件：`AgentStreamModels.kt`

## 对应测试

- 单元测试：各模块 `src/test/`
- 功能测试：`testing/安卓/功能测试/TEST_PLAN.md`
- 性能测试：`testing/安卓/性能测试/TEST_PLAN.md`、`benchmark/`

## 当前限制

- 未完成内容：IME 顶部栏修复（#19）、长历史滚动优化（#18）、草稿入口（#2）
- Blocked 内容：真机验证（无 adb）
- Deferred 内容：多模态、图片输入
- historical-only 内容：154 环境 Android 证据
