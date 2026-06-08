# feature/sales 模块开发说明

- 当前状态：列表页+开单+详情+收款+取消已完成，业务链路已走通；B10 本轮已把列表主操作收口为右下胶囊按钮“销售开单”。
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

- 历史视觉参考曾对照旧设计图 `01.png` 与 `05.png`，当前请优先对照 Stitch 导出与 `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`； 的销售单列表、销售开单、销售单详情、收款页实现。
- 列表页顶部由主壳承载“智慧记 + 销售单”，新增入口落到右下蓝色悬浮按钮；状态 Tab 使用当前 `/v2` 真实状态：全部、草稿、已完成、已取消、已确认（CONFIRMED=3）。
- 当销售列表嵌入 `DocumentsScreen` 时，页面自身不再重复绘制顶部壳栏，主操作仍保留右下胶囊按钮。
- 销售单卡片展示单号、客户、金额、日期和状态标签，底部显示总条数与合计金额。
- 开单页使用分组卡：客户、仓库、业务员、商品明细、备注、金额汇总；商品明细使用 `QuantityStepper`。
- 合计金额使用大号蓝色数字，底部固定“保存草稿/提交订单”。
- 详情页顶部展示单号和状态，汇总卡展示订单金额、已收金额、待收金额，底部固定“作废/修改/收款”。
- 收款页主金额使用超大黑色数字，收款方式和账号使用下拉卡片，底部固定“确认收款”。
- 销售退货页从 `单据中心 -> 退货` 进入 `SalesReturnScreen`，只读取 `/v2/sales-returns` 真实退货单并允许草稿确认；退款登记在后端补充退款明细合同前必须保持禁用，不能伪造退款流水。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
