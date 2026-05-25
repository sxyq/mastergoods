package com.zhihuiji.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.*
import com.zhihuiji.data.report.ReportRepository
import com.zhihuiji.data.agent.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val workbench: AgentWorkbenchDto? = null,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
            reportRepository.salesSummary(sevenDaysAgo, now).onSuccess { summary ->
                _uiState.value = _uiState.value.copy(salesSummary = summary)
            }
            reportRepository.profitSummary(sevenDaysAgo, now).onSuccess { summary ->
                _uiState.value = _uiState.value.copy(profitSummary = summary)
            }
            reportRepository.lowStockProducts().onSuccess { products ->
                _uiState.value = _uiState.value.copy(lowStockProducts = products)
            }
            reportRepository.topReceivableCustomers().onSuccess { customers ->
                _uiState.value = _uiState.value.copy(topReceivables = customers)
            }
            agentRepository.getWorkbench().onSuccess { wb ->
                _uiState.value = _uiState.value.copy(workbench = wb)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() = loadDashboard()
}
