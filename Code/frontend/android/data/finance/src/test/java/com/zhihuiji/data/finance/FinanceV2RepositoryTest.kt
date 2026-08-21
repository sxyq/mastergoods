package com.zhihuiji.data.finance

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceV2RepositoryTest {

    @Test
    fun deleteAccountDelegatesToDeleteAccountV2AndSucceedsForUnitResponse() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = FinanceV2Repository(api)
        val result = repository.deleteAccount(9L)

        assertTrue(result.isSuccess)
        assertEquals("deleteAccountV2", invokedMethod)
        assertEquals(9L, invokedId)
    }

    @Test
    fun deleteBillFundLinkDelegatesToDeleteBillFundLinkV2AndSucceedsForUnitResponse() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = FinanceV2Repository(api)
        val result = repository.deleteBillFundLink(17L)

        assertTrue(result.isSuccess)
        assertEquals("deleteBillFundLinkV2", invokedMethod)
        assertEquals(17L, invokedId)
    }

    @Test
    fun listAccountsDelegatesToAccountsV2() = runBlocking {
        var invokedMethod: String? = null
        val api = fakeApi { methodName, _ ->
            invokedMethod = methodName
            ApiResponse(
                code = 0,
                message = "ok",
                data = listOf(AccountV2Dto(id = 1L, code = "CASH", name = "Cash")),
            )
        }

        val repository = FinanceV2Repository(api)
        val result = repository.listAccounts()

        assertTrue(result.isSuccess)
        assertEquals("accountsV2", invokedMethod)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun listBillFundLinksForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.finance.BillFundLinkV2Dto>())
        }

        val repository = FinanceV2Repository(api)
        val result = repository.listBillFundLinks("sale_order", 3L, 5L)

        assertTrue(result.isSuccess)
        assertEquals("billFundLinksV2", invokedMethod)
        assertEquals(listOf("sale_order", 3L, 5L), capturedArgs.take(3))
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "FinanceV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }
}
