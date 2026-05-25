# Android API 契约与 Retrofit 草案

本文档从当前后端控制器和 DTO 反推，供重建 Android App 时直接创建 `core:model` 与 `core:network` 使用。

## 1. 通用响应

所有 JSON 接口默认返回：

```kotlin
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val timestamp: Long,
)
```

约定：

- `code == 0` 表示成功。
- 参数错误通常是 HTTP `400` + `code=400`。
- 业务错误通常是 HTTP `422` + `code=422`。
- 服务未配置通常是 HTTP `503` + `code=503`。
- 大多数接口使用 `snake_case`，Agent 接口使用 `lowerCamelCase`。

## 2. Retrofit 接口

```kotlin
interface ZhihuijiApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthResult>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResult>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<AuthResult>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String?): ApiResponse<Unit>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): ApiResponse<VerifyCodeResponse>

    @GET("auth/users/me")
    suspend fun me(@Header("Authorization") authorization: String): ApiResponse<UserProfile>

    @GET("products")
    suspend fun products(@Query("keyword") keyword: String? = null): ApiResponse<List<ProductDto>>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Long): ApiResponse<ProductDto>

    @GET("products/by-code")
    suspend fun productByCode(@Query("code") code: String): ApiResponse<ProductDto?>

    @POST("products")
    suspend fun createProduct(@Body body: ProductDto): ApiResponse<ProductDto>

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body body: ProductDto): ApiResponse<ProductDto>

    @POST("products/{id}/adjust-stock")
    suspend fun adjustStock(@Path("id") id: Long, @Body body: ProductAdjustStockRequest): ApiResponse<ProductDto>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): ApiResponse<Unit>

    @GET("customers")
    suspend fun customers(@Query("keyword") keyword: String? = null): ApiResponse<List<CustomerDto>>

    @GET("customers/{id}")
    suspend fun customer(@Path("id") id: Long): ApiResponse<CustomerDto>

    @POST("customers")
    suspend fun createCustomer(@Body body: CustomerDto): ApiResponse<CustomerDto>

    @PUT("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body body: CustomerDto): ApiResponse<CustomerDto>

    @DELETE("customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: Long): ApiResponse<Unit>

    @GET("suppliers")
    suspend fun suppliers(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<SupplierDto>>

    @GET("suppliers/{id}")
    suspend fun supplier(@Path("id") id: Long): ApiResponse<SupplierDto>

    @POST("suppliers")
    suspend fun createSupplier(@Body body: SupplierDto): ApiResponse<SupplierDto>

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: Long, @Body body: SupplierDto): ApiResponse<SupplierDto>

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: Long): ApiResponse<Unit>

    @GET("sale-orders")
    suspend fun saleOrders(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("min_total_amount") minTotalAmount: String? = null,
        @Query("max_total_amount") maxTotalAmount: String? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
        @Query("product_keyword") productKeyword: String? = null,
        @Query("payment_status") paymentStatus: String? = null,
    ): ApiResponse<List<SaleOrderDto>>

    @GET("sale-orders/{id}")
    suspend fun saleOrder(@Path("id") id: Long): ApiResponse<SaleOrderDto>

    @POST("sale-orders")
    suspend fun createSaleOrder(@Body body: CreateSaleOrderRequest): ApiResponse<SaleOrderDto>

    @PUT("sale-orders/{id}/draft")
    suspend fun updateSaleDraft(@Path("id") id: Long, @Body body: UpdateSaleDraftRequest): ApiResponse<SaleOrderDto>

    @POST("sale-orders/{id}/payments")
    suspend fun addSalePayment(@Path("id") id: Long, @Body body: PaymentRequest): ApiResponse<PaymentDto>

    @GET("sale-orders/{id}/payments")
    suspend fun salePayments(@Path("id") id: Long): ApiResponse<List<PaymentDto>>

    @PUT("sale-orders/{id}/status")
    suspend fun updateSaleStatus(@Path("id") id: Long, @Body body: StatusRequest): ApiResponse<Unit>

    @PUT("sale-orders/{id}/cancel")
    suspend fun cancelSaleOrder(@Path("id") id: Long): ApiResponse<SaleOrderDto>

    @GET("purchase-orders")
    suspend fun purchaseOrders(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<PurchaseOrderDto>>

    @GET("purchase-orders/{id}")
    suspend fun purchaseOrder(@Path("id") id: Long): ApiResponse<PurchaseOrderDto>

    @POST("purchase-orders")
    suspend fun createPurchaseOrder(@Body body: CreatePurchaseOrderRequest): ApiResponse<PurchaseOrderDto>

    @GET("pay-orders")
    suspend fun payOrders(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
    ): ApiResponse<List<PayOrderDto>>

    @GET("pay-orders/{id}")
    suspend fun payOrder(@Path("id") id: Long): ApiResponse<PayOrderDto>

    @POST("pay-orders")
    suspend fun createPayOrder(@Body body: CreatePayOrderRequest): ApiResponse<PayOrderDto>

    @PUT("pay-orders/{id}/status")
    suspend fun updatePayOrderStatus(@Path("id") id: Long, @Body body: StatusRequest): ApiResponse<PayOrderDto>

    @GET("finance-records")
    suspend fun financeRecords(
        @Query("keyword") keyword: String? = null,
        @Query("type") type: Int? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
    ): ApiResponse<List<FinanceRecordDto>>

    @POST("finance-records")
    suspend fun createFinanceRecord(@Body body: CreateFinanceRecordRequest): ApiResponse<FinanceRecordDto>

    @GET("reports/sales-summary")
    suspend fun salesSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<SalesSummaryReportDto>

    @GET("reports/profit-summary")
    suspend fun profitSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<ProfitSummaryReportDto>

    @GET("reports/refund-records")
    suspend fun refundRecords(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<RefundRecordReportDto>>

    @GET("reports/stock-out-records")
    suspend fun stockOutRecords(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<StockOutRecordReportDto>>

    @GET("reports/top-products")
    suspend fun topProducts(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<TopSellingProductReportDto>>

    @GET("reports/profit-by-products")
    suspend fun profitByProducts(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<ProfitByProductReportDto>>

    @GET("reports/profit-by-customers")
    suspend fun profitByCustomers(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<ProfitByCustomerReportDto>>

    @GET("reports/inventory-flow")
    suspend fun inventoryFlow(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<InventoryFlowRecordDto>>

    @GET("reports/customer-sales")
    suspend fun customerSales(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<CustomerSalesReportDto>>

    @GET("reports/top-receivable-customers")
    suspend fun topReceivableCustomers(@Query("limit") limit: Int = 10): ApiResponse<List<CustomerReceivableReportDto>>

    @GET("reports/low-stock-products")
    suspend fun lowStockProducts(@Query("limit") limit: Int = 10): ApiResponse<List<LowStockProductReportDto>>

    @GET("reports/reconciliation-summary")
    suspend fun reconciliationSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<ReconciliationSummaryReportDto>

    @GET("sync/health")
    suspend fun syncHealth(): ApiResponse<SyncHealthResult>

    @POST("sync/pull")
    suspend fun pull(@Body body: PullRequest): ApiResponse<PullResult>

    @POST("sync/upload")
    suspend fun upload(@Body body: UploadRequest): ApiResponse<UploadResult>

    @GET("agent/workbench")
    suspend fun agentWorkbench(@Query("window_days") windowDays: Int = 7, @Query("limit") limit: Int = 6, @Query("aging_days") agingDays: Int = 15): ApiResponse<AgentWorkbenchDto>

    @POST("agent/query")
    suspend fun agentQuery(@Body body: AgentQueryRequest): ApiResponse<AgentAnswerDto>

    @POST("agent/operation-draft")
    suspend fun operationDraft(@Body body: OperationDraftRequest): ApiResponse<OperationDraftDto>

    @POST("agent/operation-submit")
    suspend fun operationSubmit(@Body body: OperationSubmitRequest): ApiResponse<OperationSubmitResultDto>

    @POST("agent/tasks")
    suspend fun createAgentTask(@Body body: CreateAgentTaskRequest): ApiResponse<AgentTaskSummaryDto>

    @GET("agent/tasks")
    suspend fun agentTasks(): ApiResponse<List<AgentTaskSummaryDto>>

    @GET("agent/tasks/{taskId}")
    suspend fun agentTask(@Path("taskId") taskId: Long): ApiResponse<AgentTaskDetailDto>

    @GET("agent/notifications")
    suspend fun notifications(@Query("unread_only") unreadOnly: Boolean = false, @Query("undelivered_only") undeliveredOnly: Boolean = false): ApiResponse<List<AgentNotificationDto>>

    @POST("agent/notifications/{notificationId}/read")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: Long): ApiResponse<AgentNotificationDto>

    @POST("agent/notifications/{notificationId}/delivered")
    suspend fun markNotificationDelivered(@Path("notificationId") notificationId: Long): ApiResponse<AgentNotificationDto>
}
```

