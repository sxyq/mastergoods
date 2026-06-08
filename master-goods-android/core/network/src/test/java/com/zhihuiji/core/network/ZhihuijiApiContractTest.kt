package com.zhihuiji.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ZhihuijiApiContractTest {
    @Test
    fun apiContract_keepsCriticalRemoteEndpointPaths() {
        assertEquals("v1/auth/login", postPath("login"))
        assertEquals("v1/products", getPath("products"))
        assertEquals("v1/sale-orders", getPath("saleOrders"))
        assertEquals("v1/purchase-orders", getPath("purchaseOrders"))
        assertEquals("v1/pay-orders", getPath("payOrders"))
        assertEquals("v1/finance-records", getPath("financeRecords"))
        assertEquals("v1/reports/sales-summary", getPath("salesSummary"))
        assertEquals("v1/sync/health", getPath("syncHealth"))
        assertEquals("v2/agent/workbench", getPath("agentWorkbenchV2", ZhihuijiV2Api::class.java))
    }

    @Test
    fun apiContract_hasMutationEndpointsForCoreBusinessFlows() {
        assertEquals("v1/products", postPath("createProduct"))
        assertEquals("v1/sale-orders", postPath("createSaleOrder"))
        assertEquals("v1/purchase-orders", postPath("createPurchaseOrder"))
        assertEquals("v1/pay-orders", postPath("createPayOrder"))
        assertEquals("v1/finance-records", postPath("createFinanceRecord"))
        assertEquals("v2/agent/chat", postPath("agentChatV2", ZhihuijiV2Api::class.java))
    }

    private fun getPath(methodName: String, apiClass: Class<*> = ZhihuijiApi::class.java): String {
        val method = apiClass.methods.first { it.name == methodName }
        return requireNotNull(method.getAnnotation(GET::class.java)).value
    }

    private fun postPath(methodName: String, apiClass: Class<*> = ZhihuijiApi::class.java): String {
        val method = apiClass.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(POST::class.java)
        assertNotNull("Missing @POST on $methodName", annotation)
        return requireNotNull(annotation).value
    }
}
