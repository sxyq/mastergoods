package com.zhihuiji.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

class ZhihuijiV2ApiContractTest {
    @Test
    fun apiContract_coversAllAgentAndMediaRoutes() {
        val expectedGetPaths = mapOf(
            "agentConversationsV2" to "v2/agent/conversations",
            "agentConversationV2" to "v2/agent/conversations/{id}",
            "agentMessagesV2" to "v2/agent/conversations/{conversationId}/messages",
            "agentDraftsV2" to "v2/agent/drafts",
            "mediaAssetsV2" to "v2/media/assets",
            "mediaAssetV2" to "v2/media/assets/{id}",
            "mediaBindingsV2" to "v2/media/bindings",
        )
        val expectedPostPaths = mapOf(
            "createAgentConversationV2" to "v2/agent/conversations",
            "createAgentMessageV2" to "v2/agent/conversations/{conversationId}/messages",
            "createAgentDraftV2" to "v2/agent/drafts",
            "confirmAgentDraftV2" to "v2/agent/drafts/{id}/confirm",
            "cancelAgentDraftV2" to "v2/agent/drafts/{id}/cancel",
            "cancelAgentRunV2" to "v2/agent/runs/{runId}/cancel",
            "createMediaAssetV2" to "v2/media/assets",
            "createMediaBindingV2" to "v2/media/bindings",
        )
        val expectedPutPaths = mapOf(
            "updateAgentConversationV2" to "v2/agent/conversations/{id}",
            "updateAgentDraftV2" to "v2/agent/drafts/{id}",
        )
        val expectedDeletePaths = mapOf(
            "deleteAgentConversationV2" to "v2/agent/conversations/{id}",
            "deleteAgentDraftV2" to "v2/agent/drafts/{id}",
            "deleteMediaAssetV2" to "v2/media/assets/{id}",
            "deleteMediaBindingV2" to "v2/media/bindings/{id}",
        )

        expectedGetPaths.forEach { (methodName, path) ->
            assertEquals(path, getPath(methodName))
        }
        expectedPostPaths.forEach { (methodName, path) ->
            assertEquals(path, postPath(methodName))
        }
        expectedPutPaths.forEach { (methodName, path) ->
            assertEquals(path, putPath(methodName))
        }
        expectedDeletePaths.forEach { (methodName, path) ->
            assertEquals(path, deletePath(methodName))
        }

        val mediaBindingQueryValues = getQueryValues("mediaBindingsV2")
        assertEquals(listOf("target_type", "target_id"), mediaBindingQueryValues)
        val conversationQueryValues = getQueryValues("agentConversationsV2")
        assertEquals(listOf("page", "limit"), conversationQueryValues)
        val messageQueryValues = getQueryValues("agentMessagesV2")
        assertEquals(listOf("page", "limit"), messageQueryValues)
        val draftQueryValues = getQueryValues("agentDraftsV2")
        assertEquals(listOf("conversation_id", "page", "limit"), draftQueryValues)
    }

    @Test
    fun apiContract_keepsCriticalCoreDataV2Paths() {
        assertEquals("v2/products", getPath("productsV2"))
        assertEquals("v2/customers", getPath("customersV2"))
        assertEquals("v2/suppliers", getPath("suppliersV2"))
        assertEquals("v2/sale-orders", getPath("saleOrdersV2"))
        assertEquals("v2/purchase-orders", getPath("purchaseOrdersV2"))
        assertEquals("v2/pay-orders", getPath("payOrdersV2"))
        assertEquals("v2/accounts", getPath("accountsV2"))
        assertEquals("v2/inventory/ledger", getPath("inventoryLedgerV2"))
        assertEquals("v2/sync/health", getPath("syncHealthV2"))
        assertEquals("v2/import-jobs", getPath("importJobsV2"))
    }

    @Test
    fun apiContract_hasCriticalCoreDataV2Mutations() {
        assertEquals("v2/products", postPath("createProductV2"))
        assertEquals("v2/customers", postPath("createCustomerV2"))
        assertEquals("v2/suppliers", postPath("createSupplierV2"))
        assertEquals("v2/sale-orders/{id}/confirm", putPath("confirmSaleOrderV2"))
        assertEquals("v2/sales-returns", postPath("createSalesReturnV2"))
        assertEquals("v2/purchase-receipts", postPath("createPurchaseReceiptV2"))
        assertEquals("v2/account-transfers", postPath("createAccountTransferV2"))
        assertEquals("v2/sync/pull", postPath("pullSyncChangesV2"))
        assertEquals("v2/import-jobs/{id}/retry", postPath("retryImportJobV2"))
        assertEquals("v2/product-price-levels/{id}", deletePath("deleteProductPriceLevelV2"))
    }

