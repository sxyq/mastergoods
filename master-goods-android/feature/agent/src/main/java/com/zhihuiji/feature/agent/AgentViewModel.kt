package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.*
import com.zhihuiji.core.model.v2.agent.*
import com.zhihuiji.data.agent.AgentV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class AgentChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val timestampLabel: String = "",
    val answer: AgentAnswerDto? = null,
)

enum class ChatRole { USER, ASSISTANT }

data class AgentUiState(
    val isLoading: Boolean = false,
    // TODO: v2无workbench聚合端点，待后端补充后迁移；暂为null时UI展示占位数据
    val workbench: AgentWorkbenchDto? = null,
    val answer: AgentAnswerDto? = null,
    val chatMessages: List<AgentChatMessage> = emptyList(),
    val currentConversationId: Long? = null,
    val draft: AgentDraftDto? = null,
    val drafts: List<AgentDraftDto> = emptyList(),
    // TODO: v2无OperationSubmitResultDto，简化为String消息
    val submittedDraftResult: String? = null,
    // TODO: v2无task端点，待后端补充后迁移
    val tasks: List<AgentTaskSummaryDto> = emptyList(),
    // TODO: v2无task端点，待后端补充后迁移
    val selectedTask: AgentTaskDetailDto? = null,
    // TODO: v2无notification端点，待后端补充后迁移
    val notifications: List<AgentNotificationDto> = emptyList(),
    val query: String = "",
    val error: UiMessage? = null,
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentV2Repository: AgentV2Repository,
    // TODO: MediaV2Repository 待验证 - 存在但真实上传链路未验证
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var messageSeed = 0L
    private val json = Json { ignoreUnknownKeys = true }

    init { loadConversations() }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentV2Repository.listConversations()
                .onSuccess { conversations ->
                    val latestConv = conversations.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        currentConversationId = latestConv?.id,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it)) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun loadWorkbench() {
        // TODO: v2无workbench聚合端点，待后端补充后迁移；暂用conversation列表替代
        loadConversations()
    }

    fun ask(question: String) {
        viewModelScope.launch {
            val questionText = question.trim()
            if (questionText.isBlank()) return@launch
            appendChatMessage(
                AgentChatMessage(
                    id = nextMessageId(),
                    role = ChatRole.USER,
                    text = questionText,
                    timestampLabel = "刚刚",
                )
            )
            _uiState.value = _uiState.value.copy(query = questionText, isLoading = true)

            try {
                // v2 flow: createConversation → createMessage(role=user) → poll messages for AI response
                val convId = ensureConversation()

                agentV2Repository.createMessage(
                    conversationId = convId,
                    request = CreateAgentMessageRequest(
                        role = "user",
                        messageType = "text",
                        content = questionText,
                    ),
                ).onSuccess {
                    pollForAssistantResponse(convId)
                }.onFailure {
                    _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(e), isLoading = false)
            }
        }
    }

    private suspend fun ensureConversation(): Long {
        val existingId = _uiState.value.currentConversationId
        if (existingId != null) return existingId

        val conv = agentV2Repository.createConversation(
            CreateAgentConversationRequest(title = "新对话"),
        ).getOrThrow()

        _uiState.value = _uiState.value.copy(currentConversationId = conv.id)
        return conv.id
    }

    private suspend fun pollForAssistantResponse(conversationId: Long) {
        val maxAttempts = 30
        val pollIntervalMs = 1000L

        repeat(maxAttempts) {
            delay(pollIntervalMs)

            agentV2Repository.listMessages(conversationId)
                .onSuccess { messages ->
                    val shownMessageIds = _uiState.value.chatMessages.map { it.id }.toSet()
                    val newAssistantMessages = messages.filter {
                        it.role == "assistant" && it.id !in shownMessageIds
                    }

                    if (newAssistantMessages.isNotEmpty()) {
                        val latestAssistant = newAssistantMessages.last()
                        val answer = parseStructuredAnswer(latestAssistant)

                        appendChatMessage(
                            AgentChatMessage(
                                id = latestAssistant.id,
                                role = ChatRole.ASSISTANT,
                                text = latestAssistant.content,
                                timestampLabel = "刚刚",
                                answer = answer,
                            )
                        )
                        _uiState.value = _uiState.value.copy(answer = answer, isLoading = false)
                        return
                    }
                }
        }

        _uiState.value = _uiState.value.copy(
            error = UiMessage.fromThrowable(java.util.concurrent.TimeoutException("AI响应超时")),
            isLoading = false,
        )
    }

    private fun parseStructuredAnswer(message: AgentMessageDto): AgentAnswerDto? {
        val raw = message.structuredDataJson ?: return null
        return try { json.decodeFromString<AgentAnswerDto>(raw) } catch (_: Exception) { null }
    }

    fun generateOperationDraft(instruction: String) {
        viewModelScope.launch {
            val draftInstruction = instruction.trim()
            if (draftInstruction.isBlank()) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, submittedDraftResult = null)

            val convId = ensureConversation()
            val localDraftType = inferAgentDraftTypeFromInstruction(draftInstruction)
            agentV2Repository.createDraft(
                CreateAgentDraftRequest(
                    conversationId = convId,
                    draftType = localDraftType,
                    title = draftInstruction,
                    contentJson = "{}",
                    status = "draft",
                ),
            ).onSuccess { draft ->
                _uiState.value = _uiState.value.copy(
                    draft = draft,
                    drafts = mergeDraftIntoList(_uiState.value.drafts, draft),
                    isLoading = false,
                )
            }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun submitDraft() {
        viewModelScope.launch {
            val currentDraft = _uiState.value.draft ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentV2Repository.updateDraft(
                id = currentDraft.id,
                request = UpdateAgentDraftRequest(
                    conversationId = currentDraft.conversationId,
                    draftType = currentDraft.draftType,
                    title = currentDraft.title,
                    contentJson = currentDraft.contentJson,
                    status = "submitted",
                ),
            ).onSuccess { updatedDraft ->
                _uiState.value = _uiState.value.copy(
                    draft = updatedDraft,
                    drafts = mergeDraftIntoList(_uiState.value.drafts, updatedDraft),
                    submittedDraftResult = "草稿已提交，等待下一步处理。",
                    isLoading = false,
                )
            }.onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun loadDrafts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentV2Repository.listDrafts()
                .onSuccess { drafts ->
                    val currentDraftId = _uiState.value.draft?.id
                    val selectedDraft = when {
                        currentDraftId != null -> drafts.firstOrNull { it.id == currentDraftId }
                        else -> drafts.firstOrNull { it.status.equals("draft", ignoreCase = true) } ?: drafts.firstOrNull()
                    }
                    _uiState.value = _uiState.value.copy(
                        draft = selectedDraft,
                        drafts = drafts,
                        isLoading = false,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun loadTasks() {
        // TODO: v2无task端点，待后端补充后迁移
    }

    fun loadTaskDetail(taskId: Long) {
        // TODO: v2无task端点，待后端补充后迁移
    }

    fun loadNotifications(unreadOnly: Boolean = false, undeliveredOnly: Boolean = false) {
        // TODO: v2无notification端点，待后端补充后迁移
    }

    fun markNotificationRead(notificationId: Long) {
        // TODO: v2无notification端点，待后端补充后迁移
    }

    private fun appendChatMessage(message: AgentChatMessage) {
        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + message)
    }

    private fun nextMessageId(): Long {
        messageSeed += 1
        return messageSeed
    }
}

internal const val AGENT_DRAFT_CATEGORY_SALE = "sale"
internal const val AGENT_DRAFT_CATEGORY_PURCHASE = "purchase"
internal const val AGENT_DRAFT_CATEGORY_OTHER = "other"

private fun inferAgentDraftTypeFromInstruction(instruction: String): String = when (agentDraftCategoryKeyOf(instruction)) {
    AGENT_DRAFT_CATEGORY_SALE -> "sale"
    AGENT_DRAFT_CATEGORY_PURCHASE -> "purchase"
    else -> "operation"
}

internal fun agentDraftCategoryKeyOf(text: String): String {
    val normalized = text.lowercase()
    val saleKeywords = listOf("sale", "sales", "销售", "销货", "出库", "收款", "客户")
    val purchaseKeywords = listOf("purchase", "采购", "进货", "补货", "入库", "供应商")
    return when {
        saleKeywords.any(normalized::contains) -> AGENT_DRAFT_CATEGORY_SALE
        purchaseKeywords.any(normalized::contains) -> AGENT_DRAFT_CATEGORY_PURCHASE
        else -> AGENT_DRAFT_CATEGORY_OTHER
    }
}

private fun mergeDraftIntoList(
    drafts: List<AgentDraftDto>,
    draft: AgentDraftDto,
): List<AgentDraftDto> {
    val remaining = drafts.filterNot { it.id == draft.id }
    return listOf(draft) + remaining
}
