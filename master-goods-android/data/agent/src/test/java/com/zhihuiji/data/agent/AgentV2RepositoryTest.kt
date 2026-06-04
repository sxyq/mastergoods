package com.zhihuiji.data.agent

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentV2RepositoryTest {

    @Test
    fun deleteDraftDelegatesToDeleteAgentDraftV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = AgentV2Repository(api)
        val result = repository.deleteDraft(5L)

        assertTrue(result.isSuccess)
        assertEquals("deleteAgentDraftV2", invokedMethod)
        assertEquals(5L, invokedId)
    }

    @Test
    fun deleteConversationDelegatesToDeleteAgentConversationV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = AgentV2Repository(api)
        val result = repository.deleteConversation(11L)

        assertTrue(result.isSuccess)
        assertEquals("deleteAgentConversationV2", invokedMethod)
        assertEquals(11L, invokedId)
    }

    @Test
    fun deleteAssetDelegatesToDeleteMediaAssetV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = MediaV2Repository(api)
        val result = repository.deleteAsset(7L)

        assertTrue(result.isSuccess)
        assertEquals("deleteMediaAssetV2", invokedMethod)
        assertEquals(7L, invokedId)
    }

    @Test
    fun deleteBindingDelegatesToDeleteMediaBindingV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = MediaV2Repository(api)
        val result = repository.deleteBinding(13L)

        assertTrue(result.isSuccess)
        assertEquals("deleteMediaBindingV2", invokedMethod)
        assertEquals(13L, invokedId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "AgentV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }
}
