# feature/customers 模块开发说明

- 当前状态：列表页+编辑页+详情页已完成，业务链路已走通；B10 本轮已把主操作收口为右下胶囊按钮“新增客户”。
- 实际源码目录：`feature/customers/src/main/java/com/zhihuiji/feature/customers`
- 目标：实现客户列表和客户编辑。

## 需要创建的类

- `CustomerListScreen`
- `CustomerEditorScreen`
- `CustomerViewModel`

## 需要实现的关键函数

- `CustomerViewModel.loadCustomers(keyword: String = "")`
- `CustomerViewModel.loadCustomer(id: Long)`
- `CustomerViewModel.saveCustomer()`
- `CustomerViewModel.deleteCustomer(id: Long)`
- `CustomerViewModel.validateForm()`

## 验收标准

- 客户搜索、新建、编辑、删除闭环完整。

## UI 设计规范

- 历史视觉参考曾对照旧设计图 `04.png`，当前请优先对照 Stitch 导出与 `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`； 的客户列表页和客户详情页实现。
- 列表页顶部大标题“智慧记”，下方使用“客户/联系人”Tab、搜索框、状态筛选和筛选图标。
- 客户卡片显示名称、编码、联系人、脱敏手机号、应收余额和客户状态；状态语义与 Tab 保持一致：正常、欠款、已停用，不复用供应商“启用/停用”标签。
- 当客户列表嵌入 `ArchivesScreen` 时，页面自身不再重复绘制顶部壳栏，主操作仍保留右下胶囊按钮。
- 详情页顶部主卡展示客户图标、名称、编码、状态、联系人和地址。
- 详情汇总用三列或三卡展示应收余额、信用额度、可用额度。
- 底部固定操作为“联系客户”“新增销售单”“收款”，主操作按钮为蓝色。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
