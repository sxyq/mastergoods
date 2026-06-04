package com.zhihuiji.data.supplier

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierV2RepositoryTest {

    @Test
    fun listSuppliersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.partner.SupplierV2Dto>())
        }

        val repository = SupplierV2Repository(api)
        val result = repository.listSuppliers(keyword = "acme", status = 1, groupId = 5L)

        assertTrue(result.isSuccess)
        assertEquals("suppliersV2", invokedMethod)
        assertEquals(listOf("acme", 1, 5L), capturedArgs.take(3))
    }

    @Test
    fun deleteSupplierDelegatesToDeleteSupplierV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = SupplierV2Repository(api)
        val result = repository.deleteSupplier(21L)

        assertTrue(result.isSuccess)
        assertEquals("deleteSupplierV2", invokedMethod)
        assertEquals(21L, invokedId)
    }

    @Test
    fun listContactsDelegatesToSupplierContactsV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedSupplierId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedSupplierId = args?.get(0) as Long
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto>(),
            )
        }

        val repository = SupplierV2Repository(api)
        val result = repository.listContacts(44L)

        assertTrue(result.isSuccess)
        assertEquals("supplierContactsV2", invokedMethod)
        assertEquals(44L, invokedSupplierId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SupplierV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }
}
