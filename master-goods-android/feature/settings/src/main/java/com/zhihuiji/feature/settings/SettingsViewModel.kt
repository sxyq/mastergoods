package com.zhihuiji.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.data.auth.AuthRepository
import com.zhihuiji.data.sync.SyncV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val accountSubtitle: String = "正在读取真实登录状态",
    val isLoggedIn: Boolean = false,
    val serverUrl: String = "",
    val clientId: String = "",
    val syncStatus: String = "正在读取真实同步状态",
    val syncBadge: String = "",
    val importStatus: String = "正在查询真实导入任务",
    val isSyncing: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsStore: SettingsStore,
    private val syncRepository: SyncV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeLocalSettings()
        loadAccount()
        refreshSyncStatus()
    }

    private fun observeLocalSettings() {
        viewModelScope.launch {
            combine(settingsStore.baseUrl, settingsStore.clientId) { baseUrl, clientId ->
                baseUrl to clientId
            }.collectLatest { (baseUrl, clientId) ->
                _uiState.update {
                    it.copy(
                        serverUrl = baseUrl,
                        clientId = clientId.ifBlank { "未生成" },
                    )
                }
            }
        }
    }

    fun loadAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val loggedIn = authRepository.isLoggedInSnapshot()
            if (!loggedIn) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        userName = "",
                        accountSubtitle = "未登录，无法读取账号资料",
                    )
                }
                return@launch
            }

            val profile = authRepository.fetchCurrentUser()
            _uiState.update { current ->
                profile.fold(
                    onSuccess = { user ->
                        current.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userName = user.nickname.ifBlank { user.phone },
                            accountSubtitle = "账号 ${user.phone} · ID ${user.id}",
                            error = null,
                        )
                    },
                    onFailure = { throwable ->
                        current.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userName = "已登录账号",
                            accountSubtitle = "会话有效，账号资料读取失败",
                            error = throwable.message ?: "账号资料读取失败",
                        )
                    },
                )
            }
        }
    }

    fun saveBaseUrl(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { settingsStore.saveBaseUrl(url) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            syncStatus = "服务器地址已保存，等待重新同步",
                        )
                    }
                    refreshSyncStatus()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "服务器地址保存失败",
                        )
                    }
                }
        }
    }

    fun refreshSyncStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            val clientId = runCatching { settingsStore.ensureClientId() }
                .getOrElse { throwable ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncBadge = "异常",
                            syncStatus = throwable.message ?: "客户端 ID 读取失败",
                        )
                    }
                    return@launch
                }

            val healthDeferred = async { syncRepository.health() }
            val cursorDeferred = async { syncRepository.cursor(clientId) }
            val importJobsDeferred = async { syncRepository.listImportJobs() }
            val healthResult = healthDeferred.await()
            val cursorResult = cursorDeferred.await()
            val importJobsResult = importJobsDeferred.await()

            _uiState.update { current ->
                val health = healthResult.getOrNull()
                val cursor = cursorResult.getOrNull()
                val importJobs = importJobsResult.getOrNull().orEmpty()
                val latestJob = importJobs.maxByOrNull { it.updatedAt }

                val syncStatus = when {
                    healthResult.isFailure -> healthResult.exceptionOrNull()?.message ?: "同步健康检查失败"
                    cursorResult.isFailure -> cursorResult.exceptionOrNull()?.message ?: "同步游标读取失败"
                    cursor?.updatedAt?.takeIf { it > 0L } != null ->
                        "上次同步：${formatTimestamp(cursor.updatedAt)}"
                    else -> "同步服务 ${health?.status.orEmpty().ifBlank { "可访问" }}，暂无游标"
                }
                val importStatus = when {
                    importJobsResult.isFailure -> importJobsResult.exceptionOrNull()?.message ?: "导入任务读取失败"
                    latestJob != null -> "最近任务 #${latestJob.id} · ${latestJob.status.ifBlank { "未知状态" }}"
                    else -> "暂无真实导入任务"
                }

                current.copy(
                    isSyncing = false,
                    clientId = clientId,
                    syncBadge = health?.status.orEmpty().ifBlank { if (healthResult.isSuccess) "在线" else "异常" },
                    syncStatus = syncStatus,
                    importStatus = importStatus,
                    error = healthResult.exceptionOrNull()?.message
                        ?: cursorResult.exceptionOrNull()?.message
                        ?: importJobsResult.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun runSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null, syncStatus = "正在执行真实同步") }
            val clientId = runCatching { settingsStore.ensureClientId() }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncBadge = "异常",
                        syncStatus = throwable.message ?: "客户端 ID 读取失败",
                        error = throwable.message,
                    )
                }
                return@launch
            }

            syncRepository.pullApplyAndAck(clientId, limit = 100)
                .onSuccess { cursor ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncBadge = "已同步",
                            syncStatus = if (cursor.isBlank()) "同步完成，服务端暂无新游标" else "同步完成：$cursor",
                        )
                    }
                    refreshSyncStatus()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncBadge = "失败",
                            syncStatus = throwable.message ?: "同步失败",
                            error = throwable.message ?: "同步失败",
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { authRepository.logout() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            userName = "",
                            accountSubtitle = "已退出登录",
                            syncBadge = "未登录",
                            syncStatus = "退出后已清理本地会话与同步缓存",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "退出登录失败",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private suspend fun AuthRepository.isLoggedInSnapshot(): Boolean =
    isLoggedIn.first()

private fun formatTimestamp(value: Long): String {
    val millis = if (value < 10_000_000_000L) value * 1000 else value
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
}
