# Android Data 层 - Report 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 report 子模块全部 1 个 Kotlin 源文件

---

## 1. ReportRepository

- **文件路径**: `data/report/src/main/java/com/zhihuiji/data/report/ReportRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 报表数据的获取，纯远程 API 调用，无本地缓存
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用报表相关 API
- 建议：无

### 函数/方法

##### salesSummary(startAt: Long, endAt: Long): Result<SalesSummaryReportDto>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳
- 返回值：`Result<SalesSummaryReportDto>` - 销售汇总数据
- 实现逻辑：委托给 `safeApiCall { api.salesSummary(startAt, endAt) }`
- 调用关系：调用了 `safeApiCall()`、`api.salesSummary()`，被 `ReportViewModel.loadReports()`、`DashboardViewModel.loadDashboard()` 调用
- 建议：无

##### profitSummary(startAt: Long, endAt: Long): Result<ProfitSummaryReportDto>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳
- 返回值：`Result<ProfitSummaryReportDto>` - 利润汇总数据
- 实现逻辑：委托给 `safeApiCall { api.profitSummary(startAt, endAt) }`
- 调用关系：调用了 `safeApiCall()`、`api.profitSummary()`，被 `ReportViewModel.loadReports()`、`DashboardViewModel.loadDashboard()` 调用
- 建议：无

##### refundRecords(startAt: Long, endAt: Long, limit: Int = 10): Result<List<RefundRecordDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<RefundRecordDto>>` - 退款记录列表
- 实现逻辑：委托给 `safeApiCall { api.refundRecords(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.refundRecords()`
- 建议：当前未被任何 ViewModel 调用，考虑是否需要暴露给 UI 层

##### stockOutRecords(startAt: Long, endAt: Long, limit: Int = 10): Result<List<StockOutRecordDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<StockOutRecordDto>>` - 出库记录列表
- 实现逻辑：委托给 `safeApiCall { api.stockOutRecords(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.stockOutRecords()`
- 建议：当前未被任何 ViewModel 调用

##### topProducts(startAt: Long, endAt: Long, limit: Int = 10): Result<List<TopSellingProductReportDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<TopSellingProductReportDto>>` - 热销商品排行
- 实现逻辑：委托给 `safeApiCall { api.topProducts(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.topProducts()`，被 `ReportViewModel.loadReports()` 调用
- 建议：无

##### profitByProducts(startAt: Long, endAt: Long, limit: Int = 10): Result<List<ProductProfitReportDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<ProductProfitReportDto>>` - 商品利润排行
- 实现逻辑：委托给 `safeApiCall { api.profitByProducts(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.profitByProducts()`
- 建议：当前未被任何 ViewModel 调用

##### profitByCustomers(startAt: Long, endAt: Long, limit: Int = 10): Result<List<CustomerProfitReportDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<CustomerProfitReportDto>>` - 客户利润排行
- 实现逻辑：委托给 `safeApiCall { api.profitByCustomers(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.profitByCustomers()`
- 建议：当前未被任何 ViewModel 调用

##### inventoryFlow(startAt: Long, endAt: Long, limit: Int = 10): Result<List<InventoryFlowReportDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<InventoryFlowReportDto>>` - 库存流水数据
- 实现逻辑：委托给 `safeApiCall { api.inventoryFlow(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.inventoryFlow()`
- 建议：当前未被任何 ViewModel 调用

##### customerSales(startAt: Long, endAt: Long, limit: Int = 10): Result<List<CustomerSalesReportDto>>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳；`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<CustomerSalesReportDto>>` - 客户销售数据
- 实现逻辑：委托给 `safeApiCall { api.customerSales(startAt, endAt, limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.customerSales()`
- 建议：当前未被任何 ViewModel 调用

##### topReceivableCustomers(limit: Int = 10): Result<List<CustomerReceivableReportDto>>
- 参数：`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<CustomerReceivableReportDto>>` - 应收客户排行
- 实现逻辑：委托给 `safeApiCall { api.topReceivableCustomers(limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.topReceivableCustomers()`，被 `ReportViewModel.loadReports()`、`DashboardViewModel.loadDashboard()` 调用
- 建议：无

##### lowStockProducts(limit: Int = 10): Result<List<LowStockProductReportDto>>
- 参数：`limit: Int = 10` - 返回条数限制
- 返回值：`Result<List<LowStockProductReportDto>>` - 低库存商品列表
- 实现逻辑：委托给 `safeApiCall { api.lowStockProducts(limit) }`
- 调用关系：调用了 `safeApiCall()`、`api.lowStockProducts()`，被 `ReportViewModel.loadReports()`、`DashboardViewModel.loadDashboard()` 调用
- 建议：无

##### reconciliationSummary(startAt: Long, endAt: Long): Result<ReconciliationSummaryReportDto>
- 参数：`startAt: Long` - 起始时间戳；`endAt: Long` - 结束时间戳
- 返回值：`Result<ReconciliationSummaryReportDto>` - 对账汇总数据
- 实现逻辑：委托给 `safeApiCall { api.reconciliationSummary(startAt, endAt) }`
- 调用关系：调用了 `safeApiCall()`、`api.reconciliationSummary()`，被 `ReportViewModel.loadReports()` 调用
- 建议：无
