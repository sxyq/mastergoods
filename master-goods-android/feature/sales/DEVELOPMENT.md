# feature/sales 模块开发说明

- 当前状态：列表页+开单+详情+收款+取消已完成，业务链路已走通。
- 实际源码目录：`feature/sales/src/main/java/com/zhihuiji/feature/sales`
- 目标：实现销售单列表、开单、详情、收款、取消。

## 需要创建的类

- `SaleOrderListScreen`
- `SaleOrderEditorScreen`
- `SaleOrderDetailScreen`
- `SalePaymentSheet`
- `SaleOrderListViewModel`
- `SaleOrderEditorViewModel`
- `SaleOrderDetailViewModel`

## 需要实现的关键函数

- `SaleOrderListViewModel.loadOrders(filter: SaleOrderFilter)`
- `SaleOrderListViewModel.updateFilter(...)`
- `SaleOrderEditorViewModel.loadDraft(id: Long?)`
- `SaleOrderEditorViewModel.addItem(productId: Long)`
- `SaleOrderEditorViewModel.removeItem(lineId: String)`
- `SaleOrderEditorViewModel.changeQuantity(lineId: String, quantity: BigDecimal)`
- `SaleOrderEditorViewModel.selectCustomer(customerId: Long)`
- `SaleOrderEditorViewModel.saveDraft()`
- `SaleOrderEditorViewModel.submitOrder()`
- `SaleOrderDetailViewModel.loadDetail(id: Long)`
- `SaleOrderDetailViewModel.addPayment(amount: BigDecimal, note: String?)`
- `SaleOrderDetailViewModel.cancelOrder()`
- `SaleOrderDetailViewModel.exportPdf()`

## 验收标准

- 从选客户、选商品、录数量，到提交销售单、收款、取消都能走通。

## UI 设计规范

- 对照设计图 `01.png` 和 `05.png` 的销售单列表、销售开单、销售单详情、收款页实现（来源见 `docs/design-mockups`）。
- 列表页顶部为“智慧记 + 销售单下拉标题”，右侧搜索和新增图标；状态 Tab 包含全部、待审核、待发货、待收款、已完成、已作废。
- 销售单卡片展示单号、客户、金额、日期和状态标签，底部显示总条数与合计金额。
- 开单页使用分组卡：客户、仓库、业务员、商品明细、备注、金额汇总；商品明细使用 `QuantityStepper`。
- 合计金额使用大号蓝色数字，底部固定“保存草稿/提交订单”。
- 详情页顶部展示单号和状态，汇总卡展示订单金额、已收金额、待收金额，底部固定“作废/修改/收款”。
- 收款页主金额使用超大黑色数字，收款方式和账号使用下拉卡片，底部固定“确认收款”。
