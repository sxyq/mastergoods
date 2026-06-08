package com.zhihuiji.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.agent.AgentNotificationDto
import com.zhihuiji.core.model.v2.agent.AgentTaskDto
import com.zhihuiji.data.agent.AgentV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskItem(
    val id: Long,
    val taskType: String,
    val title: String,
    val status: String,
    val statusLabel: String,
    val progress: Int,
    val createdAt: Long,
)

data class NotificationItem(
    val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long,
)

data class TaskNotificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasks: List<TaskItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val selectedTab: Int = 0,
)

@HiltViewModel
class TaskNotificationViewModel @Inject constructor(
    private val repository: AgentV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskNotificationUiState())
    val uiState: StateFlow<TaskNotificationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tasksDeferred = async { repository.listTasks() }
            val notificationsDeferred = async { repository.listNotifications() }
            val tasksResult = tasksDeferred.await()
            val notificationsResult = notificationsDeferred.await()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    tasks = tasksResult.getOrDefault(emptyList()).map { dto -> dto.toTaskItem() },
                    notifications = notificationsResult.getOrDefault(emptyList()).map { dto -> dto.toNotificationItem() },
                    error = tasksResult.exceptionOrNull()?.message ?: notificationsResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map {
                                if (it.id == notificationId) updated.toNotificationItem() else it
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "通知已读更新失败") }
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val unreadIds = _uiState.value.notifications.filterNot { it.isRead }.map { it.id }
            val results = unreadIds.map { id -> async { repository.markNotificationRead(id) } }.awaitAll()
            val lastError = results.firstNotNullOfOrNull { result ->
                result.exceptionOrNull()?.message
            }
            if (lastError == null) {
                loadData()
            } else {
                _uiState.update { it.copy(error = lastError) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun AgentTaskDto.toTaskItem(): TaskItem = TaskItem(
    id = id,
    taskType = taskType,
    title = title,
    status = status,
    statusLabel = statusLabel,
    progress = progress,
    createdAt = createdAt,
)

private fun AgentNotificationDto.toNotificationItem(): NotificationItem = NotificationItem(
    id = id,
    type = level,
    title = title,
    content = body,
    isRead = isRead,
    createdAt = createdAt,
)
