package com.zhihuiji.data.agent

import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.CreateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentDraftRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentMessageRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listConversations(): Result<List<AgentConversationDto>> =
        safeApiCall { api.agentConversationsV2() }

    suspend fun getConversation(id: Long): Result<AgentConversationDto> =
        safeApiCall { api.agentConversationV2(id) }

    suspend fun createConversation(request: CreateAgentConversationRequest): Result<AgentConversationDto> =
        safeApiCall { api.createAgentConversationV2(request) }

    suspend fun updateConversation(id: Long, request: UpdateAgentConversationRequest): Result<AgentConversationDto> =
        safeApiCall { api.updateAgentConversationV2(id, request) }

    suspend fun deleteConversation(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteAgentConversationV2(id) }

    suspend fun listMessages(conversationId: Long): Result<List<AgentMessageDto>> =
        safeApiCall { api.agentMessagesV2(conversationId) }

    suspend fun createMessage(
        conversationId: Long,
        request: CreateAgentMessageRequest,
    ): Result<AgentMessageDto> = safeApiCall { api.createAgentMessageV2(conversationId, request) }

    suspend fun listDrafts(conversationId: Long? = null): Result<List<AgentDraftDto>> =
        safeApiCall { api.agentDraftsV2(conversationId) }

    suspend fun createDraft(request: CreateAgentDraftRequest): Result<AgentDraftDto> =
        safeApiCall { api.createAgentDraftV2(request) }

    suspend fun updateDraft(id: Long, request: UpdateAgentDraftRequest): Result<AgentDraftDto> =
        safeApiCall { api.updateAgentDraftV2(id, request) }

    suspend fun deleteDraft(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteAgentDraftV2(id) }
}
