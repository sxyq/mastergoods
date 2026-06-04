package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Request
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleOrderDetailUiState(
    val order: SaleOrderV2Dto? = null,
    val payments: List<SalePaymentV2Dto> = emptyList(),
    val isLoading: Boolean = false,
    val paymentSuccess: Boolean = false,
    val cancelSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SaleOrderDetailViewModel @Inject constructor(
    private val saleOrderV2Repository: SaleOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleOrderDetailUiState())
    val uiState: StateFlow<SaleOrderDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val orderDeferred = async { saleOrderV2Repository.getSaleOrder(id) }
            val paymentsDeferred = async { saleOrderV2Repository.listPayments(id) }
            val orderResult = orderDeferred.await()
            val paymentsResult = paymentsDeferred.await()
            orderResult.onSuccess { order ->
                _uiState.value = _uiState.value.copy(order = order, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
            paymentsResult.onSuccess { payments ->
                _uiState.value = _uiState.value.copy(payments = payments)
            }
        }
    }

    fun addPayment(amount: Double, method: Int, referenceNo: String?) {
        val orderId = _uiState.value.order?.id ?: return
        viewModelScope.launch {
            saleOrderV2Repository.addPayment(orderId, SalePaymentV2Request(amount, method, referenceNo)).onSuccess {
                _uiState.value = _uiState.value.copy(paymentSuccess = true)
                loadDetail(orderId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun cancelOrder() {
        val orderId = _uiState.value.order?.id ?: return
        viewModelScope.launch {
            saleOrderV2Repository.cancel(orderId).onSuccess {
                _uiState.value = _uiState.value.copy(cancelSuccess = true)
                loadDetail(orderId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun completeOrder() {
        val orderId = _uiState.value.order?.id ?: return
        viewModelScope.launch {
            saleOrderV2Repository.updateStatus(orderId, 1).onSuccess {
                loadDetail(orderId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
