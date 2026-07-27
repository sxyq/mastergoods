package com.zhihuiji.data.customer

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerV2RepositoryTest {

    @Test
    fun listCustomersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.partner.CustomerV2Dto>())
        }

        val repository = CustomerV2Repository(api)
        val result = repository.listCustomers(keyword = "alice", status = 1, groupId = 7L)

        assertTrue(result.isSuccess)
        assertEquals("customersV2", invokedMethod)
        assertEquals(listOf("alice", 1, 7L), capturedArgs.take(3))
    }

    @Test
    fun deleteCustomerDelegatesToDeleteCustomerV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = CustomerV2Repository(api)
        val result = repository.deleteCustomer(12L)

        assertTrue(result.isSuccess)
        assertEquals("deleteCustomerV2", invokedMethod)
        assertEquals(12L, invokedId)
    }

    @Test
    fun listContactsDelegatesToCustomerContactsV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedCustomerId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedCustomerId = args?.get(0) as Long
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto>(),
            )
        }

        val repository = CustomerV2Repository(api)
        val result = repository.listContacts(33L)

        assertTrue(result.isSuccess)
        assertEquals("customerContactsV2", invokedMethod)
        assertEquals(33L, invokedCustomerId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "CustomerV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }
}
