package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Agent 聊天请求（非流式，用于快速问答或兼容性接口）
 */
@Serializable
data class AgentChatRequest(
    @SerialName("conversation_id") val conversationId: Long? = null,
    val message: String,
    @SerialName("stream") val stream: Boolean = false,
    @SerialName("image_asset_ids") val imageAssetIds: List<Long> = emptyList(),
)

@Serializable
data class AgentImageGenerateRequest(
    val prompt: String,
    @SerialName("reference_asset_ids") val referenceAssetIds: List<Long> = emptyList(),
)

@Serializable
data class AgentImageGenerateResponse(
    @SerialName("image_url") val imageUrl: String,
    @SerialName("revised_prompt") val revisedPrompt: String? = null,
)

/**
 * Agent 聊天响应（非流式）
 */
@Serializable
data class AgentChatResponse(
    @SerialName("run_id") val runId: String,
    @SerialName("conversation_id") val conversationId: Long,
    val answer: String? = null,
    val blocks: List<ResultBlockDto> = emptyList(),
    @SerialName("draft_id") val draftId: Long? = null,
    @SerialName("safety_passed") val safetyPassed: Boolean = true,
    @SerialName("safety_reason") val safetyReason: String? = null,
    val mode: String? = null,
    @SerialName("llm_status") val llmStatus: String? = null,
    @SerialName("plan_source") val planSource: String? = null,
    @SerialName("plan_summary") val planSummary: String? = null,
    @SerialName("tool_calls") val toolCalls: List<AgentToolCallDto> = emptyList(),
    @SerialName("evidence_refs") val evidenceRefs: List<AgentEvidenceRefDto> = emptyList(),
    @SerialName("result_blocks") val resultBlocks: List<ResultBlockDto> = emptyList(),
    @SerialName("performance_summary") val performanceSummary: AgentPerformanceSummaryDto? = null,
    @SerialName("audit_id") val auditId: String? = null,
    @SerialName("trace_id") val traceId: String? = null,
    val observability: AgentObservabilityDto? = null,
    @SerialName("terminal_status") val terminalStatus: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("safe_message") val safeMessage: String? = null,
    @SerialName("completed_tools") val completedTools: List<String> = emptyList(),
    @SerialName("missing_target_tools") val missingTargetTools: List<String> = emptyList(),
)

@Serializable
data class AgentDraftConfirmRequest(
    @SerialName("idempotency_key") val idempotencyKey: String,
)

@Serializable
data class AgentToolCallDto(
    val sequence: Int? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_name") val toolName: String,
    val status: String,
    @SerialName("input_summary") val inputSummary: String? = null,
    @SerialName("query_window") val queryWindow: JsonElement? = null,
    @SerialName("returned_count") val returnedCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val limit: Int? = null,
    @SerialName("is_truncated") val isTruncated: Boolean? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("result_summary") val resultSummary: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
data class AgentEvidenceRefDto(
    @SerialName("evidence_id") val evidenceId: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_name") val toolName: String? = null,
    val label: String,
    val value: String,
    @SerialName("query_window") val queryWindow: JsonElement? = null,
    @SerialName("is_truncated") val isTruncated: Boolean? = null,
)

@Serializable
data class AgentPerformanceSummaryDto(
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("tool_duration_ms") val toolDurationMs: Long? = null,
    @SerialName("model_duration_ms") val modelDurationMs: Long? = null,
)

@Serializable
data class AgentObservabilityDto(
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("correlation_id") val correlationId: String? = null,
    @SerialName("trace_id") val traceId: String? = null,
    @SerialName("audit_id") val auditId: String? = null,
    @SerialName("log_ref") val logRef: String? = null,
)
