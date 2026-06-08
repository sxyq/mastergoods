package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentStreamEvent
import com.zhihuiji.core.model.v2.agent.ChatMessage
import com.zhihuiji.core.model.v2.agent.ChatMessagePart
import com.zhihuiji.core.model.v2.agent.MessageRole
import com.zhihuiji.core.model.v2.agent.PlanStep
import com.zhihuiji.core.model.v2.agent.ResultBlockDto
import com.zhihuiji.core.model.v2.agent.RunTrace
import com.zhihuiji.core.model.v2.agent.SafetyResult
import com.zhihuiji.core.model.v2.agent.AgentAuditRecord
import com.zhihuiji.core.model.v2.agent.DraftAuditInfo
import com.zhihuiji.core.model.v2.agent.ErrorAuditInfo
import com.zhihuiji.core.model.v2.agent.SafetyAuditResult
import com.zhihuiji.core.model.v2.agent.ToolAuditRecord
import com.zhihuiji.core.model.v2.agent.ToolCallRecord
import com.zhihuiji.core.model.v2.agent.ToolCallStatus
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.data.agent.AgentAuditRepository
import com.zhihuiji.data.agent.AgentV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

private const val LOCAL_STREAM_STOP_MESSAGE = "已停止本机接收，正在请求服务端取消"
private const val SERVER_CANCEL_CONFIRMED_MESSAGE = "服务端已确认取消生成"

/**
 * 聊天页 UI 状态
 */
data class AgentChatUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val conversationId: Long? = null,
    val currentRunId: String? = null,
    val inputText: String = "",
    val canStop: Boolean = false,
    val showDraftConfirm: DraftConfirmState? = null,
    val contextCompacted: ContextCompactedState? = null,
)

/**
 * 草稿确认弹窗状态
 */
data class DraftConfirmState(
    val draftId: Long,
    val draftType: String,
    val title: String,
)

/**
 * 上下文压缩提示状态
 */
data class ContextCompactedState(
    val compactedCount: Int,
    val summary: String,
)

