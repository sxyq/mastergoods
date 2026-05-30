# database 技术分析

## 文件清单
- DatabaseModule.kt
- EntityMappers.kt
- ZhihuijiDatabase.kt
- dao/AgentNotificationDao.kt
- dao/CustomerDao.kt
- dao/FinanceRecordDao.kt
- dao/PayOrderDao.kt
- dao/ProductDao.kt
- dao/PurchaseOrderDao.kt
- dao/SaleOrderDao.kt
- dao/SupplierDao.kt
- dao/SyncCursorDao.kt
- entity/AgentNotificationEntity.kt
- entity/CustomerEntity.kt
- entity/FinanceRecordEntity.kt
- entity/PayOrderEntity.kt
- entity/ProductEntity.kt
- entity/PurchaseOrderEntity.kt
- entity/SaleOrderEntity.kt
- entity/SupplierEntity.kt
- entity/SyncCursorEntity.kt

---

## DatabaseModule.kt

### DatabaseModule
- object / 注解：@Module, @InstallIn(SingletonComponent::class) / 职责：通过 Hilt DI 提供 Database 实例和各 Dao 实例 / 设计模式：依赖注入模块

#### provideDatabase(@ApplicationContext context: Context): ZhihuijiDatabase
- 参数：`@ApplicationContext context: Context` — 应用级 Context，由 Hilt 自动注入
- 返回值：`ZhihuijiDatabase` — Room 数据库实例
- 实现逻辑：使用 Room.databaseBuilder 创建 "zhihuiji.db" 数据库，设置 fallbackToDestructiveMigration（版本升级时销毁重建）
- 调用关系：被 Hilt 框架调用，注入到各 Repository
- 建议：fallbackToDestructiveMigration 在生产环境中会导致数据丢失，建议实现正式的 Migration 策略

#### provideProductDao(db: ZhihuijiDatabase): ProductDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`ProductDao`
- 实现逻辑：调用 db.productDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 ProductRepository
- 建议：无

#### provideCustomerDao(db: ZhihuijiDatabase): CustomerDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`CustomerDao`
- 实现逻辑：调用 db.customerDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 CustomerRepository
- 建议：无

#### provideSupplierDao(db: ZhihuijiDatabase): SupplierDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`SupplierDao`
- 实现逻辑：调用 db.supplierDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 SupplierRepository
- 建议：无

#### provideSaleOrderDao(db: ZhihuijiDatabase): SaleOrderDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`SaleOrderDao`
- 实现逻辑：调用 db.saleOrderDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 SaleOrderRepository
- 建议：无

#### providePurchaseOrderDao(db: ZhihuijiDatabase): PurchaseOrderDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`PurchaseOrderDao`
- 实现逻辑：调用 db.purchaseOrderDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 PurchaseOrderRepository
- 建议：无

#### providePayOrderDao(db: ZhihuijiDatabase): PayOrderDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`PayOrderDao`
- 实现逻辑：调用 db.payOrderDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 PayOrderRepository
- 建议：无

#### provideFinanceRecordDao(db: ZhihuijiDatabase): FinanceRecordDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`FinanceRecordDao`
- 实现逻辑：调用 db.financeRecordDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 FinanceRecordRepository
- 建议：无

#### provideAgentNotificationDao(db: ZhihuijiDatabase): AgentNotificationDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`AgentNotificationDao`
- 实现逻辑：调用 db.agentNotificationDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 AgentNotificationRepository
- 建议：无

#### provideSyncCursorDao(db: ZhihuijiDatabase): SyncCursorDao
- 参数：`db: ZhihuijiDatabase` — 数据库实例
- 返回值：`SyncCursorDao`
- 实现逻辑：调用 db.syncCursorDao() 获取 Dao 实例
- 调用关系：被 Hilt 注入到 SyncRepository
- 建议：无

---

## EntityMappers.kt

