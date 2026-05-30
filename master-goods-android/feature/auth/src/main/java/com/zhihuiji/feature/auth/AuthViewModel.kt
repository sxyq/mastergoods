package com.zhihuiji.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.model.UserProfile
import com.zhihuiji.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSessionReady: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userProfile: UserProfile? = null,
    val error: UiMessage? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val isLoggedInState = sessionStore.isLoggedIn.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            isLoggedInState.collect { logged ->
                _uiState.value = _uiState.value.copy(isSessionReady = true, isLoggedIn = logged)
            }
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(phone, password)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it)) }
        }
    }

    fun register(phone: String, password: String, inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(phone, password, inviteCode)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it)) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun loadMe() {
        viewModelScope.launch {
            authRepository.fetchCurrentUser()
                .onSuccess { _uiState.value = _uiState.value.copy(userProfile = it) }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun restoreSession() {
        viewModelScope.launch {
            val restored = authRepository.restoreSessionIfNeeded()
            if (!restored) {
                authRepository.clearSession()
            }
            _uiState.value = _uiState.value.copy(isLoggedIn = restored)
        }
    }
}
