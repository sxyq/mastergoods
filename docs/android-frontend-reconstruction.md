# Android 前端重建框架推导

本文档根据当前后端代码反推 Android App 的项目结构、页面模块、数据流与实现顺序。后端是 `zhihuiji-backend`，基础路径默认为 `http://<host>:18080/v1`。

## 1. 推荐技术栈

- 语言与 UI：Kotlin + Jetpack Compose + Material 3
- 架构：MVVM + Repository + UseCase 按需引入
- 依赖注入：Hilt
- 网络：Retrofit + OkHttp + Kotlinx Serialization 或 Moshi
- 本地存储：Room 保存业务表，DataStore 保存登录态、服务地址、同步游标
- 异步：Kotlin Coroutines + Flow
- 后台同步：WorkManager
- PDF：系统浏览器/下载管理器打开 `GET /v1/sale-orders/{id}/pdf`
- 实时通知：OkHttp SSE 或 EventSource 客户端连接 `/v1/agent/notifications/stream`

后端默认 JSON 字段是 `snake_case`，但 `/v1/agent/*` 的 Agent DTO 明确使用 `lowerCamelCase`。Android 端建议在 DTO 层保留后端字段名映射，不要把网络 DTO 直接当 UI 状态使用。

## 2. Gradle 模块建议

```text
master-goods-android/
  settings.gradle.kts
  build.gradle.kts
  app/
    src/main/java/com/zhihuiji/app/MainActivity.kt
    src/main/java/com/zhihuiji/app/ZhihuijiApp.kt
    src/main/java/com/zhihuiji/app/navigation/AppNavGraph.kt
  core/
    common/
      ResultExt.kt
      MoneyFormatter.kt
      TimeFormatter.kt
      StatusLabels.kt
    model/
      AuthModels.kt
      ProductModels.kt
      PartyModels.kt
      OrderModels.kt
      ReportModels.kt
      AgentModels.kt
      SyncModels.kt
    network/
      ZhihuijiApi.kt
      ApiResponse.kt
      AuthInterceptor.kt
      NetworkModule.kt
    database/
      ZhihuijiDatabase.kt
      entity/
      dao/
    datastore/
      SessionStore.kt
      SettingsStore.kt
  data/
    auth/
    product/
    customer/
    supplier/
    order/
    finance/
    report/
    agent/
    sync/
  feature/
    auth/
    dashboard/
    products/
    customers/
    suppliers/
    sales/
    purchases/
    payments/
    finance/
    reports/
    agent/
    settings/
```

如果先追求尽快恢复可用 App，可以先只建 `app + core:network + core:model + data + feature/*`，Room 和 WorkManager 放到第二阶段。

## 3. App 主导航

建议底部导航保留 5 个一级入口：

- 首页：经营看板、低库存、应收应付、Agent 摘要
- 单据：销售单、采购单、付款单、资金流水
- 档案：商品、客户、供应商
- 报表：销售、利润、库存流水、对账
- 智能助手：问答、操作草稿、任务、通知

设置页放在顶部菜单或“我的”入口：

- 服务器地址
- 登录账号与退出
- 本机 clientId
- 同步状态与手动同步
- 本地缓存清理

## 4. 页面与接口映射

| 页面 | 后端接口 | 说明 |
| --- | --- | --- |
| 登录 | `POST /v1/auth/login` | 返回 token、refreshToken |
| 注册 | `POST /v1/auth/register` | 后端使用邀请码作为 verifyCode |
| 我的账号 | `GET /v1/auth/users/me` | 需传 `Authorization: Bearer <token>` |
| 商品列表 | `GET /v1/products?keyword=` | 支持名称/编码搜索 |
| 商品详情 | `GET /v1/products/{id}` | 展示价格、库存、安全库存 |
| 新建/编辑商品 | `POST /v1/products`, `PUT /v1/products/{id}` | 直接提交 Product 字段 |
| 调库存 | `POST /v1/products/{id}/adjust-stock` | delta 正数入库，负数出库 |
| 客户列表 | `GET /v1/customers?keyword=` | 支持名称/手机号搜索 |
| 供应商列表 | `GET /v1/suppliers?keyword=&status=` | 支持状态过滤 |
| 销售单列表 | `GET /v1/sale-orders` | 支持金额、日期、商品、收款状态过滤 |
| 开销售单 | `POST /v1/sale-orders` | 创建后扣库存，增加客户应收 |
| 收款 | `POST /v1/sale-orders/{id}/payments` | 金额不能超过未收款 |
| 取消销售单 | `PUT /v1/sale-orders/{id}/cancel` | 回滚库存，必要时生成退款记录 |
| 采购单列表 | `GET /v1/purchase-orders` | 支持关键字和状态 |
| 开采购单 | `POST /v1/purchase-orders` | `status=1` 会立即入库 |
| 付款单 | `GET/POST /v1/pay-orders` | `status=1` 会扣减供应商余额 |
| 资金流水 | `GET/POST /v1/finance-records` | 收入/支出手工流水 |
| 报表 | `GET /v1/reports/*` | 以毫秒时间戳传 `start_at/end_at` |
| 同步 | `GET /v1/sync/health`, `POST /v1/sync/pull`, `POST /v1/sync/upload` | 当前上传只持久化游标，拉取返回服务端变更 |
| Agent 工作台 | `GET /v1/agent/workbench` | 首页智能摘要可直接复用 |
| Agent 问答 | `POST /v1/agent/query` | 返回答案、表格、建议动作 |
| Agent 操作草稿 | `POST /v1/agent/operation-draft` | 可转采购/销售草稿 |
| Agent 任务 | `POST/GET /v1/agent/tasks` | 后台分析任务 |
| Agent 通知 | `GET /v1/agent/notifications` | 可标记已读、已送达 |

