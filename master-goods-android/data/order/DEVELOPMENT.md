# data/order 模块开发说明

- 当前状态：三个 Repository 已实现（SaleOrderRepository/PurchaseOrderRepository/PayOrderRepository），采用在线优先 + Room 列表缓存。
- 实际源码目录：`data/order/src/main/java/com/zhihuiji/data/order`
- 目标：统一封装销售单、采购单、付款单三类单据。

## 需要创建的类

- `SaleOrderRepository`
- `PurchaseOrderRepository`
- `PayOrderRepository`

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

## 验收标准

- 单据模块需要完整覆盖后端已存在的单据闭环。

## UI 设计规范支撑

- 销售单列表需要提供状态、客户、金额、日期，支撑设计图中的状态 Tab 和卡片列表。
- 销售开单和采购开单需要提供明细行、单价、数量、金额、折扣、优惠、运费和合计。
- 详情页需要提供汇总数字、明细、收款或付款记录，并支持底部固定操作按钮。
- 付款单列表需要提供应付、已付、未付和关联采购单号。
