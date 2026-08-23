package com.zhihuiji.feature.agent

import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.AgentDraftConfirmRequest
import com.zhihuiji.core.model.v2.agent.AgentImageGenerateRequest
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentRunTraceDto
import com.zhihuiji.core.model.v2.agent.AgentRunTraceReducer
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
import com.zhihuiji.core.network.RetryState
import com.zhihuiji.data.agent.AgentAuditRepository
import com.zhihuiji.data.agent.AgentPendingMessageRepository
import com.zhihuiji.data.agent.AgentV2Repository
import com.zhihuiji.data.agent.MediaV2Repository
import com.zhihuiji.data.agent.RECENT_AGENT_MESSAGE_WINDOW_LIMIT
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

private const val LOCAL_STREAM_STOP_MESSAGE = "已停止本机接收，正在请求服务端取消"
private const val SERVER_CANCEL_CONFIRMED_MESSAGE = "服务端已确认取消生成"
private const val TOOL_ENDED_WITHOUT_COMPLETION_MESSAGE = "运行已结束，未收到工具完成事件"
private const val TOOL_CANCELLED_WITH_RUN_MESSAGE = "生成已取消，工具查询已停止"
private const val TOOL_INTERRUPTED_BY_ERROR_MESSAGE = "连接中断，工具状态未确认"
internal const val AnswerDeltaFlushDelayMs = 24L

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
    val retryState: RetryState = RetryState(),
    val conversations: List<AgentConversationDto> = emptyList(),
    val isLoadingConversations: Boolean = false,
    val isLoadingMoreConversations: Boolean = false,
    val hasMoreConversations: Boolean = true,
    val conversationPage: Int = 0,
    val isDrawerOpen: Boolean = false,
    val isLoadingMoreMessages: Boolean = false,
    val hasMoreMessages: Boolean = false,
    val messagePage: Int = 0,
    val imageAttachments: List<AgentImageAttachmentUi> = emptyList(),
    val attachmentAuthToken: String? = null,
    val isUploadingImage: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val generatedImageUrl: String? = null,
    val generatedImagePrompt: String? = null,
) {
    /** 重连中提示文案，供 UI 直接展示 */
    val retryMessage: String?
        get() = if (retryState.isRetrying) {
            "重连中... (${retryState.attempt}/${retryState.maxAttempts})"
        } else {
            null
        }
}

data class AgentImageAttachmentUi(
    val assetId: Long,
    val url: String,
)

/**
 * 草稿确认弹窗状态
 */
data class DraftConfirmState(
    val draftId: Long,
    val draftType: String,
    val title: String,
    val status: String? = null,
    val confirmPhase: DraftConfirmPhase = DraftConfirmPhase.IDLE,
    val errorMessage: String? = null,
)

/**
 * 草稿确认状态机：idle → confirming → confirmed/rejected/failed
 */
enum class DraftConfirmPhase {
    IDLE,
    CONFIRMING,
    CONFIRMED,
    REJECTED,
    FAILED,
}

/**
 * 上下文压缩提示状态
 */
data class ContextCompactedState(
    val compactedCount: Int,
    val summary: String,
    val summaryPreview: String? = null,
    val reason: String? = null,
    val reused: Boolean = false,
)

