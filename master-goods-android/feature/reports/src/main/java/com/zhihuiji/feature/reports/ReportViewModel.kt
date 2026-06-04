package com.zhihuiji.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.model.v2.inventory.InventoryMonthlyStatsV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.finance.FinanceV2Repository
import com.zhihuiji.data.order.SaleOrderV2Repository
import com.zhihuiji.data.product.ProductV2Repository
import com.zhihuiji.data.sync.SyncV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// 待验证：客户端聚合，非服务端聚合，数据准确性待验证
data class ReportUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: Int = 0,
    val saleOrders: List<SaleOrderV2Dto> = emptyList(),
    val accounts: List<AccountV2Dto> = emptyList(),
    val lowStockProducts: List<ProductV2Dto> = emptyList(),
    val inventoryStats: List<InventoryMonthlyStatsV2Dto> = emptyList(),
    val error: UiMessage? = null,
) {
    val totalSalesAmount: Double get() = saleOrders.sumOf { it.totalAmount }
    val totalPaidAmount: Double get() = saleOrders.sumOf { it.paidAmount }
    val totalUnpaidAmount: Double get() = totalSalesAmount - totalPaidAmount
    val totalAccountBalance: Double get() = accounts.sumOf { it.balance }
    val totalCostIn: Double get() = inventoryStats.sumOf { it.totalCostIn }
    val totalCostOut: Double get() = inventoryStats.sumOf { it.totalCostOut }
    val estimatedProfit: Double get() = totalSalesAmount - totalCostOut
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val saleOrderV2Repository: SaleOrderV2Repository,
    private val financeV2Repository: FinanceV2Repository,
    private val productV2Repository: ProductV2Repository,
    private val syncV2Repository: SyncV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init { loadReports() }

    fun loadReports(period: Int = _uiState.value.selectedPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedPeriod = period)
            try {
                coroutineScope {
                    val deferreds = listOf(
                        async {
                            saleOrderV2Repository.listSaleOrders().onSuccess { orders ->
                                _uiState.value = _uiState.value.copy(saleOrders = filterOrdersByPeriod(orders, period))
                            }.onFailure {
                                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                            }
                        },
                        async {
                            financeV2Repository.listAccounts().onSuccess { accounts ->
                                _uiState.value = _uiState.value.copy(accounts = accounts)
                            }.onFailure {
                                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                            }
                        },
                        async {
                            productV2Repository.listLowStockProducts().onSuccess { products ->
                                _uiState.value = _uiState.value.copy(lowStockProducts = products)
                            }.onFailure {
                                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                            }
                        },
                        async {
                            val cal = Calendar.getInstance()
                            syncV2Repository.listInventoryMonthlyStats(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1).onSuccess { stats ->
                                _uiState.value = _uiState.value.copy(inventoryStats = stats)
                            }.onFailure {
                                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                            }
                        },
                    )
                    deferreds.awaitAll()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(e))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setPeriod(period: Int) {
        if (period == _uiState.value.selectedPeriod) return
        loadReports(period)
    }

    private fun filterOrdersByPeriod(orders: List<SaleOrderV2Dto>, period: Int): List<SaleOrderV2Dto> {
        val now = System.currentTimeMillis()
        val startAt = when (period) {
            0 -> startOfToday(now)
            1 -> now - TimeUnit.DAYS.toMillis(7)
            2 -> now - TimeUnit.DAYS.toMillis(30)
            else -> startOfMonth(now)
        }
        return orders.filter { it.createdAt >= startAt }
    }

    private fun startOfToday(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startOfMonth(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
