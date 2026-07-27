package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.model.v2.finance.AccountCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountUpdateV2Request
import com.zhihuiji.data.finance.AccountV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val code: String = "",
    val type: Int = 0,
    val typeName: String = "现金",
    val balance: String = "",
    val notes: String = "",
    val status: Int = 1,
) {
    val canSave: Boolean
        get() = !isSaving && name.isNotBlank() && code.isNotBlank()
}

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    private val repository: AccountV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountEditUiState())
    val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

    fun loadAccount(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getAccount(id)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            name = dto.name,
                            code = dto.code,
                            type = dto.type,
                            typeName = accountTypeLabel(dto.type),
                            balance = MoneyFormatter.formatWithoutSymbol(dto.balance),
                            notes = dto.notes.orEmpty(),
                            status = dto.status,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "账户加载失败")
                    }
                }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun updateCode(value: String) {
        _uiState.update { it.copy(code = value, error = null) }
    }

    fun updateBalance(value: String) {
        _uiState.update { it.copy(balance = value, error = null) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun selectType(option: AccountTypeOption) {
        _uiState.update { it.copy(type = option.code, typeName = option.label, error = null) }
    }

    fun toggleStatus() {
        _uiState.update { it.copy(status = if (it.status == 1) 0 else 1) }
    }

    fun save(id: Long?) {
        val current = _uiState.value
        if (!current.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            if (id != null) {
                val request = AccountUpdateV2Request(
                    code = current.code.trim(),
                    name = current.name.trim(),
                    type = current.type,
                    isDefault = null,
                    status = current.status,
                    sortOrder = null,
                    notes = current.notes.trim().takeIf { it.isNotEmpty() },
                )
                repository.updateAccount(id, request)
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isSaving = false, error = error.message ?: "账户更新失败")
                        }
                    }
            } else {
                val balance = current.balance.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                val request = AccountCreateV2Request(
                    code = current.code.trim(),
                    name = current.name.trim(),
                    type = current.type,
                    balance = balance,
                    isDefault = null,
                    status = current.status,
                    sortOrder = null,
                    notes = current.notes.trim().takeIf { it.isNotEmpty() },
                )
                repository.createAccount(request)
                    .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isSaving = false, error = error.message ?: "账户创建失败")
                        }
                    }
            }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
