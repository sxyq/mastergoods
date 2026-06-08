package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

/**
 * 流式事件密封类，对应后端 SSE 输出的每一行事件。
 * 事件类型必须与后端 V2AgentController.chat() 的 SseEmitter 输出保持一致。
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("event_type")
sealed class AgentStreamEvent {

    /** 运行开始 */
    @Serializable
    @SerialName("run_started")
    data class RunStarted(
        @SerialName("run_id") val runId: String,
        @SerialName("conversation_id") val conversationId: Long,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val observability: AgentObservabilityDto? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 安全审查开始 */
    @Serializable
    @SerialName("safety_check_started")
    data class SafetyCheckStarted(
        @SerialName("run_id") val runId: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 安全审查通过 */
    @Serializable
    @SerialName("safety_check_passed")
    data class SafetyCheckPassed(
        @SerialName("run_id") val runId: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 安全审查拦截 */
    @Serializable
    @SerialName("safety_check_blocked")
    data class SafetyCheckBlocked(
        @SerialName("run_id") val runId: String,
        val reason: String,
        @SerialName("suggested_action") val suggestedAction: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 思考/计划片段（可折叠的过程轨迹） */
    @Serializable
    @SerialName("plan_delta")
    data class PlanDelta(
        @SerialName("run_id") val runId: String,
        val content: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 工具调用开始 */
    @Serializable
    @SerialName("tool_started")
    data class ToolStarted(
        @SerialName("event_id") val eventId: String? = null,
        val seq: Int? = null,
        @SerialName("run_id") val runId: String,
        @SerialName("conversation_id") val conversationId: Long? = null,
        @SerialName("tool_call_id") val toolCallId: String? = null,
        @SerialName("tool_name") val toolName: String,
        @SerialName("input_summary") val inputSummary: String? = null,
        @SerialName("query_window") val queryWindow: JsonElement? = null,
        @SerialName("tool_input") val toolInput: JsonElement? = null,
        @SerialName("started_at") val startedAt: Long? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 工具调用进度（可选，用于长时间查询） */
    @Serializable
    @SerialName("tool_progress")
    data class ToolProgress(
        @SerialName("run_id") val runId: String,
        @SerialName("tool_name") val toolName: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 工具调用完成 */
    @Serializable
    @SerialName("tool_completed")
    data class ToolCompleted(
        @SerialName("event_id") val eventId: String? = null,
        val seq: Int? = null,
        @SerialName("run_id") val runId: String,
        @SerialName("conversation_id") val conversationId: Long? = null,
        @SerialName("tool_call_id") val toolCallId: String? = null,
        @SerialName("tool_name") val toolName: String,
        @SerialName("result_summary") val resultSummary: String? = null,
        @SerialName("input_summary") val inputSummary: String? = null,
        @SerialName("query_window") val queryWindow: JsonElement? = null,
        @SerialName("started_at") val startedAt: Long? = null,
        @SerialName("completed_at") val completedAt: Long? = null,
        @SerialName("duration_ms") val durationMs: Long? = null,
        @SerialName("returned_count") val returnedCount: Int? = null,
        @SerialName("total_count") val totalCount: Int? = null,
        val limit: Int? = null,
        @SerialName("is_truncated") val isTruncated: Boolean? = null,
        val evidence: JsonElement? = null,
        @SerialName("next_cursor") val nextCursor: String? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 工具调用失败 */
    @Serializable
    @SerialName("tool_failed")
    data class ToolFailed(
        @SerialName("event_id") val eventId: String? = null,
        val seq: Int? = null,
        @SerialName("run_id") val runId: String,
        @SerialName("conversation_id") val conversationId: Long? = null,
        @SerialName("tool_call_id") val toolCallId: String? = null,
        @SerialName("tool_name") val toolName: String,
        @SerialName("input_summary") val inputSummary: String? = null,
        @SerialName("query_window") val queryWindow: JsonElement? = null,
        @SerialName("error_code") val errorCode: String? = null,
        @SerialName("safe_message") val safeMessage: String? = null,
        @SerialName("error_summary") val errorSummary: String? = null,
        @SerialName("started_at") val startedAt: Long? = null,
        @SerialName("completed_at") val completedAt: Long? = null,
        @SerialName("duration_ms") val durationMs: Long? = null,
        val evidence: JsonElement? = null,
        @SerialName("next_cursor") val nextCursor: String? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 回答文本流式增量 */
    @Serializable
    @SerialName("answer_delta")
    data class AnswerDelta(
        @SerialName("event_id") val eventId: String? = null,
        val seq: Int? = null,
        @SerialName("run_id") val runId: String,
        @SerialName("conversation_id") val conversationId: Long? = null,
        val delta: String,
        @SerialName("delta_source") val deltaSource: String? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val observability: AgentObservabilityDto? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 非模型真流式场景下的完整答案完成事件 */
    @Serializable
    @SerialName("answer_completed")
    data class AnswerCompleted(
        @SerialName("run_id") val runId: String,
        val answer: String,
        val mode: String? = null,
        @SerialName("llm_status") val llmStatus: String? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val observability: AgentObservabilityDto? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 结构化结果块（表格、KPI、图表等） */
    @Serializable
    @SerialName("result_block")
    data class ResultBlockEvent(
        @SerialName("run_id") val runId: String,
        val block: ResultBlockDto,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 草稿已生成，等待用户确认 */
    @Serializable
    @SerialName("draft_created")
    data class DraftCreated(
        @SerialName("run_id") val runId: String,
        @SerialName("draft_id") val draftId: Long,
        @SerialName("draft_type") val draftType: String,
        val title: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 上下文已压缩提示 */
    @Serializable
    @SerialName("context_compacted")
    data class ContextCompacted(
        @SerialName("run_id") val runId: String,
        @SerialName("compacted_count") val compactedCount: Int,
        val summary: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 运行完成 */
    @Serializable
    @SerialName("run_completed")
    data class RunCompleted(
        @SerialName("run_id") val runId: String,
        @SerialName("final_answer") val finalAnswer: String? = null,
        val mode: String? = null,
        @SerialName("llm_status") val llmStatus: String? = null,
        @SerialName("plan_source") val planSource: String? = null,
        @SerialName("audit_id") val auditId: String? = null,
        @SerialName("trace_id") val traceId: String? = null,
        val observability: AgentObservabilityDto? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 运行被取消（用户点击停止） */
    @Serializable
    @SerialName("run_cancelled")
    data class RunCancelled(
        @SerialName("run_id") val runId: String,
        val reason: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()

    /** 错误事件 */
    @Serializable
    @SerialName("error")
    data class ErrorEvent(
        @SerialName("run_id") val runId: String? = null,
        val code: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
    ) : AgentStreamEvent()
}
