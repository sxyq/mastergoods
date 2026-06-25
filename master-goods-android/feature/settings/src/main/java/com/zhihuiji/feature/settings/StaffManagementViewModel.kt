package com.zhihuiji.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.CurrentStoreProfile
import com.zhihuiji.core.model.StoreStaffMember
import com.zhihuiji.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val currentStore: CurrentStoreProfile? = null,
    val staffMembers: List<StoreStaffMember> = emptyList(),
    val createPhone: String = "",
    val createNickname: String = "",
    val createPassword: String = "",
    val createRole: String = "SALES",
    val createTitle: String = "销售员工",
    val createStatus: Int = 1,
    val backendModeNote: String = "当前真实接口已切到 `/v2/stores/current` 与 `/v2/stores/current/members`，店员、角色、岗位与权限拦截统一由后端保存并执行。",
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

    fun updateCreateRole(value: String) {
        _uiState.update {
            it.copy(
                createRole = value,
                createTitle = if (it.createTitle.isBlank() || it.createTitle == defaultTitleForRole(it.createRole)) {
                    defaultTitleForRole(value)
                } else {
                    it.createTitle
                },
            )
        }
    }

    fun updateCreateTitle(value: String) {
        _uiState.update { it.copy(createTitle = value) }
    }

    fun updateCreateStatus(value: Int) {
        _uiState.update { it.copy(createStatus = value) }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = null) }
            val state = _uiState.value
            val keyword = state.searchKeyword.trim()
            val (storeResult, membersResult) = coroutineScope {
                val storeDeferred = async { authRepository.fetchCurrentStore() }
                val membersDeferred = async { authRepository.fetchStoreMembers() }
                storeDeferred.await() to membersDeferred.await()
            }
            val currentStore = storeResult.getOrNull()
            membersResult.fold(
                onSuccess = { users ->
                    val filtered = users
                        .sortedByDescending(StoreStaffMember::updatedAt)
                        .filter { member ->
                            keyword.isBlank()
                                || member.phone.contains(keyword)
                                || member.nickname.contains(keyword)
                                || member.title.contains(keyword)
                                || member.role.contains(keyword, ignoreCase = true)
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStore = currentStore,
                            staffMembers = filtered,
                            error = storeResult.exceptionOrNull()?.message,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentStore = currentStore,
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
            authRepository.createStoreMember(
                phone = phone,
                nickname = nickname,
                password = password,
                role = state.createRole,
                title = state.createTitle.trim().ifBlank { defaultTitleForRole(state.createRole) },
                status = state.createStatus,
            ).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createPhone = nextPhone(it.staffMembers),
                            createNickname = "",
                            createPassword = "",
                            createRole = "SALES",
                            createTitle = "销售员工",
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

    fun saveUser(user: StoreStaffMember, nickname: String, password: String, role: String, title: String, keepSessions: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            authRepository.updateStoreMember(
                userId = user.id,
                nickname = nickname.trim().ifBlank { user.nickname },
                password = password.trim().ifBlank { null },
                role = role,
                title = title.trim().ifBlank { defaultTitleForRole(role) },
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

    fun toggleUserStatus(user: StoreStaffMember, nickname: String? = null, role: String = user.role, title: String = user.title) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            val nextStatus = if (user.status == 1) 0 else 1
            authRepository.updateStoreMember(
                userId = user.id,
                nickname = nickname?.trim().takeUnless { it.isNullOrBlank() } ?: user.nickname,
                password = null,
                role = role,
                title = title,
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

    private fun nextPhone(users: List<StoreStaffMember>): String {
        val maxSuffix = users
            .mapNotNull { user -> user.phone.takeLast(2).toIntOrNull() }
            .maxOrNull()
            ?: 7
        return "138000000${(maxSuffix + 1).toString().padStart(2, '0')}"
    }
}

private val defaultTitleByRole = mapOf(
    "MANAGER" to "店长助理",
    "SALES" to "销售员工",
    "PURCHASING" to "采购员工",
    "WAREHOUSE" to "仓库员工",
    "FINANCE" to "财务员工",
    "ASSISTANT" to "AI/只读助理",
)

internal fun defaultTitleForRole(role: String): String = defaultTitleByRole[role] ?: "店员"
