# Android Data 层 - Sync 子模块详尽代码分析

> 自动生成于 2026-05-28，覆盖 sync 子模块全部 1 个 Kotlin 源文件

---

## 1. SyncRepository

- **文件路径**: `data/sync/src/main/java/com/zhihuiji/data/sync/SyncRepository.kt`
- **父类/接口**: 无
- **注解**: `@Singleton`
- **职责**: 负责客户端与服务端的数据同步，包括拉取变更、上传变更、健康检查和游标管理
- **设计模式**: Repository 模式 + 单例模式（通过 Hilt `@Singleton`）

### 类属性

##### api: ZhihuijiApi
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：调用服务端同步相关 API
- 建议：无

##### syncPreferenceStore: SyncPreferenceStore
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：读取/保存同步偏好设置（游标的备用存储）
- 建议：无

##### settingsStore: SettingsStore
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：获取 clientId 用于上传请求
- 建议：无

##### syncCursorDao: SyncCursorDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：数据库层面的游标持久化
- 建议：无

##### productDao: ProductDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用产品变更到本地数据库
- 建议：无

##### customerDao: CustomerDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用客户变更到本地数据库
- 建议：无

##### supplierDao: SupplierDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用供应商变更到本地数据库
- 建议：无

##### saleOrderDao: SaleOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用销售单变更到本地数据库
- 建议：无

##### purchaseOrderDao: PurchaseOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用采购单变更到本地数据库
- 建议：无

##### payOrderDao: PayOrderDao
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：应用付款单变更到本地数据库
- 建议：无

##### json: Json
- 作用域：类私有（constructor 注入）
- 初始值：由 Hilt 注入
- 使用场景：解析同步变更的 payload JSON 字符串
- 建议：无

### companion object 常量

##### GLOBAL_CURSOR_KEY: String = "server_pull"
- 作用域：companion object
- 初始值：`"server_pull"`
- 使用场景：作为全局游标的 entityType 键名
- 建议：无

##### DEFAULT_PULL_LIMIT: Int = 200
- 作用域：companion object
- 初始值：`200`
- 使用场景：每次拉取变更的默认条数限制
- 建议：考虑将此值提取到配置中，便于运行时调整

### 函数/方法

##### healthCheck(): Result<SyncHealthResult>
- 参数：无
- 返回值：`Result<SyncHealthResult>` - 同步服务健康状态
- 实现逻辑：调用 `safeApiCall` 包装 `api.syncHealth()` 请求
- 调用关系：调用了 `ZhihuijiApi.syncHealth()`，被 `SettingsViewModel.loadSettings()` 调用
- 建议：无

##### pull(): Result<PullResult>
- 参数：无
- 返回值：`Result<PullResult>` - 拉取结果
- 实现逻辑：循环分页拉取服务端变更，每次请求 DEFAULT_PULL_LIMIT 条，应用变更到本地数据库，持久化游标，直到没有更多数据（`!page.hasMore`）或变更列表为空（`page.changes.isEmpty()`）
- 调用关系：调用了 `loadCursor()`、`safeApiCall()`、`api.pull()`、`applyPulledChanges()`、`persistCursor()`，被 `runManualSync()` 调用
- 建议：循环中如果某页 `applyPulledChanges` 抛出异常，当前实现会直接返回失败。建议增加部分失败容错机制

##### runManualSync(): Result<PullResult>
- 参数：无
- 返回值：`Result<PullResult>` - 同步结果
- 实现逻辑：直接委托给 `pull()` 方法
- 调用关系：调用了 `pull()`，被 `SettingsViewModel.runManualSync()` 调用
- 建议：无

##### upload(changes: List<SyncChangeDto>): Result<UploadResult>
- 参数：`changes: List<SyncChangeDto>` - 待上传的变更列表
- 返回值：`Result<UploadResult>` - 上传结果
- 实现逻辑：获取 clientId 和当前游标，调用 API 上传变更，成功后持久化新游标
- 调用关系：调用了 `settingsStore.ensureClientId()`、`loadCursor()`、`safeApiCall()`、`api.upload()`、`persistCursor()`
- 建议：无

