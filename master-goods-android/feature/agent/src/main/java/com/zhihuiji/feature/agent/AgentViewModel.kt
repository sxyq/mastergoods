package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.*
import com.zhihuiji.data.agent.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentUiState(
    val isLoading: Boolean = false,
    val workbench: AgentWorkbenchDto? = null,
    val answer: AgentAnswerDto? = null,
    val tasks: List<AgentTaskSummaryDto> = emptyList(),
    val notifications: List<AgentNotificationDto> = emptyList(),
    val query: String = "",
    val error: String? = null,
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init { loadWorkbench() }

    fun loadWorkbench() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            agentRepository.getWorkbench().onSuccess { _uiState.value = _uiState.value.copy(workbench = it) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun ask(question: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(query = question, isLoading = true)
            agentRepository.query(question).onSuccess { _uiState.value = _uiState.value.copy(answer = it, isLoading = false) }
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) }
        }
    }

    fun loadTasks() {
        viewModelScope.launch { agentRepository.listTasks().onSuccess { _uiState.value = _uiState.value.copy(tasks = it) } }
    }

    fun loadNotifications() {
        viewModelScope.launch { agentRepository.listNotifications().onSuccess { _uiState.value = _uiState.value.copy(notifications = it) } }
    }
}