@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val repository: AgentV2Repository,
    private val auditRepository: AgentAuditRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    private var chatJob: Job? = null
    private var answerDeltaFlushJob: Job? = null
    private var pendingAnswerDeltaMessageId: String? = null
    private val pendingAnswerDelta = StringBuilder()
    private var pendingAnswerDeltaSource: String? = null

    /** 当前运行的审计记录构建器 */
    private var currentAuditBuilder: AuditRecordBuilder? = null

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun loadConversation(conversationId: Long?) {
        if (conversationId == null || conversationId <= 0 || _uiState.value.conversationId == conversationId) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, conversationId = conversationId) }
            repository.listMessages(conversationId)
                .onSuccess { messages ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages.map { dto -> dto.toChatMessage() },
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * 发送消息（流式 SSE 版本）
     */
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.conversationId ?: 0L,
            role = MessageRole.USER,
            content = text,
            createdAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isStreaming = true,
                canStop = true,
                error = null,
                showDraftConfirm = null,
                contextCompacted = null,
            )
        }

        val assistantMessageId = UUID.randomUUID().toString()
        val request = AgentChatRequest(
            conversationId = _uiState.value.conversationId,
            message = text,
            stream = true,
        )

        // 初始化审计记录构建器
        currentAuditBuilder = AuditRecordBuilder(userMessage = text)
        clearPendingAnswerDelta()

        chatJob = viewModelScope.launch {
            val streamingMessage = ChatMessage(
                id = assistantMessageId,
                conversationId = _uiState.value.conversationId ?: 0L,
                role = MessageRole.ASSISTANT,
                content = "",
                isStreaming = true,
                animateReveal = false,
                createdAt = System.currentTimeMillis(),
            )
            _uiState.update { it.copy(messages = it.messages + streamingMessage) }

            repository.chatStream(request)
                .catch { e ->
                    handleStreamError(assistantMessageId, e.message ?: "流式连接失败")
                }
                .collect { event ->
                    handleStreamEvent(assistantMessageId, event)
                }
        }
    }

    private fun handleStreamEvent(assistantMessageId: String, event: AgentStreamEvent) {
        when (event) {
            is AgentStreamEvent.RunStarted -> {
                _uiState.update {
                    it.copy(
                        currentRunId = event.runId,
                        conversationId = event.conversationId,
                    )
                }
                updateAssistantMessage(assistantMessageId) { msg ->
                    msg.copy(
                        conversationId = event.conversationId,
                        runTrace = RunTrace(
                            runId = event.runId,
                            auditId = event.auditId,
                            traceId = event.traceId,
                            logRef = event.observability?.logRef,
                            planSteps = emptyList(),
                            toolCalls = emptyList(),
                            safetyResult = null,
                            mode = null,
                            llmStatus = null,
                            planSource = null,
                            isExpanded = false,
                        ),
                    )
                }
                currentAuditBuilder?.runId = event.runId
                currentAuditBuilder?.conversationId = event.conversationId
            }

            is AgentStreamEvent.SafetyCheckStarted -> {
                // 权限/安全检查属于内部中间态，不向用户展示“审查中”提示。
            }

            is AgentStreamEvent.SafetyCheckPassed -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(safetyResult = SafetyResult(passed = true))
                }
                currentAuditBuilder?.safetyResult = SafetyAuditResult(passed = true)
            }

            is AgentStreamEvent.SafetyCheckBlocked -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        safetyResult = SafetyResult(
                            passed = false,
                            reason = event.reason,
                            suggestedAction = event.suggestedAction,
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        canStop = false,
                        error = "安全拦截: ${event.reason}",
                    )
                }
                currentAuditBuilder?.safetyResult = SafetyAuditResult(
                    passed = false,
                    reason = event.reason,
                    suggestedAction = event.suggestedAction,
                )
                saveAuditRecord()
            }

            is AgentStreamEvent.PlanDelta -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        planSource = event.planSource ?: trace.planSource,
                        planSteps = trace.planSteps + PlanStep(
                            content = event.content,
                            timestamp = event.timestamp,
                        )
                    )
                }
                currentAuditBuilder?.addToolCall(
                    ToolAuditRecord(
                        toolName = "plan",
                        status = "completed",
                        resultSummary = event.content,
                        timestamp = event.timestamp,
                    )
                )
            }

            is AgentStreamEvent.ToolStarted -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        toolCalls = trace.toolCalls + ToolCallRecord(
                            toolName = event.toolName,
                            eventId = event.eventId,
                            seq = event.seq,
                            conversationId = event.conversationId,
                            toolCallId = event.toolCallId,
                            auditId = event.auditId,
                            traceId = event.traceId,
                            status = ToolCallStatus.RUNNING,
                            inputSummary = event.inputSummary ?: event.toolInput?.toString()?.take(160),
                            queryWindow = event.queryWindow,
                            startedAt = event.startedAt ?: event.timestamp,
                            timestamp = event.timestamp,
                        )
                    )
                }
                currentAuditBuilder?.addToolCall(
                    ToolAuditRecord(
                        toolCallId = event.toolCallId,
                        toolName = event.toolName,
                        status = "running",
                        inputSummary = event.inputSummary ?: event.toolInput?.toString()?.take(160),
                        queryWindow = event.queryWindow,
                        startedAt = event.startedAt ?: event.timestamp,
                        timestamp = event.timestamp,
                    )
                )
            }

            is AgentStreamEvent.ToolProgress -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        toolCalls = trace.toolCalls.updateToolCall(
                            toolName = event.toolName,
                            toolCallId = null,
                            eventId = null,
                            seq = null,
                            conversationId = null,
                            auditId = null,
                            traceId = null,
                            status = ToolCallStatus.RUNNING,
                            resultSummary = event.message,
                            timestamp = event.timestamp,
                        )
                    )
                }
                currentAuditBuilder?.updateToolCall(
                    toolName = event.toolName,
                    status = "running",
                    resultSummary = event.message,
                    timestamp = event.timestamp,
                )
            }

            is AgentStreamEvent.ToolCompleted -> {
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        toolCalls = trace.toolCalls.updateToolCall(
                            toolName = event.toolName,
                            toolCallId = event.toolCallId,
                            eventId = event.eventId,
                            seq = event.seq,
                            conversationId = event.conversationId,
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
                currentAuditBuilder?.updateToolCall(
                    toolName = event.toolName,
                    status = "completed",
                    toolCallId = event.toolCallId,
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
            }

            is AgentStreamEvent.ToolFailed -> {
                val errorSummary = event.errorSummary ?: event.safeMessage ?: "工具查询失败"
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        toolCalls = trace.toolCalls.updateToolCall(
                            toolName = event.toolName,
                            toolCallId = event.toolCallId,
                            eventId = event.eventId,
                            seq = event.seq,
                            conversationId = event.conversationId,
                            auditId = event.auditId,
                            traceId = event.traceId,
                            status = ToolCallStatus.FAILED,
                            inputSummary = event.inputSummary,
                            queryWindow = event.queryWindow,
                            resultSummary = errorSummary,
                            startedAt = event.startedAt,
                            completedAt = event.completedAt ?: event.timestamp,
                            durationMs = event.durationMs,
                            evidence = event.evidence,
                            nextCursor = event.nextCursor,
                            timestamp = event.timestamp,
                        )
                    )
                }
                currentAuditBuilder?.updateToolCall(
                    toolName = event.toolName,
                    status = "failed",
                    toolCallId = event.toolCallId,
                    inputSummary = event.inputSummary,
                    queryWindow = event.queryWindow,
                    resultSummary = errorSummary,
                    startedAt = event.startedAt,
                    completedAt = event.completedAt ?: event.timestamp,
                    durationMs = event.durationMs,
                    evidence = event.evidence,
                    nextCursor = event.nextCursor,
                    timestamp = event.timestamp,
                )
            }

            is AgentStreamEvent.AnswerDelta -> {
                enqueueAnswerDelta(
                    assistantMessageId = assistantMessageId,
                    delta = event.delta,
                    deltaSource = event.deltaSource,
                )
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(answerDeltaSource = event.deltaSource ?: trace.answerDeltaSource)
                }
            }

            is AgentStreamEvent.AnswerCompleted -> {
                flushPendingAnswerDelta()
                updateAssistantMessage(assistantMessageId) { msg ->
                    val finalContent = msg.content.withAuthoritativeAnswer(event.answer)
                    msg.copy(
                        content = finalContent,
                        parts = msg.parts.withAuthoritativeText(finalContent, msg.blocks),
                        animateReveal = false,
                    )
                }
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        mode = event.mode ?: trace.mode,
                        llmStatus = event.llmStatus ?: trace.llmStatus,
                        planSource = event.planSource ?: trace.planSource,
                        auditId = event.auditId ?: trace.auditId,
                        traceId = event.traceId ?: trace.traceId,
                        logRef = event.observability?.logRef ?: trace.logRef,
                    )
                }
            }

            is AgentStreamEvent.ResultBlockEvent -> {
                flushPendingAnswerDelta()
                updateAssistantMessage(assistantMessageId) { msg ->
                    val updatedBlocks = msg.blocks + event.block
                    msg.copy(
                        blocks = updatedBlocks,
                        parts = msg.parts.appendResultBlockAfterVisibleText(event.block),
                    )
                }
            }

            is AgentStreamEvent.DraftCreated -> {
                _uiState.update {
                    it.copy(
                        showDraftConfirm = DraftConfirmState(
                            draftId = event.draftId,
                            draftType = event.draftType,
                            title = event.title,
                        )
                    )
                }
                currentAuditBuilder?.draftInfo = DraftAuditInfo(
                    draftId = event.draftId,
                    draftType = event.draftType,
                    title = event.title,
                    userConfirmed = null,
                )
            }

            is AgentStreamEvent.ContextCompacted -> {
                _uiState.update {
                    it.copy(
                        contextCompacted = ContextCompactedState(
                            compactedCount = event.compactedCount,
                            summary = event.summary,
                        )
                    )
                }
                currentAuditBuilder?.contextCompacted = true
            }

            is AgentStreamEvent.RunCompleted -> {
                flushPendingAnswerDelta()
                val finalAnswer = event.finalAnswer
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        canStop = false,
                        currentRunId = null,
                    )
                }
                updateAssistantMessage(assistantMessageId) { msg ->
                    val finalContent = msg.content.withAuthoritativeAnswer(finalAnswer)
                    msg.copy(
                        content = finalContent,
                        parts = msg.parts.withAuthoritativeText(finalContent, msg.blocks),
                        isStreaming = false,
                        animateReveal = false,
                    )
                }
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        mode = event.mode ?: trace.mode,
                        llmStatus = event.llmStatus ?: trace.llmStatus,
                        planSource = event.planSource ?: trace.planSource,
                        auditId = event.auditId ?: trace.auditId,
                        traceId = event.traceId ?: trace.traceId,
                        logRef = event.observability?.logRef ?: trace.logRef,
                    )
                }
                currentAuditBuilder?.finalAnswerSummary = finalAnswer
                saveAuditRecord()
            }

            is AgentStreamEvent.RunCancelled -> {
                flushPendingAnswerDelta()
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        canStop = false,
                        currentRunId = null,
                    )
                }
                updateAssistantMessage(assistantMessageId) { msg ->
                    msg.copy(isStreaming = false, animateReveal = false)
                }
                currentAuditBuilder?.errorInfo = ErrorAuditInfo(
                    message = "用户取消生成",
                )
                saveAuditRecord()
            }

            is AgentStreamEvent.ErrorEvent -> {
                handleStreamError(assistantMessageId, "[${event.code}] ${event.message}")
            }
        }
    }

    private fun handleStreamError(assistantMessageId: String, errorMessage: String) {
        flushPendingAnswerDelta()
        _uiState.update {
            it.copy(
                isStreaming = false,
                canStop = false,
                currentRunId = null,
                error = errorMessage,
            )
        }
        updateAssistantMessage(assistantMessageId) { msg ->
            msg.copy(
                isStreaming = false,
                isError = true,
                errorMessage = errorMessage,
                animateReveal = false,
            )
        }
        currentAuditBuilder?.errorInfo = ErrorAuditInfo(
            message = errorMessage,
        )
        saveAuditRecord()
    }

    private fun saveAuditRecord() {
        val builder = currentAuditBuilder ?: return
        saveAuditRecord(builder)
    }

    private fun saveAuditRecord(builder: AuditRecordBuilder) {
        val record = builder.build()
        viewModelScope.launch {
            try {
                auditRepository.insertRecord(record)
            } catch (_: Exception) {
                // 审计记录保存失败不应影响主流程
            }
        }
        if (currentAuditBuilder === builder) {
            currentAuditBuilder = null
        }
    }

    private fun updateAssistantMessage(
        assistantMessageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ) {
        _uiState.update { state ->
            val index = state.messages.indexOfFirst { it.id == assistantMessageId }
            if (index == -1) {
                return@update state
            }
            val updated = transform(state.messages[index])
            if (updated == state.messages[index]) {
                return@update state
            }
            val updatedMessages = state.messages.toMutableList()
            updatedMessages[index] = updated
            state.copy(messages = updatedMessages)
        }
    }

    private fun enqueueAnswerDelta(
        assistantMessageId: String,
        delta: String,
        deltaSource: String?,
    ) {
        if (delta.isBlank()) return
        if (pendingAnswerDeltaMessageId != null && pendingAnswerDeltaMessageId != assistantMessageId) {
            flushPendingAnswerDelta()
        }
        if (pendingAnswerDelta.isNotEmpty() && pendingAnswerDeltaSource != null && deltaSource != pendingAnswerDeltaSource) {
            flushPendingAnswerDelta()
        }
        pendingAnswerDeltaMessageId = assistantMessageId
        pendingAnswerDelta.append(delta)
        pendingAnswerDeltaSource = deltaSource ?: pendingAnswerDeltaSource
        if (answerDeltaFlushJob?.isActive == true) return
        answerDeltaFlushJob = viewModelScope.launch {
            // UI 合帧节流：只合并服务端 answer_delta，不拆分完整回答伪造 token。
            delay(48)
            flushPendingAnswerDelta()
        }
    }

    private fun flushPendingAnswerDelta() {
        val assistantMessageId = pendingAnswerDeltaMessageId ?: return
        val delta = pendingAnswerDelta.toString()
        val deltaSource = pendingAnswerDeltaSource
        clearPendingAnswerDelta()
        if (delta.isBlank()) return
        updateAssistantMessage(assistantMessageId) { msg ->
            msg.copy(
                content = msg.content + delta,
                parts = msg.parts.appendStreamingText(delta, msg.blocks),
                animateReveal = false,
                hasServerAnswerDelta = true,
                answerDeltaSource = deltaSource ?: msg.answerDeltaSource,
            )
        }
    }

    private fun clearPendingAnswerDelta() {
        answerDeltaFlushJob?.cancel()
        answerDeltaFlushJob = null
        pendingAnswerDeltaMessageId = null
        pendingAnswerDelta.clear()
        pendingAnswerDeltaSource = null
    }

    private fun updateRunTrace(
        assistantMessageId: String,
        transform: (RunTrace) -> RunTrace,
    ) {
        updateAssistantMessage(assistantMessageId) { msg ->
            val currentTrace = msg.runTrace ?: RunTrace(
                runId = _uiState.value.currentRunId ?: "",
                planSteps = emptyList(),
                toolCalls = emptyList(),
                safetyResult = null,
                isExpanded = false,
            )
            msg.copy(runTrace = transform(currentTrace))
        }
    }

    fun toggleRunTrace(messageId: String) {
        updateAssistantMessage(messageId) { msg ->
            val trace = msg.runTrace ?: return@updateAssistantMessage msg
            msg.copy(runTrace = trace.copy(isExpanded = !trace.isExpanded))
        }
    }

    fun stopGeneration() {
        flushPendingAnswerDelta()
        val runId = _uiState.value.currentRunId
        val cancellingAuditBuilder = currentAuditBuilder
        if (!runId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.cancelRun(runId)
                    .onSuccess { response ->
                        val message = if (response.cancelled) {
                            SERVER_CANCEL_CONFIRMED_MESSAGE
                        } else {
                            "已停止本机接收，服务端取消未确认：${response.status}"
                        }
                        cancellingAuditBuilder?.errorInfo = ErrorAuditInfo(message = message)
                        _uiState.update { it.copy(error = message) }
                        cancellingAuditBuilder?.let(::saveAuditRecord)
                    }
                    .onFailure { error ->
                        val message = "已停止本机接收，服务端取消失败：${error.message ?: "未知错误"}"
                        cancellingAuditBuilder?.errorInfo = ErrorAuditInfo(message = message)
                        _uiState.update { it.copy(error = message) }
                        cancellingAuditBuilder?.let(::saveAuditRecord)
                    }
            }
        }
        chatJob?.cancel()
        chatJob = null
        cancellingAuditBuilder?.errorInfo = ErrorAuditInfo(
            message = LOCAL_STREAM_STOP_MESSAGE,
        )
        if (runId.isNullOrBlank()) {
            cancellingAuditBuilder?.let(::saveAuditRecord)
        }
        _uiState.update { state ->
            val stoppedMessages = state.messages.map { message ->
                if (message.role == MessageRole.ASSISTANT && message.isStreaming) {
                    message.copy(isStreaming = false, animateReveal = false)
                } else {
                    message
                }
            }
            state.copy(
                messages = stoppedMessages,
                isStreaming = false,
                canStop = false,
                currentRunId = null,
                error = LOCAL_STREAM_STOP_MESSAGE,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessages() {
        chatJob?.cancel()
        clearPendingAnswerDelta()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                conversationId = null,
                currentRunId = null,
                isStreaming = false,
                canStop = false,
                showDraftConfirm = null,
                contextCompacted = null,
                error = null,
            )
        }
    }

    fun dismissDraftConfirm() {
        _uiState.update { it.copy(showDraftConfirm = null) }
    }

    fun dismissContextCompacted() {
        _uiState.update { it.copy(contextCompacted = null) }
    }

    fun archiveDraftFromDialog(draftId: Long) {
        viewModelScope.launch {
            val draftResult = repository.listDrafts(_uiState.value.conversationId)
            val draft = draftResult.getOrNull()?.firstOrNull { it.id == draftId }
            if (draft == null) {
                _uiState.update { it.copy(error = "草稿不存在或已处理", showDraftConfirm = null) }
                return@launch
            }
            repository.updateDraft(
                draftId,
                UpdateAgentDraftRequest(
                    conversationId = draft.conversationId,
                    draftType = draft.draftType,
                    title = draft.title,
                    contentJson = draft.contentJson,
                    status = "archived",
                )
            ).onSuccess {
                _uiState.update { state ->
                    state.copy(showDraftConfirm = null)
                }
                saveAuditRecord()
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "草稿归档失败") }
            }
        }
    }
}