##### applyPulledChanges(result: PullResult)
- 参数：`result: PullResult` - 拉取结果
- 返回值：无
- 实现逻辑：遍历 result 中的所有变更，根据 entityType 分发到对应的 apply 方法（customer、supplier、product、sale_order、purchase_order、pay_order）
- 调用关系：调用了 `applyCustomerChange()`、`applySupplierChange()`、`applyProductChange()`、`applySaleOrderChange()`、`applyPurchaseOrderChange()`、`applyPayOrderChange()`，被 `pull()` 调用
- 建议：当前使用 when 字符串匹配 entityType，如果服务端新增实体类型会静默忽略。建议增加 else 分支记录未知类型

##### clearSyncState()
- 参数：无
- 返回值：无
- 实现逻辑：清除同步偏好存储（`syncPreferenceStore.clearAll()`）和数据库游标（`syncCursorDao.clear()`）
- 调用关系：调用了 `syncPreferenceStore.clearAll()`、`syncCursorDao.clear()`
- 建议：无

##### loadCursor(): String (private)
- 参数：无
- 返回值：`String` - 当前游标值
- 实现逻辑：先从数据库查询游标（`syncCursorDao.findByEntityType(GLOBAL_CURSOR_KEY)?.cursor`），如果为空则从偏好存储中读取（`syncPreferenceStore.observeCursor(GLOBAL_CURSOR_KEY).first()`）
- 调用关系：调用了 `syncCursorDao.findByEntityType()`、`syncPreferenceStore.observeCursor()`，被 `pull()`、`upload()` 调用
- 建议：双重存储（数据库 + 偏好）增加了复杂性，建议统一到数据库存储

##### persistCursor(cursor: String) (private)
- 参数：`cursor: String` - 要持久化的游标值
- 返回值：无
- 实现逻辑：如果游标非空，同时写入数据库（`syncCursorDao.upsert()`）和偏好存储（`syncPreferenceStore.saveCursor()`）
- 调用关系：调用了 `syncCursorDao.upsert()`、`syncPreferenceStore.saveCursor()`，被 `pull()`、`upload()` 调用
- 建议：同 loadCursor，建议统一存储策略

##### applyCustomerChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：解析 entityId（`change.entityId.toLongOrNull()`），如果是删除操作则调用 `customerDao.deleteById()`，否则解析 payload 并构建 CustomerEntity 进行 upsert
- 调用关系：调用了 `parsePayload()`、`customerDao.deleteById()`、`customerDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### applySupplierChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：与 applyCustomerChange 类似，针对供应商实体
- 调用关系：调用了 `parsePayload()`、`supplierDao.deleteById()`、`supplierDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### applyProductChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：与 applyCustomerChange 类似，针对产品实体
- 调用关系：调用了 `parsePayload()`、`productDao.deleteById()`、`productDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### applySaleOrderChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：与 applyCustomerChange 类似，针对销售单实体
- 调用关系：调用了 `parsePayload()`、`saleOrderDao.deleteById()`、`saleOrderDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### applyPurchaseOrderChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：与 applyCustomerChange 类似，针对采购单实体
- 调用关系：调用了 `parsePayload()`、`purchaseOrderDao.deleteById()`、`purchaseOrderDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### applyPayOrderChange(change: SyncChangeDto) (private)
- 参数：`change: SyncChangeDto` - 同步变更对象
- 返回值：无
- 实现逻辑：与 applyCustomerChange 类似，针对付款单实体
- 调用关系：调用了 `parsePayload()`、`payOrderDao.deleteById()`、`payOrderDao.upsert()`，被 `applyPulledChanges()` 调用
- 建议：无

##### parsePayload(payload: String): JsonObject (private)
- 参数：`payload: String` - JSON 字符串
- 返回值：`JsonObject` - 解析后的 JSON 对象
- 实现逻辑：使用 kotlinx.serialization 解析 JSON 字符串（`json.parseToJsonElement(payload).jsonObject`），失败时返回空 JsonObject（`JsonObject(emptyMap())`）
- 调用关系：被所有 applyXxxChange 方法调用
- 建议：无

##### JsonObject.string(key: String): String (private extension)
- 参数：`key: String` - JSON 字段名
- 返回值：`String` - 字段值，不存在时返回空字符串
- 实现逻辑：委托给 stringOrNull，空时返回空字符串
- 调用关系：被 applyXxxChange 方法调用
- 建议：无

##### JsonObject.stringOrNull(key: String): String? (private extension)
- 参数：`key: String` - JSON 字段名
- 返回值：`String?` - 字段值，不存在时返回 null
- 实现逻辑：从 JsonObject 中取值并提取原始字符串内容（`this[key]?.jsonPrimitive?.contentOrNull`）
- 调用关系：被 string() 和 applyXxxChange 方法调用
- 建议：无

##### JsonObject.int(key: String, default: Int = 0): Int (private extension)
- 参数：`key: String` - JSON 字段名；`default: Int = 0` - 默认值
- 返回值：`Int` - 字段整数值
- 实现逻辑：委托给 intOrNull，空时返回默认值
- 调用关系：被 applyXxxChange 方法调用
- 建议：无

##### JsonObject.intOrNull(key: String): Int? (private extension)
- 参数：`key: String` - JSON 字段名
- 返回值：`Int?` - 字段整数值
- 实现逻辑：从 JsonObject 中取值并转换为 Int（`this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()`）
- 调用关系：被 int() 和 applyXxxChange 方法调用
- 建议：无

##### JsonObject.long(key: String, default: Long = 0L): Long (private extension)
- 参数：`key: String` - JSON 字段名；`default: Long = 0L` - 默认值
- 返回值：`Long` - 字段长整数值
- 实现逻辑：委托给 longOrNull，空时返回默认值
- 调用关系：被 applyXxxChange 方法调用
- 建议：无

##### JsonObject.longOrNull(key: String): Long? (private extension)
- 参数：`key: String` - JSON 字段名
- 返回值：`Long?` - 字段长整数值
- 实现逻辑：从 JsonObject 中取值并委托给 `JsonElement.toLongValue()`
- 调用关系：被 long() 和 applyXxxChange 方法调用
- 建议：无

##### JsonObject.double(key: String, default: Double = 0.0): Double (private extension)
- 参数：`key: String` - JSON 字段名；`default: Double = 0.0` - 默认值
- 返回值：`Double` - 字段双精度浮点值
- 实现逻辑：委托给 doubleOrNull，空时返回默认值
- 调用关系：被 applyXxxChange 方法调用
- 建议：无

##### JsonObject.doubleOrNull(key: String): Double? (private extension)
- 参数：`key: String` - JSON 字段名
- 返回值：`Double?` - 字段双精度浮点值
- 实现逻辑：从 JsonObject 中取值并委托给 `JsonElement.toDoubleValue()`
- 调用关系：被 double() 和 applyXxxChange 方法调用
- 建议：无

##### JsonElement.toLongValue(): Long? (private extension)
- 参数：无
- 返回值：`Long?` - 转换后的长整数值
- 实现逻辑：提取原始字符串内容并转换为 Long（`jsonPrimitive.contentOrNull?.toLongOrNull()`）
- 调用关系：被 longOrNull() 调用
- 建议：无

##### JsonElement.toDoubleValue(): Double? (private extension)
- 参数：无
- 返回值：`Double?` - 转换后的双精度浮点值
- 实现逻辑：提取原始字符串内容并转换为 Double（`jsonPrimitive.contentOrNull?.toDoubleOrNull()`）
- 调用关系：被 doubleOrNull() 调用
- 建议：无
