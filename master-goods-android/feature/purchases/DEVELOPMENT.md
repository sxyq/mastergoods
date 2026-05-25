# feature/purchases 模块开发说明

- 当前状态：列表页+开单+详情已完成，业务链路已走通。
- 实际源码目录：`feature/purchases/src/main/java/com/zhihuiji/feature/purchases`
- 目标：实现采购单列表、开单、详情。

## 需要创建的类

- `PurchaseOrderListScreen`
- `PurchaseOrderEditorScreen`
- `PurchaseOrderDetailScreen`
- `PurchaseOrderViewModel`

## 需要实现的关键函数

- `PurchaseOrderViewModel.loadOrders(filter: PurchaseOrderFilter)`
- `PurchaseOrderViewModel.loadDetail(id: Long)`
- `PurchaseOrderViewModel.addItem(productId: Long)`
- `PurchaseOrderViewModel.removeItem(lineId: String)`
- `PurchaseOrderViewModel.changeQuantity(lineId: String, quantity: BigDecimal)`
- `PurchaseOrderViewModel.selectSupplier(supplierId: Long)`
- `PurchaseOrderViewModel.changeStatus(status: Int)`
- `PurchaseOrderViewModel.submitOrder()`

## 验收标准

- 能创建草稿采购单，也能创建“已收货”采购单。

## UI 设计规范

- 对照设计图 `06.png` 的采购单列表、采购开单、采购单详情实现。
- 采购单列表使用“采购单/退货单”顶部切换，状态 Tab 包含全部、草稿、已提交、部分入库、已完成。
- 列表卡片显示单号、供应商、金额、日期、状态，右下角蓝色浮动按钮为“新建采购单”。
- 采购开单页结构与销售开单一致，但主对象为供应商，合计字段为应付金额。
- 采购详情页明细表需要展示单价、数量、未入库、金额，底部固定“打印/入库/付款”。
- “付款”按钮在采购详情页使用绿色主操作，体现资金流出动作。
