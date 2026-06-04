# feature/suppliers 模块开发说明

- 当前状态：列表页+编辑页+详情页已完成，业务链路已走通；B10 本轮已把主操作收口为右下胶囊按钮“新增供应商”。
- 实际源码目录：`feature/suppliers/src/main/java/com/zhihuiji/feature/suppliers`
- 目标：实现供应商列表、状态筛选和编辑。

## 需要创建的类

- `SupplierListScreen`
- `SupplierEditorScreen`
- `SupplierViewModel`

## 需要实现的关键函数

- `SupplierViewModel.loadSuppliers(keyword: String = "", status: Int? = null)`
- `SupplierViewModel.changeStatusFilter(status: Int?)`
- `SupplierViewModel.loadSupplier(id: Long)`
- `SupplierViewModel.saveSupplier()`
- `SupplierViewModel.deleteSupplier(id: Long)`
- `SupplierViewModel.validateForm()`

## 验收标准

- 支持按状态切换供应商列表并保持筛选状态。

## UI 设计规范

- 对照设计图 `04.png` 的供应商列表页和供应商详情页实现（来源见 `docs/design-mockups`）。
- 供应商列表结构与客户列表保持一致，标题 Tab 为“供应商/联系人”。
- 供应商卡片展示供应商名称、编码、联系人、脱敏手机号、应付余额和状态。
- 应付余额使用红色，停用状态使用灰蓝标签，正常状态使用绿色标签。
- 详情页汇总区展示应付余额、信用额度、可用额度。
- 底部固定操作为“联系供应商”“新增采购单”“付款”。
- 当供应商列表嵌入 `ArchivesScreen` 时，页面自身不再重复绘制顶部壳栏，主操作仍保留右下胶囊按钮。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
