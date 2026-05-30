# Android Data 层 - Order 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 order 子模块全部 3 个 Kotlin 源文件

---

## 1. PayOrderRepository

- **文件路径**: `data/order/src/main/java/com/zhihuiji/data/order/PayOrderRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 付款单数据的查询、创建和状态变更，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用付款单相关 API
- 建议：无

##### payOrderDao: PayOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的付款单 CRUD 操作
- 建议：无

### 函数/方法

##### observePayOrders(filter: PayOrderFilter): Flow<List<PayOrderDto>>
- 参数：`filter: PayOrderFilter` - 筛选条件（包含 keyword、status 等字段）
- 返回值：`Flow<List<PayOrderDto>>` - 付款单列表的响应式流
- 实现逻辑：观察数据库全部数据（`payOrderDao.observeAll()`），在内存中按关键词（orderNo 或 supplierName）和状态进行过滤
- 调用关系：调用了 `payOrderDao.observeAll()`、`toDto()`，被 `PayOrderViewModel.loadOrders()` 调用
- 建议：同 SaleOrderRepository，建议将过滤下推到 DAO 层

##### refreshPayOrders(filter: PayOrderFilter)
- 参数：`filter: PayOrderFilter` - 筛选条件（包含 keyword、status、createdAfter、createdBefore 等字段）
- 返回值：无
- 实现逻辑：从 API 拉取付款单列表（传递 keyword、status、createdAfter、createdBefore 参数），成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.payOrders()`、`payOrderDao.upsertAll()`、`toEntity()`，被 `PayOrderViewModel.loadOrders()` 调用
- 建议：无

##### getPayOrder(id: Long): Result<PayOrderDto>
- 参数：`id: Long` - 付款单 ID
- 返回值：`Result<PayOrderDto>` - 付款单详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `payOrderDao.findById()`、`safeApiCall()`、`api.payOrder()`、`payOrderDao.upsert()`，被 `PayOrderDetailViewModel.loadDetail()` 调用
- 建议：无

##### createPayOrder(request: CreatePayOrderRequest): Result<PayOrderDto>
- 参数：`request: CreatePayOrderRequest` - 创建付款单请求
- 返回值：`Result<PayOrderDto>` - 创建后的付款单数据
- 实现逻辑：调用 API 创建付款单，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createPayOrder()`、`payOrderDao.upsert()`，被 `PayOrderEditorViewModel.submitOrder()` 调用
- 建议：无

##### updatePayOrderStatus(id: Long, status: Int): Result<PayOrderDto>
- 参数：`id: Long` - 付款单 ID；`status: Int` - 目标状态
- 返回值：`Result<PayOrderDto>` - 更新后的付款单数据
- 实现逻辑：调用 API 更新状态（传递 `StatusRequest(status)`），成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.updatePayOrderStatus()`、`payOrderDao.upsert()`，被 `PayOrderDetailViewModel.updateStatus()` 调用
- 建议：无

---

## 2. PurchaseOrderRepository

- **文件路径**: `data/order/src/main/java/com/zhihuiji/data/order/PurchaseOrderRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 采购单数据的查询和创建，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用采购单相关 API
- 建议：无

##### purchaseOrderDao: PurchaseOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的采购单 CRUD 操作
- 建议：无

### 函数/方法

##### observePurchaseOrders(filter: PurchaseOrderFilter): Flow<List<PurchaseOrderDto>>
- 参数：`filter: PurchaseOrderFilter` - 筛选条件（包含 keyword、status 等字段）
- 返回值：`Flow<List<PurchaseOrderDto>>` - 采购单列表的响应式流
- 实现逻辑：观察数据库全部数据（`purchaseOrderDao.observeAll()`），在内存中按关键词（orderNo 或 supplierName）和状态进行过滤
- 调用关系：调用了 `purchaseOrderDao.observeAll()`、`toDto()`，被 `PurchaseOrderViewModel.loadOrders()` 调用
- 建议：同 SaleOrderRepository，建议将过滤下推到 DAO 层

##### refreshPurchaseOrders(filter: PurchaseOrderFilter)
- 参数：`filter: PurchaseOrderFilter` - 筛选条件（包含 keyword、status 等字段）
- 返回值：无
- 实现逻辑：从 API 拉取采购单列表（传递 keyword、status 参数），成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.purchaseOrders()`、`purchaseOrderDao.upsertAll()`、`toEntity()`，被 `PurchaseOrderViewModel.loadOrders()` 调用
- 建议：无

##### getPurchaseOrder(id: Long): Result<PurchaseOrderDto>
- 参数：`id: Long` - 采购单 ID
- 返回值：`Result<PurchaseOrderDto>` - 采购单详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `purchaseOrderDao.findById()`、`safeApiCall()`、`api.purchaseOrder()`、`purchaseOrderDao.upsert()`，被 `PurchaseOrderDetailViewModel.loadDetail()` 调用
- 建议：无

##### createPurchaseOrder(request: CreatePurchaseOrderRequest): Result<PurchaseOrderDto>
- 参数：`request: CreatePurchaseOrderRequest` - 创建采购单请求
- 返回值：`Result<PurchaseOrderDto>` - 创建后的采购单数据
- 实现逻辑：调用 API 创建采购单，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createPurchaseOrder()`、`purchaseOrderDao.upsert()`，被 `PurchaseOrderEditorViewModel.submitOrder()` 调用
- 建议：无

---

## 3. SaleOrderRepository

- **文件路径**: `data/order/src/main/java/com/zhihuiji/data/order/SaleOrderRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 销售单数据的增删改查、收款、状态变更，支持本地数据库观察和远程 API 同步
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用销售单相关 API
- 建议：无

