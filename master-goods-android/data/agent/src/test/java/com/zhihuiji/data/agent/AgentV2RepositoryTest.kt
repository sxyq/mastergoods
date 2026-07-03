package com.zhihuiji.data.agent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentRunCancelDto
import com.zhihuiji.core.network.AgentSseClient
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentV2RepositoryTest {

    private val fakeSseClient = AgentSseClient(
        okHttpClient = okhttp3.OkHttpClient(),
        json = Json { ignoreUnknownKeys = true },
        baseUrlProvider = { "http://localhost" },
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val emptyDataStore = object : DataStore<Preferences> {
        override val data = flowOf(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = emptyPreferences()
    }

    private val fakeSettingsStore = SettingsStore(emptyDataStore)
    private val fakeSessionStore = SessionStore(emptyDataStore)

    @Test
    fun listConversationsPassesOptionalPageAndLimit() = runBlocking {
        var invokedMethod: String? = null
        var invokedArgs: Array<out Any?>? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedArgs = args
            ApiResponse<List<AgentConversationDto>>(code = 0, message = "ok", data = emptyList())
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.listConversations(page = 2, limit = 25)

        assertTrue(result.isSuccess)
        assertEquals("agentConversationsV2", invokedMethod)
        assertEquals(2, invokedArgs?.get(0))
        assertEquals(25, invokedArgs?.get(1))
    }

    @Test
    fun listMessagesPassesOptionalPageAndLimit() = runBlocking {
        var invokedMethod: String? = null
        var invokedArgs: Array<out Any?>? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedArgs = args
            ApiResponse<List<AgentMessageDto>>(code = 0, message = "ok", data = emptyList())
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.listMessages(conversationId = 7L, page = 1, limit = 40)

        assertTrue(result.isSuccess)
        assertEquals("agentMessagesV2", invokedMethod)
        assertEquals(7L, invokedArgs?.get(0))
        assertEquals(1, invokedArgs?.get(1))
        assertEquals(40, invokedArgs?.get(2))
    }

    @Test
    fun listRecentMessagesUsesFixedFirstPageWindow() = runBlocking {
        var invokedMethod: String? = null
        var invokedArgs: Array<out Any?>? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedArgs = args
            ApiResponse<List<AgentMessageDto>>(code = 0, message = "ok", data = emptyList())
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.listRecentMessages(conversationId = 7L)

        assertTrue(result.isSuccess)
        assertEquals("agentMessagesV2", invokedMethod)
        assertEquals(7L, invokedArgs?.get(0))
        assertEquals(0, invokedArgs?.get(1))
        assertEquals(RECENT_AGENT_MESSAGE_WINDOW_LIMIT, invokedArgs?.get(2))
        assertEquals(80, invokedArgs?.get(2))
    }

    @Test
    fun listDraftsPassesConversationPageAndLimit() = runBlocking {
        var invokedMethod: String? = null
        var invokedArgs: Array<out Any?>? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedArgs = args
            ApiResponse<List<AgentDraftDto>>(code = 0, message = "ok", data = emptyList())
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.listDrafts(conversationId = 9L, page = 3, limit = 10)

        assertTrue(result.isSuccess)
        assertEquals("agentDraftsV2", invokedMethod)
        assertEquals(9L, invokedArgs?.get(0))
        assertEquals(3, invokedArgs?.get(1))
        assertEquals(10, invokedArgs?.get(2))
    }

    @Test
    fun deleteDraftDelegatesToDeleteAgentDraftV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.deleteDraft(5L)

        assertTrue(result.isSuccess)
        assertEquals("deleteAgentDraftV2", invokedMethod)
        assertEquals(5L, invokedId)
    }

    @Test
    fun confirmDraftDelegatesToConfirmAgentDraftV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse(code = 0, message = "ok", data = AgentDraftDto(id = invokedId ?: 0L, draftType = "create_sale_order"))
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.confirmDraft(6L)

        assertTrue(result.isSuccess)
        assertEquals("confirmAgentDraftV2", invokedMethod)
        assertEquals(6L, invokedId)
    }

    @Test
    fun cancelDraftDelegatesToCancelAgentDraftV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse(code = 0, message = "ok", data = AgentDraftDto(id = invokedId ?: 0L, draftType = "create_sale_order"))
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.cancelDraft(7L)

        assertTrue(result.isSuccess)
        assertEquals("cancelAgentDraftV2", invokedMethod)
        assertEquals(7L, invokedId)
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

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.deleteConversation(11L)

        assertTrue(result.isSuccess)
        assertEquals("deleteAgentConversationV2", invokedMethod)
        assertEquals(11L, invokedId)
    }

    @Test
    fun cancelRunDelegatesToCancelAgentRunV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedRunId: String? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedRunId = args?.get(0) as String
            ApiResponse(
                code = 0,
                message = "ok",
                data = AgentRunCancelDto(runId = invokedRunId ?: "", status = "cancelled", cancelled = true),
            )
        }

        val repository = AgentV2Repository(api, fakeSseClient, json)
        val result = repository.cancelRun("run-123")

        assertTrue(result.isSuccess)
        assertEquals("cancelAgentRunV2", invokedMethod)
        assertEquals("run-123", invokedRunId)
        assertEquals(true, result.getOrThrow().cancelled)
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

        val repository = MediaV2Repository(api, fakeSettingsStore, fakeSessionStore)
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

        val repository = MediaV2Repository(api, fakeSettingsStore, fakeSessionStore)
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
