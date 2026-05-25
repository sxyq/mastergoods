# feature/reports 模块开发说明

- 当前状态：脚手架已创建，页面未开始。
- 实际源码目录：`feature/reports/src/main/java/com/zhihuiji/feature/reports`
- 目标：实现经营报表页和四个主 Tab。

## 需要创建的类

- `ReportScreen`
- `SalesReportTab`
- `ProfitReportTab`
- `InventoryFlowTab`
- `ReconciliationTab`
- `ReportViewModel`

## 需要实现的关键函数

- `ReportViewModel.loadSalesSummary(range: DateRange)`
- `ReportViewModel.loadProfitSummary(range: DateRange)`
- `ReportViewModel.loadInventoryFlow(range: DateRange, limit: Int = 20)`
- `ReportViewModel.loadReconciliation(range: DateRange)`
- `ReportViewModel.loadTopProducts(range: DateRange)`
- `ReportViewModel.loadLowStock(limit: Int = 20)`
- `ReportViewModel.changeRange(range: DateRange)`

## 验收标准

- 时间区间变化后，四个 Tab 的数据都能联动刷新。

## UI 设计规范

- 对照设计图 `07.png` 的报表总览、库存流水报表、对账汇总实现。
- 报表页顶部保持“智慧记 + 报表标题 + 搜索/筛选图标”的结构。
- 报表总览使用日期范围胶囊、2 列 KPI 卡、趋势折线图和经营情况列表。
- 库存流水报表使用 KPI 四宫格、状态 Tab 和紧凑表格，入库绿色、出库橙色、调拨蓝灰。
- 对账汇总使用“客户对账/供应商对账”Tab、KPI 四宫格、欠款 TOP5 排行条和账龄环形图。
- 图表必须放在 `ChartCard` 中，坐标、标签和金额要保持轻量。
