package com.zhihuiji.data.sync

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryV2RepositoryTest {

    @Test
    fun listInventoryLedgerBySourceForwardsSourceArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto>(),
            )
        }

        val repository = InventoryV2Repository(api)
        val result = repository.listInventoryLedgerBySource("sale_order", 3L)

        assertTrue(result.isSuccess)
        assertEquals("inventoryLedgerBySourceV2", invokedMethod)
        assertEquals(listOf("sale_order", 3L), capturedArgs.take(2))
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "InventoryV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }
}
