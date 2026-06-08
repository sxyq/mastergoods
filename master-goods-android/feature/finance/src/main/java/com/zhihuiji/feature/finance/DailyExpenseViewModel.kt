package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.model.CreateFinanceRecordRequest
import com.zhihuiji.data.finance.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DailyExpenseUiState(
    val amount: String = "",
    val category: String = "房租",
    val partnerName: String = "",
    val method: Int = StatusLabels.Codes.METHOD_CASH,
    val notes: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdRecordId: Long? = null,
) {
    val parsedAmount: Double?
        get() = amount.trim().replace(",", "").toDoubleOrNull()

    val amountText: String
        get() = "¥%.2f".format(parsedAmount?.takeIf { it > 0.0 } ?: 0.0)

    val canSubmit: Boolean
        get() = !isSaving && (parsedAmount ?: 0.0) > 0.0 && category.isNotBlank()
}

@HiltViewModel
class DailyExpenseViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DailyExpenseUiState())
    val uiState: StateFlow<DailyExpenseUiState> = _uiState.asStateFlow()

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value, error = null) }
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(category = value, error = null) }
    }

    fun selectCategory(value: String) {
        _uiState.update { it.copy(category = value, error = null) }
    }

    fun updatePartnerName(value: String) {
        _uiState.update { it.copy(partnerName = value, error = null) }
    }

    fun selectMethod(value: Int) {
        _uiState.update { it.copy(method = value, error = null) }
    }

    fun updateNotes(value: String) {
        _uiState.update { it.copy(notes = value, error = null) }
    }

    fun submit() {
        val current = _uiState.value
        val amount = current.parsedAmount
        when {
            current.isSaving -> return
            amount == null || amount <= 0.0 -> {
                _uiState.update { it.copy(error = "支出金额必须大于0") }
                return
            }
            current.category.isBlank() -> {
                _uiState.update { it.copy(error = "支出分类不能为空") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            repository.createFinanceRecord(
                CreateFinanceRecordRequest(
                    type = StatusLabels.Codes.FINANCE_EXPENSE,
                    category = current.category.trim(),
                    partnerName = current.partnerName.trim().takeIf { it.isNotEmpty() },
                    amount = amount,
                    method = current.method,
                    notes = current.notes.trim().takeIf { it.isNotEmpty() },
                )
            ).fold(
                onSuccess = { record ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            createdRecordId = record.id,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "日常支出保存失败",
                        )
                    }
                },
            )
        }
    }

    fun onCreatedNavigationHandled() {
        _uiState.update { it.copy(createdRecordId = null) }
    }
}