private fun List<ToolCallRecord>.updateToolCall(
    toolName: String,
    toolCallId: String?,
    eventId: String?,
    seq: Int?,
    conversationId: Long?,
    auditId: String?,
    traceId: String?,
    status: ToolCallStatus,
    inputSummary: String? = null,
    queryWindow: kotlinx.serialization.json.JsonElement? = null,
    resultSummary: String?,
    startedAt: Long? = null,
    completedAt: Long? = null,
    durationMs: Long? = null,
    returnedCount: Int? = null,
    totalCount: Int? = null,
    limit: Int? = null,
    isTruncated: Boolean? = null,
    evidence: kotlinx.serialization.json.JsonElement? = null,
    nextCursor: String? = null,
    timestamp: Long,
): List<ToolCallRecord> {
    val index = if (!toolCallId.isNullOrBlank()) {
        indexOfLast { it.toolCallId == toolCallId }
    } else {
        indexOfLast { it.toolName == toolName }
    }
    if (index == -1) {
        return this + ToolCallRecord(
            toolName = toolName,
            eventId = eventId,
            seq = seq,
            conversationId = conversationId,
            toolCallId = toolCallId,
            auditId = auditId,
            traceId = traceId,
            status = status,
            inputSummary = inputSummary,
            queryWindow = queryWindow,
            resultSummary = resultSummary,
            startedAt = startedAt,
            completedAt = completedAt,
            durationMs = durationMs,
            returnedCount = returnedCount,
            totalCount = totalCount,
            limit = limit,
            isTruncated = isTruncated,
            evidence = evidence,
            nextCursor = nextCursor,
            timestamp = timestamp,
        )
    }
    val updated = toMutableList()
    updated[index] = updated[index].copy(
        status = status,
        eventId = eventId ?: updated[index].eventId,
        seq = seq ?: updated[index].seq,
        conversationId = conversationId ?: updated[index].conversationId,
        toolCallId = toolCallId ?: updated[index].toolCallId,
        auditId = auditId ?: updated[index].auditId,
        traceId = traceId ?: updated[index].traceId,
        inputSummary = inputSummary ?: updated[index].inputSummary,
        queryWindow = queryWindow ?: updated[index].queryWindow,
        resultSummary = resultSummary,
        startedAt = startedAt ?: updated[index].startedAt,
        completedAt = completedAt ?: updated[index].completedAt,
        durationMs = durationMs ?: updated[index].durationMs,
        returnedCount = returnedCount ?: updated[index].returnedCount,
        totalCount = totalCount ?: updated[index].totalCount,
        limit = limit ?: updated[index].limit,
        isTruncated = isTruncated ?: updated[index].isTruncated,
        evidence = evidence ?: updated[index].evidence,
        nextCursor = nextCursor ?: updated[index].nextCursor,
        timestamp = timestamp,
    )
    return updated
}

