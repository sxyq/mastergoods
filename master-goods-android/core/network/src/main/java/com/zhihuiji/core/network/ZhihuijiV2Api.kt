package com.zhihuiji.core.network

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.AgentChatResponse
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentNotificationDto
import com.zhihuiji.core.model.v2.agent.AgentRunCancelDto
import com.zhihuiji.core.model.v2.agent.AgentTaskDto
import com.zhihuiji.core.model.v2.agent.AgentWorkbenchV2Dto
import com.zhihuiji.core.model.v2.agent.CreateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentDraftRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentMessageRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.core.model.v2.finance.AccountCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto
import com.zhihuiji.core.model.v2.finance.AccountUpdateV2Request
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.model.v2.finance.BillFundLinkCreateV2Request
import com.zhihuiji.core.model.v2.finance.BillFundLinkV2Dto
import com.zhihuiji.core.model.v2.inventory.CreateInventoryLedgerEntryV2Request
import com.zhihuiji.core.model.v2.inventory.CreateInventorySnapshotV2Request
import com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto
import com.zhihuiji.core.model.v2.inventory.InventoryMonthlyStatsV2Dto
import com.zhihuiji.core.model.v2.inventory.InventorySnapshotV2Dto
import com.zhihuiji.core.model.v2.media.CreateMediaAssetRequest
import com.zhihuiji.core.model.v2.media.CreateMediaBindingRequest
import com.zhihuiji.core.model.v2.media.MediaAssetDto
import com.zhihuiji.core.model.v2.media.MediaBindingDto
import com.zhihuiji.core.model.v2.order.ConfirmSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.ConfirmSalesReturnV2Request
import com.zhihuiji.core.model.v2.order.CreatePayOrderV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseOrderV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseReceiptV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.CreateSalesReturnV2Request
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReceiptV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Request
import com.zhihuiji.core.model.v2.order.SalesReturnRefundV2Request
import com.zhihuiji.core.model.v2.order.SalesReturnV2Dto
import com.zhihuiji.core.model.v2.order.UpdatePurchaseReceiptDraftV2Request
import com.zhihuiji.core.model.v2.order.UpdateSaleDraftV2Request
import com.zhihuiji.core.model.v2.order.UpdateSalesReturnDraftV2Request
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.core.model.v2.partner.CustomerWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerContactWriteV2Request
import com.zhihuiji.core.model.v2.partner.PartnerGroupV2Dto
import com.zhihuiji.core.model.v2.partner.PartnerGroupWriteV2Request
import com.zhihuiji.core.model.v2.partner.SupplierV2Dto
import com.zhihuiji.core.model.v2.partner.SupplierWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductCategoryV2Dto
import com.zhihuiji.core.model.v2.product.ProductCategoryWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductPriceLevelV2Dto
import com.zhihuiji.core.model.v2.product.ProductPriceLevelWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationV2Dto
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductUnitV2Dto
import com.zhihuiji.core.model.v2.product.ProductUnitWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.core.model.v2.product.ProductWriteV2Request
import com.zhihuiji.core.model.v2.sync.CreateImportJobV2Request
import com.zhihuiji.core.model.v2.sync.ImportJobV2Dto
import com.zhihuiji.core.model.v2.sync.RetryImportJobV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorAckV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorV2Dto
import com.zhihuiji.core.model.v2.sync.SyncHealthV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Request
import com.zhihuiji.core.model.v2.sync.SyncPullV2Response
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Request
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ZhihuijiV2Api {
    @GET("v2/products")
    suspend fun productsV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("unit_id") unitId: Long? = null,
    ): ApiResponse<List<ProductV2Dto>>

    @GET("v2/products/low-stock")
    suspend fun lowStockProductsV2(
        @Query("size") size: Int? = null,
    ): ApiResponse<List<ProductV2Dto>>

    @GET("v2/products/{id}")
    suspend fun productV2(@Path("id") id: Long): ApiResponse<ProductV2Dto>

    @POST("v2/products")
    suspend fun createProductV2(@Body body: ProductWriteV2Request): ApiResponse<ProductV2Dto>

    @PUT("v2/products/{id}")
    suspend fun updateProductV2(@Path("id") id: Long, @Body body: ProductWriteV2Request): ApiResponse<ProductV2Dto>

    @DELETE("v2/products/{id}")
    suspend fun deleteProductV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/product-categories")
    suspend fun productCategoriesV2(): ApiResponse<List<ProductCategoryV2Dto>>

    @POST("v2/product-categories")
    suspend fun createProductCategoryV2(@Body body: ProductCategoryWriteV2Request): ApiResponse<ProductCategoryV2Dto>

    @PUT("v2/product-categories/{id}")
    suspend fun updateProductCategoryV2(@Path("id") id: Long, @Body body: ProductCategoryWriteV2Request): ApiResponse<ProductCategoryV2Dto>

    @DELETE("v2/product-categories/{id}")
    suspend fun deleteProductCategoryV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/product-units")
    suspend fun productUnitsV2(): ApiResponse<List<ProductUnitV2Dto>>

    @POST("v2/product-units")
    suspend fun createProductUnitV2(@Body body: ProductUnitWriteV2Request): ApiResponse<ProductUnitV2Dto>

    @PUT("v2/product-units/{id}")
    suspend fun updateProductUnitV2(@Path("id") id: Long, @Body body: ProductUnitWriteV2Request): ApiResponse<ProductUnitV2Dto>

    @DELETE("v2/product-units/{id}")
    suspend fun deleteProductUnitV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/product-price-levels")
    suspend fun productPriceLevelsV2(): ApiResponse<List<ProductPriceLevelV2Dto>>

    @POST("v2/product-price-levels")
    suspend fun createProductPriceLevelV2(@Body body: ProductPriceLevelWriteV2Request): ApiResponse<ProductPriceLevelV2Dto>

    @PUT("v2/product-price-levels/{id}")
    suspend fun updateProductPriceLevelV2(
        @Path("id") id: Long,
        @Body body: ProductPriceLevelWriteV2Request,
    ): ApiResponse<ProductPriceLevelV2Dto>

    @DELETE("v2/product-price-levels/{id}")
    suspend fun deleteProductPriceLevelV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/product-supplier-relations")
    suspend fun productSupplierRelationsV2(
        @Query("product_id") productId: Long,
    ): ApiResponse<List<ProductSupplierRelationV2Dto>>

    @POST("v2/product-supplier-relations")
    suspend fun createProductSupplierRelationV2(
        @Body body: ProductSupplierRelationWriteV2Request,
    ): ApiResponse<ProductSupplierRelationV2Dto>

    @PUT("v2/product-supplier-relations/{id}")
    suspend fun updateProductSupplierRelationV2(
        @Path("id") id: Long,
        @Body body: ProductSupplierRelationWriteV2Request,
    ): ApiResponse<ProductSupplierRelationV2Dto>

    @DELETE("v2/product-supplier-relations/{id}")
    suspend fun deleteProductSupplierRelationV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/customers")
    suspend fun customersV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("group_id") groupId: Long? = null,
    ): ApiResponse<List<CustomerV2Dto>>

    @GET("v2/customers/{id}")
    suspend fun customerV2(@Path("id") id: Long): ApiResponse<CustomerV2Dto>

    @POST("v2/customers")
    suspend fun createCustomerV2(@Body body: CustomerWriteV2Request): ApiResponse<CustomerV2Dto>

    @PUT("v2/customers/{id}")
    suspend fun updateCustomerV2(@Path("id") id: Long, @Body body: CustomerWriteV2Request): ApiResponse<CustomerV2Dto>

    @DELETE("v2/customers/{id}")
    suspend fun deleteCustomerV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/customer-groups")
    suspend fun customerGroupsV2(): ApiResponse<List<PartnerGroupV2Dto>>

    @POST("v2/customer-groups")
    suspend fun createCustomerGroupV2(@Body body: PartnerGroupWriteV2Request): ApiResponse<PartnerGroupV2Dto>

    @PUT("v2/customer-groups/{id}")
    suspend fun updateCustomerGroupV2(@Path("id") id: Long, @Body body: PartnerGroupWriteV2Request): ApiResponse<PartnerGroupV2Dto>

    @DELETE("v2/customer-groups/{id}")
    suspend fun deleteCustomerGroupV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/customer-contacts")
    suspend fun customerContactsV2(@Query("customer_id") customerId: Long): ApiResponse<List<PartnerContactV2Dto>>

    @POST("v2/customer-contacts")
    suspend fun createCustomerContactV2(@Body body: PartnerContactWriteV2Request): ApiResponse<PartnerContactV2Dto>

    @PUT("v2/customer-contacts/{id}")
    suspend fun updateCustomerContactV2(@Path("id") id: Long, @Body body: PartnerContactWriteV2Request): ApiResponse<PartnerContactV2Dto>

    @DELETE("v2/customer-contacts/{id}")
    suspend fun deleteCustomerContactV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/suppliers")
    suspend fun suppliersV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("group_id") groupId: Long? = null,
    ): ApiResponse<List<SupplierV2Dto>>

    @GET("v2/suppliers/{id}")
    suspend fun supplierV2(@Path("id") id: Long): ApiResponse<SupplierV2Dto>

    @POST("v2/suppliers")
    suspend fun createSupplierV2(@Body body: SupplierWriteV2Request): ApiResponse<SupplierV2Dto>

    @PUT("v2/suppliers/{id}")
    suspend fun updateSupplierV2(@Path("id") id: Long, @Body body: SupplierWriteV2Request): ApiResponse<SupplierV2Dto>

    @DELETE("v2/suppliers/{id}")
    suspend fun deleteSupplierV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/supplier-groups")
    suspend fun supplierGroupsV2(): ApiResponse<List<PartnerGroupV2Dto>>

    @POST("v2/supplier-groups")
    suspend fun createSupplierGroupV2(@Body body: PartnerGroupWriteV2Request): ApiResponse<PartnerGroupV2Dto>

    @PUT("v2/supplier-groups/{id}")
    suspend fun updateSupplierGroupV2(@Path("id") id: Long, @Body body: PartnerGroupWriteV2Request): ApiResponse<PartnerGroupV2Dto>

    @DELETE("v2/supplier-groups/{id}")
    suspend fun deleteSupplierGroupV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/supplier-contacts")
    suspend fun supplierContactsV2(@Query("supplier_id") supplierId: Long): ApiResponse<List<PartnerContactV2Dto>>

    @POST("v2/supplier-contacts")
    suspend fun createSupplierContactV2(@Body body: PartnerContactWriteV2Request): ApiResponse<PartnerContactV2Dto>

    @PUT("v2/supplier-contacts/{id}")
    suspend fun updateSupplierContactV2(@Path("id") id: Long, @Body body: PartnerContactWriteV2Request): ApiResponse<PartnerContactV2Dto>

    @DELETE("v2/supplier-contacts/{id}")
    suspend fun deleteSupplierContactV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/sale-orders")
    suspend fun saleOrdersV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("min_total_amount") minTotalAmount: String? = null,
        @Query("max_total_amount") maxTotalAmount: String? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
        @Query("product_keyword") productKeyword: String? = null,
        @Query("payment_status") paymentStatus: String? = null,
    ): ApiResponse<List<SaleOrderV2Dto>>

    @GET("v2/sale-orders/{id}")
    suspend fun saleOrderV2(@Path("id") id: Long): ApiResponse<SaleOrderV2Dto>

    @POST("v2/sale-orders")
    suspend fun createSaleOrderV2(@Body body: CreateSaleOrderV2Request): ApiResponse<SaleOrderV2Dto>

    @PUT("v2/sale-orders/{id}")
    suspend fun updateSaleOrderDraftV2(@Path("id") id: Long, @Body body: UpdateSaleDraftV2Request): ApiResponse<SaleOrderV2Dto>

    @PUT("v2/sale-orders/{id}/draft")
    suspend fun updateSaleOrderDraftAliasV2(@Path("id") id: Long, @Body body: UpdateSaleDraftV2Request): ApiResponse<SaleOrderV2Dto>

    @PUT("v2/sale-orders/{id}/confirm")
    suspend fun confirmSaleOrderV2(@Path("id") id: Long, @Body body: ConfirmSaleOrderV2Request): ApiResponse<SaleOrderV2Dto>

    @POST("v2/sale-orders/{id}/payments")
    suspend fun addSaleOrderPaymentV2(@Path("id") id: Long, @Body body: SalePaymentV2Request): ApiResponse<SalePaymentV2Dto>

    @GET("v2/sale-orders/{id}/payments")
    suspend fun saleOrderPaymentsV2(@Path("id") id: Long): ApiResponse<List<SalePaymentV2Dto>>

    @PUT("v2/sale-orders/{id}/status")
    suspend fun updateSaleOrderStatusV2(@Path("id") id: Long, @Body body: com.zhihuiji.core.model.StatusRequest): ApiResponse<Unit>

    @PUT("v2/sale-orders/{id}/cancel")
    suspend fun cancelSaleOrderV2(@Path("id") id: Long): ApiResponse<SaleOrderV2Dto>

    @GET("v2/sales-returns")
    suspend fun salesReturnsV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<SalesReturnV2Dto>>

    @GET("v2/sales-returns/{id}")
    suspend fun salesReturnV2(@Path("id") id: Long): ApiResponse<SalesReturnV2Dto>

    @GET("v2/sales-returns/by-order/{orderId}")
    suspend fun salesReturnsByOrderV2(@Path("orderId") orderId: Long): ApiResponse<List<SalesReturnV2Dto>>

    @POST("v2/sales-returns")
    suspend fun createSalesReturnV2(@Body body: CreateSalesReturnV2Request): ApiResponse<SalesReturnV2Dto>

    @PUT("v2/sales-returns/{id}/draft")
    suspend fun updateSalesReturnDraftV2(@Path("id") id: Long, @Body body: UpdateSalesReturnDraftV2Request): ApiResponse<SalesReturnV2Dto>

    @PUT("v2/sales-returns/{id}/confirm")
    suspend fun confirmSalesReturnV2(@Path("id") id: Long, @Body body: ConfirmSalesReturnV2Request): ApiResponse<SalesReturnV2Dto>

    @POST("v2/sales-returns/{id}/refunds")
    suspend fun addSalesReturnRefundV2(@Path("id") id: Long, @Body body: SalesReturnRefundV2Request): ApiResponse<SalesReturnV2Dto>

    @PUT("v2/sales-returns/{id}/cancel")
    suspend fun cancelSalesReturnV2(@Path("id") id: Long): ApiResponse<SalesReturnV2Dto>

    @GET("v2/purchase-orders")
    suspend fun purchaseOrdersV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<PurchaseOrderV2Dto>>

    @GET("v2/purchase-orders/{id}")
    suspend fun purchaseOrderV2(@Path("id") id: Long): ApiResponse<PurchaseOrderV2Dto>

    @POST("v2/purchase-orders")
    suspend fun createPurchaseOrderV2(@Body body: CreatePurchaseOrderV2Request): ApiResponse<PurchaseOrderV2Dto>

    @PUT("v2/purchase-orders/{id}")
    suspend fun updatePurchaseOrderV2(@Path("id") id: Long, @Body body: CreatePurchaseOrderV2Request): ApiResponse<PurchaseOrderV2Dto>

    @DELETE("v2/purchase-orders/{id}")
    suspend fun deletePurchaseOrderV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/purchase-receipts")
    suspend fun purchaseReceiptsV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
    ): ApiResponse<List<PurchaseReceiptV2Dto>>

    @GET("v2/purchase-receipts/{id}")
    suspend fun purchaseReceiptV2(@Path("id") id: Long): ApiResponse<PurchaseReceiptV2Dto>

    @GET("v2/purchase-receipts/by-order/{orderId}")
    suspend fun purchaseReceiptsByOrderV2(@Path("orderId") orderId: Long): ApiResponse<List<PurchaseReceiptV2Dto>>

    @POST("v2/purchase-receipts")
    suspend fun createPurchaseReceiptV2(@Body body: CreatePurchaseReceiptV2Request): ApiResponse<PurchaseReceiptV2Dto>

    @PUT("v2/purchase-receipts/{id}/draft")
    suspend fun updatePurchaseReceiptDraftV2(
        @Path("id") id: Long,
        @Body body: UpdatePurchaseReceiptDraftV2Request,
    ): ApiResponse<PurchaseReceiptV2Dto>

    @PUT("v2/purchase-receipts/{id}/confirm")
    suspend fun confirmPurchaseReceiptV2(@Path("id") id: Long): ApiResponse<PurchaseReceiptV2Dto>

    @PUT("v2/purchase-receipts/{id}/cancel")
    suspend fun cancelPurchaseReceiptV2(@Path("id") id: Long): ApiResponse<PurchaseReceiptV2Dto>

    @GET("v2/pay-orders")
    suspend fun payOrdersV2(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: Int? = null,
        @Query("created_after") createdAfter: String? = null,
        @Query("created_before") createdBefore: String? = null,
    ): ApiResponse<List<PayOrderV2Dto>>

    @GET("v2/pay-orders/{id}")
    suspend fun payOrderV2(@Path("id") id: Long): ApiResponse<PayOrderV2Dto>

    @POST("v2/pay-orders")
    suspend fun createPayOrderV2(@Body body: CreatePayOrderV2Request): ApiResponse<PayOrderV2Dto>

    @PUT("v2/pay-orders/{id}/status")
    suspend fun updatePayOrderStatusV2(@Path("id") id: Long, @Body body: com.zhihuiji.core.model.StatusRequest): ApiResponse<PayOrderV2Dto>

    @GET("v2/accounts")
    suspend fun accountsV2(): ApiResponse<List<AccountV2Dto>>

    @GET("v2/accounts/{id}")
    suspend fun accountV2(@Path("id") id: Long): ApiResponse<AccountV2Dto>

    @POST("v2/accounts")
    suspend fun createAccountV2(@Body body: AccountCreateV2Request): ApiResponse<AccountV2Dto>

    @PUT("v2/accounts/{id}")
    suspend fun updateAccountV2(@Path("id") id: Long, @Body body: AccountUpdateV2Request): ApiResponse<AccountV2Dto>

    @DELETE("v2/accounts/{id}")
    suspend fun deleteAccountV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/account-transfers")
    suspend fun accountTransfersV2(): ApiResponse<List<AccountTransferV2Dto>>

    @GET("v2/account-transfers/{id}")
    suspend fun accountTransferV2(@Path("id") id: Long): ApiResponse<AccountTransferV2Dto>

    @POST("v2/account-transfers")
    suspend fun createAccountTransferV2(@Body body: AccountTransferCreateV2Request): ApiResponse<AccountTransferV2Dto>

    // bill-fund-links: @Query names match backend V2BillFundLinkController param names (camelCase, no explicit @RequestParam name)
    @GET("v2/bill-fund-links")
    suspend fun billFundLinksV2(
        @Query("billType") billType: String? = null,
        @Query("billId") billId: Long? = null,
        @Query("accountId") accountId: Long? = null,
    ): ApiResponse<List<BillFundLinkV2Dto>>

    @POST("v2/bill-fund-links")
    suspend fun createBillFundLinkV2(@Body body: BillFundLinkCreateV2Request): ApiResponse<BillFundLinkV2Dto>

    @DELETE("v2/bill-fund-links/{id}")
    suspend fun deleteBillFundLinkV2(@Path("id") id: Long): ApiResponse<Unit>

    // inventory/ledger: @Query names match backend V2InventoryController param names (camelCase, no explicit @RequestParam name)
    @GET("v2/inventory/ledger")
    suspend fun inventoryLedgerV2(
        @Query("productId") productId: Long? = null,
        @Query("startAt") startAt: Long? = null,
        @Query("endAt") endAt: Long? = null,
    ): ApiResponse<List<InventoryLedgerEntryV2Dto>>

    @GET("v2/inventory/ledger/by-source")
    suspend fun inventoryLedgerBySourceV2(
        @Query("source_type") sourceType: String,
        @Query("source_id") sourceId: Long,
    ): ApiResponse<List<InventoryLedgerEntryV2Dto>>

    @POST("v2/inventory/ledger")
    suspend fun createInventoryLedgerEntryV2(@Body body: CreateInventoryLedgerEntryV2Request): ApiResponse<InventoryLedgerEntryV2Dto>

    // inventory/snapshots: @Query names match backend V2InventoryController param names (camelCase, no explicit @RequestParam name)
    @GET("v2/inventory/snapshots")
    suspend fun inventorySnapshotsV2(
        @Query("snapshotDate") snapshotDate: Long? = null,
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null,
    ): ApiResponse<List<InventorySnapshotV2Dto>>

    @POST("v2/inventory/snapshots")
    suspend fun createInventorySnapshotV2(@Body body: CreateInventorySnapshotV2Request): ApiResponse<InventorySnapshotV2Dto>

    @GET("v2/inventory/monthly-stats")
    suspend fun inventoryMonthlyStatsV2(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): ApiResponse<List<InventoryMonthlyStatsV2Dto>>

    @GET("v2/sync/health")
    suspend fun syncHealthV2(): ApiResponse<SyncHealthV2Dto>

    @GET("v2/sync/cursor/{clientId}")
    suspend fun syncCursorV2(@Path("clientId") clientId: String): ApiResponse<SyncCursorV2Dto>

    @POST("v2/sync/cursor/ack")
    suspend fun acknowledgeSyncCursorV2(@Body body: SyncCursorAckV2Request): ApiResponse<SyncCursorV2Dto>

    @POST("v2/sync/upload")
    suspend fun uploadSyncChangesV2(@Body body: SyncUploadV2Request): ApiResponse<SyncUploadV2Response>

    @POST("v2/sync/pull")
    suspend fun pullSyncChangesV2(@Body body: SyncPullV2Request): ApiResponse<SyncPullV2Response>

    @GET("v2/import-jobs")
    suspend fun importJobsV2(@Query("status") status: String? = null): ApiResponse<List<ImportJobV2Dto>>

    @GET("v2/import-jobs/{id}")
    suspend fun importJobV2(@Path("id") id: Long): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs")
    suspend fun createImportJobV2(@Body body: CreateImportJobV2Request): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs/{id}/retry")
    suspend fun retryImportJobV2(@Path("id") id: Long, @Body body: RetryImportJobV2Request? = null): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs/{id}/cancel")
    suspend fun cancelImportJobV2(@Path("id") id: Long): ApiResponse<ImportJobV2Dto>

    @GET("v2/agent/conversations")
    suspend fun agentConversationsV2(): ApiResponse<List<AgentConversationDto>>

    @GET("v2/agent/conversations/{id}")
    suspend fun agentConversationV2(@Path("id") id: Long): ApiResponse<AgentConversationDto>

    @POST("v2/agent/conversations")
    suspend fun createAgentConversationV2(@Body body: CreateAgentConversationRequest): ApiResponse<AgentConversationDto>

    @PUT("v2/agent/conversations/{id}")
    suspend fun updateAgentConversationV2(@Path("id") id: Long, @Body body: UpdateAgentConversationRequest): ApiResponse<AgentConversationDto>

    @DELETE("v2/agent/conversations/{id}")
    suspend fun deleteAgentConversationV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/agent/conversations/{conversationId}/messages")
    suspend fun agentMessagesV2(@Path("conversationId") conversationId: Long): ApiResponse<List<AgentMessageDto>>

    @POST("v2/agent/conversations/{conversationId}/messages")
    suspend fun createAgentMessageV2(
        @Path("conversationId") conversationId: Long,
        @Body body: CreateAgentMessageRequest,
    ): ApiResponse<AgentMessageDto>

    @GET("v2/agent/drafts")
    suspend fun agentDraftsV2(@Query("conversation_id") conversationId: Long? = null): ApiResponse<List<AgentDraftDto>>

    @POST("v2/agent/drafts")
    suspend fun createAgentDraftV2(@Body body: CreateAgentDraftRequest): ApiResponse<AgentDraftDto>

    @PUT("v2/agent/drafts/{id}")
    suspend fun updateAgentDraftV2(@Path("id") id: Long, @Body body: UpdateAgentDraftRequest): ApiResponse<AgentDraftDto>

    @DELETE("v2/agent/drafts/{id}")
    suspend fun deleteAgentDraftV2(@Path("id") id: Long): ApiResponse<Unit>

    // ========== Agent V2 Chat / Workbench ==========

    @GET("v2/agent/workbench")
    suspend fun agentWorkbenchV2(): ApiResponse<AgentWorkbenchV2Dto>

    @GET("v2/agent/tasks")
    suspend fun agentTasksV2(): ApiResponse<List<AgentTaskDto>>

    @GET("v2/agent/notifications")
    suspend fun agentNotificationsV2(
        @Query("unread_only") unreadOnly: Boolean? = null,
    ): ApiResponse<List<AgentNotificationDto>>

    @POST("v2/agent/notifications/{id}/read")
    suspend fun markAgentNotificationReadV2(@Path("id") id: Long): ApiResponse<AgentNotificationDto>

    @POST("v2/agent/chat")
    suspend fun agentChatV2(
        @Body body: AgentChatRequest,
    ): ApiResponse<AgentChatResponse>

    @POST("v2/agent/runs/{runId}/cancel")
    suspend fun cancelAgentRunV2(@Path("runId") runId: String): ApiResponse<AgentRunCancelDto>

    @GET("v2/media/assets")
    suspend fun mediaAssetsV2(): ApiResponse<List<MediaAssetDto>>

    @GET("v2/media/assets/{id}")
    suspend fun mediaAssetV2(@Path("id") id: Long): ApiResponse<MediaAssetDto>

    @POST("v2/media/assets")
    suspend fun createMediaAssetV2(@Body body: CreateMediaAssetRequest): ApiResponse<MediaAssetDto>

    @DELETE("v2/media/assets/{id}")
    suspend fun deleteMediaAssetV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/media/bindings")
    suspend fun mediaBindingsV2(
        @Query("target_type") targetType: String,
        @Query("target_id") targetId: Long,
    ): ApiResponse<List<MediaBindingDto>>

    @POST("v2/media/bindings")
    suspend fun createMediaBindingV2(@Body body: CreateMediaBindingRequest): ApiResponse<MediaBindingDto>

    @DELETE("v2/media/bindings/{id}")
    suspend fun deleteMediaBindingV2(@Path("id") id: Long): ApiResponse<Unit>
}