### 顶层扩展函数集
- 职责：在 Entity 和 Dto 之间进行双向映射 / 设计模式：双向映射器模式（扩展函数实现）

#### ProductEntity.toDto(): ProductDto
- 参数：无（接收者为 ProductEntity）
- 返回值：`ProductDto`
- 实现逻辑：逐字段将 Entity 映射为 Dto
- 调用关系：被 Repository 层从数据库读取数据后转换为领域模型时调用
- 建议：无

#### ProductDto.toEntity(): ProductEntity
- 参数：无（接收者为 ProductDto）
- 返回值：`ProductEntity`
- 实现逻辑：逐字段将 Dto 映射为 Entity，id 为 null 时默认 0L
- 调用关系：被 Repository 层将网络数据写入数据库时调用
- 建议：id 为 null 时默认 0L 可能导致 Room 主键冲突，建议使用自动生成策略

#### CustomerEntity.toDto(): CustomerDto
- 参数：无（接收者为 CustomerEntity）
- 返回值：`CustomerDto`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

#### CustomerDto.toEntity(): CustomerEntity
- 参数：无（接收者为 CustomerDto）
- 返回值：`CustomerEntity`
- 实现逻辑：逐字段映射，id 为 null 时默认 0L
- 调用关系：被 Repository 层调用
- 建议：同 ProductDto.toEntity()

#### SupplierEntity.toDto(): SupplierDto
- 参数：无（接收者为 SupplierEntity）
- 返回值：`SupplierDto`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

#### SupplierDto.toEntity(): SupplierEntity
- 参数：无（接收者为 SupplierDto）
- 返回值：`SupplierEntity`
- 实现逻辑：逐字段映射，id 为 null 时默认 0L
- 调用关系：被 Repository 层调用
- 建议：同上

#### SaleOrderEntity.toDto(): SaleOrderDto
- 参数：无（接收者为 SaleOrderEntity）
- 返回值：`SaleOrderDto`
- 实现逻辑：逐字段映射，items 固定为 emptyList()（Entity 不存储订单项）
- 调用关系：被 Repository 层调用
- 建议：items 固定为空列表，调用方需额外获取订单项，建议在文档中说明或在 Dto 中标注

#### SaleOrderDto.toEntity(): SaleOrderEntity
- 参数：无（接收者为 SaleOrderDto）
- 返回值：`SaleOrderEntity`
- 实现逻辑：逐字段映射，丢弃 items 列表（不持久化订单项）
- 调用关系：被 Repository 层调用
- 建议：订单项未持久化，离线场景下可能丢失数据

#### PurchaseOrderEntity.toDto(): PurchaseOrderDto
- 参数：无（接收者为 PurchaseOrderEntity）
- 返回值：`PurchaseOrderDto`
- 实现逻辑：逐字段映射，items 固定为 emptyList()
- 调用关系：被 Repository 层调用
- 建议：同 SaleOrderEntity.toDto()

#### PurchaseOrderDto.toEntity(): PurchaseOrderEntity
- 参数：无（接收者为 PurchaseOrderDto）
- 返回值：`PurchaseOrderEntity`
- 实现逻辑：逐字段映射，丢弃 items 列表
- 调用关系：被 Repository 层调用
- 建议：同 SaleOrderDto.toEntity()

#### PayOrderEntity.toDto(): PayOrderDto
- 参数：无（接收者为 PayOrderEntity）
- 返回值：`PayOrderDto`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

#### PayOrderDto.toEntity(): PayOrderEntity
- 参数：无（接收者为 PayOrderDto）
- 返回值：`PayOrderEntity`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

#### FinanceRecordEntity.toDto(): FinanceRecordDto
- 参数：无（接收者为 FinanceRecordEntity）
- 返回值：`FinanceRecordDto`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

