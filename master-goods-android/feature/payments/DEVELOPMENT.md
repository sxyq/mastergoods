# feature/payments 模块开发说明

- 当前状态：列表页+新建+详情+状态更新已完成，业务链路已走通；B10 本轮已把列表主操作收口为右下胶囊按钮“新建付款单”。
- 实际源码目录：`feature/payments/src/main/java/com/zhihuiji/feature/payments`
- 目标：实现付款单列表、详情、创建和状态更新。

## 需要创建的类

- `PayOrderListScreen`
- `PayOrderEditorScreen`
- `PayOrderDetailScreen`
- `PayOrderViewModel`

## 需要实现的关键函数

- `PayOrderViewModel.loadOrders(filter: PayOrderFilter)`
- `PayOrderViewModel.loadDetail(id: Long)`
- `PayOrderViewModel.selectSupplier(supplierId: Long)`
- `PayOrderViewModel.submitOrder()`
- `PayOrderViewModel.updateStatus(id: Long, status: Int)`
- `PayOrderViewModel.validateForm()`

## 验收标准

- 付款单状态切换后，前端要能及时刷新余额相关展示。

## UI 设计规范

- 对照设计图 `06.png` 的付款单列表实现（来源见 `docs/design-mockups`）。
- 顶部标题由主壳承载“付款单”，状态 Tab 使用当前 `/v2` 真实状态：全部、待付款、已付款、已取消；右下角使用蓝色浮动按钮“新建付款单”。
- 搜索框支持单号、供应商、备注，右侧保留筛选按钮。
- 付款单卡片展示付款单号、供应商、关联单号、应付金额、已付金额、日期和状态。
- 右下角使用蓝色浮动按钮“新建付款单”。
- 当付款单列表嵌入 `DocumentsScreen` 时，页面自身不再重复绘制顶部壳栏，主操作仍保留右下胶囊按钮。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
