# Android Data 层 - Supplier 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 supplier 子模块全部 1 个 Kotlin 源文件

---

## 1. SupplierRepository

- **文件路径**: `data/supplier/src/main/java/com/zhihuiji/data/supplier/SupplierRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 供应商数据的增删改查，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用供应商相关 API
- 建议：无

##### supplierDao: SupplierDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的供应商 CRUD 操作
- 建议：无

### 函数/方法

##### observeSuppliers(keyword: String, status: Int?): Flow<List<SupplierDto>>
- 参数：`keyword: String` - 搜索关键词；`status: Int?` - 状态筛选
- 返回值：`Flow<List<SupplierDto>>` - 供应商列表的响应式流
- 实现逻辑：调用 `supplierDao.search()` 查询，空关键词传 null（`keyword.ifBlank { null }`），将 Entity 列表映射为 Dto 列表
- 调用关系：调用了 `supplierDao.search()`、`toDto()`，被 `SupplierViewModel.loadSuppliers()`、`PurchaseOrderEditorViewModel.searchSuppliers()`、`PayOrderEditorViewModel.searchSuppliers()` 调用
- 建议：无

##### refreshSuppliers(keyword: String?, status: Int?)
- 参数：`keyword: String?` - 搜索关键词；`status: Int?` - 状态筛选
- 返回值：无
- 实现逻辑：从 API 拉取供应商列表，成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.suppliers()`、`supplierDao.upsertAll()`、`toEntity()`，被 `SupplierViewModel.loadSuppliers()`、`PurchaseOrderEditorViewModel.init`、`PayOrderEditorViewModel.init` 调用
- 建议：无

##### getSupplier(id: Long): Result<SupplierDto>
- 参数：`id: Long` - 供应商 ID
- 返回值：`Result<SupplierDto>` - 供应商详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `supplierDao.findById()`、`safeApiCall()`、`api.supplier()`、`supplierDao.upsert()`，被 `SupplierEditorViewModel.loadSupplier()`、`SupplierDetailViewModel.loadSupplier()` 调用
- 建议：此离线优先策略很好，但远程失败时本地数据可能过时。建议增加数据过期时间标记

##### createSupplier(draft: SupplierDto): Result<SupplierDto>
- 参数：`draft: SupplierDto` - 待创建的供应商数据
- 返回值：`Result<SupplierDto>` - 创建后的供应商数据
- 实现逻辑：调用 API 创建供应商，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createSupplier()`、`supplierDao.upsert()`，被 `SupplierEditorViewModel.saveSupplier()` 调用
- 建议：无

##### updateSupplier(id: Long, draft: SupplierDto): Result<SupplierDto>
- 参数：`id: Long` - 供应商 ID；`draft: SupplierDto` - 更新数据
- 返回值：`Result<SupplierDto>` - 更新后的供应商数据
- 实现逻辑：调用 API 更新供应商，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.updateSupplier()`、`supplierDao.upsert()`，被 `SupplierEditorViewModel.saveSupplier()` 调用
- 建议：无

##### deleteSupplier(id: Long): Result<Unit>
- 参数：`id: Long` - 供应商 ID
- 返回值：`Result<Unit>` - 删除结果
- 实现逻辑：调用 API 删除供应商，成功后从本地数据库删除对应记录
- 调用关系：调用了 `safeApiCall()`、`api.deleteSupplier()`、`supplierDao.deleteById()`，被 `SupplierViewModel.deleteSupplier()` 调用
- 建议：无