`GET sale-orders/{id}/pdf` 返回 `application/pdf`，建议另建 `@Streaming` 下载接口，或直接用浏览器打开完整 URL。

## 3. 核心 Kotlin DTO

下面 DTO 使用 Kotlinx Serialization 写法。若使用 Moshi，把 `@SerialName` 改成 `@Json(name = "...")` 即可。

```kotlin
@Serializable data class RegisterRequest(val phone: String, val password: String, val verifyCode: String)
@Serializable data class LoginRequest(val phone: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class VerifyCodeRequest(val phone: String, val type: String)
@Serializable data class VerifyCodeResponse(val success: Boolean, val expireSeconds: Int)
@Serializable data class AuthResult(val userId: Long, val token: String, val refreshToken: String, val expiresIn: Int)
@Serializable data class UserProfile(val id: Long, val phone: String, val nickname: String, val status: Int)

@Serializable
data class ProductDto(
    val id: Long? = null,
    val code: String,
    val name: String,
    val category: String,
    val unit: String,
    val salePrice: Double,
    val purchasePrice: Double,
    val stock: Double,
    val safeStock: Double,
    val status: Int = 1,
    val syncStatus: Int? = null,
    val syncVersion: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class CustomerDto(
    val id: Long? = null,
    val name: String,
    val phone: String,
    val level: Int = 0,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    val syncStatus: Int? = null,
    val syncVersion: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class SupplierDto(
    val id: Long? = null,
    val name: String,
    val phone: String,
    val address: String? = null,
    val notes: String? = null,
    val balance: Double = 0.0,
    val status: Int = 1,
    val syncStatus: Int? = null,
    val syncVersion: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable data class ProductAdjustStockRequest(val delta: Double, val reason: String? = null, val operator: String? = null)
@Serializable data class StatusRequest(val status: Int)
```