#### FinanceRecordDto.toEntity(): FinanceRecordEntity
- 参数：无（接收者为 FinanceRecordDto）
- 返回值：`FinanceRecordEntity`
- 实现逻辑：逐字段映射
- 调用关系：被 Repository 层调用
- 建议：无

---

## ZhihuijiDatabase.kt

### ZhihuijiDatabase
- abstract class / 父类：RoomDatabase / 注解：@Database(entities=[...], version=1, exportSchema=false) / 职责：Room 数据库定义，注册所有 Entity 和 Dao / 设计模式：抽象工厂模式（Room 生成实现）

#### productDao(): ProductDao
- 返回值：`ProductDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### customerDao(): CustomerDao
- 返回值：`CustomerDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### supplierDao(): SupplierDao
- 返回值：`SupplierDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### saleOrderDao(): SaleOrderDao
- 返回值：`SaleOrderDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### purchaseOrderDao(): PurchaseOrderDao
- 返回值：`PurchaseOrderDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### payOrderDao(): PayOrderDao
- 返回值：`PayOrderDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### financeRecordDao(): FinanceRecordDao
- 返回值：`FinanceRecordDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### agentNotificationDao(): AgentNotificationDao
- 返回值：`AgentNotificationDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

#### syncCursorDao(): SyncCursorDao
- 返回值：`SyncCursorDao`
- 实现逻辑：抽象方法，由 Room 编译器自动生成实现
- 调用关系：被 DatabaseModule 调用
- 建议：无

---

## dao/AgentNotificationDao.kt

### AgentNotificationDao
- interface / 注解：@Dao / 职责：Agent 通知的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<AgentNotificationEntity\>\>
- 返回值：`Flow<List<AgentNotificationEntity>>` — 按创建时间倒序的所有通知流
- 实现逻辑：@Query("SELECT * FROM agent_notifications ORDER BY createdAt DESC")，返回响应式 Flow
- 调用关系：被 ViewModel 观察通知列表时调用
- 建议：无

#### observeUnread(): Flow\<List\<AgentNotificationEntity\>\>
- 返回值：`Flow<List<AgentNotificationEntity>>` — 未读通知流（isRead = 0）
- 实现逻辑：@Query 查询 isRead = 0 的记录
- 调用关系：被 ViewModel 观察未读通知时调用
- 建议：无

#### findById(id: Long): AgentNotificationEntity?
- 参数：`id: Long` — 通知 ID
- 返回值：`AgentNotificationEntity?` — 找到返回实体，否则 null
- 实现逻辑：@Query 按 id 查询
- 调用关系：被需要获取单条通知详情时调用
- 建议：无

#### upsert(entity: AgentNotificationEntity)
- 参数：`entity: AgentNotificationEntity` — 通知实体
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)，存在则替换
- 调用关系：被 Repository 同步单条通知时调用
- 建议：无

#### upsertAll(entities: List\<AgentNotificationEntity\>)
- 参数：`entities: List<AgentNotificationEntity>` — 通知实体列表
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)，批量替换
- 调用关系：被 Repository 批量同步通知时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long` — 通知 ID
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM agent_notifications WHERE id = :id")
- 调用关系：被删除单条通知时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM agent_notifications")，清空表
- 调用关系：被登出或重置数据时调用
- 建议：无

---

## dao/CustomerDao.kt

### CustomerDao
- interface / 注解：@Dao / 职责：客户的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<CustomerEntity\>\>
- 返回值：`Flow<List<CustomerEntity>>` — 按 updatedAt 倒序的客户流
- 实现逻辑：@Query("SELECT * FROM customers ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察客户列表时调用
- 建议：无

#### search(keyword: String): Flow\<List\<CustomerEntity\>\>
- 参数：`keyword: String` — 搜索关键词
- 返回值：`Flow<List<CustomerEntity>>` — 匹配名称或电话的客户流
- 实现逻辑：@Query 使用 LIKE 模糊匹配 name 或 phone 字段
- 调用关系：被搜索客户时调用
- 建议：无

#### findById(id: Long): CustomerEntity?
- 参数：`id: Long` — 客户 ID
- 返回值：`CustomerEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取客户详情时调用
- 建议：无

