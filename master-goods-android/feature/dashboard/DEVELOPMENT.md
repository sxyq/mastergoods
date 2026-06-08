# feature/dashboard 模块开发说明

- 当前状态：首页经营看板首版已完成，并已按 `UI-DESIGN-SPEC.md` 继续收紧视觉层次与诚实态文案。
- 实际源码目录：`feature/dashboard/src/main/java/com/zhihuiji/feature/dashboard`
- 当前真实依赖：`data/product`、`data/customer`、`data/report`、`data/agent`
- 目标：实现首页经营看板，并明确区分“当前已接入数据”与“仍待联调能力”。

## 需要创建的类

- `DashboardScreen`
- `DashboardViewModel`
- `DashboardUiState`

## 当前已实现的关键函数

- `DashboardViewModel.loadDashboard()`
- `DashboardViewModel.refresh()`

## 当前页面内容

- 经营总览卡：销售额、账户余额、库存预警、回款状态与数据边界说明。
- 搜索/筛选：搜索框 + 焦点胶囊（总览 / 待办提醒 / 库存预警 / 回款重点）。
- 待处理提醒：销售单概览、待收款客户、低库存商品三类提醒。
- 资金与回款：按应收金额排序的重点回款客户列表。
- 快捷开单：销售开单、商品档案、客户跟进、AI 助手入口。
- 低库存预警：低库存商品列表与空态。

## UI 设计规范

- 历史视觉参考曾对照旧设计图 `01.png` 与 `02.png`，当前请优先对照 Stitch 导出与 `docs/spec/42-android-liquid-glass-ui-refactor-plan.md`； 的首页经营看板实现。
- 顶部使用大标题“智慧记”，当前右侧保留刷新与设置入口，不伪装成尚未接入的通知/扫码能力。
- KPI 使用 2 列网格玻璃卡片，包含图标、主数字、较昨日增长或说明。
- 待处理提醒使用列表卡片，左侧彩色图标，右侧状态胶囊和金额层级。

## 本轮补强

- 统一改为更接近设计稿的浅蓝毛玻璃首页信息结构，补充了总览卡、搜索/筛选、待办/资金/库存分组。
- 复用 `GlassTopBar`、`GlassCard`、`KpiCard`、`BusinessListItem`、`SearchFilterBar`、`FilterChipRow`、`StatusPill`、`EmptyState` 等现有组件，避免私有样式漂移。
- 明确补入“当前仅汇总销售单、账户余额、低库存接口”的诚实态文案，不把本地 UI 调整描述成完整经营总览已经联调完成。
- 不改 UI 的性能补强：`receivableAmount` 和 `receivableCustomerCount` 优先使用 `ReportRepository.reconciliationSummary()` 的服务端 SUM / COUNT 字段，只有汇总失败或旧后端缺 count 时才兜底拉客户列表；销售额、订单数和趋势图改走 `ReportRepository.salesSummary()` / `salesTrend()` 服务端聚合，不再为首页拉全量销售订单；`netCashFlow` 改走 `ReportRepository.cashflowSummary()` 后端聚合，并继续保留资金流水收入 / 支出的原有口径，避免用回款 / 付款单口径替代。
- 回归门禁：`DashboardViewModelDependencyTest` 固定 Dashboard 依赖 `ReportRepository` 且不重新引入 `FinanceRepository`；后端性能证据见 `docs/acceptance-evidence/performance/20260609-052957-backend-report-performance/`。

## 验收标准

- 首屏能在一次刷新中聚合报表和提醒数据。
- 页面截图应能明显识别为同一套浅蓝毛玻璃经营工具，而不是功能占位页。
- 本地编译通过后，仍需在 B11 中补真机截图和交互走查证据。

## 待验证边界

- 销售趋势、应收金额、应收客户数和净现金流已优先走服务端汇总；客户列表仅作为旧接口 / 失败兜底。仍需真机记录首页刷新首个可见耗时、frame timing 和截图。
- 页面未接入通知中心或扫码入口；文案与图标需保持诚实态，不把占位能力伪装成已上线功能。
- 视觉已进入收口阶段，但仍需通过真机截图继续核对首屏信息密度、状态色和留白。

## 下一步

- 真机复核首页首屏的信息密度、胶囊层级和卡片留白，继续贴近 `01.png`。
- 如果后端补齐首页报表接口，再把趋势/通知等区块从“诚实态占位”升级成真实联动。

## UI 统一约束

- 本模块后续新增业务必须继续复用当前设计图对应的页面母版，不允许因为领域变厚就切换成另一套视觉语言。
- 页面结构优先落入既有模式：列表页、详情页、编辑页、报表页、AI 页、设置页。
- 视觉基线固定为：浅蓝渐变背景、玻璃卡片、蓝色主按钮、白色次按钮、统一状态标签、五栏主壳。
- 如需新增 UI 组件，先沉淀到 `core/designsystem`，再由本模块复用；不允许长期保留 feature 私有样式组件。
- 验收时当前优先对照：`docs/spec/42-android-liquid-glass-ui-refactor-plan.md`、Stitch 导出清单、`master-goods-android/UI-DESIGN-SPEC.md`、`docs/technical-analysis/android/core/designsystem/README.md`；`docs/design-mockups/` 仅作历史参考。
