package com.zhihuiji.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.*
import com.zhihuiji.data.report.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val isLoading: Boolean = false,
    val salesSummary: SalesSummaryReportDto? = null,
    val profitSummary: ProfitSummaryReportDto? = null,
    val reconciliation: ReconciliationSummaryReportDto? = null,
    val lowStockProducts: List<LowStockProductReportDto> = emptyList(),
    val topProducts: List<TopSellingProductReportDto> = emptyList(),
    val topReceivables: List<CustomerReceivableReportDto> = emptyList(),
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init { loadReports() }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000L
            reportRepository.salesSummary(thirtyDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(salesSummary = it) }
            reportRepository.profitSummary(thirtyDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(profitSummary = it) }
            reportRepository.reconciliationSummary(thirtyDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(reconciliation = it) }
            reportRepository.lowStockProducts().onSuccess { _uiState.value = _uiState.value.copy(lowStockProducts = it) }
            reportRepository.topProducts(thirtyDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(topProducts = it) }
            reportRepository.topReceivableCustomers().onSuccess { _uiState.value = _uiState.value.copy(topReceivables = it) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
