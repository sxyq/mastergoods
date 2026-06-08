package com.zhihuiji.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.FinanceFilter
import com.zhihuiji.core.model.FinanceRecordDto
import com.zhihuiji.data.finance.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val records: List<FinanceItem> = emptyList(),
    val keyword: String = "",
    val selectedTabIndex: Int = 0
)

data class FinanceItem(
    val id: Long,
    val recordNo: String,
    val title: String,
    val category: String,
    val account: String,
    val amount: String,
    val amountValue: Double,
    val type: String,
    val date: String,
    val updatedDate: String,
    val partnerName: String?,
    val notes: String?
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val repository: FinanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceListUiState())
    val uiState: StateFlow<FinanceListUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        loadRecords()
    }

    fun loadRecords() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeFinanceRecords(currentFilter())
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "财务记录加载失败"
                        )
                    }
                }
                .collect { records ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            records = records.map(FinanceRecordDto::toFinanceItem)
                        )
                    }
                }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.refreshFinanceRecords(currentFilter())
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "财务记录刷新失败"
                    )
                }
            }
        }
    }

    private fun currentFilter(): FinanceFilter =
        FinanceFilter(
            keyword = _uiState.value.keyword.takeIf { it.isNotBlank() },
            type = when (_uiState.value.selectedTabIndex) {
                1 -> StatusLabels.Codes.FINANCE_INCOME
                2 -> StatusLabels.Codes.FINANCE_EXPENSE
                else -> null
            }
        )

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadRecords()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadRecords()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun FinanceRecordDto.toFinanceItem(): FinanceItem {
    val typeLabel = StatusLabels.financeType(type)
    val methodLabel = StatusLabels.paymentMethod(method)
    val titleText = listOf(category, partnerName)
        .filter { !it.isNullOrBlank() }
        .joinToString("-")
        .ifBlank { recordNo }
    return FinanceItem(
        id = id,
        recordNo = recordNo,
        title = titleText,
        category = category,
        account = methodLabel,
        amount = "¥%.2f".format(amount),
        amountValue = amount,
        type = typeLabel,
        date = TimeFormatter.formatDate(createdAt),
        updatedDate = TimeFormatter.formatDate(updatedAt),
        partnerName = partnerName,
        notes = notes
    )
}
