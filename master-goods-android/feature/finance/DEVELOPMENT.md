# feature/finance 模块开发说明

- 当前状态：列表页+新增流水已完成，业务链路已走通。
- 实际源码目录：`feature/finance/src/main/java/com/zhihuiji/feature/finance`
- 目标：实现资金流水列表和新增流水。

## 需要创建的类

- `FinanceRecordListScreen`
- `FinanceRecordEditorSheet`
- `FinanceViewModel`

## 需要实现的关键函数

- `FinanceViewModel.loadRecords(filter: FinanceFilter)`
- `FinanceViewModel.changeType(type: Int?)`
- `FinanceViewModel.changeDateRange(startAt: Long?, endAt: Long?)`
- `FinanceViewModel.createRecord()`
- `FinanceViewModel.validateForm()`

## 验收标准

- 收入/支出筛选、时间筛选、新增流水三条链路可用。

## UI 设计规范

- 对照设计图 `07.png` 的资金流水页实现。
- 顶部为大标题“智慧记”和二级标题“资金流水”，右侧搜索与筛选图标。
- 日期范围使用浅色胶囊选择器。
- 摘要区使用 2 列 KPI 卡：收入、支出、净流入、期末余额。
- 流水列表使用图标区分销售收款、采购付款、费用支出；正金额绿色，负金额红色或黑色增强。
