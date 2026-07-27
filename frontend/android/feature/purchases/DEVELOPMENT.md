# feature/purchases 模块开发说明

- 当前状态：采购单列表、开单、详情和采购入库已接入。
- 实际源码目录：`feature/purchases/src/main/java/com/zhihuiji/feature/purchases`
- 目标：实现采购单列表、开单、详情，并通过 `/v2/purchase-receipts` 承接采购入库。

## 已接入的类

- `PurchaseOrderListScreen`
- `PurchaseOrderEditScreen`
- `PurchaseOrderDetailScreen`
- `PurchaseOrderListViewModel`
- `PurchaseOrderEditViewModel`
- `PurchaseOrderDetailViewModel`
- `PurchaseReceiptScreen`
- `PurchaseReceiptViewModel`

## 已接入的关键函数

- `PurchaseOrderListViewModel.loadOrders()`
- `PurchaseOrderListViewModel.selectTab(index: Int)`
- `PurchaseOrderDetailViewModel.loadOrder()`
- `PurchaseOrderDetailViewModel.deleteOrder(onDeleteSuccess: () -> Unit)`
- `PurchaseOrderEditViewModel.addItem()`
- `PurchaseOrderEditViewModel.removeItem(index: Int)`
- `PurchaseOrderEditViewModel.saveOrder()`
- `PurchaseReceiptViewModel.loadReceipts()`
- `PurchaseReceiptViewModel.selectReceipt(id: Long)`
- `PurchaseReceiptViewModel.confirmSelectedReceipt()`

## 验收标准

- 能创建草稿采购单，也能创建“已收货”采购单。
- 能从单据中心进入采购入库，读取后端返回的入库单并执行确认入库；仓库字段和部分收货数量编辑需后端合同补齐后再放开。

## UI 设计规范

- 历史视觉参考曾对照旧设计图 `06.png`，当前请优先对照 Stitch 导出与 `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`。
- 采购单列表使用主壳“采购单”入口，状态 Tab 当前按 `/v2` 首轮真实状态收口为全部、草稿、已收货，右下角蓝色浮动按钮为“新建采购单”。
- 列表卡片显示单号、供应商、金额、日期、状态，右下角蓝色浮动按钮为“新建采购单”。
- 采购开单页结构与销售开单一致，但主对象为供应商，合计字段为应付金额。
- 采购详情页明细表需要展示单价、数量、未入库、金额，底部固定“打印/入库/付款”。
- 采购入库页从单据中心顶栏“入库”进入，只展示 `/v2/purchase-receipts` 可证实字段；当前“部分收货”按钮禁用，避免在没有数量拆分合同前写入不可验证状态。
- “付款”按钮在采购详情页使用绿色主操作，体现资金流出动作。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