#### upsert(entity: CustomerEntity)
- 参数：`entity: CustomerEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步客户时调用
- 建议：无

#### upsertAll(entities: List\<CustomerEntity\>)
- 参数：`entities: List<CustomerEntity>`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)，批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除客户时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM customers")
- 调用关系：被清空客户数据时调用
- 建议：无

---

## dao/FinanceRecordDao.kt

### FinanceRecordDao
- interface / 注解：@Dao / 职责：财务记录的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<FinanceRecordEntity\>\>
- 返回值：`Flow<List<FinanceRecordEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM finance_records ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察财务记录时调用
- 建议：缺少按类型/日期范围筛选的查询方法，建议补充

#### findById(id: Long): FinanceRecordEntity?
- 参数：`id: Long`
- 返回值：`FinanceRecordEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取财务记录详情时调用
- 建议：无

#### upsert(entity: FinanceRecordEntity)
- 参数：`entity: FinanceRecordEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<FinanceRecordEntity\>)
- 参数：`entities: List<FinanceRecordEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除记录时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM finance_records")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/PayOrderDao.kt

### PayOrderDao
- interface / 注解：@Dao / 职责：付款单的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<PayOrderEntity\>\>
- 返回值：`Flow<List<PayOrderEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM pay_orders ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察付款单列表时调用
- 建议：缺少按状态/日期筛选的查询方法

#### findById(id: Long): PayOrderEntity?
- 参数：`id: Long`
- 返回值：`PayOrderEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取付款单详情时调用
- 建议：无

#### upsert(entity: PayOrderEntity)
- 参数：`entity: PayOrderEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<PayOrderEntity\>)
- 参数：`entities: List<PayOrderEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除付款单时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM pay_orders")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/ProductDao.kt

### ProductDao
- interface / 注解：@Dao / 职责：商品的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<ProductEntity\>\>
- 返回值：`Flow<List<ProductEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM products ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察商品列表时调用
- 建议：无

#### search(keyword: String): Flow\<List\<ProductEntity\>\>
- 参数：`keyword: String` — 搜索关键词
- 返回值：`Flow<List<ProductEntity>>` — 匹配名称或编码的商品流
- 实现逻辑：@Query 使用 LIKE 模糊匹配 name 或 code 字段
- 调用关系：被搜索商品时调用
- 建议：无

#### findById(id: Long): ProductEntity?
- 参数：`id: Long`
- 返回值：`ProductEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取商品详情时调用
- 建议：无

#### upsert(entity: ProductEntity)
- 参数：`entity: ProductEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<ProductEntity\>)
- 参数：`entities: List<ProductEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除商品时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM products")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/PurchaseOrderDao.kt

### PurchaseOrderDao
- interface / 注解：@Dao / 职责：采购订单的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<PurchaseOrderEntity\>\>
- 返回值：`Flow<List<PurchaseOrderEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM purchase_orders ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察采购订单列表时调用
- 建议：缺少按状态筛选的查询方法

#### findById(id: Long): PurchaseOrderEntity?
- 参数：`id: Long`
- 返回值：`PurchaseOrderEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取采购订单详情时调用
- 建议：无

#### upsert(entity: PurchaseOrderEntity)
- 参数：`entity: PurchaseOrderEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<PurchaseOrderEntity\>)
- 参数：`entities: List<PurchaseOrderEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除采购订单时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM purchase_orders")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/SaleOrderDao.kt

### SaleOrderDao
- interface / 注解：@Dao / 职责：销售订单的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<SaleOrderEntity\>\>
- 返回值：`Flow<List<SaleOrderEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM sale_orders ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察销售订单列表时调用
- 建议：缺少按状态/日期/客户筛选的查询方法

