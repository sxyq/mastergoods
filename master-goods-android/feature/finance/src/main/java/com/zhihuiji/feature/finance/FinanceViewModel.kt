package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.finance.AccountCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.data.finance.FinanceV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO(B09): finance_records 写入路径已废弃，后续由 account-based 流程替代
data class FinanceListUiState(
    val accounts: List<AccountV2Dto> = emptyList(),
    val transfers: List<AccountTransferV2Dto> = emptyList(),
    val isLoading: Boolean = false,
    val createSuccess: Boolean = false,
    val error: UiMessage? = null,
) {
    val totalBalance: Double get() = accounts.sumOf { it.balance }
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeV2Repository: FinanceV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FinanceListUiState())
    val uiState: StateFlow<FinanceListUiState> = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            financeV2Repository.listAccounts().onSuccess { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
            financeV2Repository.listTransfers().onSuccess { transfers ->
                _uiState.value = _uiState.value.copy(transfers = transfers)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun createAccount(code: String, name: String, type: Int, balance: Double?, notes: String?) {
        viewModelScope.launch {
            val request = AccountCreateV2Request(
                code = code,
                name = name,
                type = type,
                balance = balance,
                notes = notes,
            )
            financeV2Repository.createAccount(request).onSuccess {
                _uiState.value = _uiState.value.copy(createSuccess = true)
                loadData()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearCreateSuccess() { _uiState.value = _uiState.value.copy(createSuccess = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
