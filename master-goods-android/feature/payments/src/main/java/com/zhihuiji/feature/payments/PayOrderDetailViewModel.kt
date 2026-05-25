package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.PayOrderDto
import com.zhihuiji.data.order.PayOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderDetailUiState(
    val order: PayOrderDto? = null,
    val isLoading: Boolean = false,
    val statusUpdateSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class PayOrderDetailViewModel @Inject constructor(
    private val payOrderRepository: PayOrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayOrderDetailUiState())
    val uiState: StateFlow<PayOrderDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            payOrderRepository.getPayOrder(id).onSuccess { order ->
                _uiState.value = _uiState.value.copy(order = order, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateStatus(status: Int) {
        val orderId = _uiState.value.order?.id ?: return
        viewModelScope.launch {
            payOrderRepository.updatePayOrderStatus(orderId, status).onSuccess {
                _uiState.value = _uiState.value.copy(statusUpdateSuccess = true)
                loadDetail(orderId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun cancelOrder() { updateStatus(2) }
    fun completeOrder() { updateStatus(1) }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