#### findById(id: Long): SaleOrderEntity?
- 参数：`id: Long`
- 返回值：`SaleOrderEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取销售订单详情时调用
- 建议：无

#### upsert(entity: SaleOrderEntity)
- 参数：`entity: SaleOrderEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<SaleOrderEntity\>)
- 参数：`entities: List<SaleOrderEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除销售订单时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM sale_orders")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/SupplierDao.kt

### SupplierDao
- interface / 注解：@Dao / 职责：供应商的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<SupplierEntity\>\>
- 返回值：`Flow<List<SupplierEntity>>` — 按 updatedAt 倒序
- 实现逻辑：@Query("SELECT * FROM suppliers ORDER BY updatedAt DESC")
- 调用关系：被 ViewModel 观察供应商列表时调用
- 建议：无

#### search(keyword: String?, status: Int?): Flow\<List\<SupplierEntity\>\>
- 参数：`keyword: String?` — 搜索关键词（可选）；`status: Int?` — 状态筛选（可选）
- 返回值：`Flow<List<SupplierEntity>>` — 匹配条件的供应商流
- 实现逻辑：@Query 使用 IS NULL 条件实现可选筛选，支持名称/电话模糊搜索和状态精确匹配
- 调用关系：被搜索/筛选供应商时调用
- 建议：此搜索模式较为完善，其他 Dao 可参考此实现添加筛选功能

#### findById(id: Long): SupplierEntity?
- 参数：`id: Long`
- 返回值：`SupplierEntity?`
- 实现逻辑：@Query 按 id 查询
- 调用关系：被获取供应商详情时调用
- 建议：无

#### upsert(entity: SupplierEntity)
- 参数：`entity: SupplierEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被 Repository 同步时调用
- 建议：无

#### upsertAll(entities: List\<SupplierEntity\>)
- 参数：`entities: List<SupplierEntity>`
- 返回值：无（suspend）
- 实现逻辑：批量替换
- 调用关系：被 Repository 批量同步时调用
- 建议：无

#### deleteById(id: Long)
- 参数：`id: Long`
- 返回值：无（suspend）
- 实现逻辑：@Query DELETE
- 调用关系：被删除供应商时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM suppliers")
- 调用关系：被清空数据时调用
- 建议：无

---

## dao/SyncCursorDao.kt

### SyncCursorDao
- interface / 注解：@Dao / 职责：同步游标的数据库访问对象 / 设计模式：DAO 模式

#### observeAll(): Flow\<List\<SyncCursorEntity\>\>
- 返回值：`Flow<List<SyncCursorEntity>>` — 所有同步游标流
- 实现逻辑：@Query("SELECT * FROM sync_cursors")
- 调用关系：被观察同步状态时调用
- 建议：无

#### findByEntityType(entityType: String): SyncCursorEntity?
- 参数：`entityType: String` — 实体类型标识
- 返回值：`SyncCursorEntity?`
- 实现逻辑：@Query 按 entityType 查询
- 调用关系：被同步逻辑获取某类型游标时调用
- 建议：无

#### upsert(entity: SyncCursorEntity)
- 参数：`entity: SyncCursorEntity`
- 返回值：无（suspend）
- 实现逻辑：@Insert(onConflict = OnConflictStrategy.REPLACE)
- 调用关系：被同步逻辑更新游标时调用
- 建议：无

#### clear()
- 返回值：无（suspend）
- 实现逻辑：@Query("DELETE FROM sync_cursors")
- 调用关系：被重置同步状态时调用
- 建议：无

---

## entity/AgentNotificationEntity.kt

### AgentNotificationEntity
- data class / 注解：@Entity(tableName = "agent_notifications") / 职责：Agent 通知的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：通知唯一标识
- 建议：无

#### type: String
- 作用域：成员变量 / 使用场景：通知类型
- 建议：无

#### title: String
- 作用域：成员变量 / 使用场景：通知标题
- 建议：无

