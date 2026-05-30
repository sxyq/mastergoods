# Android Data 层 - Product 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 product 子模块全部 1 个 Kotlin 源文件

---

## 1. ProductRepository

- **文件路径**: `data/product/src/main/java/com/zhihuiji/data/product/ProductRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 商品数据的增删改查、库存调整，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用商品相关 API
- 建议：无

##### productDao: ProductDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的商品 CRUD 操作
- 建议：无

### 函数/方法

##### observeProducts(keyword: String): Flow<List<ProductDto>>
- 参数：`keyword: String` - 搜索关键词
- 返回值：`Flow<List<ProductDto>>` - 商品列表的响应式流
- 实现逻辑：空关键词时调用 `productDao.observeAll()` 观察全部，否则调用 `productDao.search(keyword)` 搜索；将 Entity 列表映射为 Dto 列表
- 调用关系：调用了 `productDao.observeAll()`、`productDao.search()`、`toDto()`，被 `ProductListViewModel.loadProducts()`、`SaleOrderEditorViewModel.searchProducts()`、`PurchaseOrderEditorViewModel.searchProducts()` 调用
- 建议：无

##### refreshProducts(keyword: String?)
- 参数：`keyword: String?` - 搜索关键词
- 返回值：无
- 实现逻辑：从 API 拉取商品列表，成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.products()`、`productDao.upsertAll()`、`toEntity()`，被 `ProductListViewModel.loadProducts()`、`SaleOrderEditorViewModel.init`、`PurchaseOrderEditorViewModel.init` 调用
- 建议：无

##### getProduct(id: Long): Result<ProductDto>
- 参数：`id: Long` - 商品 ID
- 返回值：`Result<ProductDto>` - 商品详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `productDao.findById()`、`safeApiCall()`、`api.product()`、`productDao.upsert()`，被 `ProductEditorViewModel.loadProduct()` 调用
- 建议：此离线优先策略很好，但远程失败时本地数据可能过时。建议增加数据过期时间标记

##### findProductByCode(code: String): Result<ProductDto?>
- 参数：`code: String` - 商品编码
- 返回值：`Result<ProductDto?>` - 按编码查询的商品
- 实现逻辑：委托给 `safeApiCall { api.productByCode(code) }`，纯远程查询无本地缓存
- 调用关系：调用了 `safeApiCall()`、`api.productByCode()`
- 建议：当前未被任何 ViewModel 调用，考虑用于扫码场景。建议增加本地缓存支持

##### createProduct(draft: ProductDraft): Result<ProductDto>
- 参数：`draft: ProductDraft` - 待创建的商品数据
- 返回值：`Result<ProductDto>` - 创建后的商品数据
- 实现逻辑：调用 API 创建商品（draft 先通过 `toDto()` 转换），成功后 upsert 到本地数据库
- 调用关系：调用了 `draft.toDto()`、`safeApiCall()`、`api.createProduct()`、`productDao.upsert()`，被 `ProductEditorViewModel.saveProduct()` 调用
- 建议：无

##### updateProduct(id: Long, draft: ProductDraft): Result<ProductDto>
- 参数：`id: Long` - 商品 ID；`draft: ProductDraft` - 更新数据
- 返回值：`Result<ProductDto>` - 更新后的商品数据
- 实现逻辑：调用 API 更新商品（draft 先通过 `toDto()` 转换），成功后 upsert 到本地数据库
- 调用关系：调用了 `draft.toDto()`、`safeApiCall()`、`api.updateProduct()`、`productDao.upsert()`，被 `ProductEditorViewModel.saveProduct()` 调用
- 建议：无

##### adjustStock(id: Long, delta: Double, reason: String?, operator: String?): Result<ProductDto>
- 参数：`id: Long` - 商品 ID；`delta: Double` - 库存变化量（正数为入库，负数为出库）；`reason: String?` - 调整原因；`operator: String?` - 操作人
- 返回值：`Result<ProductDto>` - 调整后的商品数据
- 实现逻辑：调用 API 调整库存（传递 `ProductAdjustStockRequest(delta, reason, operator)`），成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.adjustStock()`、`productDao.upsert()`，被 `ProductEditorViewModel.adjustStock()` 调用
- 建议：无

##### deleteProduct(id: Long): Result<Unit>
- 参数：`id: Long` - 商品 ID
- 返回值：`Result<Unit>` - 删除结果
- 实现逻辑：调用 API 删除商品，成功后从本地数据库删除对应记录
- 调用关系：调用了 `safeApiCall()`、`api.deleteProduct()`、`productDao.deleteById()`，被 `ProductListViewModel.deleteProduct()` 调用
- 建议：无
