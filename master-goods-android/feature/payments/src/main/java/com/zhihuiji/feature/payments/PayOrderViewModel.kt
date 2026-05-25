package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.PayOrderDto
import com.zhihuiji.core.model.PayOrderFilter
import com.zhihuiji.data.order.PayOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderListUiState(
    val orders: List<PayOrderDto> = emptyList(),
    val filter: PayOrderFilter = PayOrderFilter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PayOrderViewModel @Inject constructor(
    private val payOrderRepository: PayOrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayOrderListUiState())
    val uiState: StateFlow<PayOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: PayOrderFilter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            payOrderRepository.refreshPayOrders(filter)
            payOrderRepository.observePayOrders(filter).collect { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }
        }
    }
}