#### content: String
- 作用域：成员变量 / 使用场景：通知内容
- 建议：无

#### isRead: Boolean
- 作用域：成员变量 / 使用场景：是否已读
- 建议：无

#### isDelivered: Boolean
- 作用域：成员变量 / 使用场景：是否已推送
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

---

## entity/CustomerEntity.kt

### CustomerEntity
- data class / 注解：@Entity(tableName = "customers") / 职责：客户的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：客户唯一标识
- 建议：无

#### name: String
- 作用域：成员变量 / 使用场景：客户名称
- 建议：无

#### phone: String
- 作用域：成员变量 / 使用场景：联系电话
- 建议：无

#### level: Int
- 作用域：成员变量 / 使用场景：客户等级（0=普通, 1=VIP, 2=SVIP）
- 建议：无

#### address: String?
- 作用域：成员变量 / 使用场景：地址，可为空
- 建议：无

#### notes: String?
- 作用域：成员变量 / 使用场景：备注，可为空
- 建议：无

#### balance: Double
- 作用域：成员变量 / 使用场景：账户余额
- 建议：金融数据使用 Double 存在精度风险，建议改用 BigDecimal 或以分为单位的 Long

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=停用, 1=启用）
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / 使用场景：同步状态标记
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / 使用场景：同步版本号
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/FinanceRecordEntity.kt

### FinanceRecordEntity
- data class / 注解：@Entity(tableName = "finance_records") / 职责：财务记录的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：记录唯一标识
- 建议：无

#### recordNo: String
- 作用域：成员变量 / 使用场景：记录编号
- 建议：无

#### type: Int
- 作用域：成员变量 / 使用场景：类型（1=收入, 2=支出）
- 建议：无

#### category: String
- 作用域：成员变量 / 使用场景：分类
- 建议：无

#### partnerName: String?
- 作用域：成员变量 / 使用场景：往来方名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：同 CustomerEntity.balance 建议

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### notes: String?
- 作用域：成员变量 / 使用场景：备注
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/PayOrderEntity.kt

### PayOrderEntity
- data class / 注解：@Entity(tableName = "pay_orders") / 职责：付款单的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：付款单唯一标识
- 建议：无

#### orderNo: String
- 作用域：成员变量 / 使用场景：付款单编号
- 建议：无

#### supplierId: Long?
- 作用域：成员变量 / 使用场景：供应商 ID
- 建议：无

#### supplierName: String
- 作用域：成员变量 / 使用场景：供应商名称
- 建议：无

#### amount: Double
- 作用域：成员变量 / 使用场景：金额
- 建议：同上精度建议

#### method: Int
- 作用域：成员变量 / 使用场景：支付方式
- 建议：无

#### referenceNo: String?
- 作用域：成员变量 / 使用场景：参考编号
- 建议：无

#### notes: String?
- 作用域：成员变量 / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=待付款, 1=已付款, 2=已取消）
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/ProductEntity.kt

### ProductEntity
- data class / 注解：@Entity(tableName = "products") / 职责：商品的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：商品唯一标识
- 建议：无

#### code: String
- 作用域：成员变量 / 使用场景：商品编码
- 建议：无

#### name: String
- 作用域：成员变量 / 使用场景：商品名称
- 建议：无

#### category: String
- 作用域：成员变量 / 使用场景：分类
- 建议：无

#### unit: String
- 作用域：成员变量 / 使用场景：计量单位
- 建议：无

#### salePrice: Double
- 作用域：成员变量 / 使用场景：销售价格
- 建议：同上精度建议

#### purchasePrice: Double
- 作用域：成员变量 / 使用场景：采购价格
- 建议：同上

#### stock: Double
- 作用域：成员变量 / 使用场景：当前库存量
- 建议：库存量使用 Double 不常见，若商品均为整数件数建议改用 Int/Long

