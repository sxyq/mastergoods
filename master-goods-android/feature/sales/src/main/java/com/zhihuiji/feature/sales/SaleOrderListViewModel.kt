package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Filter
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleOrderListUiState(
    val orders: List<SaleOrderV2Dto> = emptyList(),
    val filter: SaleOrderV2Filter = SaleOrderV2Filter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class SaleOrderListViewModel @Inject constructor(
    private val saleOrderV2Repository: SaleOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleOrderListUiState())
    val uiState: StateFlow<SaleOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: SaleOrderV2Filter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            saleOrderV2Repository.listSaleOrders(filter).onSuccess { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateFilter(
        keyword: String? = _uiState.value.filter.keyword,
        status: Int? = _uiState.value.filter.status,
    ) {
        loadOrders(_uiState.value.filter.copy(keyword = keyword, status = status))
    }
}