private fun AgentMessageDto.toChatMessage(): ChatMessage {
    val parsedBlocks = parseStoredResultBlocks(structuredDataJson)
    return ChatMessage(
        id = id.toString(),
        conversationId = conversationId,
        role = when (role.lowercase()) {
            "assistant" -> MessageRole.ASSISTANT
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.USER
        },
        content = content,
        blocks = parsedBlocks,
        parts = buildStoredMessageParts(content, parsedBlocks),
        createdAt = createdAt,
    )
}

private fun buildStoredMessageParts(
    content: String,
    blocks: List<ResultBlockDto>,
): List<ChatMessagePart> {
    val parts = mutableListOf<ChatMessagePart>()
    if (content.isNotBlank()) {
        parts += ChatMessagePart.Text(content)
    }
    parts += blocks.map(ChatMessagePart::ResultBlock)
    return parts
}

private fun parseStoredResultBlocks(rawJson: String?): List<ResultBlockDto> {
    if (rawJson.isNullOrBlank()) {
        return emptyList()
    }
    return runCatching {
        StoredResultBlockJson.decodeFromString(
            ListSerializer(ResultBlockDto.serializer()),
            rawJson
        )
    }.getOrElse { error ->
        listOf(
            ResultBlockDto(
                blockType = "parse_error",
                title = "历史结构化结果解析失败",
                data = buildJsonObject {
                    put("error", error.message ?: "无法解析历史结构化结果")
                    put("raw", rawJson.take(600))
                },
            )
        )
    }
}