## 5. 状态值约定

| 类型 | 值 | 含义 |
| --- | --- | --- |
| 通用启停 `status` | `1` | 启用 |
| 通用启停 `status` | `0` | 停用 |
| 销售单 `status` | `0` | 草稿/未完成 |
| 销售单 `status` | `1` | 已完成 |
| 销售单 `status` | `2` | 已取消 |
| 销售单 `payment_status` 查询 | `0` | 未收齐 |
| 销售单 `payment_status` 查询 | `1` | 已收齐 |
| 收款 `type` | `1` | 收款 |
| 收款 `type` | `2` | 退款 |
| 采购单 `status` | `0` | 草稿 |
| 采购单 `status` | `1` | 已收货 |
| 付款单 `status` | `0` | 草稿 |
| 付款单 `status` | `1` | 已付款 |
| 付款单 `status` | `2` | 已取消 |
| 资金流水 `type` | `1` | 收入 |
| 资金流水 `type` | `2` | 支出 |
| 库存流水 `flow_type` | `0` | 出库 |
| 库存流水 `flow_type` | `1` | 入库 |
| 库存流水 `source_type` | `0` | 销售单 |
| 库存流水 `source_type` | `1` | 库存调整 |
| Agent 任务 `status` | `queued/running/completed/failed` | 排队/运行/完成/失败 |

支付方式 `method` 后端只校验为正整数，没有枚举。前端可以先约定：`1=现金`、`2=微信`、`3=支付宝`、`4=银行卡`、`5=其他`，并允许后续扩展。

## 6. 核心业务流

### 登录态

1. 用户登录成功后保存 `token`、`refreshToken`、`expiresIn`、`userId`。
2. 请求头统一加 `Authorization: Bearer <token>`，虽然当前后端多数接口未强制鉴权，但 App 端应按已登录设计。
3. 收到 `401` 或会话过期时调用 `POST /v1/auth/refresh`。
4. 退出登录调用 `POST /v1/auth/logout`，本地清理 DataStore 与 Room 缓存。

### 销售开单

1. 选择客户或录入散客名。
2. 选择商品，按当前库存限制数量。
3. 提交 `POST /v1/sale-orders`。
4. 后端会扣减商品库存，并把客户 `balance` 增加为应收。
5. 收款时调用 `POST /v1/sale-orders/{id}/payments`。
6. 收齐后后端自动把销售单置为 `status=1`。

### 采购入库

1. 选择供应商。
2. 选择商品，录入数量和进价。
3. `status=0` 保存草稿，不改库存。
4. `status=1` 保存已收货，后端会增加库存并更新商品进价。

### 同步策略

当前后端同步是偏“服务端变更拉取”的版本：

- `pull` 会返回 `customer/supplier/product/sale_order/purchase_order/pay_order` 的 upsert 变更。
- `upload` 当前只接受变更数量并保存游标，没有真正把业务变更写回业务表。
- 因此 Android 第一版建议在线优先，Room 作为缓存；离线编辑先不要默认开启，除非后端继续补上传应用逻辑。

建议本地保存：

- `clientId`：首次启动生成 UUID
- `lastSyncCursor`：每次 pull 成功后保存
- `lastSyncAt`：展示给用户

## 7. Compose 页面骨架

```text
AuthNavGraph
  LoginScreen
  RegisterScreen

MainNavGraph
  DashboardScreen
  DocumentHubScreen
    SaleOrderListScreen
    SaleOrderEditorScreen
    SaleOrderDetailScreen
    PurchaseOrderListScreen
    PurchaseOrderEditorScreen
    PayOrderListScreen
    FinanceRecordListScreen
  MasterDataScreen
    ProductListScreen
    ProductEditorScreen
    StockAdjustSheet
    CustomerListScreen
    CustomerEditorScreen
    SupplierListScreen
    SupplierEditorScreen
  ReportScreen
    SalesReportTab
    ProfitReportTab
    InventoryFlowTab
    ReconciliationTab
  AgentScreen
    AgentWorkbenchTab
    AgentChatTab
    OperationDraftTab
    AgentTaskTab
    NotificationTab
  SettingsScreen
```

## 8. Repository 分层

每个业务模块建议保持同一种形状：

```kotlin
class ProductRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val dao: ProductDao,
) {
    fun observeProducts(keyword: String): Flow<List<Product>>
    suspend fun refreshProducts(keyword: String?)
    suspend fun createProduct(draft: ProductDraft): Product
    suspend fun updateProduct(id: Long, draft: ProductDraft): Product
    suspend fun adjustStock(id: Long, delta: Double, reason: String?, operator: String?): Product
}
```

第一版如果不做 Room，可以把 Repository 直接包装 Retrofit，并让 ViewModel 用 `StateFlow<UiState>` 管状态。

## 9. 第一版最小可用范围

建议按下面顺序恢复：

1. 登录/注册/服务器地址设置
2. 商品、客户、供应商列表与编辑
3. 销售单开单、详情、收款、取消
4. 采购单开单、付款单、资金流水
5. 首页经营看板与报表
6. Agent 工作台、问答、操作草稿
7. Room 缓存、同步、SSE 通知

这样每一步都能和当前后端真实闭环，避免一上来把离线同步做重。

