package com.zhihuiji.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.model.SyncHealthResult
import com.zhihuiji.core.model.UserProfile
import com.zhihuiji.data.auth.AuthRepository
import com.zhihuiji.data.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
    val clientId: String = "",
    val userProfile: UserProfile? = null,
    val syncHealth: SyncHealthResult? = null,
    val isSyncing: Boolean = false,
    val isLoggedOut: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsStore: SettingsStore,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.baseUrl.combine(settingsStore.clientId) { url, id ->
                _uiState.value.copy(baseUrl = url, clientId = id)
            }.collect { _uiState.value = it }
        }
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val userDeferred = async { authRepository.fetchCurrentUser() }
                    val healthDeferred = async { syncRepository.healthCheck() }
                    userDeferred.await().onSuccess { _uiState.value = _uiState.value.copy(userProfile = it) }
                        .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it)) }
                    healthDeferred.await().onSuccess { _uiState.value = _uiState.value.copy(syncHealth = it) }
                        .onFailure { _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it)) }
                }
            } catch (_: Exception) {}
        }
    }

    fun saveBaseUrl(baseUrl: String) {
        viewModelScope.launch { settingsStore.saveBaseUrl(baseUrl) }
    }

    fun runManualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            syncRepository.runManualSync()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSyncing = false, error = null)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSyncing = false, error = UiMessage.fromThrowable(it))
                }
            syncRepository.healthCheck().onSuccess { _uiState.value = _uiState.value.copy(syncHealth = it) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(isLoggedOut = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