internal fun String.withAuthoritativeAnswer(answer: String?): String {
    val normalizedAnswer = answer?.takeIf { it.isNotBlank() } ?: return this
    return when {
        this == normalizedAnswer -> this
        isBlank() -> normalizedAnswer
        normalizedAnswer.startsWith(this) -> normalizedAnswer
        else -> this
    }
}

internal fun List<ChatMessagePart>.appendStreamingText(
    delta: String,
    pendingBlocks: List<ResultBlockDto> = emptyList(),
): List<ChatMessagePart> {
    if (delta.isBlank()) return this
    if (none { it is ChatMessagePart.Text } && pendingBlocks.isNotEmpty()) {
        return listOf(ChatMessagePart.Text(delta)) + pendingBlocks.map(ChatMessagePart::ResultBlock)
    }
    val last = lastOrNull()
    return if (last is ChatMessagePart.Text) {
        dropLast(1) + last.copy(markdown = last.markdown + delta)
    } else {
        this + ChatMessagePart.Text(delta)
    }
}

internal fun List<ChatMessagePart>.appendResultBlockAfterVisibleText(block: ResultBlockDto): List<ChatMessagePart> {
    if (none { it is ChatMessagePart.Text }) {
        return this
    }
    return this + ChatMessagePart.ResultBlock(block)
}

