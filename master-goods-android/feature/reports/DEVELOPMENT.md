# feature/reports 模块开发说明

- 当前状态：报表页首版已完成，并已按 `UI-DESIGN-SPEC.md` 继续收紧视觉层次、时间标签语义和诚实态文案。
- 实际源码目录：`feature/reports/src/main/java/com/zhihuiji/feature/reports`
- 当前真实依赖：`data/report`
- 目标：实现经营报表页，并明确哪些指标会随时间标签联动、哪些仍是当前快照。

## 需要创建的类

- `ReportScreen`
- `ReportViewModel`
- `ReportUiState`

## 当前已实现的关键函数

- `ReportViewModel.loadReports(period: Int = currentPeriod)`
- `ReportViewModel.setPeriod(period: Int)`
- `ReportViewModel.filterOrdersByPeriod(orders, period)`

## 本轮补强

- 补充总览卡、搜索/筛选、焦点切换、重点回款客户与风险洞察列表，让报表页更接近设计稿中的“高信息密度但可扫读”目标。
- 修复时间区间 Tab 之前只改本地展示、不驱动真实数据刷新的问题；当前 `selectedPeriod` 已下沉到 `ReportViewModel`，切换后会重新按时间范围筛选订单数据并刷新报表状态。
- 继续复用 `GlassTopBar`、`ChartCard`、`KpiCard`、`SearchFilterBar`、`FilterChipRow`、`BusinessListItem`、`EmptyState` 等现有组件，不新增 feature 私有视觉体系。
- 明确保留“销售与应收随时间标签变化，库存成本/账户余额/占位图表仍是当前快照”的边界说明，避免把当前环境下的本地报表 UI 误写成完整动态报表已联调完成。
- 不改 UI 的性能补强：`ReportViewModel` 的往来余额改走 `ReportRepository.reconciliationSummary()`，不再为应收 / 应付金额全量拉取客户和供应商列表；feature 模块也清理为只依赖 `data:report`。

## 验收标准

- 时间区间变化后，销售额、应收和重点回款客户列表能联动刷新。
- 图表、排行和风险分组应统一落在浅蓝毛玻璃报表母版中。
- 本地编译通过后，仍需在 B11 中补真机截图、性能与发布环境证据。

## UI 设计规范

- 对照 Stitch 设计稿 `经营报表 - 多维数据可视化版` 与 `经营报表 (亮色极光玻璃版)` 的报表总览、图表卡、排行卡实现。
- 报表页顶部保持“智慧记 + 报表标题 + 搜索/筛选图标”的结构。
- 报表总览使用日期范围胶囊、2 列 KPI 卡、趋势折线图和经营情况列表。
- 当前实现使用时间分段、KPI 双行布局、环形结构图、低库存缺口图、重点回款客户与风险洞察区块。
- 图表必须放在 `ChartCard` 中；对尚未接入真实序列的数据保持空态或静态说明，不伪装为实时图表。

## 待验证边界

- 当前时间标签驱动销售汇总、利润预估、商品排行和往来汇总；账户余额、低库存、库存月统计仍来自当前快照。
- “销售趋势”仍是诚实态占位，真实趋势序列、坐标与 tooltip 尚未联调。
- 成本与利润仍基于客户端可得库存月统计估算，不代表服务端最终报表口径。

## 下一步

- 真机核对报表首屏 KPI 密度、图表比例和空态层级，继续贴近 Stitch 报表设计稿。
- 如果后端补齐趋势或更细的报表接口，再把现有静态占位图和估算口径替换为真实联动实现。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
