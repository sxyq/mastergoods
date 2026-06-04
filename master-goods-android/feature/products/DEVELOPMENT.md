# feature/products 模块开发说明

- 当前状态：列表页、详情页、编辑页、库存调整已落地；本轮已补商品“列表 -> 详情 -> 编辑”路径，避免继续用“点列表即进编辑”代替详情页。
- 实际源码目录：`feature/products/src/main/java/com/zhihuiji/feature/products`
- 目标：实现商品列表、商品编辑、库存调整。

## 需要创建的类

- `ProductListScreen`
- `ProductEditorScreen`
- `ProductDetailScreen`
- `StockAdjustSheet`
- `ProductListViewModel`
- `ProductEditorViewModel`

## 需要实现的关键函数

- `ProductListViewModel.loadProducts(keyword: String = "")`
- `ProductListViewModel.refresh()`
- `ProductListViewModel.deleteProduct(id: Long)`
- `ProductEditorViewModel.loadProduct(id: Long?)`
- `ProductEditorViewModel.saveProduct()`
- `ProductEditorViewModel.adjustStock(delta: BigDecimal, reason: String?)`
- `ProductEditorViewModel.findByCode(code: String)`
- `ProductEditorViewModel.validateForm()`

## 验收标准

- 商品搜索、创建、编辑、删改库存都能独立完成。

## UI 设计规范

- 对照设计图 `01.png` 和 `03.png` 的商品库存、商品列表、商品详情、编辑商品、库存调整实现（来源见 `docs/design-mockups`）。
- 商品列表顶部为标题、搜索框、筛选入口和分类 Chip，右下角使用“新增商品”蓝色浮动按钮。
- 列表项使用玻璃卡片，左侧商品图，右侧显示编码、名称、库存、安全库存、售价和状态标签。
- 商品详情首卡展示商品图、编码、名称、条码、分类和状态，下面分基础信息、库存信息、其他信息卡。
- 编辑页使用表单分组卡：基本信息、价格设置、库存设置，底部固定“取消/保存”。
- 库存调整页顶部展示商品摘要，中间为入库/出库/盘盈/盘亏分段按钮，底部使用调整确认面板。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