如果序列化库没有全局 `snake_case` 策略，需要给 `salePrice/safeStock/syncStatus` 等字段加 `@SerialName("sale_price")`。Agent DTO 不要加 snake_case。

## 4. 订单 DTO

```kotlin
@Serializable
data class CreateSaleOrderRequest(
    val customerId: Long? = null,
    val customerName: String? = null,
    val items: List<CreateSaleOrderItemRequest>,
    val notes: String? = null,
    val discountAmount: Double = 0.0,
)

@Serializable data class CreateSaleOrderItemRequest(val productId: Long, val quantity: Double, val unitPrice: Double)
@Serializable data class UpdateSaleDraftRequest(val discountAmount: Double? = null, val notes: String? = null)
@Serializable data class PaymentRequest(val amount: Double, val method: Int, val referenceNo: String? = null)

@Serializable
data class SaleOrderDto(
    val id: Long,
    val orderNo: String,
    val customerId: Long? = null,
    val customerName: String? = null,
    val items: List<SaleOrderItemDto>,
    val subtotalAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val notes: String? = null,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class SaleOrderItemDto(
    val id: Long,
    val orderId: Long,
    val productId: Long,
    val productCode: String,
    val productName: String,
    val customerId: Long? = null,
    val customerName: String? = null,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
    val createdAt: Long,
)

@Serializable
data class PaymentDto(
    val id: Long,
    val orderId: Long,
    val amount: Double,
    val method: Int,
    val referenceNo: String? = null,
    val type: Int,
    val createdAt: Long,
)

@Serializable
data class CreatePurchaseOrderRequest(
    val supplierName: String,
    val items: List<CreatePurchaseOrderItemRequest>,
    val notes: String? = null,
    val status: Int? = null,
)

@Serializable data class CreatePurchaseOrderItemRequest(val productId: Long? = null, val productCode: String? = null, val productName: String? = null, val quantity: Double, val unitCost: Double)

@Serializable
data class PurchaseOrderDto(
    val id: Long,
    val orderNo: String,
    val supplierName: String,
    val items: List<PurchaseOrderItemDto>,
    val totalAmount: Double,
    val notes: String? = null,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable data class PurchaseOrderItemDto(val id: Long, val orderId: Long, val productCode: String, val productName: String, val quantity: Double, val unitCost: Double, val amount: Double, val createdAt: Long)

@Serializable data class CreatePayOrderRequest(val supplierId: Long? = null, val supplierName: String? = null, val amount: Double, val method: Int, val referenceNo: String? = null, val notes: String? = null, val status: Int? = null)
@Serializable data class PayOrderDto(val id: Long, val orderNo: String, val supplierId: Long? = null, val supplierName: String, val amount: Double, val method: Int, val referenceNo: String? = null, val notes: String? = null, val status: Int, val createdAt: Long, val updatedAt: Long)

@Serializable data class CreateFinanceRecordRequest(val type: Int, val category: String, val partnerName: String? = null, val amount: Double, val method: Int? = null, val notes: String? = null)
@Serializable data class FinanceRecordDto(val id: Long, val recordNo: String, val type: Int, val category: String, val partnerName: String? = null, val amount: Double, val method: Int, val notes: String? = null, val createdAt: Long, val updatedAt: Long)
```

