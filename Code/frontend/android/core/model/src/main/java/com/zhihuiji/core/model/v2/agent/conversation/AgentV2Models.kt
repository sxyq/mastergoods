package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgentConversationDto(
    val id: Long = 0,
    val title: String = "",
    val status: String = "",
    @SerialName("latest_summary") val latestSummary: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("last_message_at") val lastMessageAt: Long? = null,
)

@Serializable
data class CreateAgentConversationRequest(
    val title: String,
    val status: String? = null,
)

@Serializable
data class UpdateAgentConversationRequest(
    val title: String? = null,
    val status: String? = null,
)

@Serializable
data class AgentMessageDto(
    val id: Long = 0,
    @SerialName("conversation_id") val conversationId: Long = 0L,
    val role: String = "",
    @SerialName("message_type") val messageType: String = "",
    val content: String = "",
    @SerialName("structured_data_json") val structuredDataJson: String? = null,
    @SerialName("run_id") val runId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateAgentMessageRequest(
    val role: String,
    @SerialName("message_type") val messageType: String,
    val content: String,
    @SerialName("structured_data_json") val structuredDataJson: String? = null,
)

@Serializable
data class AgentDraftDto(
    val id: Long = 0,
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("draft_type") val draftType: String = "",
    val title: String = "",
    @SerialName("content_json") val contentJson: String = "",
    val status: String = "",
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CreateAgentDraftRequest(
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("draft_type") val draftType: String,
    val title: String,
    @SerialName("content_json") val contentJson: String,
    val status: String? = null,
)

@Serializable
data class UpdateAgentDraftRequest(
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("draft_type") val draftType: String,
    val title: String,
    @SerialName("content_json") val contentJson: String,
    val status: String? = null,
)

@Serializable
data class AgentRunCancelDto(
    @SerialName("run_id") val runId: String,
    val status: String,
    val cancelled: Boolean = false,
)
