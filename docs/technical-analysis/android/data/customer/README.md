# Android Data 层 - Customer 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 customer 子模块全部 1 个 Kotlin 源文件

---

## 1. CustomerRepository

- **文件路径**: `data/customer/src/main/java/com/zhihuiji/data/customer/CustomerRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 客户数据的增删改查，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用客户相关 API
- 建议：无

##### customerDao: CustomerDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的客户 CRUD 操作
- 建议：无

### 函数/方法

##### observeCustomers(keyword: String): Flow<List<CustomerDto>>
- 参数：`keyword: String` - 搜索关键词
- 返回值：`Flow<List<CustomerDto>>` - 客户列表的响应式流
- 实现逻辑：空关键词时调用 `customerDao.observeAll()` 观察全部，否则调用 `customerDao.search(keyword)` 搜索；将 Entity 列表映射为 Dto 列表
- 调用关系：调用了 `customerDao.observeAll()`、`customerDao.search()`、`toDto()`，被 `CustomerViewModel.loadCustomers()`、`SaleOrderEditorViewModel.searchCustomers()` 调用
- 建议：无

##### refreshCustomers(keyword: String?)
- 参数：`keyword: String?` - 搜索关键词
- 返回值：无
- 实现逻辑：从 API 拉取客户列表，成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.customers()`、`customerDao.upsertAll()`、`toEntity()`，被 `CustomerViewModel.loadCustomers()`、`SaleOrderEditorViewModel.init` 调用
- 建议：无

##### getCustomer(id: Long): Result<CustomerDto>
- 参数：`id: Long` - 客户 ID
- 返回值：`Result<CustomerDto>` - 客户详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `customerDao.findById()`、`safeApiCall()`、`api.customer()`、`customerDao.upsert()`，被 `CustomerEditorViewModel.loadCustomer()`、`CustomerDetailViewModel.loadCustomer()` 调用
- 建议：此离线优先策略很好，但远程失败时本地数据可能过时。建议增加数据过期时间标记

##### createCustomer(draft: CustomerDto): Result<CustomerDto>
- 参数：`draft: CustomerDto` - 待创建的客户数据
- 返回值：`Result<CustomerDto>` - 创建后的客户数据
- 实现逻辑：调用 API 创建客户，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createCustomer()`、`customerDao.upsert()`，被 `CustomerEditorViewModel.saveCustomer()` 调用
- 建议：无

##### updateCustomer(id: Long, draft: CustomerDto): Result<CustomerDto>
- 参数：`id: Long` - 客户 ID；`draft: CustomerDto` - 更新数据
- 返回值：`Result<CustomerDto>` - 更新后的客户数据
- 实现逻辑：调用 API 更新客户，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.updateCustomer()`、`customerDao.upsert()`，被 `CustomerEditorViewModel.saveCustomer()` 调用
- 建议：无

##### deleteCustomer(id: Long): Result<Unit>
- 参数：`id: Long` - 客户 ID
- 返回值：`Result<Unit>` - 删除结果
- 实现逻辑：调用 API 删除客户，成功后从本地数据库删除对应记录
- 调用关系：调用了 `safeApiCall()`、`api.deleteCustomer()`、`customerDao.deleteById()`，被 `CustomerViewModel.deleteCustomer()` 调用
- 建议：无