    @Test
    fun apiContract_billFundLinksQueryParamsMatchBackend() {
        // Backend V2BillFundLinkController uses @RequestParam without explicit name,
        // so param names default to Java parameter names (camelCase).
        // Android @Query values must match these exactly.
        val queryValues = getQueryValues("billFundLinksV2")
        assertTrue(queryValues.contains("billType"))
        assertTrue(queryValues.contains("billId"))
        assertTrue(queryValues.contains("accountId"))
    }

    @Test
    fun apiContract_inventoryLedgerQueryParamsMatchBackend() {
        // Backend V2InventoryController /ledger uses @RequestParam without explicit name (camelCase).
        val queryValues = getQueryValues("inventoryLedgerV2")
        assertTrue(queryValues.contains("productId"))
        assertTrue(queryValues.contains("startAt"))
        assertTrue(queryValues.contains("endAt"))
    }

    @Test
    fun apiContract_inventoryLedgerBySourceQueryParamsMatchBackend() {
        // Backend V2InventoryController /ledger/by-source uses explicit @RequestParam("source_type"/"source_id").
        val queryValues = getQueryValues("inventoryLedgerBySourceV2")
        assertTrue(queryValues.contains("source_type"))
        assertTrue(queryValues.contains("source_id"))
    }

    @Test
    fun apiContract_inventorySnapshotsQueryParamsMatchBackend() {
        // Backend V2InventoryController /snapshots uses @RequestParam without explicit name (camelCase).
        val queryValues = getQueryValues("inventorySnapshotsV2")
        assertTrue(queryValues.contains("snapshotDate"))
        assertTrue(queryValues.contains("startDate"))
        assertTrue(queryValues.contains("endDate"))
    }

    // ========== Reports V2 ==========

    @Test
    fun apiContract_coversAllReportV2Routes() {
        val expected = mapOf(
            "salesSummaryV2" to "v2/reports/sales-summary",
            "salesTrendV2" to "v2/reports/sales-trend",
            "profitSummaryV2" to "v2/reports/profit-summary",
            "refundRecordsV2" to "v2/reports/refund-records",
            "stockOutRecordsV2" to "v2/reports/stock-out-records",
            "topProductsV2" to "v2/reports/top-products",
            "profitByProductsV2" to "v2/reports/profit-by-products",
            "profitByCustomersV2" to "v2/reports/profit-by-customers",
            "inventoryFlowV2" to "v2/reports/inventory-flow",
            "customerSalesV2" to "v2/reports/customer-sales",
            "topReceivableCustomersV2" to "v2/reports/top-receivable-customers",
            "lowStockProductsReportV2" to "v2/reports/low-stock-products",
            "reconciliationSummaryV2" to "v2/reports/reconciliation-summary",
            "cashflowSummaryV2" to "v2/reports/cashflow-summary",
        )
        assertEquals(14, expected.size)
        expected.forEach { (methodName, path) ->
            assertEquals(path, getPath(methodName))
        }
    }