internal fun List<ChatMessagePart>.withAuthoritativeText(
    content: String,
    pendingBlocks: List<ResultBlockDto> = emptyList(),
): List<ChatMessagePart> {
    if (content.isBlank()) return this
    val textIndexes = mapIndexedNotNull { index, part ->
        if (part is ChatMessagePart.Text) index else null
    }
    if (textIndexes.isEmpty()) {
        val resultParts = if (any { it is ChatMessagePart.ResultBlock }) {
            this
        } else {
            pendingBlocks.map(ChatMessagePart::ResultBlock)
        }
        return listOf(ChatMessagePart.Text(content)) + resultParts
    }
    if (textIndexes.size > 1) {
        return mergeAuthoritativeTextAcrossTimeline(content, textIndexes)
    }
    val firstTextIndex = textIndexes.first()
    val currentText = this[firstTextIndex] as ChatMessagePart.Text
    return when {
        currentText.markdown == content -> this
        content.startsWith(currentText.markdown) -> {
            toMutableList().also { parts ->
                parts[firstTextIndex] = currentText.copy(markdown = content)
            }
        }
        else -> this
    }
}

private fun List<ChatMessagePart>.mergeAuthoritativeTextAcrossTimeline(
    content: String,
    textIndexes: List<Int>,
): List<ChatMessagePart> {
    val visibleText = textIndexes.joinToString(separator = "") { index ->
        (this[index] as ChatMessagePart.Text).markdown
    }
    return when {
        visibleText == content || visibleText.startsWith(content) -> this
        content.startsWith(visibleText) -> {
            val missingTail = content.removePrefix(visibleText)
            if (missingTail.isBlank()) {
                this
            } else {
                val lastTextIndex = textIndexes.last()
                val lastText = this[lastTextIndex] as ChatMessagePart.Text
                toMutableList().also { parts ->
                    parts[lastTextIndex] = lastText.copy(markdown = lastText.markdown + missingTail)
                }
            }
        }
        else -> this
    }
}

