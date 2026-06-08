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
        val draftQueryValues = getQueryValues("agentDraftsV2")
        assertEquals(listOf("conversation_id"), draftQueryValues)
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
