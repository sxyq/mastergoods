package com.zhihuiji.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.finance.AccountV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.finance.FinanceV2Repository
import com.zhihuiji.data.order.SaleOrderV2Repository
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 待验证：客户端聚合，非服务端聚合，数据准确性待验证
data class DashboardUiState(
    val isLoading: Boolean = false,
    val saleOrders: List<SaleOrderV2Dto> = emptyList(),
    val accounts: List<AccountV2Dto> = emptyList(),
    val lowStockProducts: List<ProductV2Dto> = emptyList(),
    val error: UiMessage? = null,
) {
    val totalSalesAmount: Double get() = saleOrders.sumOf { it.totalAmount }
    val totalPaidAmount: Double get() = saleOrders.sumOf { it.paidAmount }
    val totalUnpaidAmount: Double get() = totalSalesAmount - totalPaidAmount
    val totalAccountBalance: Double get() = accounts.sumOf { it.balance }
    val lowStockCount: Int get() = lowStockProducts.size
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val saleOrderV2Repository: SaleOrderV2Repository,
    private val financeV2Repository: FinanceV2Repository,
    private val productV2Repository: ProductV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                coroutineScope {
                    val deferreds = listOf(
                        async {
                            saleOrderV2Repository.listSaleOrders().onSuccess { orders ->
                                _uiState.value = _uiState.value.copy(saleOrders = orders)
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
