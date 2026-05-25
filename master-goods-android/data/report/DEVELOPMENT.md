# data/report 模块开发说明

- 当前状态：脚手架已创建，仓储未开始。
- 实际源码目录：`data/report/src/main/java/com/zhihuiji/data/report`
- 目标：封装经营报表拉取逻辑。

## 需要创建的类

- `ReportRepository`

## 需要实现的关键函数

- `salesSummary(startAt: Long, endAt: Long)`
- `profitSummary(startAt: Long, endAt: Long)`
- `refundRecords(startAt: Long, endAt: Long, limit: Int)`
- `stockOutRecords(startAt: Long, endAt: Long, limit: Int)`
- `topProducts(startAt: Long, endAt: Long, limit: Int)`
- `profitByProducts(startAt: Long, endAt: Long, limit: Int)`
- `profitByCustomers(startAt: Long, endAt: Long, limit: Int)`
- `inventoryFlow(startAt: Long, endAt: Long, limit: Int)`
- `customerSales(startAt: Long, endAt: Long, limit: Int)`
- `topReceivableCustomers(limit: Int)`
- `lowStockProducts(limit: Int)`
- `reconciliationSummary(startAt: Long, endAt: Long)`

## 验收标准

- 首页和报表页都从这个模块取数，不各自直接打接口。

## UI 设计规范支撑

- 报表数据要能支撑四宫格 KPI、趋势折线图、库存流水表、排行榜、环形账龄图。
- 所有报表方法都要接受统一日期范围，供报表页顶部日期选择器联动刷新。
