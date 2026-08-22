package com.zhihuiji.data.order

import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.StatusRequest
import com.zhihuiji.core.model.v2.order.PayOrderV2Filter
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Filter
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Filter
import com.zhihuiji.core.network.CacheScopeProvider
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OrderV2RepositoryTest {

    @Test
    fun listSaleOrdersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.order.SaleOrderV2Dto>())
        }

        val repository = SaleOrderV2Repository(api, fixedCacheScopeProvider(), fakeSaleOrderDao())
        val result = repository.listSaleOrders(
            SaleOrderV2Filter(
                keyword = "SO",
                status = 1,
                minTotalAmount = "10",
                maxTotalAmount = "99",
                createdAfter = "2026-06-01T00:00:00Z",
                createdBefore = "2026-06-03T00:00:00Z",
                productKeyword = "milk",
                paymentStatus = "0",
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals("saleOrdersV2", invokedMethod)
        assertEquals(
            listOf("SO", 1, "10", "99", "2026-06-01T00:00:00Z", "2026-06-03T00:00:00Z", "milk", "0"),
            capturedArgs.take(8),
        )
    }

    @Test
    fun listSaleOrdersDoesNotReuseCacheAcrossCacheScopes() = runBlocking {
        var scope = "base=http://a/|user=1"
        var invocationCount = 0
        val api = fakeApi { methodName, _ ->
            if (methodName == "saleOrdersV2") {
                invocationCount += 1
                ApiResponse(
                    code = 0,
                    message = "ok",
                    data = listOf(SaleOrderV2Dto(id = invocationCount.toLong(), orderNo = "SO-$invocationCount")),
                )
            } else {
                error("Unexpected method $methodName")
            }
        }
        val repository = SaleOrderV2Repository(
            api = api,
            cacheScopeProvider = object : CacheScopeProvider {
                override fun scopeKey(): String = scope
            },
            saleOrderDao = fakeSaleOrderDao(),
        )

        val first = repository.listSaleOrders().getOrThrow()
        val secondSameScope = repository.listSaleOrders().getOrThrow()
        scope = "base=http://a/|user=2"
        val thirdDifferentScope = repository.listSaleOrders().getOrThrow()

        assertEquals(1, first.single().id)
        assertEquals(1, secondSameScope.single().id)
        assertEquals(2, thirdDifferentScope.single().id)
        assertEquals(2, invocationCount)
    }

    @Test
    fun updateSaleOrderStatusDelegatesToUpdateSaleOrderStatusV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        var invokedStatus: StatusRequest? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            invokedStatus = args[1] as StatusRequest
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = SaleOrderV2Repository(api, fixedCacheScopeProvider(), fakeSaleOrderDao())
        val result = repository.updateStatus(6L, 2)

        assertTrue(result.isSuccess)
        assertEquals("updateSaleOrderStatusV2", invokedMethod)
        assertEquals(6L, invokedId)
        assertEquals(2, invokedStatus?.status)
    }

    @Test
    fun downloadReceiptPdfReadsBinaryResponse() = runBlocking {
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            assertEquals("saleOrderReceiptPdfV2", methodName)
            invokedId = args?.get(0) as Long
            Response.success("%PDF-1.7".toResponseBody("application/pdf".toMediaType()))
        }

        val result = SaleOrderV2Repository(api, fixedCacheScopeProvider(), fakeSaleOrderDao()).downloadReceiptPdf(8L)

        assertTrue(result.isSuccess)
        assertEquals("%PDF-1.7", result.getOrThrow().decodeToString())
        assertEquals(8L, invokedId)
    }

    @Test
    fun listPurchaseOrdersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto>())
        }

        val repository = PurchaseOrderV2Repository(api)
        val result = repository.listPurchaseOrders(PurchaseOrderV2Filter(keyword = "PO", status = 0))

        assertTrue(result.isSuccess)
        assertEquals("purchaseOrdersV2", invokedMethod)
        assertEquals(listOf("PO", 0), capturedArgs.take(2))
    }

    @Test
    fun listPayOrdersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.order.PayOrderV2Dto>())
        }

        val repository = PayOrderV2Repository(api)
        val result = repository.listPayOrders(
            PayOrderV2Filter(
                keyword = "PAY",
                status = 1,
                createdAfter = "2026-06-01T00:00:00Z",
                createdBefore = "2026-06-03T00:00:00Z",
                page = 3,
                size = 25,
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals("payOrdersV2", invokedMethod)
        assertEquals(
            listOf("PAY", 1, "2026-06-01T00:00:00Z", "2026-06-03T00:00:00Z", 3, 25),
            capturedArgs.take(6),
        )
    }

    @Test
    fun deletePurchaseOrderAcceptsSuccessfulNullDataResponse() = runBlocking {
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            assertEquals("deletePurchaseOrderV2", methodName)
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val result = PurchaseOrderV2Repository(api).deletePurchaseOrder(12L)

        assertTrue(result.isSuccess)
        assertEquals(12L, invokedId)
    }

    @Test
    fun updatePayOrderStatusDelegatesToUpdatePayOrderStatusV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        var invokedStatus: StatusRequest? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            invokedStatus = args[1] as StatusRequest
            ApiResponse(
                code = 0,
                message = "ok",
                data = com.zhihuiji.core.model.v2.order.PayOrderV2Dto(id = 8L, status = 2),
            )
        }

        val repository = PayOrderV2Repository(api)
        val result = repository.updateStatus(8L, 2)

        assertTrue(result.isSuccess)
        assertEquals("updatePayOrderStatusV2", invokedMethod)
        assertEquals(8L, invokedId)
        assertEquals(2, invokedStatus?.status)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "OrderV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }

    private fun fixedCacheScopeProvider(): CacheScopeProvider =
        object : CacheScopeProvider {
            override fun scopeKey(): String = "test"
        }

    private fun fakeSaleOrderDao(): SaleOrderDao = Proxy.newProxyInstance(
        SaleOrderDao::class.java.classLoader,
        arrayOf(SaleOrderDao::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "hashCode" -> 0
            "toString" -> "SaleOrderDaoProxy"
            "equals" -> false
            else -> Unit
        }
    } as SaleOrderDao
}
