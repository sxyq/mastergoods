package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Filter
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseOrderListUiState(
    val orders: List<PurchaseOrderV2Dto> = emptyList(),
    val filter: PurchaseOrderV2Filter = PurchaseOrderV2Filter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PurchaseOrderViewModel @Inject constructor(
    private val purchaseOrderV2Repository: PurchaseOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseOrderListUiState())
    val uiState: StateFlow<PurchaseOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: PurchaseOrderV2Filter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            purchaseOrderV2Repository.listPurchaseOrders(filter).onSuccess { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
