package com.zhihuiji.data.agent

import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.AgentChatResponse
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.AgentImageGenerateRequest
import com.zhihuiji.core.model.v2.agent.AgentImageGenerateResponse
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentNotificationDto
import com.zhihuiji.core.model.v2.agent.AgentRunCancelDto
import com.zhihuiji.core.model.v2.agent.AgentStreamEvent
import com.zhihuiji.core.model.v2.agent.AgentTaskDto
import com.zhihuiji.core.model.v2.agent.AgentWorkbenchV2Dto
import com.zhihuiji.core.model.v2.agent.CreateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentDraftRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentMessageRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.core.network.AgentSseClient
import com.zhihuiji.core.network.RetryState
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

const val RECENT_AGENT_MESSAGE_WINDOW_LIMIT = 80

@Singleton
class AgentV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val sseClient: AgentSseClient,
    private val json: Json,
) {
    /** SSE 连接重连状态，透传自 AgentSseClient */
    val retryState: StateFlow<RetryState> get() = sseClient.retryState

    // ---------- Conversation ----------

    suspend fun listConversations(
        page: Int? = null,
        limit: Int? = null,
    ): Result<List<AgentConversationDto>> =
        safeApiCall { api.agentConversationsV2(page, limit) }

    suspend fun getConversation(id: Long): Result<AgentConversationDto> =
        safeApiCall { api.agentConversationV2(id) }

    suspend fun createConversation(request: CreateAgentConversationRequest): Result<AgentConversationDto> =
        safeApiCall { api.createAgentConversationV2(request) }

    suspend fun updateConversation(id: Long, request: UpdateAgentConversationRequest): Result<AgentConversationDto> =
        safeApiCall { api.updateAgentConversationV2(id, request) }

    suspend fun deleteConversation(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteAgentConversationV2(id) }

    // ---------- Message ----------

    suspend fun listMessages(
        conversationId: Long,
        page: Int? = null,
        limit: Int? = null,
    ): Result<List<AgentMessageDto>> =
        safeApiCall { api.agentMessagesV2(conversationId, page, limit) }

    suspend fun createMessage(
        conversationId: Long,
        request: CreateAgentMessageRequest,
    ): Result<AgentMessageDto> = safeApiCall { api.createAgentMessageV2(conversationId, request) }

    // ---------- Draft ----------

    suspend fun listDrafts(
        conversationId: Long? = null,
        page: Int? = null,
        limit: Int? = null,
    ): Result<List<AgentDraftDto>> =
        safeApiCall { api.agentDraftsV2(conversationId, page, limit) }

    suspend fun createDraft(request: CreateAgentDraftRequest): Result<AgentDraftDto> =
        safeApiCall { api.createAgentDraftV2(request) }

    suspend fun confirmDraft(id: Long): Result<AgentDraftDto> =
        safeApiCall { api.confirmAgentDraftV2(id) }

    suspend fun cancelDraft(id: Long): Result<AgentDraftDto> =
        safeApiCall { api.cancelAgentDraftV2(id) }

    suspend fun updateDraft(id: Long, request: UpdateAgentDraftRequest): Result<AgentDraftDto> =
        safeApiCall { api.updateAgentDraftV2(id, request) }

    suspend fun deleteDraft(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteAgentDraftV2(id) }

    // ---------- Workbench ----------

    suspend fun getWorkbench(): Result<AgentWorkbenchV2Dto> =
        safeApiCall { api.agentWorkbenchV2() }

    suspend fun listTasks(): Result<List<AgentTaskDto>> =
        safeApiCall { api.agentTasksV2() }

    suspend fun listNotifications(unreadOnly: Boolean? = null): Result<List<AgentNotificationDto>> =
        safeApiCall { api.agentNotificationsV2(unreadOnly) }

    suspend fun markNotificationRead(id: Long): Result<AgentNotificationDto> =
        safeApiCall { api.markAgentNotificationReadV2(id) }

    // ---------- Chat (non-streaming fallback) ----------

    suspend fun chat(request: AgentChatRequest): Result<AgentChatResponse> =
        safeApiCall { api.agentChatV2(request) }

    suspend fun generateImage(request: AgentImageGenerateRequest): Result<AgentImageGenerateResponse> =
        safeApiCall { api.agentGenerateImageV2(request) }

    suspend fun cancelRun(runId: String): Result<AgentRunCancelDto> =
        safeApiCall { api.cancelAgentRunV2(runId) }

    // ---------- Chat Stream (SSE) ----------

    /**
     * 流式聊天，返回 AgentStreamEvent 的 Flow。
     *
     * @param request 聊天请求（stream 字段会被忽略，始终走 SSE）
     */
    fun chatStream(request: AgentChatRequest): Flow<AgentStreamEvent> {
        val requestJson = json.encodeToString(AgentChatRequest.serializer(), request.copy(stream = true))
        return sseClient.chatStream(requestJson)
    }
}

suspend fun AgentV2Repository.listRecentMessages(conversationId: Long): Result<List<AgentMessageDto>> =
    listMessages(
        conversationId = conversationId,
        page = 0,
        limit = RECENT_AGENT_MESSAGE_WINDOW_LIMIT,
    )
