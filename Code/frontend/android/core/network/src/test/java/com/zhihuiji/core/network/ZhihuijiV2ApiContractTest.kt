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
            "confirmAgentDraftV2" to "v2/agent/drafts/{id}/confirm",
            "cancelAgentDraftV2" to "v2/agent/drafts/{id}/cancel",
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
        val conversationQueryValues = getQueryValues("agentConversationsV2")
        assertEquals(listOf("page", "limit"), conversationQueryValues)
        val messageQueryValues = getQueryValues("agentMessagesV2")
        assertEquals(listOf("page", "limit"), messageQueryValues)
        val draftQueryValues = getQueryValues("agentDraftsV2")
        assertEquals(listOf("conversation_id", "page", "limit"), draftQueryValues)
    }

    @Test
    fun apiContract_coversSyncV2Routes() {
        val expectedGetPaths = mapOf(
            "syncHealthV2" to "v2/sync/health",
            "syncCursorV2" to "v2/sync/cursor/{clientId}",
        )
        val expectedPostPaths = mapOf(
            "acknowledgeSyncCursorV2" to "v2/sync/cursor/ack",
            "uploadSyncChangesV2" to "v2/sync/upload",
            "pullSyncChangesV2" to "v2/sync/pull",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    // ========== Import Jobs V2 ==========

    @Test
    fun apiContract_coversImportJobV2Routes() {
        val expectedGetPaths = mapOf(
            "importJobsV2" to "v2/import-jobs",
            "importJobV2" to "v2/import-jobs/{id}",
        )
        val expectedPostPaths = mapOf(
            "createImportJobV2" to "v2/import-jobs",
            "retryImportJobV2" to "v2/import-jobs/{id}/retry",
            "cancelImportJobV2" to "v2/import-jobs/{id}/cancel",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    @Test
    fun apiContract_importJobsQueryParamsMatchBackend() {
        // status (snake_case not required; single token).
        val queryValues = getQueryValues("importJobsV2")
        assertEquals(listOf("status"), queryValues)
    }

    // ========== Agent Workbench / Tasks / Notifications / Chat ==========

    @Test
    fun apiContract_coversAgentWorkbenchV2Routes() {
        val expectedGetPaths = mapOf(
            "agentWorkbenchV2" to "v2/agent/workbench",
            "agentTasksV2" to "v2/agent/tasks",
            "agentNotificationsV2" to "v2/agent/notifications",
        )
        val expectedPostPaths = mapOf(
            "markAgentNotificationReadV2" to "v2/agent/notifications/{id}/read",
            "agentChatV2" to "v2/agent/chat",
            "cancelAgentRunV2" to "v2/agent/runs/{runId}/cancel",
        )
        expectedGetPaths.forEach { (methodName, path) -> assertEquals(path, getPath(methodName)) }
        expectedPostPaths.forEach { (methodName, path) -> assertEquals(path, postPath(methodName)) }
    }

    @Test
    fun apiContract_agentNotificationsQueryParamsMatchBackend() {
        // unread_only (snake_case for explicit @RequestParam name).
        val queryValues = getQueryValues("agentNotificationsV2")
        assertEquals(listOf("unread_only"), queryValues)
    }

    // ========== Media (additional upload route) ==========

    @Test
    fun apiContract_coversMediaAssetUploadRoute() {
        // Multipart upload route has its own distinct path under /upload.
        assertEquals("v2/media/assets/upload", postPath("uploadMediaAssetV2"))
    }

    // ========== Helpers ==========

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
