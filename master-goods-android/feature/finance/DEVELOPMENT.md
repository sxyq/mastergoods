# feature/finance 模块开发说明

- 当前状态：列表页+新增流水已完成，业务链路已走通；B10 本轮已把主操作收口为右下胶囊按钮“新增账户”，并补齐了搜索入口与账户/转账分段切换。
- 实际源码目录：`feature/finance/src/main/java/com/zhihuiji/feature/finance`
- 目标：承接新版财务域首轮前台入口，当前以账户总览、转账记录和新增账户为主链；更完整的收支流水、时间筛选、找零与项目能力留待后续扩域。

## 需要创建的类

- `FinanceRecordListScreen`
- `FinanceRecordEditorSheet`
- `FinanceViewModel`

## 需要实现的关键函数

- `FinanceViewModel.loadData()`
- `FinanceViewModel.createAccount(code: String, name: String, type: Int, balance: Double?, notes: String?)`
- `FinanceViewModel.clearCreateSuccess()`
- `FinanceViewModel.clearError()`

## 验收标准

- 搜索账户/转账单号、账户/转账分段切换、新增账户三条链路可用。
- 当前首轮页面真实承接的是 `accounts + transfers` 视图，不再把旧 `finance_records` 录入闭环误写成“已完成”。
- `07.png` 中更完整的收入/支出筛选、时间筛选、现金流摘要与找零/项目动作仍待继续贴合，当前文档不能把这部分写成已完成。

## UI 设计规范

- 对照设计图 `07.png` 的财务页实现（来源见 `docs/design-mockups`），但当前首轮只承接账户/转账主链，不假装已经完整覆盖旧版更厚的财务流水场景。
- 顶部为大标题“智慧记”和二级标题“资金流水”对应的财务入口气质；当前实现以 `GlassTopBar + SearchFilterBar + SegmentedTabs + KPI + 账户/转账卡片` 组织页面。
- 当前 KPI 真实展示的是账户余额与账户数，用来承接 `accounts + transfers` 首轮视图；收入/支出/净流入类摘要仍待后续扩域后再补。
- 当前壳层嵌入 `DocumentsScreen` 时，页面自身不再重复绘制顶部壳栏，主操作仍保留右下胶囊按钮；已补上列表页最低要求的搜索与分段切换，但 `07.png` 的更完整收入/支出筛选、时间筛选与报表化摘要仍待后续继续贴合。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时同时对照：`docs/design-mockups/01.png ~ 08.png`、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`。
