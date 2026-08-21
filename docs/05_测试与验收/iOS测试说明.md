# iOS 测试说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 测试验收 |
| 当前状态 | 待验证 |
| 适用端 | iOS |
| 依据源码 | `Code/frontend/ios/ZhihuijiIOSTests/` |
| 依据测试 | `testing/ios/功能测试/TEST_PLAN.md`、`testing/ios/单元测试/TEST_PLAN.md`、`testing/ios/性能测试/TEST_PLAN.md` |
| 依据证据 | 无 8220 运行证据 |
| 最后核对 | 2026-08-20 |

## 一、单元测试覆盖（源码测试）

| 类别 | 测试 |
|---|---|
| API | `APIClientTests.swift`、`APIClientSessionTests.swift` |
| 会话 | `AppSessionTests.swift` |
| 权限 | `AuthPermissionTests.swift`、`AgentAccessPolicyTests.swift`、`InlinePermissionAuditTests.swift`、各 ActionPolicyTests |
| 模型 | `ModelDecodingTests.swift` |
| 页面 | `LoginViewModelTests.swift`、`DashboardViewModelTests.swift`、`FinanceRecordViewModelTests.swift`、`PayOrderDetailViewModelTests.swift`、`InventoryAdjustViewModelTests.swift`、`PurchaseEditViewModelTests.swift`、`MediaAssetsViewModelTests.swift`、`PlanningOverviewViewModelTests.swift`、`CustomerDetailViewModelTests.swift` 等 |

## 二、功能与性能测试

- `testing/ios/功能测试/TEST_PLAN.md` + `functional_feature_matrix.csv`。
- `testing/ios/性能测试/TEST_PLAN.md` + `performance_scope_matrix.csv`。
- 本轮 Agent 主流程未展开（`testing/Agent/功能测试/TEST_PLAN.md`）。

## 三、当前状态

- 单元测试源码存在（策略、模型解码覆盖较好）。
- Agent 对话/SSE/结果块运行验证：待验证。

## 对应实现

- iOS 代码：`Code/frontend/ios/ZhihuijiIOS/`
- 后端代码：不适用
- Android 代码：不适用
- Web 代码：不适用
- Agent 代码：`Features/Agent/`

## 对应接口

- 接口路径：`/v2/*`
- 请求模型：`Core/Models/`
- 响应模型：同上
- SSE 事件：未发现消费实现（待验证）

## 对应测试

- 单元测试：`ZhihuijiIOSTests/`
- 功能测试：`testing/ios/功能测试/TEST_PLAN.md`
- 性能测试：`testing/ios/性能测试/TEST_PLAN.md`

## 当前限制

- 未完成内容：Agent 主流程测试、SSE 消费验证
- Blocked 内容：无
- Deferred 内容：多模态
- historical-only 内容：无
