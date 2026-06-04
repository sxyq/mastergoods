# core/model 模块开发说明

- 当前状态：脚手架已创建，模型定义未开始。
- 实际源码目录：`core/model/src/main/java/com/zhihuiji/core/model`
- 目标：集中定义网络 DTO、领域模型、筛选条件和表单草稿对象。

## 需要创建的模型文件

- `AuthModels`
- `ProductModels`
- `PartyModels`
- `OrderModels`
- `FinanceModels`
- `ReportModels`
- `AgentModels`
- `SyncModels`

## 需要覆盖的核心模型

- 认证：`LoginRequest`、`RegisterRequest`、`RefreshRequest`、`AuthResult`、`UserProfile`
- 商品：`ProductDto`、`ProductDraft`、`ProductAdjustStockRequest`
- 客户供应商：`CustomerDto`、`SupplierDto`
- 销售：`SaleOrderDto`、`SaleOrderItemDto`、`CreateSaleOrderRequest`、`UpdateSaleDraftRequest`、`PaymentDto`
- 采购：`PurchaseOrderDto`、`CreatePurchaseOrderRequest`
- 付款：`PayOrderDto`、`CreatePayOrderRequest`
- 财务：`FinanceRecordDto`、`CreateFinanceRecordRequest`
- 报表：`SalesSummaryReportDto`、`ProfitSummaryReportDto`、`ReconciliationSummaryReportDto` 及明细项
- Agent：`AgentWorkbenchDto`、`AgentAnswerDto`、`OperationDraftDto`、`AgentTaskDetailDto`、`AgentNotificationDto`
- 同步：`PullRequest`、`PullResult`、`UploadRequest`、`SyncHealthResult`

## 需要实现的关键函数

- 每个筛选模型需要 `toQueryMap()` 或等价转换方法。
- 每个表单草稿需要 `toRequest()`。
- 需要统一定义状态值常量，避免魔法数字散落到 UI。

## UI 设计规范支撑

- 状态常量必须覆盖 `StatusPill` 所需状态：正常、低库存、缺货、待收款、已完成、作废、部分入库、待付款、进行中、失败。
- 筛选模型要能支撑设计图中的 Tab 和 Chip，但文案必须绑定真实 `/v2` 状态，不允许直接沿用泛化旧词；例如销售列表当前使用：全部、草稿、已完成、已取消、已确认（CONFIRMED=3）。
- 金额字段不要在模型层转字符串，保留数值，交给 `MoneyFormatter` 和 UI 组件处理。

## 验收标准

- 所有 Repository 和 ViewModel 都只依赖这里的模型定义。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
