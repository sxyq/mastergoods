package com.zhihuiji.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ZhihuijiApiContractTest {
    @Test
    fun apiContract_keepsCriticalRemoteEndpointPaths() {
        assertEquals("auth/login", postPath("login"))
        assertEquals("products", getPath("products"))
        assertEquals("sale-orders", getPath("saleOrders"))
        assertEquals("purchase-orders", getPath("purchaseOrders"))
        assertEquals("pay-orders", getPath("payOrders"))
        assertEquals("finance-records", getPath("financeRecords"))
        assertEquals("reports/sales-summary", getPath("salesSummary"))
        assertEquals("sync/health", getPath("syncHealth"))
        assertEquals("agent/workbench", getPath("agentWorkbench"))
    }

    @Test
    fun apiContract_hasMutationEndpointsForCoreBusinessFlows() {
        assertEquals("products", postPath("createProduct"))
        assertEquals("sale-orders", postPath("createSaleOrder"))
        assertEquals("purchase-orders", postPath("createPurchaseOrder"))
        assertEquals("pay-orders", postPath("createPayOrder"))
        assertEquals("finance-records", postPath("createFinanceRecord"))
    }

    private fun getPath(methodName: String): String {
        val method = ZhihuijiApi::class.java.methods.first { it.name == methodName }
        return requireNotNull(method.getAnnotation(GET::class.java)).value
    }

    private fun postPath(methodName: String): String {
        val method = ZhihuijiApi::class.java.methods.first { it.name == methodName }
        val annotation = method.getAnnotation(POST::class.java)
        assertNotNull("Missing @POST on $methodName", annotation)
        return requireNotNull(annotation).value
    }
}