#### safeStock: Double
- 作用域：成员变量 / 使用场景：安全库存量
- 建议：同上

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=停用, 1=正常）
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / 使用场景：同步状态
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / 使用场景：同步版本号
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/PurchaseOrderEntity.kt

### PurchaseOrderEntity
- data class / 注解：@Entity(tableName = "purchase_orders") / 职责：采购订单的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：采购订单唯一标识
- 建议：无

#### orderNo: String
- 作用域：成员变量 / 使用场景：订单编号
- 建议：无

#### supplierName: String
- 作用域：成员变量 / 使用场景：供应商名称
- 建议：缺少 supplierId 字段，无法关联供应商实体

#### totalAmount: Double
- 作用域：成员变量 / 使用场景：总金额
- 建议：同上精度建议

#### notes: String?
- 作用域：成员变量 / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=草稿, 1=已收货）
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/SaleOrderEntity.kt

### SaleOrderEntity
- data class / 注解：@Entity(tableName = "sale_orders") / 职责：销售订单的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：销售订单唯一标识
- 建议：无

#### orderNo: String
- 作用域：成员变量 / 使用场景：订单编号
- 建议：无

#### customerId: Long?
- 作用域：成员变量 / 使用场景：客户 ID
- 建议：无

#### customerName: String?
- 作用域：成员变量 / 使用场景：客户名称
- 建议：无

#### subtotalAmount: Double
- 作用域：成员变量 / 使用场景：小计金额
- 建议：同上精度建议

#### discountAmount: Double
- 作用域：成员变量 / 使用场景：折扣金额
- 建议：同上

#### totalAmount: Double
- 作用域：成员变量 / 使用场景：总金额
- 建议：同上

#### paidAmount: Double
- 作用域：成员变量 / 使用场景：已付金额
- 建议：同上

#### notes: String?
- 作用域：成员变量 / 使用场景：备注
- 建议：无

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=草稿, 1=已完成, 2=已取消）
- 建议：无

#### createdAt: Long
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/SupplierEntity.kt

### SupplierEntity
- data class / 注解：@Entity(tableName = "suppliers") / 职责：供应商的数据库实体 / 设计模式：数据实体

#### id: Long
- 作用域：@PrimaryKey / 使用场景：供应商唯一标识
- 建议：无

#### name: String
- 作用域：成员变量 / 使用场景：供应商名称
- 建议：无

#### phone: String
- 作用域：成员变量 / 使用场景：联系电话
- 建议：无

#### address: String?
- 作用域：成员变量 / 使用场景：地址
- 建议：无

#### notes: String?
- 作用域：成员变量 / 使用场景：备注
- 建议：无

#### balance: Double
- 作用域：成员变量 / 使用场景：账户余额
- 建议：同上精度建议

#### status: Int
- 作用域：成员变量 / 使用场景：状态（0=停用, 1=启用）
- 建议：无

#### syncStatus: Int?
- 作用域：成员变量 / 使用场景：同步状态
- 建议：无

#### syncVersion: Long?
- 作用域：成员变量 / 使用场景：同步版本号
- 建议：无

#### createdAt: Long?
- 作用域：成员变量 / 使用场景：创建时间戳
- 建议：无

#### updatedAt: Long?
- 作用域：成员变量 / 使用场景：更新时间戳
- 建议：无

---

## entity/SyncCursorEntity.kt

### SyncCursorEntity
- data class / 注解：@Entity(tableName = "sync_cursors") / 职责：同步游标的数据库实体，记录各实体类型的同步进度 / 设计模式：数据实体

#### entityType: String
- 作用域：@PrimaryKey / 使用场景：实体类型标识（如 "products", "customers" 等）
- 建议：无

#### cursor: String
- 作用域：成员变量 / 使用场景：同步游标值
- 建议：无

#### updatedAt: Long
- 作用域：成员变量 / 使用场景：最后同步时间戳
- 建议：无