## 5. 同步 DTO

```kotlin
@Serializable data class SyncHealthResult(val status: String, val message: String, val serverTime: Long)
@Serializable data class SyncChangeDto(val entityType: String, val entityId: String, val operation: String, val payload: String, val updatedAt: Long)
@Serializable data class PullRequest(val sinceCursor: String? = null, val limit: Int? = 200)
@Serializable data class PullResult(val changes: List<SyncChangeDto>, val nextCursor: String, val hasMore: Boolean)
@Serializable data class UploadRequest(val clientId: String, val changes: List<SyncChangeDto>, val lastSyncCursor: String? = null)
@Serializable data class UploadResult(val acceptedCount: Int, val failedCount: Int, val message: String, val nextCursor: String)
```

`payload` 是后端序列化后的 JSON 字符串，Android 端需要按 `entityType` 再反序列化为本地表。

## 6. Agent DTO 摘要

Agent 接口字段为 lowerCamelCase：

```kotlin
@Serializable data class AgentQueryRequest(val query: String)
@Serializable data class OperationDraftRequest(val instruction: String)
@Serializable data class OperationSubmitRequest(val draft: OperationDraftDto)
@Serializable data class CreateAgentTaskRequest(val taskType: String, val title: String, val input: String? = null)

@Serializable
data class AgentAnswerDto(
    val query: String,
    val intent: String,
    val answer: String,
    val highlights: List<String>,
    val columns: List<String>,
    val rows: List<List<String>>,
    val suggestedActions: List<String>,
)

@Serializable
data class OperationDraftDto(
    val operationType: String,
    val summary: String,
    val partnerRole: String,
    val partnerId: Long? = null,
    val partnerName: String,
    val items: List<OperationDraftItemDto>,
    val notes: String? = null,
    val canSubmit: Boolean,
    val warnings: List<String>,
    val suggestedActions: List<String>,
)

@Serializable data class OperationDraftItemDto(val productId: Long? = null, val productCode: String, val productName: String, val quantity: Double, val unitPrice: Double, val amount: Double, val currentStock: Double)
@Serializable data class OperationSubmitResultDto(val operationType: String, val orderId: Long? = null, val orderNo: String? = null, val message: String, val nextAction: String)
```

完整 Agent DTO 较多，建议前端第一版只实现 `workbench/query/operation-draft/operation-submit/tasks/notifications` 的展示字段，复杂的 `renderBlocks` 可以做通用渲染器：`hero`、`metric_grid`、`bullet_list`、`table`、`chart`、`draft`。

## 7. 报表 DTO 建议

报表接口全部使用毫秒时间戳：

```kotlin
@Serializable data class SalesSummaryReportDto(val startAt: Long, val endAt: Long, val totalSalesAmount: Double, val totalPaidAmount: Double, val totalRefundAmount: Double, val totalUnpaidAmount: Double, val totalOrderCount: Int)
@Serializable data class ProfitSummaryReportDto(val startAt: Long, val endAt: Long, val estimatedCostAmount: Double, val estimatedProfitAmount: Double, val estimatedProfitRate: Double)
@Serializable data class CustomerReceivableReportDto(val customerId: Long, val customerName: String, val phone: String, val balance: Double)
@Serializable data class LowStockProductReportDto(val productId: Long, val productCode: String, val productName: String, val stock: Double, val safeStock: Double)
@Serializable data class ReconciliationSummaryReportDto(val startAt: Long, val endAt: Long, val totalReceivableAmount: Double, val totalPayableAmount: Double, val totalReceivedAmount: Double, val totalPaidAmount: Double, val netCashFlow: Double)
```

其他明细报表按 Retrofit 方法名逐个补齐即可，字段以 `ReportDto.java` 为准。

## 8. 当前后端对前端的注意点

- README 中部分报表 URL 是旧路径，Android 端应以本文档和 Controller 代码为准。
- `POST /v1/auth/verify-code` 当前固定返回失败，注册实际用 `verifyCode` 字段提交邀请码。
- `SecurityConfig` 当前允许所有请求通过，只有 `/auth/users/me` 等个别接口在服务层校验 token。App 仍应实现 token 头，便于后端后续收紧权限。
- `POST /v1/sync/upload` 当前不会把离线变更落到商品/订单等业务表，只保存游标。离线写入要等后端增强后再开放。
- `Product/Customer/Supplier` 创建更新直接接收实体字段，前端要避免提交只读字段导致混淆；建议 UI Draft 转 DTO 时只填业务字段。
- `SaleOrderDto` 当前不包含 payments，付款明细需要额外调用 `GET /v1/sale-orders/{id}/payments`。

