package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.*
import com.zhihuiji.data.agent.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val workbench: AgentWorkbenchDto? = null,
    val answer: AgentAnswerDto? = null,
    val chatMessages: List<AgentChatMessage> = emptyList(),
    val draft: OperationDraftDto? = null,
    val submittedDraftResult: OperationSubmitResultDto? = null,
    val tasks: List<AgentTaskSummaryDto> = emptyList(),
    val selectedTask: AgentTaskDetailDto? = null,
    val notifications: List<AgentNotificationDto> = emptyList(),
    val query: String = "",
    val error: UiMessage? = null,
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var messageSeed = 0L

    init { loadWorkbench() }

    fun loadWorkbench() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.getWorkbench()
                .onSuccess { _uiState.value = _uiState.value.copy(workbench = it) }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it)) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
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
            agentRepository.query(question)
                .onSuccess { answer ->
                    appendChatMessage(
                        AgentChatMessage(
                            id = nextMessageId(),
                            role = ChatRole.ASSISTANT,
                            text = answer.answer,
                            timestampLabel = "刚刚",
                            answer = answer,
                        )
                    )
                    _uiState.value = _uiState.value.copy(answer = answer, isLoading = false)
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun generateOperationDraft(instruction: String) {
        viewModelScope.launch {
            val draftInstruction = instruction.trim()
            if (draftInstruction.isBlank()) return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, submittedDraftResult = null)
            agentRepository.generateOperationDraft(draftInstruction)
                .onSuccess { _uiState.value = _uiState.value.copy(draft = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun submitDraft() {
        viewModelScope.launch {
            val currentDraft = _uiState.value.draft ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.submitOperationDraft(currentDraft)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        submittedDraftResult = result,
                        isLoading = false,
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.listTasks()
                .onSuccess { _uiState.value = _uiState.value.copy(tasks = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun loadTaskDetail(taskId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.getTask(taskId)
                .onSuccess { _uiState.value = _uiState.value.copy(selectedTask = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun loadNotifications(unreadOnly: Boolean = false, undeliveredOnly: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.listNotifications(unreadOnly, undeliveredOnly)
                .onSuccess { _uiState.value = _uiState.value.copy(notifications = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it), isLoading = false) }
        }
    }

    fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            agentRepository.markNotificationRead(notificationId)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications.map {
                            if (it.id == notificationId) updated else it
                        },
                    )
                }
                .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it)) }
        }
    }

    private fun appendChatMessage(message: AgentChatMessage) {
        _uiState.value = _uiState.value.copy(chatMessages = _uiState.value.chatMessages + message)
    }

    private fun nextMessageId(): Long {
        messageSeed += 1
        return messageSeed
    }
}
