package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.CreateFinanceRecordRequest
import com.zhihuiji.core.model.FinanceRecordDto
import com.zhihuiji.core.model.FinanceFilter
import com.zhihuiji.data.finance.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceListUiState(
    val records: List<FinanceRecordDto> = emptyList(),
    val filter: FinanceFilter = FinanceFilter(),
    val isLoading: Boolean = false,
    val createSuccess: Boolean = false,
    val error: UiMessage? = null,
) {
    val totalIncome: Double get() = records.filter { it.type == 1 }.sumOf { it.amount }
    val totalExpense: Double get() = records.filter { it.type == 2 }.sumOf { it.amount }
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FinanceListUiState())
    val uiState: StateFlow<FinanceListUiState> = _uiState.asStateFlow()

    init { loadRecords() }

    fun loadRecords(filter: FinanceFilter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            financeRepository.refreshFinanceRecords(filter)
            financeRepository.observeFinanceRecords(filter).collectLatest { list ->
                _uiState.value = _uiState.value.copy(records = list, isLoading = false)
            }
        }
    }

    fun changeType(type: Int?) { loadRecords(_uiState.value.filter.copy(type = type)) }

    fun createRecord(type: Int, category: String, amount: Double, method: Int?, notes: String?) {
        viewModelScope.launch {
            val request = CreateFinanceRecordRequest(
                type = type,
                category = category,
                amount = amount,
                method = method,
                notes = notes,
            )
            financeRepository.createFinanceRecord(request).onSuccess {
                _uiState.value = _uiState.value.copy(createSuccess = true)
                loadRecords()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearCreateSuccess() { _uiState.value = _uiState.value.copy(createSuccess = false) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