    @Test
    fun apiContract_reportQueryParamsMatchBackend() {
        // All report methods use snake_case @Query names matching explicit @RequestParam names.
        assertEquals(listOf("start_at", "end_at"), getQueryValues("salesSummaryV2"))
        assertEquals(listOf("start_at", "end_at", "bucket"), getQueryValues("salesTrendV2"))
        assertEquals(listOf("start_at", "end_at"), getQueryValues("profitSummaryV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("refundRecordsV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("stockOutRecordsV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("topProductsV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("profitByProductsV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("profitByCustomersV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("inventoryFlowV2"))
        assertEquals(listOf("start_at", "end_at", "limit"), getQueryValues("customerSalesV2"))
        assertEquals(listOf("limit"), getQueryValues("topReceivableCustomersV2"))
        assertEquals(listOf("limit"), getQueryValues("lowStockProductsReportV2"))
        assertEquals(listOf("start_at", "end_at"), getQueryValues("reconciliationSummaryV2"))
        assertEquals(listOf("start_at", "end_at"), getQueryValues("cashflowSummaryV2"))
    }

    @Test
    fun apiContract_lowStockProductsV2AndReportV2AreDistinctRoutes() {
        // lowStockProductsV2 (product list low-stock) and lowStockProductsReportV2 (reports low-stock-products)
        // are TWO DIFFERENT methods/endpoints — both must be asserted with their correct distinct paths.
        assertEquals("v2/products/low-stock", getPath("lowStockProductsV2"))
        assertEquals("v2/reports/low-stock-products", getPath("lowStockProductsReportV2"))
        val productLowStockQuery = getQueryValues("lowStockProductsV2")
        assertEquals(listOf("size"), productLowStockQuery)
    }

    // ========== Finance Records V2 ==========

    @Test
    fun apiContract_coversFinanceRecordsV2Routes() {
        assertEquals("v2/finance-records", getPath("financeRecordsV2"))
        assertEquals("v2/finance-records", postPath("createFinanceRecordV2"))
    }

    @Test
    fun apiContract_financeRecordsQueryParamsMatchBackend() {
        // Backend V2FinanceRecordController uses explicit @RequestParam("created_after") etc. (snake_case).
        val queryValues = getQueryValues("financeRecordsV2")
        assertEquals(listOf("keyword", "type", "created_after", "created_before"), queryValues)
    }

    // ========== Product Catalog V2 ==========

    @Test
    fun apiContract_coversProductCatalogV2Routes() {
        val expectedGetPaths = mapOf(
            "productsV2" to "v2/products",
            "lowStockProductsV2" to "v2/products/low-stock",
            "productV2" to "v2/products/{id}",
            "productCategoriesV2" to "v2/product-categories",
            "productUnitsV2" to "v2/product-units",
            "productPriceLevelsV2" to "v2/product-price-levels",
            "productSupplierRelationsV2" to "v2/product-supplier-relations",
        )
        val expectedPostPaths = mapOf(
            "createProductV2" to "v2/products",
            "createProductCategoryV2" to "v2/product-categories",
            "createProductUnitV2" to "v2/product-units",
            "createProductPriceLevelV2" to "v2/product-price-levels",
            "createProductSupplierRelationV2" to "v2/product-supplier-relations",
        )
        val expectedPutPaths = mapOf(
            "updateProductV2" to "v2/products/{id}",
            "updateProductCategoryV2" to "v2/product-categories/{id}",
            "updateProductUnitV2" to "v2/product-units/{id}",
            "updateProductPriceLevelV2" to "v2/product-price-levels/{id}",
            "updateProductSupplierRelationV2" to "v2/product-supplier-relations/{id}",
        )
        val expectedDeletePaths = mapOf(
            "deleteProductV2" to "v2/products/{id}",
            "deleteProductCategoryV2" to "v2/product-categories/{id}",
            "deleteProductUnitV2" to "v2/product-units/{id}",
            "deleteProductPriceLevelV2" to "v2/product-price-levels/{id}",
            "deleteProductSupplierRelationV2" to "v2/product-supplier-relations/{id}",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
        expectedPutPaths.forEach { (methodName, path) -> assertEquals(path, putPath(methodName)) }
        expectedDeletePaths.forEach { (methodName, path) -> assertEquals(path, deletePath(methodName)) }
    }

    @Test
    fun apiContract_productSupplierRelationsQueryParamsMatchBackend() {
        // Backend uses explicit @RequestParam("product_id") (snake_case).
        val queryValues = getQueryValues("productSupplierRelationsV2")
        assertEquals(listOf("product_id"), queryValues)
    }

    // ========== Partner V2 (customers / suppliers / groups / contacts) ==========

    @Test
    fun apiContract_coversPartnerV2Routes() {
        val expectedGetPaths = mapOf(
            "customersV2" to "v2/customers",
            "customerV2" to "v2/customers/{id}",
            "customerGroupsV2" to "v2/customer-groups",
            "customerContactsV2" to "v2/customer-contacts",
            "suppliersV2" to "v2/suppliers",
            "supplierV2" to "v2/suppliers/{id}",
            "supplierGroupsV2" to "v2/supplier-groups",
            "supplierContactsV2" to "v2/supplier-contacts",
        )
        val expectedPostPaths = mapOf(
            "createCustomerV2" to "v2/customers",
            "createCustomerGroupV2" to "v2/customer-groups",
            "createCustomerContactV2" to "v2/customer-contacts",
            "createSupplierV2" to "v2/suppliers",
            "createSupplierGroupV2" to "v2/supplier-groups",
            "createSupplierContactV2" to "v2/supplier-contacts",
        )
        val expectedPutPaths = mapOf(
            "updateCustomerV2" to "v2/customers/{id}",
            "updateCustomerGroupV2" to "v2/customer-groups/{id}",
            "updateCustomerContactV2" to "v2/customer-contacts/{id}",
            "updateSupplierV2" to "v2/suppliers/{id}",
            "updateSupplierGroupV2" to "v2/supplier-groups/{id}",
            "updateSupplierContactV2" to "v2/supplier-contacts/{id}",
        )
        val expectedDeletePaths = mapOf(
            "deleteCustomerV2" to "v2/customers/{id}",
            "deleteCustomerGroupV2" to "v2/customer-groups/{id}",
            "deleteCustomerContactV2" to "v2/customer-contacts/{id}",
            "deleteSupplierV2" to "v2/suppliers/{id}",
            "deleteSupplierGroupV2" to "v2/supplier-groups/{id}",
            "deleteSupplierContactV2" to "v2/supplier-contacts/{id}",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
        expectedPutPaths.forEach { (methodName, path) -> assertEquals(path, putPath(methodName)) }
        expectedDeletePaths.forEach { (methodName, path) -> assertEquals(path, deletePath(methodName)) }
    }

    @Test
    fun apiContract_partnerQueryParamsMatchBackend() {
        // customers/suppliers use keyword/status/group_id (snake_case for group_id).
        assertEquals(listOf("keyword", "status", "group_id"), getQueryValues("customersV2"))
        assertEquals(listOf("keyword", "status", "group_id"), getQueryValues("suppliersV2"))
        // contact list endpoints filter by owner + partner id (snake_case).
        assertEquals(listOf("customer_id"), getQueryValues("customerContactsV2"))
        assertEquals(listOf("supplier_id"), getQueryValues("supplierContactsV2"))
    }

    // ========== Order V2 (sale-orders / sales-returns / purchase-orders / receipts / returns) ==========

    @Test
    fun apiContract_coversOrderV2Routes() {
        val expectedGetPaths = mapOf(
            "saleOrdersV2" to "v2/sale-orders",
            "saleOrderV2" to "v2/sale-orders/{id}",
            "saleOrderPaymentsV2" to "v2/sale-orders/{id}/payments",
            "saleOrderReceiptPdfV2" to "v2/sale-orders/{id}/receipt.pdf",
            "salesReturnsV2" to "v2/sales-returns",
            "salesReturnV2" to "v2/sales-returns/{id}",
            "salesReturnsByOrderV2" to "v2/sales-returns/by-order/{orderId}",
            "purchaseOrdersV2" to "v2/purchase-orders",
            "purchaseOrderV2" to "v2/purchase-orders/{id}",
            "purchaseReceiptsV2" to "v2/purchase-receipts",
            "purchaseReceiptV2" to "v2/purchase-receipts/{id}",
            "purchaseReceiptsByOrderV2" to "v2/purchase-receipts/by-order/{orderId}",
            "purchaseReturnsV2" to "v2/purchase-returns",
            "purchaseReturnV2" to "v2/purchase-returns/{id}",
            "purchaseReturnsByOrderV2" to "v2/purchase-returns/by-order/{orderId}",
        )
        val expectedPostPaths = mapOf(
            "createSaleOrderV2" to "v2/sale-orders",
            "addSaleOrderPaymentV2" to "v2/sale-orders/{id}/payments",
            "createSalesReturnV2" to "v2/sales-returns",
            "addSalesReturnRefundV2" to "v2/sales-returns/{id}/refunds",
            "createPurchaseOrderV2" to "v2/purchase-orders",
            "createPurchaseReceiptV2" to "v2/purchase-receipts",
            "createPurchaseReturnV2" to "v2/purchase-returns",
            "addPurchaseReturnRefundV2" to "v2/purchase-returns/{id}/refunds",
        )
        val expectedPutPaths = mapOf(
            "updateSaleOrderDraftV2" to "v2/sale-orders/{id}",
            "updateSaleOrderDraftAliasV2" to "v2/sale-orders/{id}/draft",
            "confirmSaleOrderV2" to "v2/sale-orders/{id}/confirm",
            "updateSaleOrderStatusV2" to "v2/sale-orders/{id}/status",
            "cancelSaleOrderV2" to "v2/sale-orders/{id}/cancel",
            "updateSalesReturnDraftV2" to "v2/sales-returns/{id}/draft",
            "confirmSalesReturnV2" to "v2/sales-returns/{id}/confirm",
            "cancelSalesReturnV2" to "v2/sales-returns/{id}/cancel",
            "updatePurchaseOrderV2" to "v2/purchase-orders/{id}",
            "updatePurchaseReceiptDraftV2" to "v2/purchase-receipts/{id}/draft",
            "confirmPurchaseReceiptV2" to "v2/purchase-receipts/{id}/confirm",
            "cancelPurchaseReceiptV2" to "v2/purchase-receipts/{id}/cancel",
            "updatePurchaseReturnDraftV2" to "v2/purchase-returns/{id}/draft",
            "confirmPurchaseReturnV2" to "v2/purchase-returns/{id}/confirm",
            "cancelPurchaseReturnV2" to "v2/purchase-returns/{id}/cancel",
        )
        val expectedDeletePaths = mapOf(
            "deletePurchaseOrderV2" to "v2/purchase-orders/{id}",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
        expectedPutPaths.forEach { (methodName, path) -> assertEquals(path, putPath(methodName)) }
        expectedDeletePaths.forEach { (methodName, path) -> assertEquals(path, deletePath(methodName)) }
    }

    @Test
    fun apiContract_saleOrdersQueryParamsMatchBackend() {
        // 8 query params, snake_case for explicit @RequestParam names.
        val queryValues = getQueryValues("saleOrdersV2")
        assertEquals(
            listOf(
                "keyword",
                "status",
                "min_total_amount",
                "max_total_amount",
                "created_after",
                "created_before",
                "product_keyword",
                "payment_status",
            ),
            queryValues,
        )
    }

    @Test
    fun apiContract_salesReturnsQueryParamsMatchBackend() {
        assertEquals(listOf("keyword", "status"), getQueryValues("salesReturnsV2"))
    }

    @Test
    fun apiContract_purchaseOrdersQueryParamsMatchBackend() {
        assertEquals(listOf("keyword", "status"), getQueryValues("purchaseOrdersV2"))
    }

    @Test
    fun apiContract_purchaseReceiptsQueryParamsMatchBackend() {
        assertEquals(listOf("keyword", "status"), getQueryValues("purchaseReceiptsV2"))
    }

    @Test
    fun apiContract_purchaseReturnsQueryParamsMatchBackend() {
        assertEquals(listOf("keyword", "status"), getQueryValues("purchaseReturnsV2"))
    }

    // ========== Finance V2 (pay-orders / accounts / account-transfers / bill-fund-links) ==========

    @Test
    fun apiContract_coversFinanceV2Routes() {
        val expectedGetPaths = mapOf(
            "payOrdersV2" to "v2/pay-orders",
            "payOrderV2" to "v2/pay-orders/{id}",
            "accountsV2" to "v2/accounts",
            "accountV2" to "v2/accounts/{id}",
            "accountTransfersV2" to "v2/account-transfers",
            "accountTransferV2" to "v2/account-transfers/{id}",
            "billFundLinksV2" to "v2/bill-fund-links",
        )
        val expectedPostPaths = mapOf(
            "createPayOrderV2" to "v2/pay-orders",
            "createAccountV2" to "v2/accounts",
            "createAccountTransferV2" to "v2/account-transfers",
            "createBillFundLinkV2" to "v2/bill-fund-links",
        )
        val expectedPutPaths = mapOf(
            "updatePayOrderStatusV2" to "v2/pay-orders/{id}/status",
            "updateAccountV2" to "v2/accounts/{id}",
        )
        val expectedDeletePaths = mapOf(
            "deleteAccountV2" to "v2/accounts/{id}",
            "deleteBillFundLinkV2" to "v2/bill-fund-links/{id}",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
        expectedPutPaths.forEach { (methodName, path) -> assertEquals(path, putPath(methodName)) }
        expectedDeletePaths.forEach { (methodName, path) -> assertEquals(path, deletePath(methodName)) }
    }

    @Test
    fun apiContract_payOrdersQueryParamsMatchBackend() {
        // keyword/status/created_after/created_before (snake_case for explicit @RequestParam names).
        val queryValues = getQueryValues("payOrdersV2")
        assertEquals(listOf("keyword", "status", "created_after", "created_before"), queryValues)
    }

    // ========== Inventory V2 (ledger / ledger-by-source / snapshots / monthly-stats) ==========

    @Test
    fun apiContract_coversInventoryV2Routes() {
        val expectedGetPaths = mapOf(
            "inventoryLedgerV2" to "v2/inventory/ledger",
            "inventoryLedgerBySourceV2" to "v2/inventory/ledger/by-source",
            "inventorySnapshotsV2" to "v2/inventory/snapshots",
            "inventoryMonthlyStatsV2" to "v2/inventory/monthly-stats",
        )
        val expectedPostPaths = mapOf(
            "createInventoryLedgerEntryV2" to "v2/inventory/ledger",
            "createInventorySnapshotV2" to "v2/inventory/snapshots",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    @Test
    fun apiContract_inventoryMonthlyStatsQueryParamsMatchBackend() {
        // year/month use camelCase (backend V2InventoryController without explicit @RequestParam name).
        val queryValues = getQueryValues("inventoryMonthlyStatsV2")
        assertEquals(listOf("year", "month"), queryValues)
    }

    // ========== Sync V2 ==========

    @Test
    fun apiContract_coversSyncV2Routes() {
        val expectedGetPaths = mapOf(
            "syncHealthV2" to "v2/sync/health",
            "syncCursorV2" to "v2/sync/cursor/{clientId}",
        )
        val expectedPostPaths = mapOf(
            "acknowledgeSyncCursorV2" to "v2/sync/cursor/ack",
            "uploadSyncChangesV2" to "v2/sync/upload",
            "pullSyncChangesV2" to "v2/sync/pull",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    // ========== Import Jobs V2 ==========

    @Test
    fun apiContract_coversImportJobV2Routes() {
        val expectedGetPaths = mapOf(
            "importJobsV2" to "v2/import-jobs",
            "importJobV2" to "v2/import-jobs/{id}",
        )
        val expectedPostPaths = mapOf(
            "createImportJobV2" to "v2/import-jobs",
            "retryImportJobV2" to "v2/import-jobs/{id}/retry",
            "cancelImportJobV2" to "v2/import-jobs/{id}/cancel",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    @Test
    fun apiContract_importJobsQueryParamsMatchBackend() {
        // status (snake_case not required; single token).
        val queryValues = getQueryValues("importJobsV2")
        assertEquals(listOf("status"), queryValues)
    }

    // ========== Agent Workbench / Tasks / Notifications / Chat ==========

    @Test
    fun apiContract_coversAgentWorkbenchV2Routes() {
        val expectedGetPaths = mapOf(
            "agentWorkbenchV2" to "v2/agent/workbench",
            "agentTasksV2" to "v2/agent/tasks",
            "agentNotificationsV2" to "v2/agent/notifications",
        )
        val expectedPostPaths = mapOf(
            "markAgentNotificationReadV2" to "v2/agent/notifications/{id}/read",
            "agentChatV2" to "v2/agent/chat",
            "cancelAgentRunV2" to "v2/agent/runs/{runId}/cancel",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    @Test
    fun apiContract_agentNotificationsQueryParamsMatchBackend() {
        // unread_only (snake_case for explicit @RequestParam name).
        val queryValues = getQueryValues("agentNotificationsV2")
        assertEquals(listOf("unread_only"), queryValues)
    }

    // ========== Media (additional upload route) ==========

    @Test
    fun apiContract_coversMediaAssetUploadRoute() {
        // Multipart upload route has its own distinct path under /upload.
        assertEquals("v2/media/assets/upload", postPath("uploadMediaAssetV2"))
    }

    // ========== Helpers ==========

    private fun getPath(methodName: String): String {
        val method = ZhihuijiV2Api::class.java.methods.first { it.name == methodName }
        return requireNotNull(method.getAnnotation(GET::class.java)).value
    }

    private fun postPath(methodName: String): String {
        val method = ZhihuijiV2Api::class.java.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(POST::class.java)
        assertNotNull("Missing @POST on $methodName", annotation)
        return requireNotNull(annotation).value
    }

    private fun putPath(methodName: String): String {
        val method = ZhihuijiV2Api::class.java.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(PUT::class.java)
        assertNotNull("Missing @PUT on $methodName", annotation)
        return requireNotNull(annotation).value
    }

    private fun deletePath(methodName: String): String {
        val method = ZhihuijiV2Api::class.java.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(DELETE::class.java)
        assertNotNull("Missing @DELETE on $methodName", annotation)
        return requireNotNull(annotation).value
    }

    /**
     * Returns the list of @Query annotation values for a given method.
     * Used to verify that @Query values match backend @RequestParam names.
     */
    private fun getQueryValues(methodName: String): List<String> {
        val method = ZhihuijiV2Api::class.java.methods.first { it.name == methodName }
        return method.parameters.mapNotNull { param ->
            param.getAnnotation(Query::class.java)?.value
        }
    }
}
