package com.zhihuiji.core.model.v2.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 会话级运行轨迹响应。字段与后端脱敏审计展示契约保持一致；原始 SQL、凭据和整行数据不属于此模型。
 */
@Serializable
data class AgentRunTraceDto(
    @SerialName("run_id") val runId: String,
    @SerialName("conversation_id") val conversationId: Long? = null,
    val status: String? = null,
    val mode: String? = null,
    @SerialName("llm_status") val llmStatus: String? = null,
    @SerialName("plan_source") val planSource: String? = null,
    @SerialName("tool_count") val toolCount: Int? = null,
    @SerialName("event_count") val eventCount: Int? = null,
    @SerialName("audit_write_dropped_count") val auditWriteDroppedCount: Int? = null,
    @SerialName("audit_write_failed_count") val auditWriteFailedCount: Int? = null,
    @SerialName("audit_lossy") val auditLossy: Boolean = false,
    @SerialName("emitted_event_count") val emittedEventCount: Int? = null,
    val warnings: List<String> = emptyList(),
    @SerialName("audit_id") val auditId: String? = null,
    @SerialName("trace_id") val traceId: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("completed_at") val completedAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
    val events: List<AgentTraceEventDto> = emptyList(),
)

@Serializable
data class AgentTraceEventDto(
    @SerialName("event_id") val eventId: String? = null,
    val seq: Int? = null,
    @SerialName("tool_sequence") val toolSequence: Int? = null,
    @SerialName("event_type") val eventType: String,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_name") val toolName: String? = null,
    @SerialName("tool_label") val toolLabel: String? = null,
    val content: String? = null,
    @SerialName("delta_source") val deltaSource: String? = null,
    @SerialName("input_summary") val inputSummary: String? = null,
    @SerialName("query_window") val queryWindow: JsonElement? = null,
    @SerialName("result_summary") val resultSummary: String? = null,
    @SerialName("returned_count") val returnedCount: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null,
    val limit: Int? = null,
    @SerialName("is_truncated") val isTruncated: Boolean? = null,
    val evidence: JsonElement? = null,
    @SerialName("safe_message") val safeMessage: String? = null,
    @SerialName("draft_id") val draftId: Long? = null,
    @SerialName("draft_type") val draftType: String? = null,
    val title: String? = null,
    /** 草稿状态（active/confirmed/cancelled 等），用于 draft_created 审计事件恢复 */
    val status: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
)

/**
 * 将实时 SSE 事件和历史审计事件收敛到同一条可折叠时间线。
 * 这里只处理后端明确下发的可见计划摘要，不展示隐藏推理链。
 */
object AgentRunTraceReducer {
    fun initial(
        runId: String,
        auditId: String? = null,
        traceId: String? = null,
        logRef: String? = null,
    ): RunTrace = RunTrace(
        runId = runId,
        auditId = auditId,
        traceId = traceId,
        logRef = logRef,
        auditState = RunTraceAuditState.LIVE,
    )

