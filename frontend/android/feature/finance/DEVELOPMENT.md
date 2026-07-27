# feature/finance 模块开发说明

- 当前状态：资金流水列表、资金流水详情、日常支出表单已接入真实后端；资金流水页右下胶囊按钮为“记录支出”，进入 `DailyExpenseScreen`。
- 实际源码目录：`feature/finance/src/main/java/com/zhihuiji/feature/finance`
- 目标：承接新版财务/资金流水入口，当前以 `/v1/finance-records` 的真实收支流水、支出录入、流水详情为主链；账户余额扣减、附件和自定义发生日期需等待后端合同扩展。

## 需要创建的类

- `FinanceRecordListScreen`
- `FinanceRecordDetailScreen`
- `DailyExpenseScreen`
- `FinanceViewModel`
- `DailyExpenseViewModel`

## 需要实现的关键函数

- `FinanceViewModel.loadRecords()`
- `FinanceViewModel.search(keyword: String)`
- `FinanceViewModel.selectTab(index: Int)`
- `DailyExpenseViewModel.submit()`
- `FinanceViewModel.clearError()`

## 验收标准

- 资金流水列表通过 `FinanceRepository.refreshFinanceRecords()` 读取真实 `/v1/finance-records`，支持搜索入口、时间分段视觉、收入/支出分段和账户/方式筛选。
- 日常支出表单通过 `FinanceRepository.createFinanceRecord(CreateFinanceRecordRequest(type=FINANCE_EXPENSE))` 写入真实支出流水；未录入金额时“记录支出”不可提交。
- 不允许伪造账户余额扣减、附件照片或自定义发生日期；当前后端合同只提供 `type/category/partnerName/amount/method/notes`，发生时间由后端写入。

## UI 设计规范

- 对照 Stitch 资金流水、资金流水详情、日常支出设计稿实现；列表嵌入 `DocumentsScreen` 时不重复绘制顶部壳栏。
- 资金流水列表以 `SearchFilterBar + SegmentedTabs + FilterChipRow + BusinessListItem + FloatingGlassActionButton` 组织，主操作为右下“记录支出”。
- 日常支出使用 `GlassScaffold + GlassTopBar + LiquidGlassCard + GlassTextField + BottomActionBar`；必须展示真实合同边界，不能把未支持的附件、账户余额或自定义日期写成已提交能力。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
