package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.core.model.v2.order.PayOrderV2Filter
import com.zhihuiji.data.order.PayOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderListUiState(
    val orders: List<PayOrderV2Dto> = emptyList(),
    val filter: PayOrderV2Filter = PayOrderV2Filter(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PayOrderViewModel @Inject constructor(
    private val payOrderV2Repository: PayOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayOrderListUiState())
    val uiState: StateFlow<PayOrderListUiState> = _uiState.asStateFlow()

    init { loadOrders() }

    fun loadOrders(filter: PayOrderV2Filter = _uiState.value.filter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, filter = filter)
            payOrderV2Repository.listPayOrders(filter).onSuccess { list ->
                _uiState.value = _uiState.value.copy(orders = list, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
