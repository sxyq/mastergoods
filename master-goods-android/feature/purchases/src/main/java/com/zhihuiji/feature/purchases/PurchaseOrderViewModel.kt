package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.PurchaseOrderDto
import com.zhihuiji.core.model.PurchaseOrderFilter
import com.zhihuiji.data.order.PurchaseOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseOrderListUiState(
    val orders: List<PurchaseOrderDto> = emptyList(),
    val filter: PurchaseOrderFilter = PurchaseOrderFilter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PurchaseOrderViewModel @Inject constructor(
    private val purchaseOrderRepository: PurchaseOrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseOrderListUiState())
    val uiState: StateFlow<PurchaseOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: PurchaseOrderFilter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            purchaseOrderRepository.refreshPurchaseOrders(filter)
            purchaseOrderRepository.observePurchaseOrders(filter).collectLatest { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }
        }
    }
}
