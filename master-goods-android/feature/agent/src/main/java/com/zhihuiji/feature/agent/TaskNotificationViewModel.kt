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
    val tasksError: String? = null,
    val notificationsError: String? = null,
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
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    tasksError = null,
                    notificationsError = null,
                )
            }
            val tasksDeferred = async { repository.listTasks() }
            val notificationsDeferred = async { repository.listNotifications() }
            val tasksResult = tasksDeferred.await()
            val notificationsResult = notificationsDeferred.await()
            val tasksError = tasksResult.exceptionOrNull()?.message
            val notificationsError = notificationsResult.exceptionOrNull()?.message
            val taskDtos = tasksResult.getOrDefault(emptyList())
            val notificationDtos = notificationsResult.getOrDefault(emptyList())
            val taskItems = ArrayList<TaskItem>(taskDtos.size)
            for (dto in taskDtos) {
                taskItems.add(dto.toTaskItem())
            }
            val notificationItems = ArrayList<NotificationItem>(notificationDtos.size)
            for (dto in notificationDtos) {
                notificationItems.add(dto.toNotificationItem())
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    tasks = taskItems,
                    notifications = notificationItems,
                    tasksError = tasksError,
                    notificationsError = notificationsError,
                    error = tasksError ?: notificationsError,
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
                        val notifications = state.notifications.toMutableList()
                        for (index in notifications.indices) {
                            if (notifications[index].id == notificationId) {
                                notifications[index] = updated.toNotificationItem()
                                break
                            }
                        }
                        state.copy(
                            notifications = notifications,
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
            val notifications = _uiState.value.notifications
            val unreadIds = ArrayList<Long>(notifications.size)
            for (notification in notifications) {
                if (!notification.isRead) {
                    unreadIds.add(notification.id)
                }
            }
            val results = ArrayList<kotlinx.coroutines.Deferred<Result<AgentNotificationDto>>>(unreadIds.size)
            for (id in unreadIds) {
                results.add(async { repository.markNotificationRead(id) })
            }
            val completedResults = results.awaitAll()
            val lastError = completedResults.firstNotNullOfOrNull { result ->
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
