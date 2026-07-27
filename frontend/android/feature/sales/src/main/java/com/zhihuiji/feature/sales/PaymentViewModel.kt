package com.zhihuiji.feature.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Request
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val isPaying: Boolean = false,
    val error: String? = null,
    val paySuccess: Boolean = false,
    val order: SaleOrderV2Dto? = null,
    val amount: String = "",
    val paymentMethod: Int = 0,
    val remark: String = "",
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val saleOrderRepository: SaleOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle.get<Long>("orderId")) { "orderId is required" }

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            saleOrderRepository.getSaleOrder(orderId)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            amount = "%.2f".format(order.totalAmount - order.paidAmount)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateAmount(value: String) {
        _uiState.update { it.copy(amount = value) }
    }

    fun updatePaymentMethod(method: Int) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun updateRemark(value: String) {
        _uiState.update { it.copy(remark = value) }
    }

    fun pay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, error = null) }
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                _uiState.update { it.copy(isPaying = false, error = "请输入有效的收款金额") }
                return@launch
            }
            val request = SalePaymentV2Request(
                amount = amount,
                method = state.paymentMethod,
                referenceNo = state.remark.takeIf { it.isNotBlank() },
            )
            saleOrderRepository.addPayment(orderId, request)
                .onSuccess {
                    _uiState.update { it.copy(isPaying = false, paySuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isPaying = false, error = error.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onPaySuccessHandled() {
        _uiState.update { it.copy(paySuccess = false) }
    }
}

private val paymentMethodLabels = mapOf(
    0 to "现金",
    1 to "微信",
    2 to "支付宝",
    3 to "银行转账",
)

fun paymentMethodText(method: Int): String = paymentMethodLabels[method] ?: "其他"