@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val repository: AgentV2Repository,
    private val auditRepository: AgentAuditRepository,
    private val mediaRepository: MediaV2Repository,
    private val pendingMessageRepository: AgentPendingMessageRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    private var chatJob: Job? = null
    private var answerDeltaFlushJob: Job? = null
    private var pendingAnswerDeltaMessageId: String? = null
    private val pendingAnswerDelta = StringBuilder()
    private var pendingAnswerDeltaSource: String? = null
    private var consumedInitialQuestionKey: String? = null
    private var currentStreamingAssistantMessageId: String? = null

    /** 当前运行的审计记录构建器 */
    private var currentAuditBuilder: AuditRecordBuilder? = null
    private val draftConfirmKeys = mutableMapOf<Long, String>()

    init {
        // 监听 SSE 重连状态，更新 UI 提示“重连中...”
        viewModelScope.launch {
            repository.retryState.collect { retry ->
                _uiState.update { it.copy(retryState = retry) }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true, error = null) }
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val fileName = readDisplayName(resolver, uri)
            val bytes = runCatching {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                _uiState.update { it.copy(isUploadingImage = false, error = "读取图片失败") }
                return@launch
            }
            mediaRepository.uploadAsset(
                bytes = bytes,
                fileName = fileName,
                mimeType = mimeType,
                assetType = "agent_chat_image",
            ).onSuccess { asset ->
                _uiState.update { state ->
                    state.copy(
                        isUploadingImage = false,
                        attachmentAuthToken = mediaRepository.peekAuthToken(),
                        imageAttachments = state.imageAttachments + AgentImageAttachmentUi(
                            assetId = asset.id,
                            url = mediaRepository.contentUrlFor(asset.id),
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isUploadingImage = false, error = error.message ?: "上传图片失败") }
            }
        }
    }

    fun removeImageAttachment(assetId: Long) {
        _uiState.update { state ->
            state.copy(imageAttachments = state.imageAttachments.filterNot { it.assetId == assetId })
        }
    }

    fun generateImage(
        prompt: String,
        useReferenceImages: Boolean = _uiState.value.imageAttachments.isNotEmpty(),
    ) {
        val normalizedPrompt = prompt.trim()
        if (normalizedPrompt.isEmpty() || _uiState.value.isGeneratingImage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingImage = true, error = null) }
            val referenceAssetIds = if (useReferenceImages) {
                _uiState.value.imageAttachments
                    .take(1)
                    .map { it.assetId }
            } else {
                emptyList()
            }
            repository.generateImage(
                AgentImageGenerateRequest(
                    prompt = normalizedPrompt,
                    referenceAssetIds = referenceAssetIds,
                )
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isGeneratingImage = false,
                        generatedImageUrl = response.imageUrl,
                        generatedImagePrompt = response.revisedPrompt ?: normalizedPrompt,
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isGeneratingImage = false, error = error.message ?: "生图失败") }
            }
        }
    }

    fun dismissGeneratedImage() {
        _uiState.update { it.copy(generatedImageUrl = null, generatedImagePrompt = null) }
    }

    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
        if (_uiState.value.conversations.isEmpty() && !_uiState.value.isLoadingConversations) {
            loadConversations()
        }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    fun loadConversations() {
        if (_uiState.value.isLoadingConversations || _uiState.value.isLoadingMoreConversations) return
        _uiState.update {
            it.copy(
                isLoadingConversations = true,
                conversationPage = 0,
                hasMoreConversations = true,
            )
        }
        viewModelScope.launch {
            repository.listConversations(page = 0, limit = 50)
                .onSuccess { conversations ->
                    _uiState.update {
                        it.copy(
                            conversations = conversations,
                            isLoadingConversations = false,
                            conversationPage = 0,
                            hasMoreConversations = conversations.size >= 50,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingConversations = false,
                            error = e.message ?: "加载会话列表失败",
                        )
                    }
                }
        }
    }

    fun loadMoreConversations() {
        val state = _uiState.value
        if (state.isLoadingConversations || state.isLoadingMoreConversations || !state.hasMoreConversations) {
            return
        }
        val nextPage = state.conversationPage + 1
        _uiState.update { it.copy(isLoadingMoreConversations = true) }
        viewModelScope.launch {
            repository.listConversations(page = nextPage, limit = 50)
                .onSuccess { conversations ->
                    _uiState.update { current ->
                        val knownIds = current.conversations.asSequence().map { it.id }.toHashSet()
                        current.copy(
                            conversations = current.conversations + conversations.filter { it.id !in knownIds },
                            isLoadingMoreConversations = false,
                            conversationPage = nextPage,
                            hasMoreConversations = conversations.size >= 50,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingMoreConversations = false,
                            error = e.message ?: "加载更多会话失败",
                        )
                    }
                }
        }
    }

    fun loadMoreMessages() {
        val state = _uiState.value
        val conversationId = state.conversationId ?: return
        if (state.isLoading || state.isLoadingMoreMessages || !state.hasMoreMessages || state.isStreaming) {
            return
        }
        val nextPage = state.messagePage + 1
        _uiState.update { it.copy(isLoadingMoreMessages = true) }
        viewModelScope.launch {
            repository.listMessages(
                conversationId = conversationId,
                page = nextPage,
                limit = RECENT_AGENT_MESSAGE_WINDOW_LIMIT,
            ).onSuccess { messages ->
                val parsed = restoreMessagesWithRunTraces(conversationId, messages)
                _uiState.update { current ->
                    val knownIds = current.messages.asSequence().map { it.id }.toHashSet()
                    val olderMessages = parsed.filter { it.id !in knownIds }
                    current.copy(
                        messages = olderMessages + current.messages,
                        isLoadingMoreMessages = false,
                        messagePage = nextPage,
                        hasMoreMessages = messages.size >= RECENT_AGENT_MESSAGE_WINDOW_LIMIT,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoadingMoreMessages = false,
                        error = e.message ?: "加载更多历史消息失败",
                    )
                }
            }
        }
    }

    fun switchConversation(id: Long) {
        if (id <= 0) return
        // 关闭抽屉，清空当前对话并加载目标会话消息
        closeDrawer()
        if (_uiState.value.conversationId == id && _uiState.value.messages.isNotEmpty()) return
        clearMessages()
        viewModelScope.launch {
            loadConversationMessages(id, forceReload = true)
        }
    }

    fun deleteConversation(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            repository.deleteConversation(id)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.conversations.filterNot { it.id == id }
                        val activeCleared = if (state.conversationId == id) {
                            state.copy(
                                conversations = updated,
                                conversationId = null,
                                messages = emptyList(),
                                currentRunId = null,
                                isStreaming = false,
                                canStop = false,
                            )
                        } else {
                            state.copy(conversations = updated)
                        }
                        activeCleared
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "删除会话失败") }
                }
        }
    }

    /**
     * 重新生成指定助手消息：删除该消息及其前一条用户消息，重新发起请求。
     */
    fun regenerateMessage(messageId: String) {
        if (_uiState.value.isStreaming) return
        val messages = _uiState.value.messages
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0) return
        val target = messages[targetIndex]
        if (target.role != MessageRole.ASSISTANT) return

        // 找到该助手消息前最近的一条用户消息
        var userIndex = targetIndex - 1
        while (userIndex >= 0 && messages[userIndex].role != MessageRole.USER) {
            userIndex--
        }
        if (userIndex < 0) return
        val userContent = messages[userIndex].content.trim()
        if (userContent.isEmpty()) return

        // 删除这两条消息，重置输入框，重新发送
        val toRemove = if (userIndex == targetIndex - 1) {
            setOf(messages[userIndex].id, messages[targetIndex].id)
        } else {
            setOf(messages[targetIndex].id)
        }
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id in toRemove },
                inputText = userContent,
            )
        }
        sendMessage()
    }

    fun editAndResend(messageId: String, newText: String) {
        if (_uiState.value.isStreaming) return
        val trimmedText = newText.trim()
        if (trimmedText.isEmpty()) return
        val messages = _uiState.value.messages
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0) return
        val target = messages[targetIndex]
        if (target.role != MessageRole.USER) return

        _uiState.update { state ->
            state.copy(
                messages = state.messages.take(targetIndex),
                inputText = trimmedText,
                error = null,
                showDraftConfirm = null,
                contextCompacted = null,
            )
        }
        sendMessage(trimmedText)
    }

    fun startConversation(
        conversationId: Long?,
        initialQuestion: String?,
    ) {
        viewModelScope.launch {
            val normalizedQuestion = initialQuestion?.trim().orEmpty()
            val questionKey = initialQuestionKey(conversationId, normalizedQuestion)
            val shouldSendInitialQuestion = shouldSendInitialQuestion(
                initialQuestion = normalizedQuestion,
                consumedInitialQuestionKey = consumedInitialQuestionKey,
                nextInitialQuestionKey = questionKey,
                isStreaming = _uiState.value.isStreaming,
            )
            if (conversationId != null && conversationId > 0) {
                loadConversationMessages(conversationId, forceReload = false)
            }
            if (shouldSendInitialQuestion && consumedInitialQuestionKey != questionKey) {
                consumedInitialQuestionKey = questionKey
                onInputChange(normalizedQuestion)
                sendMessage()
            }
        }
    }

    fun loadConversation(conversationId: Long?) {
        if (conversationId == null || conversationId <= 0) return
        viewModelScope.launch {
            loadConversationMessages(conversationId, forceReload = true)
        }
    }

    private suspend fun loadConversationMessages(
        conversationId: Long,
        forceReload: Boolean,
    ) {
        if (!forceReload && shouldReuseLoadedConversation(_uiState.value, conversationId)) return
        _uiState.update { it.copy(isLoading = true, error = null, conversationId = conversationId) }
        repository.listMessages(
            conversationId = conversationId,
            page = 0,
            limit = RECENT_AGENT_MESSAGE_WINDOW_LIMIT,
        )
            .onSuccess { messages ->
                val chatMessages = restoreMessagesWithRunTraces(conversationId, messages)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMoreMessages = false,
                        messagePage = 0,
                        hasMoreMessages = messages.size >= RECENT_AGENT_MESSAGE_WINDOW_LIMIT,
                        messages = mergeLoadedConversationMessages(
                            loadedMessages = chatMessages,
                            currentMessages = it.messages,
                            isStreaming = it.isStreaming,
                        ),
                    )
                }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
    }

    private suspend fun restoreMessagesWithRunTraces(
        conversationId: Long,
        messages: List<AgentMessageDto>,
    ): List<ChatMessage> {
        val tracesByRunId = repository.listRunTraces(conversationId)
            .getOrElse { emptyList() }
            .associateBy { it.runId }
        return withContext(Dispatchers.Default) {
            messages.map { it.toChatMessage().restoreRunTrace(tracesByRunId) }
        }
    }

    /**
     * 发送消息（流式 SSE 版本）
     */
    fun sendMessage(prefilledText: String? = null) {
        val state = _uiState.value
        val text = prefilledText?.trim().orEmpty()
            .ifBlank { state.inputText.trim() }
            .ifBlank { if (state.imageAttachments.isNotEmpty()) "请帮我分析这张图片" else "" }
        if (text.isEmpty() || _uiState.value.isStreaming) return
        val imageAssetIds = state.imageAttachments.map { it.assetId }
        val visibleUserContent = if (imageAssetIds.isEmpty()) {
            text
        } else {
            text + "\n\n[已附带图片 ${imageAssetIds.size} 张]"
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = _uiState.value.conversationId ?: 0L,
            role = MessageRole.USER,
            content = visibleUserContent,
            createdAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                imageAttachments = emptyList(),
                isStreaming = true,
                canStop = true,
                currentRunId = null,
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
            imageAssetIds = imageAssetIds,
        )

        // 初始化审计记录构建器
        currentAuditBuilder = AuditRecordBuilder(userMessage = text)
        clearPendingAnswerDelta()

        if (request.conversationId?.let { it < 0L } == true || !appContext.hasValidatedInternetConnection()) {
            chatJob = viewModelScope.launch {
                queueOfflineMessage(userMessage.id, null, request)
            }
            return
        }

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
            currentStreamingAssistantMessageId = assistantMessageId
            _uiState.update { it.copy(messages = it.messages + streamingMessage) }

            repository.chatStream(request)
                .catch { e ->
                    if (_uiState.value.currentRunId == null && e.isQueueableNetworkFailure()) {
                        queueOfflineMessage(userMessage.id, assistantMessageId, request)
                    } else {
                        handleStreamError(assistantMessageId, e.message ?: "流式连接失败")
                    }
                }
                .collect { event ->
                    handleStreamEvent(assistantMessageId, event)
                }
        }
    }

    private suspend fun queueOfflineMessage(
        userMessageId: String,
        assistantMessageId: String?,
        request: AgentChatRequest,
    ) {
        val queuedConversationId = pendingMessageRepository.enqueue(
            conversationId = request.conversationId,
            content = request.message,
            imageAssetIds = request.imageAssetIds,
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages
                    .asSequence()
                    .filter { it.id != assistantMessageId }
                    .map { message ->
                        if (message.id == userMessageId) {
                            message.copy(conversationId = queuedConversationId)
                        } else {
                            message
                        }
                    }
                    .toList(),
                conversationId = if (state.conversationId == null || state.conversationId < 0L) {
                    queuedConversationId
                } else {
                    state.conversationId
                },
                currentRunId = null,
                isStreaming = false,
                canStop = false,
                error = null,
                retryState = RetryState(),
            )
        }
        currentStreamingAssistantMessageId = null
        currentAuditBuilder?.errorInfo = ErrorAuditInfo(message = "agent message queued for network delivery")
        saveAuditRecord()
    }

    private fun Throwable.isQueueableNetworkFailure(): Boolean =
        this is com.zhihuiji.core.network.NetworkException && (code == -1 || code >= 500)

    private fun handleStreamEvent(assistantMessageId: String, event: AgentStreamEvent) {
        if (!reduceLiveTrace(assistantMessageId, event)) return
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
                val blockedSafetyResult = SafetyResult(
                    passed = false,
                    reason = event.reason,
                    suggestedAction = event.suggestedAction,
                )
                updateAssistantMessage(assistantMessageId) { msg ->
                    msg.withSafetyBlockedResult(
                        safetyResult = blockedSafetyResult,
                        errorMessage = "当前请求无法完成，请调整后重试。",
                    )
                }
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        canStop = false,
                        error = "当前请求无法完成，请调整后重试。",
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
                currentAuditBuilder?.addToolCall(
                    ToolAuditRecord(
                        toolCallId = event.toolCallId,
                        toolName = event.toolName,
                        status = "running",
                        inputSummary = event.inputSummary,
                        queryWindow = event.queryWindow,
                        startedAt = event.startedAt ?: event.timestamp,
                        timestamp = event.timestamp,
                    )
                )
            }

            is AgentStreamEvent.ToolProgress -> {
                currentAuditBuilder?.updateToolCall(
                    toolName = event.toolName,
                    status = "running",
                    resultSummary = event.message,
                    timestamp = event.timestamp,
                )
            }

            is AgentStreamEvent.ToolCompleted -> {
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
                if (event.deltaSource.isVisibleAnswerDeltaSource()) {
                    enqueueAnswerDelta(
                        assistantMessageId = assistantMessageId,
                        delta = event.delta,
                        deltaSource = event.deltaSource,
                    )
                }
                updateRunTrace(assistantMessageId) { trace ->
                    trace.withAnswerDeltaSourceIfChanged(event.deltaSource)
                }
            }

            is AgentStreamEvent.AnswerCompleted -> {
                flushPendingAnswerDelta()
                updateAssistantMessage(assistantMessageId) { msg ->
                    val finalContent = msg.content.withAuthoritativeAnswerIfVisible(
                        answer = event.answer,
                        hasServerAnswerDelta = msg.hasServerAnswerDelta,
                    )
                    msg.copy(
                        content = finalContent,
                        parts = msg.parts.withAuthoritativeText(finalContent, msg.blocks),
                        animateReveal = false,
                    )
                }
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        logRef = event.observability?.logRef ?: trace.logRef,
                    )
                }
            }

            is AgentStreamEvent.ResultBlockEvent -> {
                flushPendingAnswerDelta()
                updateAssistantMessage(assistantMessageId) { msg ->
                    val updatedBlocks = msg.blocks.appendDistinctResultBlock(event.block)
                    val updatedParts = if (updatedBlocks.size == msg.blocks.size) {
                        msg.parts
                    } else {
                        msg.parts.appendResultBlockAfterVisibleText(event.block)
                    }
                    msg.copy(
                        blocks = updatedBlocks,
                        parts = updatedParts,
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
                            status = event.status,
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
                            summary = event.summaryPreview ?: event.reason ?: "上下文已压缩",
                            summaryPreview = event.summaryPreview,
                            reason = event.reason,
                            reused = event.reused,
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
                currentStreamingAssistantMessageId = null
                updateAssistantMessage(assistantMessageId) { msg ->
                    msg.withRunCompletedState(
                        finalAnswer = finalAnswer,
                        safeMessage = event.safeMessage,
                    )
                }
                updateRunTrace(assistantMessageId) { trace ->
                    trace.copy(
                        logRef = event.observability?.logRef ?: trace.logRef,
                    )
                }
                currentAuditBuilder?.finalAnswerSummary = event.safeMessage ?: finalAnswer
                saveAuditRecord()
            }

            is AgentStreamEvent.RunFailed -> {
                handleTerminalFailure(assistantMessageId, event.safeMessage ?: event.errorCode ?: "运行失败")
            }

            is AgentStreamEvent.RunBlocked -> {
                handleTerminalFailure(assistantMessageId, event.safeMessage ?: "运行被安全策略阻止")
            }

            is AgentStreamEvent.RunExhausted -> {
                handleTerminalFailure(assistantMessageId, event.safeMessage ?: "运行预算耗尽，未能完成任务")
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
                currentStreamingAssistantMessageId = null
                updateAssistantMessage(assistantMessageId) { msg ->
                    msg.copy(
                        isStreaming = false,
                        animateReveal = false,
                    )
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

    private fun readDisplayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex) ?: "agent-image.jpg"
            }
        }
        return "agent-image.jpg"
    }

    private fun handleStreamError(assistantMessageId: String, errorMessage: String) {
        flushPendingAnswerDelta()
        _uiState.update {
            it.copy(
                isStreaming = false,
                canStop = false,
                currentRunId = null,
                error = errorMessage,
                retryState = RetryState(),
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
        currentStreamingAssistantMessageId = null
        currentAuditBuilder?.errorInfo = ErrorAuditInfo(
            message = errorMessage,
        )
        saveAuditRecord()
    }

    /**
     * 处理统一终态中的非成功事件（FAILED/BLOCKED/EXHAUSTED）。
     * 不显示成功样式；保留已有回答内容，标记错误状态。
     */
    private fun handleTerminalFailure(assistantMessageId: String, errorMessage: String) {
        flushPendingAnswerDelta()
        _uiState.update {
            it.copy(
                isStreaming = false,
                canStop = false,
                currentRunId = null,
                error = errorMessage,
                retryState = RetryState(),
            )
        }
        currentStreamingAssistantMessageId = null
        updateAssistantMessage(assistantMessageId) { msg ->
            msg.copy(
                isStreaming = false,
                isError = true,
                errorMessage = errorMessage,
                animateReveal = false,
            )
        }
        currentAuditBuilder?.errorInfo = ErrorAuditInfo(message = errorMessage)
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
            val messages = state.messages
            var index = messages.lastIndex
            while (index >= 0 && messages[index].id != assistantMessageId) {
                index--
            }
            if (index == -1) {
                return@update state
            }
            val current = messages[index]
            val updated = transform(current)
            if (updated == current) {
                return@update state
            }
            val updatedMessages = messages.toMutableList()
            updatedMessages[index] = updated
            state.copy(messages = updatedMessages)
        }
    }

    private fun reduceLiveTrace(
        assistantMessageId: String,
        event: AgentStreamEvent,
    ): Boolean {
        val eventRunId = event.runIdOrNull()
        var accepted = true
        updateAssistantMessage(assistantMessageId) { message ->
            val currentTrace = message.runTrace
            val seed = currentTrace ?: AgentRunTraceReducer.initial(
                runId = eventRunId ?: _uiState.value.currentRunId.orEmpty(),
            )
            val reduced = AgentRunTraceReducer.reduce(seed, event)
            if (currentTrace != null && reduced === currentTrace) {
                accepted = false
                return@updateAssistantMessage message
            }
            message.copy(
                runId = message.runId ?: eventRunId ?: reduced.runId.takeIf { it.isNotBlank() },
                runTrace = reduced,
            )
        }
        return accepted
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
            delay(AnswerDeltaFlushDelayMs)
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

    fun stopGeneration() {
        stopActiveGeneration(showStatus = true)
    }

    private fun stopActiveGeneration(showStatus: Boolean) {
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
                        if (showStatus) {
                            _uiState.update { it.copy(error = message) }
                        }
                        cancellingAuditBuilder?.let(::saveAuditRecord)
                    }
                    .onFailure { error ->
                        val message = "已停止本机接收，服务端取消失败：${error.message ?: "未知错误"}"
                        cancellingAuditBuilder?.errorInfo = ErrorAuditInfo(message = message)
                        if (showStatus) {
                            _uiState.update { it.copy(error = message) }
                        }
                        cancellingAuditBuilder?.let(::saveAuditRecord)
                    }
            }
        }
        chatJob?.cancel()
        chatJob = null
        cancellingAuditBuilder?.errorInfo = ErrorAuditInfo(
            message = LOCAL_STREAM_STOP_MESSAGE,
        )
        cancellingAuditBuilder?.closeOpenToolCalls(
            status = "cancelled",
            resultSummary = TOOL_CANCELLED_WITH_RUN_MESSAGE,
        )
        if (runId.isNullOrBlank()) {
            cancellingAuditBuilder?.let(::saveAuditRecord)
        }
        val streamingAssistantMessageId = currentStreamingAssistantMessageId
        currentStreamingAssistantMessageId = null
        if (streamingAssistantMessageId != null) {
            updateAssistantMessage(streamingAssistantMessageId) { message ->
                message.copy(
                    isStreaming = false,
                    animateReveal = false,
                    runTrace = AgentRunTraceReducer.cancelled(
                        trace = message.runTrace ?: AgentRunTraceReducer.initial(
                            runId = message.runId ?: runId.orEmpty(),
                        ),
                        message = LOCAL_STREAM_STOP_MESSAGE,
                    ),
                )
            }
        }
        _uiState.update { state ->
            state.copy(
                isStreaming = false,
                canStop = false,
                currentRunId = null,
                error = if (showStatus) LOCAL_STREAM_STOP_MESSAGE else state.error,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessages() {
        if (shouldCancelServerRunBeforeClearing(_uiState.value, chatJob?.isActive == true)) {
            stopActiveGeneration(showStatus = false)
        } else {
            chatJob?.cancel()
        }
        currentStreamingAssistantMessageId = null
        clearPendingAnswerDelta()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                conversationId = null,
                currentRunId = null,
                isStreaming = false,
                canStop = false,
                isLoadingMoreMessages = false,
                hasMoreMessages = false,
                messagePage = 0,
                imageAttachments = emptyList(),
                attachmentAuthToken = null,
                generatedImageUrl = null,
                generatedImagePrompt = null,
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

    /**
     * 草稿确认状态机：idle → confirming → confirmed/failed。
     * 幂等处理：confirming/confirmed 状态下重复调用直接返回。
     * "确认"只调用草稿确认接口 POST /v2/agent/drafts/{id}/confirm，不触发正式业务创建接口。
     */
    fun confirmDraftFromDialog(draftId: Long) {
        val current = _uiState.value.showDraftConfirm ?: return
        if (current.draftId != draftId) return
        // 幂等：已经在确认中或已确认，不重复调用
        if (current.confirmPhase == DraftConfirmPhase.CONFIRMING ||
            current.confirmPhase == DraftConfirmPhase.CONFIRMED
        ) return

        _uiState.update { state ->
            state.copy(
                showDraftConfirm = current.copy(
                    confirmPhase = DraftConfirmPhase.CONFIRMING,
                    errorMessage = null,
                )
            )
        }
        viewModelScope.launch {
            repository.confirmDraft(
                draftId,
                AgentDraftConfirmRequest(draftConfirmKeys.getOrPut(draftId) { UUID.randomUUID().toString() }),
            )
                .onSuccess { updated ->
                    currentAuditBuilder?.draftInfo = currentAuditBuilder?.draftInfo?.copy(userConfirmed = true)
                    _uiState.update { state ->
                        state.copy(
                            showDraftConfirm = state.showDraftConfirm?.copy(
                                confirmPhase = DraftConfirmPhase.CONFIRMED,
                                status = updated.status,
                            ),
                            error = "草稿已确认执行",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            showDraftConfirm = state.showDraftConfirm?.copy(
                                confirmPhase = DraftConfirmPhase.FAILED,
                                errorMessage = e.message ?: "草稿确认失败",
                            ),
                            error = e.message ?: "草稿确认失败",
                        )
                    }
                }
        }
    }

    /**
     * 草稿拒绝状态机：idle → confirming → rejected/failed。
     * 拒绝只调用草稿取消接口，不触发正式写入。
     */
    fun cancelDraftFromDialog(draftId: Long) {
        val current = _uiState.value.showDraftConfirm ?: return
        if (current.draftId != draftId) return
        // 确认中不能取消
        if (current.confirmPhase == DraftConfirmPhase.CONFIRMING) return
        // 幂等：已经取消，不重复调用
        if (current.confirmPhase == DraftConfirmPhase.REJECTED) return

        _uiState.update { state ->
            state.copy(
                showDraftConfirm = current.copy(
                    confirmPhase = DraftConfirmPhase.CONFIRMING,
                    errorMessage = null,
                )
            )
        }
        viewModelScope.launch {
            repository.cancelDraft(draftId)
                .onSuccess { updated ->
                    currentAuditBuilder?.draftInfo = currentAuditBuilder?.draftInfo?.copy(userConfirmed = false)
                    _uiState.update { state ->
                        state.copy(
                            showDraftConfirm = state.showDraftConfirm?.copy(
                                confirmPhase = DraftConfirmPhase.REJECTED,
                                status = updated.status,
                            ),
                            error = "草稿已取消",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(
                            showDraftConfirm = state.showDraftConfirm?.copy(
                                confirmPhase = DraftConfirmPhase.FAILED,
                                errorMessage = e.message ?: "草稿取消失败",
                            ),
                            error = e.message ?: "草稿取消失败",
                        )
                    }
                }
        }
    }
}

internal fun isUsableAgentNetwork(hasInternetCapability: Boolean, isValidated: Boolean): Boolean =
    hasInternetCapability && isValidated

/**
 * 草稿确认幂等判断：confirming/confirmed 状态下不重复发起确认请求。
 */
internal fun shouldSkipConfirm(phase: DraftConfirmPhase): Boolean =
    phase == DraftConfirmPhase.CONFIRMING || phase == DraftConfirmPhase.CONFIRMED

/**
 * 草稿取消幂等判断：confirming 状态下不能取消；rejected 状态下不重复取消。
 */
internal fun shouldSkipCancel(phase: DraftConfirmPhase): Boolean =
    phase == DraftConfirmPhase.CONFIRMING || phase == DraftConfirmPhase.REJECTED

private fun AgentStreamEvent.runIdOrNull(): String? = when (this) {
    is AgentStreamEvent.RunStarted -> runId
    is AgentStreamEvent.SafetyCheckStarted -> runId
    is AgentStreamEvent.SafetyCheckPassed -> runId
    is AgentStreamEvent.SafetyCheckBlocked -> runId
    is AgentStreamEvent.PlanDelta -> runId
    is AgentStreamEvent.ToolStarted -> runId
    is AgentStreamEvent.ToolProgress -> runId
    is AgentStreamEvent.ToolCompleted -> runId
    is AgentStreamEvent.ToolFailed -> runId
    is AgentStreamEvent.AnswerDelta -> runId
    is AgentStreamEvent.AnswerCompleted -> runId
    is AgentStreamEvent.ResultBlockEvent -> runId
    is AgentStreamEvent.DraftCreated -> runId
    is AgentStreamEvent.ContextCompacted -> runId
    is AgentStreamEvent.RunCompleted -> runId
    is AgentStreamEvent.RunFailed -> runId
    is AgentStreamEvent.RunBlocked -> runId
    is AgentStreamEvent.RunExhausted -> runId
    is AgentStreamEvent.RunCancelled -> runId
    is AgentStreamEvent.ErrorEvent -> runId
}

private fun Context.hasValidatedInternetConnection(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return isUsableAgentNetwork(
        hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )
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
    var index = lastIndex
    if (!toolCallId.isNullOrBlank()) {
        while (index >= 0 && this[index].toolCallId != toolCallId) {
            index--
        }
    } else {
        while (index >= 0 && this[index].toolName != toolName) {
            index--
        }
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
    val existing = this[index]
    val updatedRecord = existing.copy(
        status = status,
        eventId = eventId ?: existing.eventId,
        seq = seq ?: existing.seq,
        conversationId = conversationId ?: existing.conversationId,
        toolCallId = toolCallId ?: existing.toolCallId,
        auditId = auditId ?: existing.auditId,
        traceId = traceId ?: existing.traceId,
        inputSummary = inputSummary ?: existing.inputSummary,
        queryWindow = queryWindow ?: existing.queryWindow,
        resultSummary = resultSummary,
        startedAt = startedAt ?: existing.startedAt,
        completedAt = completedAt ?: existing.completedAt,
        durationMs = durationMs ?: existing.durationMs,
        returnedCount = returnedCount ?: existing.returnedCount,
        totalCount = totalCount ?: existing.totalCount,
        limit = limit ?: existing.limit,
        isTruncated = isTruncated ?: existing.isTruncated,
        evidence = evidence ?: existing.evidence,
        nextCursor = nextCursor ?: existing.nextCursor,
        timestamp = timestamp,
    )
    if (updatedRecord == existing) return this
    val updated = toMutableList()
    updated[index] = updatedRecord
    return updated
}

internal fun List<ToolCallRecord>.closeOpenToolCalls(
    resultSummary: String,
    completedAt: Long = System.currentTimeMillis(),
): List<ToolCallRecord> {
    var hasOpenCall = false
    for (call in this) {
        if (call.status == ToolCallStatus.RUNNING || call.status == ToolCallStatus.PENDING) {
            hasOpenCall = true
            break
        }
    }
    if (!hasOpenCall) {
        return this
    }
    val updated = ArrayList<ToolCallRecord>(size)
    for (call in this) {
        if (call.status == ToolCallStatus.RUNNING || call.status == ToolCallStatus.PENDING) {
            updated.add(
                call.copy(
                    status = ToolCallStatus.FAILED,
                    resultSummary = call.resultSummary?.takeIf { it.isNotBlank() } ?: resultSummary,
                    completedAt = call.completedAt ?: completedAt,
                    timestamp = completedAt,
                )
            )
        } else {
            updated.add(call)
        }
    }
    return updated
}

internal fun RunTrace.withAnswerDeltaSourceIfChanged(deltaSource: String?): RunTrace {
    val nextSource = deltaSource ?: answerDeltaSource
    return if (nextSource == answerDeltaSource) {
        this
    } else {
        copy(answerDeltaSource = nextSource)
    }
}

internal fun ChatMessage.withRunCompletedState(
    finalAnswer: String?,
    safeMessage: String?,
): ChatMessage {
    val terminalMessage = safeMessage ?: finalAnswer
    val finalContent = if (content.isBlank() && !terminalMessage.isNullOrBlank()) {
        terminalMessage
    } else {
        content.withAuthoritativeAnswerIfVisible(
            answer = terminalMessage,
            hasServerAnswerDelta = hasServerAnswerDelta,
        )
    }
    return copy(
        content = finalContent,
        parts = parts.withAuthoritativeText(finalContent, blocks),
        isStreaming = false,
        animateReveal = false,
    )
}

internal fun String?.isVisibleAnswerDeltaSource(): Boolean =
    this == null || this == DeltaSourceModelStream

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
        runId = runId,
        createdAt = createdAt,
    )
}

private fun ChatMessage.restoreRunTrace(
    tracesByRunId: Map<String, AgentRunTraceDto>,
): ChatMessage {
    if (role != MessageRole.ASSISTANT) return this
    val messageRunId = runId?.takeIf { it.isNotBlank() } ?: return this
    val restoredTrace = tracesByRunId[messageRunId]?.let(AgentRunTraceReducer::reduceAudit)
        ?: AgentRunTraceReducer.missing(messageRunId)
    return copy(runTrace = restoredTrace)
}

internal fun mergeLoadedConversationMessages(
    loadedMessages: List<ChatMessage>,
    currentMessages: List<ChatMessage>,
    isStreaming: Boolean,
): List<ChatMessage> {
    if (!isStreaming) return loadedMessages
    if (currentMessages.isEmpty()) return loadedMessages
    val loadedIds = HashSet<String>(loadedMessages.size * 2)
    for (message in loadedMessages) {
        loadedIds.add(message.id)
    }
    val liveMessages = ArrayList<ChatMessage>(currentMessages.size)
    for (message in currentMessages) {
        if (message.id !in loadedIds) {
            liveMessages.add(message)
        }
    }
    return loadedMessages + liveMessages
}

internal fun shouldSendInitialQuestion(
    initialQuestion: String,
    consumedInitialQuestionKey: String?,
    nextInitialQuestionKey: String,
    isStreaming: Boolean,
): Boolean =
    initialQuestion.isNotBlank() && !isStreaming && consumedInitialQuestionKey != nextInitialQuestionKey

internal fun shouldReuseLoadedConversation(
    state: AgentChatUiState,
    conversationId: Long,
): Boolean =
    state.conversationId == conversationId && state.messages.isNotEmpty()

internal fun shouldCancelServerRunBeforeClearing(
    state: AgentChatUiState,
    chatJobActive: Boolean,
): Boolean = state.isStreaming || chatJobActive

internal fun ChatMessage.withSafetyBlockedResult(
    safetyResult: SafetyResult,
    errorMessage: String,
): ChatMessage =
    copy(
        isStreaming = false,
        isError = true,
        errorMessage = errorMessage,
        animateReveal = false,
        runTrace = runTrace?.copy(safetyResult = safetyResult)
            ?: RunTrace(
                runId = "unknown",
                safetyResult = safetyResult,
                isExpanded = true,
            ),
    )

private fun initialQuestionKey(
    conversationId: Long?,
    initialQuestion: String,
): String = "${conversationId ?: 0L}:${initialQuestion.trim()}"

private fun buildStoredMessageParts(
    content: String,
    blocks: List<ResultBlockDto>,
): List<ChatMessagePart> {
    val parts = ArrayList<ChatMessagePart>(if (content.isNotBlank()) blocks.size + 1 else blocks.size)
    if (content.isNotBlank()) {
        parts += ChatMessagePart.Text(content)
    }
    for (block in blocks) {
        parts += ChatMessagePart.ResultBlock(block)
    }
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

internal fun String.withAuthoritativeAnswerIfVisible(
    answer: String?,
    hasServerAnswerDelta: Boolean,
): String =
    if (hasServerAnswerDelta || isNotBlank()) {
        withAuthoritativeAnswer(answer)
    } else {
        this
    }

internal fun List<ChatMessagePart>.appendStreamingText(
    delta: String,
    pendingBlocks: List<ResultBlockDto> = emptyList(),
): List<ChatMessagePart> {
    if (delta.isBlank()) return this
    if (none { it is ChatMessagePart.Text }) {
        val resultBlocks = ArrayList<ChatMessagePart>(pendingBlocks.size)
        if (pendingBlocks.isNotEmpty()) {
            for (block in pendingBlocks) {
                resultBlocks.add(ChatMessagePart.ResultBlock(block))
            }
        } else {
            val seenBlocks = HashSet<ResultBlockDto>()
            for (part in this) {
                if (part is ChatMessagePart.PendingResultBlock && seenBlocks.add(part.block)) {
                    resultBlocks.add(ChatMessagePart.ResultBlock(part.block))
                }
            }
        }
        return buildList(1 + resultBlocks.size) {
            add(ChatMessagePart.Text(delta))
            addAll(resultBlocks)
        }
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
        if (containsResultBlock(block)) {
            return this
        }
        return this + ChatMessagePart.PendingResultBlock(block)
    }
    if (any { it is ChatMessagePart.ResultBlock && it.block == block }) {
        return this
    }
    val pendingIndex = indexOfFirst { it is ChatMessagePart.PendingResultBlock && it.block == block }
    if (pendingIndex >= 0) {
        return toMutableList().also { parts ->
            parts[pendingIndex] = ChatMessagePart.ResultBlock(block)
        }
    }
    return this + ChatMessagePart.ResultBlock(block)
}

internal fun List<ResultBlockDto>.appendDistinctResultBlock(block: ResultBlockDto): List<ResultBlockDto> =
    if (contains(block)) this else this + block

internal fun List<ChatMessagePart>.withAuthoritativeText(
    content: String,
    pendingBlocks: List<ResultBlockDto> = emptyList(),
): List<ChatMessagePart> {
    if (content.isBlank()) return promotePendingResultBlocks()
    val textIndexes = mapIndexedNotNull { index, part ->
        if (part is ChatMessagePart.Text) index else null
    }
    if (textIndexes.isEmpty()) {
        val resultParts = ArrayList<ChatMessagePart>(pendingBlocks.size + size)
        val seenBlocks = HashSet<ResultBlockDto>()
        if (pendingBlocks.isNotEmpty()) {
            for (block in pendingBlocks) {
                if (seenBlocks.add(block)) {
                    resultParts.add(ChatMessagePart.ResultBlock(block))
                }
            }
        } else {
            for (part in this) {
                when (part) {
                    is ChatMessagePart.PendingResultBlock -> if (seenBlocks.add(part.block)) {
                        resultParts.add(ChatMessagePart.ResultBlock(part.block))
                    }
                    is ChatMessagePart.ResultBlock -> if (seenBlocks.add(part.block)) {
                        resultParts.add(ChatMessagePart.ResultBlock(part.block))
                    }
                    is ChatMessagePart.Text -> Unit
                }
            }
        }
        return buildList(1 + resultParts.size) {
            add(ChatMessagePart.Text(content))
            addAll(resultParts)
        }
    }
    if (textIndexes.size > 1) {
        return mergeAuthoritativeTextAcrossTimeline(content, textIndexes)
    }
    val firstTextIndex = textIndexes.first()
    val currentText = this[firstTextIndex] as ChatMessagePart.Text
    return when {
        currentText.markdown == content -> promotePendingResultBlocks()
        content.startsWith(currentText.markdown) -> {
            toMutableList().also { parts ->
                parts[firstTextIndex] = currentText.copy(markdown = content)
            }.promotePendingResultBlocks()
        }
        else -> promotePendingResultBlocks()
    }
}

private fun List<ChatMessagePart>.containsResultBlock(block: ResultBlockDto): Boolean =
    any { part ->
        when (part) {
            is ChatMessagePart.ResultBlock -> part.block == block
            is ChatMessagePart.PendingResultBlock -> part.block == block
            is ChatMessagePart.Text -> false
        }
    }

private fun List<ChatMessagePart>.promotePendingResultBlocks(): List<ChatMessagePart> {
    if (none { it is ChatMessagePart.PendingResultBlock }) return this
    val promoted = ArrayList<ChatMessagePart>(size)
    for (part in this) {
        promoted.add(
            when (part) {
                is ChatMessagePart.PendingResultBlock -> ChatMessagePart.ResultBlock(part.block)
                else -> part
            }
        )
    }
    return promoted.distinctResultBlocks()
}

private fun List<ChatMessagePart>.distinctResultBlocks(): List<ChatMessagePart> {
    val seenBlocks = mutableSetOf<ResultBlockDto>()
    val distinctParts = ArrayList<ChatMessagePart>(size)
    for (part in this) {
        when (part) {
            is ChatMessagePart.ResultBlock -> if (seenBlocks.add(part.block)) distinctParts.add(part)
            else -> distinctParts.add(part)
        }
    }
    return distinctParts
}

private fun List<ChatMessagePart>.mergeAuthoritativeTextAcrossTimeline(
    content: String,
    textIndexes: List<Int>,
): List<ChatMessagePart> {
    val visibleBuilder = StringBuilder()
    for (index in textIndexes) {
        visibleBuilder.append((this[index] as ChatMessagePart.Text).markdown)
    }
    val visibleText = visibleBuilder.toString()
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

    fun closeOpenToolCalls(
        status: String,
        resultSummary: String,
        completedAt: Long = System.currentTimeMillis(),
    ) {
        toolsCalled.replaceAll { tool ->
            if (tool.status == "running" || tool.status == "pending") {
                tool.copy(
                    status = status,
                    resultSummary = tool.resultSummary?.takeIf { it.isNotBlank() } ?: resultSummary,
                    completedAt = tool.completedAt ?: completedAt,
                    timestamp = completedAt,
                )
            } else {
                tool
            }
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
