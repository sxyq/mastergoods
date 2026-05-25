package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.SaleOrderDto
import com.zhihuiji.core.model.SaleOrderFilter
import com.zhihuiji.data.order.SaleOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleOrderListUiState(
    val orders: List<SaleOrderDto> = emptyList(),
    val filter: SaleOrderFilter = SaleOrderFilter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class SaleOrderListViewModel @Inject constructor(
    private val saleOrderRepository: SaleOrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleOrderListUiState())
    val uiState: StateFlow<SaleOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: SaleOrderFilter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            saleOrderRepository.refreshSaleOrders(filter)
            saleOrderRepository.observeSaleOrders(filter).collect { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }
        }
    }

    fun updateFilter(keyword: String? = null, status: Int? = null) {
        loadOrders(_uiState.value.filter.copy(keyword = keyword, status = status))
    }
}
