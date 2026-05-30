package com.zhihuiji.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.*
import com.zhihuiji.data.report.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val salesSummary: SalesSummaryReportDto? = null,
    val profitSummary: ProfitSummaryReportDto? = null,
    val lowStockProducts: List<LowStockProductReportDto> = emptyList(),
    val topReceivables: List<CustomerReceivableReportDto> = emptyList(),
    val error: UiMessage? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
            try {
                coroutineScope {
                    val deferreds = listOf(
                        async { reportRepository.salesSummary(sevenDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(salesSummary = it) } },
                        async { reportRepository.profitSummary(sevenDaysAgo, now).onSuccess { _uiState.value = _uiState.value.copy(profitSummary = it) } },
                        async { reportRepository.lowStockProducts().onSuccess { _uiState.value = _uiState.value.copy(lowStockProducts = it) } },
                        async { reportRepository.topReceivableCustomers().onSuccess { _uiState.value = _uiState.value.copy(topReceivables = it) } },
                    )
                    deferreds.awaitAll()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(e))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() = loadDashboard()
}