private val StoredResultBlockJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

/**
 * 审计记录构建器，用于在流式过程中逐步收集审计信息。
 */
private class AuditRecordBuilder(
    val userMessage: String,
) {
    var runId: String? = null
    var conversationId: Long? = null
    var safetyResult: SafetyAuditResult? = null
    private val toolsCalled = mutableListOf<ToolAuditRecord>()
    var draftInfo: DraftAuditInfo? = null
    var contextCompacted: Boolean = false
    var finalAnswerSummary: String? = null
    var errorInfo: ErrorAuditInfo? = null

    fun addToolCall(tool: ToolAuditRecord) {
        toolsCalled.add(tool)
    }

    fun updateToolCall(
        toolName: String,
        status: String,
        toolCallId: String? = null,
        inputSummary: String? = null,
        queryWindow: kotlinx.serialization.json.JsonElement? = null,
        resultSummary: String?,
        timestamp: Long,
        startedAt: Long? = null,
        completedAt: Long? = null,
        durationMs: Long? = null,
        returnedCount: Int? = null,
        totalCount: Int? = null,
        limit: Int? = null,
        isTruncated: Boolean? = null,
        evidence: kotlinx.serialization.json.JsonElement? = null,
        nextCursor: String? = null,
    ) {
        val index = toolsCalled.indexOfLast { it.toolName == toolName }
        if (index != -1) {
            toolsCalled[index] = toolsCalled[index].copy(
                status = status,
                toolCallId = toolCallId ?: toolsCalled[index].toolCallId,
                inputSummary = inputSummary ?: toolsCalled[index].inputSummary,
                queryWindow = queryWindow ?: toolsCalled[index].queryWindow,
                resultSummary = resultSummary,
                startedAt = startedAt ?: toolsCalled[index].startedAt,
                completedAt = completedAt ?: toolsCalled[index].completedAt,
                durationMs = durationMs ?: toolsCalled[index].durationMs,
                returnedCount = returnedCount ?: toolsCalled[index].returnedCount,
                totalCount = totalCount ?: toolsCalled[index].totalCount,
                limit = limit ?: toolsCalled[index].limit,
                isTruncated = isTruncated ?: toolsCalled[index].isTruncated,
                evidence = evidence ?: toolsCalled[index].evidence,
                nextCursor = nextCursor ?: toolsCalled[index].nextCursor,
                timestamp = timestamp,
            )
        } else {
            toolsCalled.add(
                ToolAuditRecord(
                    toolCallId = toolCallId,
                    toolName = toolName,
                    status = status,
                    inputSummary = inputSummary,
                    queryWindow = queryWindow,
                    resultSummary = resultSummary,
                    startedAt = startedAt,
                    completedAt = completedAt,
                    durationMs = durationMs,
                    returnedCount = returnedCount,
                    totalCount = totalCount,
                    limit = limit,
                    isTruncated = isTruncated,
                    evidence = evidence,
                    nextCursor = nextCursor,
                    timestamp = timestamp,
                )
            )
        }
    }

    fun build(): AgentAuditRecord = AgentAuditRecord(
        id = UUID.randomUUID().toString(),
        runId = runId,
        conversationId = conversationId,
        userMessage = userMessage,
        safetyResult = safetyResult,
        toolsCalled = toolsCalled.toList(),
        draftGenerated = draftInfo,
        contextCompacted = contextCompacted,
        finalAnswerSummary = finalAnswerSummary?.take(500), // 限制摘要长度
        errorInfo = errorInfo,
        timestamp = System.currentTimeMillis(),
    )
}
