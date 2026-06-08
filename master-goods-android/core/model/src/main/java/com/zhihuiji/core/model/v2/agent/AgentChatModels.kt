package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 聊天消息领域模型，用于 UI 层展示。
 * 与 AgentMessageDto 的区别：本模型包含流式过程中的临时状态（如 isStreaming、runTrace 等）。
 */
data class ChatMessage(
    val id: String, // 本地生成 UUID，或后端返回的消息 ID
    val conversationId: Long,
    val role: MessageRole,
    val content: String = "",
    val blocks: List<ResultBlockDto> = emptyList(),
    val parts: List<ChatMessagePart> = emptyList(),
    val runTrace: RunTrace? = null,
    val isStreaming: Boolean = false,
    val animateReveal: Boolean = false,
    val hasServerAnswerDelta: Boolean = false,
    val answerDeltaSource: String? = null,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

sealed interface ChatMessagePart {
    data class Text(val markdown: String) : ChatMessagePart
    data class ResultBlock(val block: ResultBlockDto) : ChatMessagePart
}

/**
 * 单次运行的过程轨迹，展示在助手消息中，可折叠。
 */
data class RunTrace(
    val runId: String,
    val auditId: String? = null,
    val traceId: String? = null,
    val logRef: String? = null,
    val planSteps: List<PlanStep> = emptyList(),
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val safetyResult: SafetyResult? = null,
    val mode: String? = null,
    val llmStatus: String? = null,
    val planSource: String? = null,
    val answerDeltaSource: String? = null,
    val isExpanded: Boolean = false,
)

data class PlanStep(
    val content: String,
    val timestamp: Long,
)

data class ToolCallRecord(
    val toolName: String,
    val eventId: String? = null,
    val seq: Int? = null,
    val conversationId: Long? = null,
    val toolCallId: String? = null,
    val auditId: String? = null,
    val traceId: String? = null,
    val status: ToolCallStatus,
    val inputSummary: String? = null,
    val queryWindow: JsonElement? = null,
    val resultSummary: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val durationMs: Long? = null,
    val returnedCount: Int? = null,
    val totalCount: Int? = null,
    val limit: Int? = null,
    val isTruncated: Boolean? = null,
    val evidence: JsonElement? = null,
    val nextCursor: String? = null,
    val timestamp: Long,
)

enum class ToolCallStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
}

data class SafetyResult(
    val passed: Boolean,
    val reason: String? = null,
    val suggestedAction: String? = null,
)

/**
 * 工作台聚合数据 DTO（V2 新版）
 */
@Serializable
data class AgentWorkbenchV2Dto(
    val greeting: String,
    @SerialName("kpi_cards") val kpiCards: List<KpiCardItem>,
    @SerialName("quick_questions") val quickQuestions: List<String>,
    @SerialName("recent_conversations") val recentConversations: List<RecentConversationItem>,
    @SerialName("pending_drafts") val pendingDrafts: List<PendingDraftItem>,
    @SerialName("risk_alerts") val riskAlerts: List<RiskAlertItem>,
    @SerialName("today_summary") val todaySummary: String? = null,
)

@Serializable
data class KpiCardItem(
    val label: String,
    val value: String,
    @SerialName("trend_direction") val trendDirection: String? = null,
    @SerialName("trend_value") val trendValue: String? = null,
    val route: String? = null, // 点击可跳转的路由标识
)

@Serializable
data class RecentConversationItem(
    val id: Long,
    val title: String,
    @SerialName("last_message_at") val lastMessageAt: Long,
    @SerialName("message_count") val messageCount: Int = 0,
)

@Serializable
data class PendingDraftItem(
    val id: Long,
    @SerialName("draft_type") val draftType: String,
    val title: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class RiskAlertItem(
    val level: String, // high / medium / low
    val title: String,
    val description: String,
)

@Serializable
data class AgentTaskDto(
    val id: Long,
    @SerialName("task_type") val taskType: String,
    val title: String,
    @SerialName("trigger_source") val triggerSource: String,
    val status: String,
    @SerialName("status_label") val statusLabel: String,
    val progress: Int,
    @SerialName("input_text") val inputText: String? = null,
    @SerialName("result_json") val resultJson: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("completed_at") val completedAt: Long? = null,
)

@Serializable
data class AgentNotificationDto(
    val id: Long,
    @SerialName("task_id") val taskId: Long? = null,
    val title: String,
    val body: String,
    val level: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("is_delivered") val isDelivered: Boolean,
    @SerialName("created_at") val createdAt: Long,
)
