package com.zhihuiji.feature.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.SaleOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleOrderDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val order: SaleOrderV2Dto? = null,
    val payments: List<SalePaymentV2Dto> = emptyList(),
)

@HiltViewModel
class SaleOrderDetailViewModel @Inject constructor(
    private val repository: SaleOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long = checkNotNull(savedStateHandle.get<Long>("orderId")) { "orderId is required" }

    private val _uiState = MutableStateFlow(SaleOrderDetailUiState())
    val uiState: StateFlow<SaleOrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSaleOrder(orderId)
                .onSuccess { order ->
                    val paymentsResult = repository.listPayments(orderId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            payments = paymentsResult.getOrDefault(emptyList())
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun downloadReceiptPdf(): Result<ByteArray> = repository.downloadReceiptPdf(orderId)
}

fun SaleOrderV2Dto.statusText(): String = StatusLabels.saleOrderStatus(status)

fun SaleOrderV2Dto.createdAtText(): String = TimeFormatter.formatDateTime(createdAt)

fun SaleOrderItemV2Dto.subtotalText(): String = MoneyFormatter.format(amount)
