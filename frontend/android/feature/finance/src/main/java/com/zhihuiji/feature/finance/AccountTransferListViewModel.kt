package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.finance.AccountTransferCreateV2Request
import com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.data.finance.AccountV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountTransferItem(
    val id: Long,
    val transferNo: String,
    val fromAccountName: String,
    val toAccountName: String,
    val amount: String,
    val fee: String,
    val statusLabel: String,
    val date: String,
    val notes: String?,
)

data class AccountTransferListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val transfers: List<AccountTransferItem> = emptyList(),
)

@HiltViewModel
class AccountTransferListViewModel @Inject constructor(
    private val repository: AccountV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountTransferListUiState())
    val uiState: StateFlow<AccountTransferListUiState> = _uiState.asStateFlow()

    init {
        loadTransfers()
    }

    fun loadTransfers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listTransfers()
                .onSuccess { list ->
                    val items = list.map { it.toTransferItem() }
                    _uiState.update { it.copy(isLoading = false, transfers = items) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "转账记录加载失败")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun AccountTransferV2Dto.toTransferItem(): AccountTransferItem = AccountTransferItem(
    id = id,
    transferNo = transferNo,
    fromAccountName = fromAccountName,
    toAccountName = toAccountName,
    amount = MoneyFormatter.format(amount),
    fee = fee?.takeIf { it > 0.0 }?.let { MoneyFormatter.format(it) } ?: "—",
    statusLabel = when (status) {
        1 -> "已完成"
        0 -> "处理中"
        else -> "未知"
    },
    date = TimeFormatter.formatDateTime(createdAt),
    notes = notes,
)

data class AccountSelectOption(
    val id: Long,
    val label: String,
)

data class AccountTransferUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val accounts: List<AccountSelectOption> = emptyList(),
    val fromAccountId: Long? = null,
    val fromAccountLabel: String = "",
    val toAccountId: Long? = null,
    val toAccountLabel: String = "",
    val amount: String = "",
    val fee: String = "",
    val notes: String = "",
) {
    val parsedAmount: Double?
        get() = amount.trim().replace(",", "").toDoubleOrNull()

    val parsedFee: Double?
        get() = fee.trim().replace(",", "").toDoubleOrNull()

    val canSubmit: Boolean
        get() = !isSaving && fromAccountId != null && toAccountId != null &&
            fromAccountId != toAccountId && (parsedAmount ?: 0.0) > 0.0
}

@HiltViewModel
class AccountTransferViewModel @Inject constructor(
    private val repository: AccountV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountTransferUiState())
    val uiState: StateFlow<AccountTransferUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listAccounts()
                .onSuccess { list ->
                    val options = list.filter { it.status == 1 }.map { it.toSelectOption() }
                    _uiState.update { it.copy(isLoading = false, accounts = options) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "账户加载失败")
                    }
                }
        }
    }

    fun selectFromAccount(option: AccountSelectOption) {
        _uiState.update {
            it.copy(fromAccountId = option.id, fromAccountLabel = option.label, error = null)
        }
    }

    fun selectToAccount(option: AccountSelectOption) {
        _uiState.update {
            it.copy(toAccountId = option.id, toAccountLabel = option.label, error = null)
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value, error = null) }
    }

    fun updateFee(value: String) {
        _uiState.update { it.copy(fee = value, error = null) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (!current.canSubmit) return
        val fromId = current.fromAccountId ?: return
        val toId = current.toAccountId ?: return
        val amount = current.parsedAmount ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val request = AccountTransferCreateV2Request(
                fromAccountId = fromId,
                toAccountId = toId,
                amount = amount,
                fee = current.parsedFee?.takeIf { it > 0.0 },
                notes = current.notes.trim().takeIf { it.isNotEmpty() },
            )
            repository.createTransfer(request)
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "转账提交失败")
                    }
                }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

private fun AccountV2Dto.toSelectOption(): AccountSelectOption = AccountSelectOption(
    id = id,
    label = "$name（${accountTypeLabel(type)}）",
)
