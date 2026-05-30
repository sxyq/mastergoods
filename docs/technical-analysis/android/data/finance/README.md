# Android Data 层 - Finance 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 finance 子模块全部 1 个 Kotlin 源文件

---

## 1. FinanceRepository

- **文件路径**: `data/finance/src/main/java/com/zhihuiji/data/finance/FinanceRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 资金流水数据的查询和创建，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用资金流水相关 API
- 建议：无

##### financeRecordDao: FinanceRecordDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的资金流水 CRUD 操作
- 建议：无

### 函数/方法

##### observeFinanceRecords(filter: FinanceFilter): Flow<List<FinanceRecordDto>>
- 参数：`filter: FinanceFilter` - 筛选条件（包含 keyword、type 等字段）
- 返回值：`Flow<List<FinanceRecordDto>>` - 资金流水列表的响应式流
- 实现逻辑：观察数据库全部数据（`financeRecordDao.observeAll()`），在内存中按关键词（recordNo 或 partnerName）和类型进行过滤
- 调用关系：调用了 `financeRecordDao.observeAll()`、`toDto()`，被 `FinanceViewModel.loadRecords()` 调用
- 建议：同其他 Repository，内存过滤在大数据量时性能不佳，建议将过滤逻辑下推到 DAO 层的 SQL 查询中

##### refreshFinanceRecords(filter: FinanceFilter)
- 参数：`filter: FinanceFilter` - 筛选条件（包含 keyword、type、createdAfter、createdBefore 等字段）
- 返回值：无
- 实现逻辑：从 API 拉取资金流水列表（传递 keyword、type、createdAfter、createdBefore 参数），成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.financeRecords()`、`financeRecordDao.upsertAll()`、`toEntity()`，被 `FinanceViewModel.loadRecords()` 调用
- 建议：无

##### createFinanceRecord(request: CreateFinanceRecordRequest): Result<FinanceRecordDto>
- 参数：`request: CreateFinanceRecordRequest` - 创建资金流水请求
- 返回值：`Result<FinanceRecordDto>` - 创建后的资金流水数据
- 实现逻辑：调用 API 创建资金流水，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createFinanceRecord()`、`financeRecordDao.upsert()`，被 `FinanceViewModel.createRecord()` 调用
- 建议：无