    fun reduce(trace: RunTrace, event: AgentStreamEvent): RunTrace {
        val eventKey = event.eventKey()
        if (eventKey != null && eventKey in trace.seenEventKeys) return trace
        val marked = if (eventKey == null) trace else trace.copy(
            seenEventKeys = (trace.seenEventKeys + eventKey).takeLast(MAX_SEEN_EVENT_KEYS),
        )
        return when (event) {
            is AgentStreamEvent.RunStarted -> marked.copy(
                runId = event.runId,
                auditId = event.auditId ?: marked.auditId,
                traceId = event.traceId ?: marked.traceId,
                logRef = event.observability?.logRef ?: marked.logRef,
                auditState = RunTraceAuditState.LIVE,
            )

            is AgentStreamEvent.SafetyCheckStarted -> marked.upsertSafety(
                status = SafetyTraceStatus.CHECKING,
                result = null,
                message = null,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.SafetyCheckPassed -> marked.upsertSafety(
                status = SafetyTraceStatus.PASSED,
                result = SafetyResult(passed = true),
                message = null,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.SafetyCheckBlocked -> {
                val result = SafetyResult(
                    passed = false,
                    reason = event.reason,
                    suggestedAction = event.suggestedAction,
                )
                marked.copy(
                    safetyResult = result,
                    answerStatus = AnswerTraceStatus.BLOCKED,
                    timeline = marked.timeline.upsert(
                        RunTraceItem.Safety(
                            status = SafetyTraceStatus.BLOCKED,
                            result = result,
                            message = event.reason,
                            timestamp = event.timestamp,
                        )
                    ),
                )
            }

            is AgentStreamEvent.PlanDelta -> marked.copy(
                planSource = event.planSource ?: marked.planSource,
                planSteps = marked.planSteps.appendDistinctPlan(event.content, event.timestamp),
                timeline = marked.timeline.appendDistinct(
                    RunTraceItem.PlanSummary(
                        id = "plan:${event.timestamp}:${event.content.hashCode()}",
                        content = event.content,
                        planSource = event.planSource,
                        timestamp = event.timestamp,
                    )
                ),
            )

            is AgentStreamEvent.ToolStarted -> {
                val record = ToolCallRecord(
                    toolName = event.toolName,
                    eventId = event.eventId,
                    seq = event.seq,
                    conversationId = event.conversationId,
                    toolCallId = event.toolCallId,
                    auditId = event.auditId,
                    traceId = event.traceId,
                    status = ToolCallStatus.RUNNING,
                    inputSummary = event.inputSummary,
                    queryWindow = event.queryWindow,
                    startedAt = event.startedAt ?: event.timestamp,
                    timestamp = event.timestamp,
                )
                marked.upsertTool(record)
            }

            is AgentStreamEvent.ToolProgress -> {
                val existing = marked.toolCalls.lastOrNull { it.toolName == event.toolName }
                if (existing == null) {
                    marked.upsertTool(
                        ToolCallRecord(
                            toolName = event.toolName,
                            status = ToolCallStatus.RUNNING,
                            resultSummary = event.message,
                            progressMessage = event.message,
                            timestamp = event.timestamp,
                        )
                    )
                } else {
                    marked.upsertTool(existing.copy(
                        status = ToolCallStatus.RUNNING,
                        resultSummary = event.message,
                        progressMessage = event.message,
                        timestamp = event.timestamp,
                    ))
                }
            }

            is AgentStreamEvent.ToolCompleted -> {
                val existing = marked.toolRecordForEvent(
                    toolName = event.toolName,
                    toolCallId = event.toolCallId,
                    eventId = event.eventId,
                    seq = event.seq,
                )
                marked.upsertTool(
                    existing?.copy(
                        eventId = event.eventId ?: existing.eventId,
                        seq = event.seq ?: existing.seq,
                        conversationId = event.conversationId ?: existing.conversationId,
                        toolCallId = event.toolCallId ?: existing.toolCallId,
                        auditId = event.auditId ?: existing.auditId,
                        traceId = event.traceId ?: existing.traceId,
                        status = ToolCallStatus.COMPLETED,
                        inputSummary = event.inputSummary ?: existing.inputSummary,
                        queryWindow = event.queryWindow ?: existing.queryWindow,
                        resultSummary = event.resultSummary ?: existing.resultSummary,
                        startedAt = event.startedAt ?: existing.startedAt,
                        completedAt = event.completedAt ?: event.timestamp,
                        durationMs = event.durationMs ?: existing.durationMs,
                        returnedCount = event.returnedCount ?: existing.returnedCount,
                        totalCount = event.totalCount ?: existing.totalCount,
                        limit = event.limit ?: existing.limit,
                        isTruncated = event.isTruncated ?: existing.isTruncated,
                        evidence = event.evidence ?: existing.evidence,
                        nextCursor = event.nextCursor ?: existing.nextCursor,
                        timestamp = event.timestamp,
                    ) ?: ToolCallRecord(
                        toolName = event.toolName,
                        eventId = event.eventId,
                        seq = event.seq,
                        conversationId = event.conversationId,
                        toolCallId = event.toolCallId,
                        auditId = event.auditId,
                        traceId = event.traceId,
                        status = ToolCallStatus.COMPLETED,
                        inputSummary = event.inputSummary,
                        queryWindow = event.queryWindow,
                        resultSummary = event.resultSummary,
                        startedAt = event.startedAt,
                        completedAt = event.completedAt ?: event.timestamp,
                        durationMs = event.durationMs,
                        returnedCount = event.returnedCount,
                        totalCount = event.totalCount,
                        limit = event.limit,
                        isTruncated = event.isTruncated,
                        evidence = event.evidence,
                        nextCursor = event.nextCursor,
                        timestamp = event.timestamp,
                    )
                )
            }

            is AgentStreamEvent.ToolFailed -> {
                val existing = marked.toolRecordForEvent(
                    toolName = event.toolName,
                    toolCallId = event.toolCallId,
                    eventId = event.eventId,
                    seq = event.seq,
                )
                marked.upsertTool(
                    existing?.copy(
                        eventId = event.eventId ?: existing.eventId,
                        seq = event.seq ?: existing.seq,
                        conversationId = event.conversationId ?: existing.conversationId,
                        toolCallId = event.toolCallId ?: existing.toolCallId,
                        auditId = event.auditId ?: existing.auditId,
                        traceId = event.traceId ?: existing.traceId,
                        status = ToolCallStatus.FAILED,
                        inputSummary = event.inputSummary ?: existing.inputSummary,
                        queryWindow = event.queryWindow ?: existing.queryWindow,
                        resultSummary = event.errorSummary ?: event.safeMessage ?: existing.resultSummary ?: "工具查询失败",
                        startedAt = event.startedAt ?: existing.startedAt,
                        completedAt = event.completedAt ?: event.timestamp,
                        durationMs = event.durationMs ?: existing.durationMs,
                        evidence = event.evidence ?: existing.evidence,
                        nextCursor = event.nextCursor ?: existing.nextCursor,
                        timestamp = event.timestamp,
                    ) ?: ToolCallRecord(
                        toolName = event.toolName,
                        eventId = event.eventId,
                        seq = event.seq,
                        conversationId = event.conversationId,
                        toolCallId = event.toolCallId,
                        auditId = event.auditId,
                        traceId = event.traceId,
                        status = ToolCallStatus.FAILED,
                        inputSummary = event.inputSummary,
                        queryWindow = event.queryWindow,
                        resultSummary = event.errorSummary ?: event.safeMessage ?: "工具查询失败",
                        startedAt = event.startedAt,
                        completedAt = event.completedAt ?: event.timestamp,
                        durationMs = event.durationMs,
                        evidence = event.evidence,
                        nextCursor = event.nextCursor,
                        timestamp = event.timestamp,
                    )
                )
            }

            is AgentStreamEvent.AnswerDelta -> marked.copy(
                answerStatus = AnswerTraceStatus.STREAMING,
                answerDeltaSource = event.deltaSource ?: marked.answerDeltaSource,
                timeline = marked.timeline.upsert(
                    RunTraceItem.Answer(
                        status = AnswerTraceStatus.STREAMING,
                        deltaSource = event.deltaSource ?: marked.answerDeltaSource,
                        timestamp = event.timestamp,
                    )
                ),
            )

            is AgentStreamEvent.AnswerCompleted -> marked.copy(
                mode = event.mode ?: marked.mode,
                llmStatus = event.llmStatus ?: marked.llmStatus,
                planSource = event.planSource ?: marked.planSource,
                auditId = event.auditId ?: marked.auditId,
                traceId = event.traceId ?: marked.traceId,
                answerStatus = AnswerTraceStatus.COMPLETED,
                timeline = marked.timeline.upsert(
                    RunTraceItem.Answer(
                        status = AnswerTraceStatus.COMPLETED,
                        deltaSource = marked.answerDeltaSource,
                        timestamp = event.timestamp,
                    )
                ),
            )

            is AgentStreamEvent.ResultBlockEvent -> marked.copy(
                timeline = marked.timeline.appendDistinct(
                    RunTraceItem.ResultBlock(
                        block = event.block,
                        id = "result:${event.block.blockType}:${event.block.title}:${event.block.data}",
                        timestamp = event.timestamp,
                    )
                ),
            )

            is AgentStreamEvent.DraftCreated -> {
                val draft = DraftTrace(
                    draftId = event.draftId,
                    draftType = event.draftType,
                    title = event.title,
                    status = event.status,
                    timestamp = event.timestamp,
                )
                marked.copy(
                    draft = draft,
                    timeline = marked.timeline.upsert(RunTraceItem.Draft(draft = draft, timestamp = event.timestamp)),
                )
            }

            is AgentStreamEvent.ContextCompacted -> marked

            is AgentStreamEvent.RunCompleted -> {
                // 终态以 terminal_status 为准：COMPLETED / CONFIRMATION_PENDING / BLOCKED
                val status = event.terminalStatus.toTerminalStatusOrDefault(RunTerminalStatus.COMPLETED)
                val answerStatus = when (status) {
                    RunTerminalStatus.BLOCKED -> AnswerTraceStatus.BLOCKED
                    RunTerminalStatus.CONFIRMATION_PENDING -> AnswerTraceStatus.COMPLETED
                    else -> marked.answerStatus
                }
                val terminal = TerminalTrace(
                    status = status,
                    message = event.safeMessage ?: event.finalAnswer,
                    timestamp = event.timestamp,
                )
                marked.copy(
                    mode = event.mode ?: marked.mode,
                    llmStatus = event.llmStatus ?: marked.llmStatus,
                    planSource = event.planSource ?: marked.planSource,
                    auditId = event.auditId ?: marked.auditId,
                    traceId = event.traceId ?: marked.traceId,
                    answerStatus = answerStatus,
                    toolCalls = marked.toolCalls.closeOpenTools("运行已结束，未收到工具完成事件", event.timestamp),
                    terminal = terminal,
                    timeline = marked.timeline
                        .replaceTools(marked.toolCalls.closeOpenTools("运行已结束，未收到工具完成事件", event.timestamp))
                        .upsert(RunTraceItem.Terminal(terminal)),
                )
            }

            is AgentStreamEvent.RunFailed -> marked.withTerminal(
                status = RunTerminalStatus.FAILED,
                answerStatus = AnswerTraceStatus.FAILED,
                message = event.safeMessage ?: event.errorCode,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.RunBlocked -> marked.withTerminal(
                status = RunTerminalStatus.BLOCKED,
                answerStatus = AnswerTraceStatus.BLOCKED,
                message = event.safeMessage,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.RunExhausted -> marked.withTerminal(
                status = RunTerminalStatus.EXHAUSTED,
                answerStatus = AnswerTraceStatus.FAILED,
                message = event.safeMessage,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.RunCancelled -> marked.withTerminal(
                status = RunTerminalStatus.CANCELLED,
                answerStatus = AnswerTraceStatus.CANCELLED,
                message = event.reason ?: event.safeMessage,
                timestamp = event.timestamp,
            )

            is AgentStreamEvent.ErrorEvent -> marked.withTerminal(
                status = if (event.code == "STREAM_PARSE_ERROR") RunTerminalStatus.INTERRUPTED else RunTerminalStatus.FAILED,
                answerStatus = if (event.code == "STREAM_PARSE_ERROR") AnswerTraceStatus.INTERRUPTED else AnswerTraceStatus.FAILED,
                message = event.message,
                timestamp = event.timestamp,
            )
        }
    }

    fun reduceAudit(trace: AgentRunTraceDto): RunTrace {
        var result = RunTrace(
            runId = trace.runId,
            auditId = trace.auditId,
            traceId = trace.traceId,
            mode = trace.mode ?: trace.status?.takeIf { it.equals("blocked", ignoreCase = true) },
            llmStatus = trace.llmStatus,
            planSource = trace.planSource,
            auditState = if (trace.auditLossy) RunTraceAuditState.LOSSY else RunTraceAuditState.RESTORED,
            auditWarnings = trace.warnings,
        )
        for (event in trace.events.sortedWith(compareBy<AgentTraceEventDto>({ it.seq ?: Int.MAX_VALUE }, { it.createdAt ?: Long.MAX_VALUE }, { it.eventId ?: "" }))) {
            result = reduceAuditEvent(
                trace = result,
                event = event,
                conversationId = trace.conversationId,
            )
        }
        if (trace.auditLossy) {
            result = result.copy(
                auditState = RunTraceAuditState.LOSSY,
                auditWarnings = (result.auditWarnings + trace.lossyWarning()).distinct(),
                timeline = result.timeline.upsert(
                    RunTraceItem.AuditLossy(
                        message = trace.lossyWarning(),
                        droppedCount = trace.auditWriteDroppedCount,
                        failedCount = trace.auditWriteFailedCount,
                        timestamp = trace.updatedAt ?: trace.completedAt ?: trace.startedAt ?: 0L,
                    )
                ),
            )
        }
        if (trace.status != null) {
            val expectedStatus = trace.status.toTerminalStatus()
            if (result.terminal?.status != expectedStatus) {
                result = result.withTerminal(
                    status = expectedStatus,
                    answerStatus = trace.status.toAnswerStatus(),
                    message = result.terminal?.message ?: trace.errorMessage,
                    timestamp = result.terminal?.timestamp
                        ?: trace.completedAt ?: trace.updatedAt ?: trace.startedAt ?: 0L,
                )
            }
        }
        return result
    }

    fun missing(runId: String, reason: String = "运行轨迹暂不可用"): RunTrace = RunTrace(
        runId = runId,
        auditState = RunTraceAuditState.MISSING,
        auditWarnings = listOf(reason),
        timeline = listOf(
            RunTraceItem.AuditLossy(
                message = reason,
                timestamp = 0L,
            )
        ),
    )

    fun interrupted(trace: RunTrace, message: String): RunTrace = trace.withTerminal(
        status = RunTerminalStatus.INTERRUPTED,
        answerStatus = AnswerTraceStatus.INTERRUPTED,
        message = message,
        timestamp = System.currentTimeMillis(),
    )

    fun cancelled(trace: RunTrace, message: String): RunTrace = trace.withTerminal(
        status = RunTerminalStatus.CANCELLED,
        answerStatus = AnswerTraceStatus.CANCELLED,
        message = message,
        timestamp = System.currentTimeMillis(),
    )

    private fun reduceAuditEvent(
        trace: RunTrace,
        event: AgentTraceEventDto,
        conversationId: Long?,
    ): RunTrace = event.toStreamEvent(
        runId = trace.runId,
        conversationId = conversationId,
        auditId = trace.auditId,
        traceId = trace.traceId,
        mode = trace.mode,
        llmStatus = trace.llmStatus,
        planSource = trace.planSource,
    )?.let { reduce(trace, it) } ?: trace

    private fun AgentTraceEventDto.toStreamEvent(
        runId: String,
        conversationId: Long?,
        auditId: String?,
        traceId: String?,
        mode: String?,
        llmStatus: String?,
        planSource: String?,
    ): AgentStreamEvent? {
        val timestamp = createdAt ?: 0L
        return when (eventType) {
            "safety_check_started" -> AgentStreamEvent.SafetyCheckStarted(
                runId = runId,
                timestamp = timestamp,
            )
            "safety_check_passed" -> AgentStreamEvent.SafetyCheckPassed(
                runId = runId,
                timestamp = timestamp,
            )
            "safety_check_blocked" -> AgentStreamEvent.SafetyCheckBlocked(
                runId = runId,
                reason = safeMessage ?: content ?: "请求被安全策略拦截",
                timestamp = timestamp,
            )
            "plan_delta" -> content?.let {
                AgentStreamEvent.PlanDelta(
                    runId = runId,
                    planSource = planSource,
                    content = it,
                    timestamp = timestamp,
                )
            }
            "tool_started" -> toolName?.let {
                AgentStreamEvent.ToolStarted(
                    eventId = eventId,
                    seq = seq,
                    toolSequence = toolSequence,
                    runId = runId,
                    conversationId = conversationId,
                    toolCallId = toolCallId,
                    toolName = it,
                    inputSummary = inputSummary,
                    queryWindow = queryWindow,
                    toolInput = null,
                    startedAt = timestamp,
                    auditId = auditId,
                    traceId = traceId,
                    timestamp = timestamp,
                )
            }
            "tool_progress" -> toolName?.let { name ->
                content?.let { message ->
                    AgentStreamEvent.ToolProgress(
                        runId = runId,
                        toolName = name,
                        message = message,
                        timestamp = timestamp,
                    )
                }
            }
            "tool_completed" -> toolName?.let {
                AgentStreamEvent.ToolCompleted(
                    eventId = eventId,
                    seq = seq,
                    toolSequence = toolSequence,
                    runId = runId,
                    conversationId = conversationId,
                    toolCallId = toolCallId,
                    toolName = it,
                    resultSummary = resultSummary,
                    inputSummary = inputSummary,
                    queryWindow = queryWindow,
                    startedAt = null,
                    completedAt = timestamp,
                    durationMs = null,
                    returnedCount = returnedCount,
                    totalCount = totalCount,
                    limit = limit,
                    isTruncated = isTruncated,
                    evidence = evidence,
                    nextCursor = null,
                    auditId = auditId,
                    traceId = traceId,
                    timestamp = timestamp,
                )
            }
            "tool_failed" -> toolName?.let {
                AgentStreamEvent.ToolFailed(
                    eventId = eventId,
                    seq = seq,
                    toolSequence = toolSequence,
                    runId = runId,
                    conversationId = conversationId,
                    toolCallId = toolCallId,
                    toolName = it,
                    inputSummary = inputSummary,
                    queryWindow = queryWindow,
                    errorCode = null,
                    safeMessage = safeMessage,
                    errorSummary = safeMessage,
                    startedAt = null,
                    completedAt = timestamp,
                    durationMs = null,
                    evidence = evidence,
                    nextCursor = null,
                    auditId = auditId,
                    traceId = traceId,
                    timestamp = timestamp,
                )
            }
            "answer_delta" -> content?.let {
                AgentStreamEvent.AnswerDelta(
                    eventId = eventId,
                    seq = seq,
                    runId = runId,
                    conversationId = conversationId,
                    delta = it,
                    deltaSource = deltaSource,
                    auditId = auditId,
                    traceId = traceId,
                    observability = null,
                    timestamp = timestamp,
                )
            }
            "answer_completed" -> content?.let {
                AgentStreamEvent.AnswerCompleted(
                    runId = runId,
                    answer = it,
                    mode = mode,
                    llmStatus = llmStatus,
                    planSource = planSource,
                    auditId = auditId,
                    traceId = traceId,
                    observability = null,
                    timestamp = timestamp,
                )
            }
            "draft_created" -> if (draftId != null && draftType != null && title != null) {
                AgentStreamEvent.DraftCreated(
                    runId = runId,
                    draftId = draftId,
                    draftType = draftType,
                    title = title,
                    status = status,
                    timestamp = timestamp,
                )
            } else {
                null
            }
            "run_completed" -> AgentStreamEvent.RunCompleted(
                runId = runId,
                terminalStatus = null,
                finalAnswer = content,
                safeMessage = safeMessage,
                mode = mode,
                llmStatus = llmStatus,
                planSource = planSource,
                auditId = auditId,
                traceId = traceId,
                observability = null,
                timestamp = timestamp,
            )
            "run_failed" -> AgentStreamEvent.RunFailed(
                runId = runId,
                terminalStatus = "FAILED",
                safeMessage = safeMessage ?: content,
                auditId = auditId,
                traceId = traceId,
                timestamp = timestamp,
            )
            "run_blocked" -> AgentStreamEvent.RunBlocked(
                runId = runId,
                terminalStatus = "BLOCKED",
                safeMessage = safeMessage ?: content,
                auditId = auditId,
                traceId = traceId,
                timestamp = timestamp,
            )
            "run_exhausted" -> AgentStreamEvent.RunExhausted(
                runId = runId,
                terminalStatus = "EXHAUSTED",
                safeMessage = safeMessage ?: content,
                auditId = auditId,
                traceId = traceId,
                timestamp = timestamp,
            )
            "run_cancelled" -> AgentStreamEvent.RunCancelled(
                runId = runId,
                terminalStatus = "CANCELLED",
                reason = safeMessage ?: content,
                auditId = auditId,
                traceId = traceId,
                timestamp = timestamp,
            )
            else -> null
        }
    }

    private fun RunTrace.upsertSafety(
        status: SafetyTraceStatus,
        result: SafetyResult?,
        message: String?,
        timestamp: Long,
    ): RunTrace = copy(
        safetyResult = result ?: safetyResult,
        timeline = timeline.upsert(
            RunTraceItem.Safety(
                status = status,
                result = result,
                message = message,
                timestamp = timestamp,
            )
        ),
    )

    private fun RunTrace.upsertTool(record: ToolCallRecord): RunTrace {
        val key = record.stableKey()
        val updatedCalls = toolCalls.upsertRecord(record)
        return copy(
            toolCalls = updatedCalls,
            timeline = timeline.upsert(
                RunTraceItem.Tool(
                    call = updatedCalls.first { it.stableKey() == key },
                    id = "tool:$key",
                )
            ),
        )
    }

    private fun RunTrace.toolRecordForEvent(
        toolName: String,
        toolCallId: String?,
        eventId: String?,
        seq: Int?,
    ): ToolCallRecord? = toolCalls.findLast { record ->
        (!toolCallId.isNullOrBlank() && record.toolCallId == toolCallId) ||
            (!eventId.isNullOrBlank() && record.eventId == eventId) ||
            (seq != null && record.seq == seq) ||
            (toolCallId.isNullOrBlank() && eventId.isNullOrBlank() && seq == null && record.toolName == toolName)
    }

    private fun RunTrace.withTerminal(
        status: RunTerminalStatus,
        answerStatus: AnswerTraceStatus,
        message: String?,
        timestamp: Long,
    ): RunTrace {
        val terminal = TerminalTrace(status = status, message = message, timestamp = timestamp)
        val closedTools = toolCalls.closeOpenTools(
            resultSummary = message ?: "运行已结束，工具状态未确认",
            completedAt = timestamp,
        )
        return copy(
            answerStatus = answerStatus,
            toolCalls = closedTools,
            terminal = terminal,
            timeline = timeline.replaceTools(closedTools).upsert(RunTraceItem.Terminal(terminal)),
        )
    }

    private fun AgentRunTraceDto.lossyWarning(): String = when {
        auditWriteDroppedCount != null && auditWriteFailedCount != null ->
            "运行轨迹不完整：丢弃 ${auditWriteDroppedCount} 条，写入失败 ${auditWriteFailedCount} 条"
        auditWriteDroppedCount != null -> "运行轨迹不完整：丢弃 ${auditWriteDroppedCount} 条事件"
        auditWriteFailedCount != null -> "运行轨迹不完整：${auditWriteFailedCount} 条事件写入失败"
        else -> "运行轨迹不完整，部分过程事件可能缺失"
    }

    private fun String.toTerminalStatus(): RunTerminalStatus = when (uppercase()) {
        "COMPLETED", "SUCCESS", "SUCCEEDED" -> RunTerminalStatus.COMPLETED
        "CONFIRMATION_PENDING" -> RunTerminalStatus.CONFIRMATION_PENDING
        "BLOCKED" -> RunTerminalStatus.BLOCKED
        "CANCELLED", "CANCELED" -> RunTerminalStatus.CANCELLED
        "FAILED", "ERROR" -> RunTerminalStatus.FAILED
        "EXHAUSTED" -> RunTerminalStatus.EXHAUSTED
        else -> RunTerminalStatus.INTERRUPTED
    }

    private fun String?.toTerminalStatusOrDefault(fallback: RunTerminalStatus): RunTerminalStatus =
        if (isNullOrBlank()) fallback else toTerminalStatus()

    private fun String.toAnswerStatus(): AnswerTraceStatus = when (toTerminalStatus()) {
        RunTerminalStatus.COMPLETED -> AnswerTraceStatus.COMPLETED
        RunTerminalStatus.CONFIRMATION_PENDING -> AnswerTraceStatus.COMPLETED
        RunTerminalStatus.BLOCKED -> AnswerTraceStatus.BLOCKED
        RunTerminalStatus.CANCELLED -> AnswerTraceStatus.CANCELLED
        RunTerminalStatus.FAILED -> AnswerTraceStatus.FAILED
        RunTerminalStatus.EXHAUSTED -> AnswerTraceStatus.FAILED
        RunTerminalStatus.INTERRUPTED -> AnswerTraceStatus.INTERRUPTED
    }

    private fun AgentStreamEvent.eventKey(): String? = when (this) {
        is AgentStreamEvent.ToolStarted -> eventId ?: seq?.let { "tool_started:$it" }
        is AgentStreamEvent.ToolProgress -> null
        is AgentStreamEvent.ToolCompleted -> eventId ?: seq?.let { "tool_completed:$it" }
        is AgentStreamEvent.ToolFailed -> eventId ?: seq?.let { "tool_failed:$it" }
        is AgentStreamEvent.AnswerDelta -> eventId ?: seq?.let { "answer_delta:$it" }
        // 终态事件去重：每个 run 只接受一次终态，后续重复/乱序终态事件直接忽略
        is AgentStreamEvent.RunCompleted,
        is AgentStreamEvent.RunFailed,
        is AgentStreamEvent.RunBlocked,
        is AgentStreamEvent.RunExhausted,
        is AgentStreamEvent.RunCancelled -> "terminal"
        else -> null
    }

    private fun List<PlanStep>.appendDistinctPlan(content: String, timestamp: Long): List<PlanStep> =
        if (any { it.content == content && it.timestamp == timestamp }) this else this + PlanStep(content, timestamp)

    private fun List<ToolCallRecord>.upsertRecord(record: ToolCallRecord): List<ToolCallRecord> {
        val index = indexOfLast { existing ->
                (!record.toolCallId.isNullOrBlank() && existing.toolCallId == record.toolCallId) ||
                (!record.eventId.isNullOrBlank() && existing.eventId == record.eventId) ||
                (record.seq != null && existing.seq == record.seq) ||
                (record.toolCallId.isNullOrBlank() && record.eventId.isNullOrBlank() && record.seq == null && existing.toolName == record.toolName && existing.status == ToolCallStatus.RUNNING)
        }
        if (index < 0) return this + record
        val existing = this[index]
        val merged = record.copy(
            eventId = record.eventId ?: existing.eventId,
            seq = record.seq ?: existing.seq,
            conversationId = record.conversationId ?: existing.conversationId,
            toolCallId = record.toolCallId ?: existing.toolCallId,
            auditId = record.auditId ?: existing.auditId,
            traceId = record.traceId ?: existing.traceId,
            inputSummary = record.inputSummary ?: existing.inputSummary,
            queryWindow = record.queryWindow ?: existing.queryWindow,
            resultSummary = record.resultSummary ?: existing.resultSummary,
            progressMessage = record.progressMessage ?: existing.progressMessage,
            startedAt = record.startedAt ?: existing.startedAt,
            completedAt = record.completedAt ?: existing.completedAt,
            durationMs = record.durationMs ?: existing.durationMs,
            returnedCount = record.returnedCount ?: existing.returnedCount,
            totalCount = record.totalCount ?: existing.totalCount,
            limit = record.limit ?: existing.limit,
            isTruncated = record.isTruncated ?: existing.isTruncated,
            evidence = record.evidence ?: existing.evidence,
            nextCursor = record.nextCursor ?: existing.nextCursor,
        )
        if (merged == existing) return this
        return toMutableList().also { it[index] = merged }
    }

    private fun ToolCallRecord.stableKey(): String =
        toolCallId?.takeIf { it.isNotBlank() } ?: eventId?.takeIf { it.isNotBlank() } ?:
            seq?.let { "seq:$it" } ?: "${toolName}:${startedAt ?: timestamp}"

    private fun List<ToolCallRecord>.closeOpenTools(
        resultSummary: String,
        completedAt: Long,
    ): List<ToolCallRecord> = map { record ->
        if (record.status == ToolCallStatus.RUNNING || record.status == ToolCallStatus.PENDING) {
            record.copy(
                status = ToolCallStatus.FAILED,
                resultSummary = record.resultSummary ?: resultSummary,
                completedAt = record.completedAt ?: completedAt,
                timestamp = completedAt,
            )
        } else {
            record
        }
    }

    private fun List<RunTraceItem>.upsert(item: RunTraceItem): List<RunTraceItem> {
        val index = indexOfFirst { it.id == item.id }
        if (index < 0) return appendInStableOrder(item)
        return toMutableList().also { it[index] = item }
    }

    private fun List<RunTraceItem>.appendDistinct(item: RunTraceItem): List<RunTraceItem> =
        if (any { it.id == item.id }) this else appendInStableOrder(item)

    private fun List<RunTraceItem>.replaceTools(calls: List<ToolCallRecord>): List<RunTraceItem> {
        val result = ArrayList<RunTraceItem>(size + calls.size)
        val seen = calls.map { it.stableKey() }.toSet()
        for (item in this) {
            if (item !is RunTraceItem.Tool || item.call.stableKey() !in seen) result += item
        }
        for (call in calls) {
            result.add(RunTraceItem.Tool(call = call, id = "tool:${call.stableKey()}"))
        }
        return result.sortedWith(compareBy<RunTraceItem>({ it.seq ?: Int.MAX_VALUE }, { it.timestamp }, { it.id }))
    }

    private fun List<RunTraceItem>.appendInStableOrder(item: RunTraceItem): List<RunTraceItem> =
        (this + item).sortedWith(compareBy<RunTraceItem>({ it.seq ?: Int.MAX_VALUE }, { it.timestamp }, { it.id }))

    private const val MAX_SEEN_EVENT_KEYS = 512
}

private val AgentStreamEvent.eventIdOrSeq: String?
    get() = when (this) {
        is AgentStreamEvent.ToolStarted -> eventId ?: seq?.toString()
        is AgentStreamEvent.ToolCompleted -> eventId ?: seq?.toString()
        is AgentStreamEvent.ToolFailed -> eventId ?: seq?.toString()
        is AgentStreamEvent.AnswerDelta -> eventId ?: seq?.toString()
        else -> null
    }
