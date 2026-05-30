package com.zhihuiji.core.network

import com.zhihuiji.core.model.*
import retrofit2.http.*

interface ZhihuijiApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthResult>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResult>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<AuthResult>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): ApiResponse<VerifyCodeResponse>

    @GET("auth/users/me")
    suspend fun me(): ApiResponse<UserProfile>

    @GET("products")
    suspend fun products(@Query("keyword") keyword: String? = null): ApiResponse<List<ProductDto>>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Long): ApiResponse<ProductDto>

    @GET("products/by-code")
    suspend fun productByCode(@Query("code") code: String): ApiResponse<ProductDto?>

    @POST("products")
    suspend fun createProduct(@Body body: CreateProductRequest): ApiResponse<ProductDto>

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body body: UpdateProductRequest): ApiResponse<ProductDto>

    @POST("products/{id}/adjust-stock")
    suspend fun adjustStock(@Path("id") id: Long, @Body body: ProductAdjustStockRequest): ApiResponse<ProductDto>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): ApiResponse<Unit>

    @GET("customers")
    suspend fun customers(@Query("keyword") keyword: String? = null): ApiResponse<List<CustomerDto>>

    @GET("customers/{id}")
    suspend fun customer(@Path("id") id: Long): ApiResponse<CustomerDto>

    @POST("customers")
    suspend fun createCustomer(@Body body: CreateCustomerRequest): ApiResponse<CustomerDto>

    @PUT("customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body body: UpdateCustomerRequest): ApiResponse<CustomerDto>

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
    suspend fun createSupplier(@Body body: CreateSupplierRequest): ApiResponse<SupplierDto>

    @PUT("suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: Long, @Body body: UpdateSupplierRequest): ApiResponse<SupplierDto>

    @DELETE("suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: Long): ApiResponse<Unit>

    @GET("sale-orders")
    suspend fun saleOrders(@QueryMap filter: Map<String, @JvmSuppressWildcards String?>): ApiResponse<List<SaleOrderDto>>

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