##### saleOrderDao: SaleOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：本地数据库的销售单 CRUD 操作
- 建议：无

### 函数/方法

##### observeSaleOrders(filter: SaleOrderFilter): Flow<List<SaleOrderDto>>
- 参数：`filter: SaleOrderFilter` - 筛选条件（包含 keyword、status 等字段）
- 返回值：`Flow<List<SaleOrderDto>>` - 销售单列表的响应式流
- 实现逻辑：观察数据库全部数据（`saleOrderDao.observeAll()`），在内存中按关键词（orderNo 或 customerName）和状态进行过滤
- 调用关系：调用了 `saleOrderDao.observeAll()`、`toDto()`，被 `SaleOrderListViewModel.loadOrders()` 调用
- 建议：内存过滤在大数据量时性能不佳，建议将过滤逻辑下推到 DAO 层的 SQL 查询中

##### refreshSaleOrders(filter: SaleOrderFilter)
- 参数：`filter: SaleOrderFilter` - 筛选条件（包含 keyword、status、minTotalAmount、maxTotalAmount、createdAfter、createdBefore、productKeyword、paymentStatus 等字段）
- 返回值：无
- 实现逻辑：从 API 拉取销售单列表（传递全部筛选参数），成功后批量 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.saleOrders()`、`saleOrderDao.upsertAll()`、`toEntity()`，被 `SaleOrderListViewModel.loadOrders()` 调用
- 建议：无

##### getSaleOrder(id: Long): Result<SaleOrderDto>
- 参数：`id: Long` - 销售单 ID
- 返回值：`Result<SaleOrderDto>` - 销售单详情
- 实现逻辑：先从本地数据库查询作为回退数据，再从远程 API 获取。远程成功时更新本地并返回远程数据，远程失败时回退到本地数据
- 调用关系：调用了 `saleOrderDao.findById()`、`safeApiCall()`、`api.saleOrder()`、`saleOrderDao.upsert()`，被 `SaleOrderDetailViewModel.loadDetail()` 调用
- 建议：无

##### createSaleOrder(request: CreateSaleOrderRequest): Result<SaleOrderDto>
- 参数：`request: CreateSaleOrderRequest` - 创建销售单请求
- 返回值：`Result<SaleOrderDto>` - 创建后的销售单数据
- 实现逻辑：调用 API 创建销售单，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.createSaleOrder()`、`saleOrderDao.upsert()`，被 `SaleOrderEditorViewModel.submitOrder()` 调用
- 建议：无

##### updateSaleDraft(id: Long, request: UpdateSaleDraftRequest): Result<SaleOrderDto>
- 参数：`id: Long` - 销售单 ID；`request: UpdateSaleDraftRequest` - 更新草稿请求
- 返回值：`Result<SaleOrderDto>` - 更新后的销售单数据
- 实现逻辑：调用 API 更新销售单草稿，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.updateSaleDraft()`、`saleOrderDao.upsert()`
- 建议：当前未被任何 ViewModel 调用，UI 层的"保存草稿"按钮实际未调用此方法

##### addSalePayment(id: Long, request: PaymentRequest): Result<PaymentDto>
- 参数：`id: Long` - 销售单 ID；`request: PaymentRequest` - 收款请求
- 返回值：`Result<PaymentDto>` - 收款记录
- 实现逻辑：调用 API 添加收款记录
- 调用关系：调用了 `safeApiCall()`、`api.addSalePayment()`，被 `SaleOrderDetailViewModel.addPayment()` 调用
- 建议：收款成功后未更新本地销售单的 paidAmount，依赖后续 loadDetail 重新拉取。建议在 addSalePayment 成功后也更新本地订单

##### listSalePayments(id: Long): Result<List<PaymentDto>>
- 参数：`id: Long` - 销售单 ID
- 返回值：`Result<List<PaymentDto>>` - 收款记录列表
- 实现逻辑：委托给 `safeApiCall { api.salePayments(id) }`
- 调用关系：调用了 `safeApiCall()`、`api.salePayments()`，被 `SaleOrderDetailViewModel.loadDetail()` 调用
- 建议：无

##### updateSaleStatus(id: Long, status: Int): Result<Unit>
- 参数：`id: Long` - 销售单 ID；`status: Int` - 目标状态
- 返回值：`Result<Unit>` - 更新结果
- 实现逻辑：调用 API 更新状态，成功后重新拉取销售单详情并更新本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.updateSaleStatus()`、`api.saleOrder()`、`saleOrderDao.upsert()`，被 `SaleOrderDetailViewModel.completeOrder()` 调用
- 建议：无

##### cancelSaleOrder(id: Long): Result<SaleOrderDto>
- 参数：`id: Long` - 销售单 ID
- 返回值：`Result<SaleOrderDto>` - 作废后的销售单数据
- 实现逻辑：调用 API 作废销售单，成功后 upsert 到本地数据库
- 调用关系：调用了 `safeApiCall()`、`api.cancelSaleOrder()`、`saleOrderDao.upsert()`，被 `SaleOrderDetailViewModel.cancelOrder()` 调用
- 建议：无
