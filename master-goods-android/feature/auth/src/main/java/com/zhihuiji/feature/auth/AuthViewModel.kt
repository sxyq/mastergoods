package com.zhihuiji.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isSessionReady: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(phone, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.message) }
                }
        }
    }

    fun register(phone: String, password: String, verifyCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(phone, password, verifyCode)
                .onSuccess {
                    _uiState.update { it.copy(isLoggedIn = true, isLoading = false) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isLoading = false, error = throwable.message) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.logout()
            _uiState.update {
                it.copy(isLoggedIn = false, isSessionReady = false, isLoading = false)
            }
        }
    }

    private suspend fun restoreSession() {
        val restored = authRepository.restoreSessionIfNeeded()
        _uiState.update { it.copy(isSessionReady = true, isLoggedIn = restored) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
