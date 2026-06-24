package com.zhihuiji.core.network

import com.zhihuiji.core.model.*
import retrofit2.http.*

interface ZhihuijiApi {
    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthResult>

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResult>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<AuthResult>

    @POST("v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): ApiResponse<VerifyCodeResponse>

    @GET("v1/auth/users/me")
    suspend fun me(): ApiResponse<UserProfile>

    @GET("v1/admin/users")
    suspend fun adminUsers(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<List<AdminUser>>

    @POST("v1/admin/users")
    suspend fun createAdminUser(@Body body: CreateAdminUserRequest): ApiResponse<AdminUser>

    @PUT("v1/admin/users/{userId}")
    suspend fun updateAdminUser(
        @Path("userId") userId: Long,
        @Body body: UpdateAdminUserRequest,
    ): ApiResponse<AdminUser>

    @GET("v2/stores/current")
    suspend fun currentStore(): ApiResponse<CurrentStoreProfile>

    @GET("v2/stores/current/members")
    suspend fun storeMembers(): ApiResponse<List<StoreStaffMember>>

    @POST("v2/stores/current/members")
    suspend fun createStoreMember(@Body body: CreateStoreStaffMemberRequest): ApiResponse<StoreStaffMember>

    @PUT("v2/stores/current/members/{userId}")
    suspend fun updateStoreMember(
        @Path("userId") userId: Long,
        @Body body: UpdateStoreStaffMemberRequest,
    ): ApiResponse<StoreStaffMember>

    @GET("v1/products")
    suspend fun products(@Query("keyword") keyword: String? = null): ApiResponse<List<ProductDto>>

    @GET("v1/products/{id}")
    suspend fun product(@Path("id") id: Long): ApiResponse<ProductDto>

    @GET("v1/products/by-code")
    suspend fun productByCode(@Query("code") code: String): ApiResponse<ProductDto?>

    @POST("v1/products")
    suspend fun createProduct(@Body body: CreateProductRequest): ApiResponse<ProductDto>

    @PUT("v1/products/{id}")
    suspend fun updateProduct(@Path("id") id: Long, @Body body: UpdateProductRequest): ApiResponse<ProductDto>

    @POST("v1/products/{id}/adjust-stock")
    suspend fun adjustStock(@Path("id") id: Long, @Body body: ProductAdjustStockRequest): ApiResponse<ProductDto>

    @DELETE("v1/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v1/customers")
    suspend fun customers(@Query("keyword") keyword: String? = null): ApiResponse<List<CustomerDto>>

    @GET("v1/customers/{id}")
    suspend fun customer(@Path("id") id: Long): ApiResponse<CustomerDto>

    @POST("v1/customers")
    suspend fun createCustomer(@Body body: CreateCustomerRequest): ApiResponse<CustomerDto>

    @PUT("v1/customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Long, @Body body: UpdateCustomerRequest): ApiResponse<CustomerDto>

    @DELETE("v1/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v1/suppliers")
    suspend fun suppliers(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<SupplierDto>>

    @GET("v1/suppliers/{id}")
    suspend fun supplier(@Path("id") id: Long): ApiResponse<SupplierDto>

    @POST("v1/suppliers")
    suspend fun createSupplier(@Body body: CreateSupplierRequest): ApiResponse<SupplierDto>

    @PUT("v1/suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: Long, @Body body: UpdateSupplierRequest): ApiResponse<SupplierDto>

    @DELETE("v1/suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v1/sale-orders")
    suspend fun saleOrders(@QueryMap filter: Map<String, @JvmSuppressWildcards String?>): ApiResponse<List<SaleOrderDto>>

    @GET("v1/sale-orders/{id}")
    suspend fun saleOrder(@Path("id") id: Long): ApiResponse<SaleOrderDto>

    @POST("v1/sale-orders")
    suspend fun createSaleOrder(@Body body: CreateSaleOrderRequest): ApiResponse<SaleOrderDto>

    @PUT("v1/sale-orders/{id}/draft")
    suspend fun updateSaleDraft(@Path("id") id: Long, @Body body: UpdateSaleDraftRequest): ApiResponse<SaleOrderDto>

    @POST("v1/sale-orders/{id}/payments")
    suspend fun addSalePayment(@Path("id") id: Long, @Body body: PaymentRequest): ApiResponse<PaymentDto>

    @GET("v1/sale-orders/{id}/payments")
    suspend fun salePayments(@Path("id") id: Long): ApiResponse<List<PaymentDto>>

    @PUT("v1/sale-orders/{id}/status")
    suspend fun updateSaleStatus(@Path("id") id: Long, @Body body: StatusRequest): ApiResponse<Unit>

    @PUT("v1/sale-orders/{id}/cancel")
    suspend fun cancelSaleOrder(@Path("id") id: Long): ApiResponse<SaleOrderDto>

    @GET("v1/purchase-orders")
    suspend fun purchaseOrders(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<PurchaseOrderDto>>

    @GET("v1/purchase-orders/{id}")
    suspend fun purchaseOrder(@Path("id") id: Long): ApiResponse<PurchaseOrderDto>

    @POST("v1/purchase-orders")
    suspend fun createPurchaseOrder(@Body body: CreatePurchaseOrderRequest): ApiResponse<PurchaseOrderDto>

    @GET("v1/pay-orders")
    suspend fun payOrders(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
    ): ApiResponse<List<PayOrderDto>>

    @GET("v1/pay-orders/{id}")
    suspend fun payOrder(@Path("id") id: Long): ApiResponse<PayOrderDto>

    @POST("v1/pay-orders")
    suspend fun createPayOrder(@Body body: CreatePayOrderRequest): ApiResponse<PayOrderDto>

    @PUT("v1/pay-orders/{id}/status")
    suspend fun updatePayOrderStatus(@Path("id") id: Long, @Body body: StatusRequest): ApiResponse<PayOrderDto>

    @GET("v2/finance-records")
    suspend fun financeRecords(
        @Query("keyword") keyword: String? = null,
        @Query("type") type: Int? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
    ): ApiResponse<List<FinanceRecordDto>>

    @POST("v2/finance-records")
    suspend fun createFinanceRecord(@Body body: CreateFinanceRecordRequest): ApiResponse<FinanceRecordDto>

    @GET("v2/reports/sales-summary")
    suspend fun salesSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<SalesSummaryReportDto>

    @GET("v2/reports/sales-trend")
    suspend fun salesTrend(
        @Query("start_at") startAt: Long,
        @Query("end_at") endAt: Long,
        @Query("bucket") bucket: String = "day",
    ): ApiResponse<List<SalesTrendPointReportDto>>

    @GET("v2/reports/profit-summary")
    suspend fun profitSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<ProfitSummaryReportDto>

    @GET("v2/reports/refund-records")
    suspend fun refundRecords(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<RefundRecordReportDto>>

    @GET("v2/reports/stock-out-records")
    suspend fun stockOutRecords(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<StockOutRecordReportDto>>

    @GET("v2/reports/top-products")
    suspend fun topProducts(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<TopSellingProductReportDto>>

    @GET("v2/reports/profit-by-products")
    suspend fun profitByProducts(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<ProfitByProductReportDto>>

    @GET("v2/reports/profit-by-customers")
    suspend fun profitByCustomers(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<ProfitByCustomerReportDto>>

    @GET("v2/reports/inventory-flow")
    suspend fun inventoryFlow(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<InventoryFlowRecordDto>>

    @GET("v2/reports/customer-sales")
    suspend fun customerSales(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long, @Query("limit") limit: Int = 10): ApiResponse<List<CustomerSalesReportDto>>

    @GET("v2/reports/top-receivable-customers")
    suspend fun topReceivableCustomers(@Query("limit") limit: Int = 10): ApiResponse<List<CustomerReceivableReportDto>>

    @GET("v2/reports/low-stock-products")
    suspend fun lowStockProducts(@Query("limit") limit: Int = 10): ApiResponse<List<LowStockProductReportDto>>

    @GET("v2/reports/reconciliation-summary")
    suspend fun reconciliationSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<ReconciliationSummaryReportDto>

    @GET("v2/reports/cashflow-summary")
    suspend fun cashflowSummary(@Query("start_at") startAt: Long, @Query("end_at") endAt: Long): ApiResponse<CashflowSummaryReportDto>

    @GET("v1/sync/health")
    suspend fun syncHealth(): ApiResponse<SyncHealthResult>

    @POST("v1/sync/pull")
    suspend fun pull(@Body body: PullRequest): ApiResponse<PullResult>

    @POST("v1/sync/upload")
    suspend fun upload(@Body body: UploadRequest): ApiResponse<UploadResult>


}
