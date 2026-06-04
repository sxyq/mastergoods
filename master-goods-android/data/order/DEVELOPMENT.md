# data/order 模块开发说明

- 当前状态：`SaleOrderRepository` / `PurchaseOrderRepository` / `PayOrderRepository` 已实现 `/v1` 兼容链路；同时已新增 `SaleOrderV2Repository`、`SalesReturnV2Repository`、`PurchaseOrderV2Repository`、`PurchaseReceiptV2Repository`、`PayOrderV2Repository` 承接 `/v2` 首轮合同。
- 实际源码目录：`data/order/src/main/java/com/zhihuiji/data/order`
- 目标：统一封装销售单、采购单、付款单三类单据。

## 需要创建的类

- `SaleOrderRepository`
- `PurchaseOrderRepository`
- `PayOrderRepository`
- `SaleOrderV2Repository`
- `SalesReturnV2Repository`
- `PurchaseOrderV2Repository`
- `PurchaseReceiptV2Repository`
- `PayOrderV2Repository`

## 需要实现的关键函数

- 销售单：
  - `observeSaleOrders(filter: SaleOrderFilter): Flow<List<SaleOrderDto>>`
  - `refreshSaleOrders(filter: SaleOrderFilter)`
  - `getSaleOrder(id: Long): SaleOrderDto`
  - `createSaleOrder(request: CreateSaleOrderRequest): SaleOrderDto`
  - `updateSaleDraft(id: Long, request: UpdateSaleDraftRequest): SaleOrderDto`
  - `addSalePayment(id: Long, amount: BigDecimal, note: String?): PaymentDto`
  - `listSalePayments(id: Long): List<PaymentDto>`
  - `updateSaleStatus(id: Long, status: Int)`
  - `cancelSaleOrder(id: Long): SaleOrderDto`
  - `downloadSalePdf(id: Long)`
- 采购单：
  - `observePurchaseOrders(filter: PurchaseOrderFilter): Flow<List<PurchaseOrderDto>>`
  - `refreshPurchaseOrders(filter: PurchaseOrderFilter)`
  - `getPurchaseOrder(id: Long): PurchaseOrderDto`
  - `createPurchaseOrder(request: CreatePurchaseOrderRequest): PurchaseOrderDto`
- 付款单：
  - `observePayOrders(filter: PayOrderFilter): Flow<List<PayOrderDto>>`
  - `refreshPayOrders(filter: PayOrderFilter)`
  - `getPayOrder(id: Long): PayOrderDto`
  - `createPayOrder(request: CreatePayOrderRequest): PayOrderDto`
  - `updatePayOrderStatus(id: Long, status: Int): PayOrderDto`
- `/v2`：
  - 销售：
    - `listSaleOrders(filter: SaleOrderV2Filter)`
    - `createSaleOrder()/updateDraft()/confirm()/addPayment()/listPayments()/updateStatus()/cancel()`
  - 销售退货：
    - `listSalesReturns()/listByOrder()/createSalesReturn()/updateDraft()/confirm()/addRefund()/cancel()`
  - 采购：
    - `listPurchaseOrders()/createPurchaseOrder()`
  - 采购收货：
    - `listPurchaseReceipts()/listByOrder()/createPurchaseReceipt()/updateDraft()/confirm()/cancel()`
  - 付款：
    - `listPayOrders()/createPayOrder()/updateStatus()`

## 验收标准

- 单据模块需要完整覆盖后端已存在的单据闭环。

## UI 设计规范支撑

- 销售单列表需要提供状态、客户、金额、日期，支撑设计图中的状态 Tab 和卡片列表。
- 销售开单和采购开单需要提供明细行、单价、数量、金额、折扣、优惠、运费和合计。
- 详情页需要提供汇总数字、明细、收款或付款记录，并支持底部固定操作按钮。
- 付款单列表需要提供应付、已付、未付和关联采购单号。

## UI 联动约束

- 本模块虽然不直接负责页面绘制，但其输出的数据结构、状态枚举、错误语义和交互支撑能力必须服务于统一的 Android UI 基线。
- 后续新增业务不能倒逼页面切换成另一套视觉风格；应优先通过补充 `core/designsystem` 通用组件或扩展既有页面母版来承接。
- 需要映射到 UI 的状态、金额、风险、同步结果等，应继续服从统一的颜色语义、状态标签和信息层级。
- Android 视觉真源固定为 `docs/design-mockups/01.png ~ 08.png` 与 `master-goods-android/UI-DESIGN-SPEC.md`。
