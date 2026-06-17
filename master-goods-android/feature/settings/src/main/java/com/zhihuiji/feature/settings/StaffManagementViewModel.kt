package com.zhihuiji.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.AdminUser
import com.zhihuiji.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffManagementUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val searchKeyword: String = "",
    val staffMembers: List<AdminUser> = emptyList(),
    val createPhone: String = "",
    val createNickname: String = "",
    val createPassword: String = "",
    val createStatus: Int = 1,
    val backendModeNote: String = "当前真实接口对应 `/v1/admin/users`，先承接真实账号/店员管理；store/member 级角色绑定仍待后端补齐。",
)

@HiltViewModel
class StaffManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffManagementUiState())
    val uiState: StateFlow<StaffManagementUiState> = _uiState.asStateFlow()

    init {
        refreshUsers()
    }

    fun updateSearchKeyword(value: String) {
        _uiState.update { it.copy(searchKeyword = value) }
    }

    fun updateCreatePhone(value: String) {
        _uiState.update { it.copy(createPhone = value) }
    }

    fun updateCreateNickname(value: String) {
        _uiState.update { it.copy(createNickname = value) }
    }

    fun updateCreatePassword(value: String) {
        _uiState.update { it.copy(createPassword = value) }
    }

    fun updateCreateStatus(value: Int) {
        _uiState.update { it.copy(createStatus = value) }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = null) }
            val state = _uiState.value
            authRepository.fetchAdminUsers(
                keyword = state.searchKeyword.trim().ifBlank { null },
                page = 0,
                size = 200,
            ).fold(
                onSuccess = { users ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            staffMembers = users.sortedByDescending(AdminUser::updatedAt),
                            error = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "真实店员列表读取失败",
                        )
                    }
                },
            )
        }
    }

    fun createUser() {
        val state = _uiState.value
        val phone = state.createPhone.trim()
        val nickname = state.createNickname.trim()
        val password = state.createPassword.trim()
        if (phone.isBlank() || nickname.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "手机号、昵称和初始密码都需要填写", success = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            authRepository.createAdminUser(
                phone = phone,
                nickname = nickname,
                password = password,
                status = state.createStatus,
            ).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createPhone = nextPhone(it.staffMembers),
                            createNickname = "",
                            createPassword = "",
                            createStatus = 1,
                            success = "真实店员“${user.nickname}”已创建",
                            error = null,
                        )
                    }
                    refreshUsers()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "真实店员创建失败",
                        )
                    }
                },
            )
        }
    }

    fun saveUser(user: AdminUser, nickname: String, password: String, keepSessions: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            authRepository.updateAdminUser(
                userId = user.id,
                nickname = nickname.trim().ifBlank { user.nickname },
                password = password.trim().ifBlank { null },
                status = user.status,
                keepSessions = keepSessions,
            ).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            success = "真实店员“${updated.nickname}”已更新",
                            error = null,
                        )
                    }
                    refreshUsers()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "真实店员更新失败",
                        )
                    }
                },
            )
        }
    }

    fun toggleUserStatus(user: AdminUser, nickname: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            val nextStatus = if (user.status == 1) 0 else 1
            authRepository.updateAdminUser(
                userId = user.id,
                nickname = nickname?.trim().takeUnless { it.isNullOrBlank() } ?: user.nickname,
                password = null,
                status = nextStatus,
                keepSessions = true,
            ).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            success = "真实店员“${updated.nickname}”已${if (nextStatus == 1) "启用" else "停用"}",
                            error = null,
                        )
                    }
                    refreshUsers()
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = throwable.message ?: "店员状态更新失败",
                        )
                    }
                },
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, success = null) }
    }

    private fun nextPhone(users: List<AdminUser>): String {
        val maxSuffix = users
            .mapNotNull { user -> user.phone.takeLast(2).toIntOrNull() }
            .maxOrNull()
            ?: 7
        return "138000000${(maxSuffix + 1).toString().padStart(2, '0')}"
    }
}
