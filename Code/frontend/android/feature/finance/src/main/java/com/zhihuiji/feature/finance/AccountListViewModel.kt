package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.data.finance.AccountV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 账户类型码与后端 AccountHealthLookupTool.accountTypeLabel 对齐
internal val ACCOUNT_TYPE_LABELS = mapOf(
    0 to "现金",
    1 to "银行",
    2 to "支付宝",
    3 to "微信",
)

internal fun accountTypeLabel(type: Int): String = ACCOUNT_TYPE_LABELS[type] ?: "其他"

data class AccountTypeOption(val code: Int, val label: String)

val ACCOUNT_TYPE_OPTIONS: List<AccountTypeOption> = listOf(
    AccountTypeOption(0, "现金"),
    AccountTypeOption(1, "银行"),
    AccountTypeOption(2, "支付宝"),
    AccountTypeOption(3, "微信"),
)

data class AccountItem(
    val id: Long,
    val code: String,
    val name: String,
    val typeLabel: String,
    val balance: String,
    val isDefault: Boolean,
    val statusLabel: String,
    val notes: String?,
)

data class AccountListUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val accounts: List<AccountItem> = emptyList(),
)

@HiltViewModel
class AccountListViewModel @Inject constructor(
    private val repository: AccountV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountListUiState())
    val uiState: StateFlow<AccountListUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listAccounts()
                .onSuccess { list ->
                    val items = list.map { it.toAccountItem() }
                    _uiState.update { it.copy(isLoading = false, accounts = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "账户加载失败")
                    }
                }
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            repository.deleteAccount(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeleting = false,
                            accounts = state.accounts.filterNot { it.id == id },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isDeleting = false, error = error.message ?: "账户删除失败")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun AccountV2Dto.toAccountItem(): AccountItem = AccountItem(
    id = id,
    code = code,
    name = name,
    typeLabel = accountTypeLabel(type),
    balance = MoneyFormatter.format(balance),
    isDefault = isDefault,
    statusLabel = if (status == 1) "启用" else "停用",
    notes = notes,
)
