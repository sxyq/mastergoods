package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI 助手审计记录模型。
 * 用于本地记录每次 AI 运行的关键行为，便于后续追溯和问题排查。
 */
@Serializable
data class AgentAuditRecord(
    val id: String, // 本地 UUID
    @SerialName("run_id") val runId: String? = null,
    @SerialName("conversation_id") val conversationId: Long? = null,
    @SerialName("user_message") val userMessage: String,
    @SerialName("safety_result") val safetyResult: SafetyAuditResult? = null,
    @SerialName("tools_called") val toolsCalled: List<ToolAuditRecord> = emptyList(),
    @SerialName("draft_generated") val draftGenerated: DraftAuditInfo? = null,
    @SerialName("context_compacted") val contextCompacted: Boolean = false,
    @SerialName("final_answer_summary") val finalAnswerSummary: String? = null,
    @SerialName("error_info") val errorInfo: ErrorAuditInfo? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class SafetyAuditResult(
    val passed: Boolean,
    val reason: String? = null,
    @SerialName("suggested_action") val suggestedAction: String? = null,
)

@Serializable
data class ToolAuditRecord(
    @SerialName("tool_name") val toolName: String,
    val status: String, // running / completed / failed
    @SerialName("result_summary") val resultSummary: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("returned_count") val returnedCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val limit: Int? = null,
    @SerialName("is_truncated") val isTruncated: Boolean? = null,
    val timestamp: Long,
)

@Serializable
data class DraftAuditInfo(
    @SerialName("draft_id") val draftId: Long? = null,
    @SerialName("draft_type") val draftType: String,
    val title: String,
    @SerialName("user_confirmed") val userConfirmed: Boolean? = null,
)

@Serializable
data class ErrorAuditInfo(
    val code: String? = null,
    val message: String,
)
