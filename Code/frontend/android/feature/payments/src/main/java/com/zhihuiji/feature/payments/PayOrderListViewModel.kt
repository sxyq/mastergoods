package com.zhihuiji.feature.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.core.model.v2.order.PayOrderV2Filter
import com.zhihuiji.data.order.PayOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayOrderListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val orders: List<PayOrderItem> = emptyList(),
    val keyword: String = "",
    val selectedTabIndex: Int = 0,
    val page: Int = 0,
    val pageSize: Int = 50,
)

data class PayOrderItem(
    val id: Long,
    val orderNo: String,
    val payee: String,
    val amount: String,
    val status: String,
    val method: String,
    val referenceNo: String?,
    val date: String
)

fun PayOrderV2Dto.toPayOrderItem(): PayOrderItem = PayOrderItem(
    id = id,
    orderNo = orderNo,
    payee = supplierName ?: "",
    amount = MoneyFormatter.format(amount),
    status = StatusLabels.payOrderStatus(status),
    method = StatusLabels.paymentMethod(method),
    referenceNo = referenceNo,
    date = TimeFormatter.formatDate(createdAt)
)

@HiltViewModel
class PayOrderListViewModel @Inject constructor(
    private val repository: PayOrderV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayOrderListUiState())
    val uiState: StateFlow<PayOrderListUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders(page: Int = _uiState.value.page) {
        val normalizedPage = page.coerceAtLeast(0)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, page = normalizedPage) }
            val keyword = _uiState.value.keyword.takeIf { it.isNotBlank() }
            val pageSize = _uiState.value.pageSize
            val filter = PayOrderV2Filter(
                keyword = keyword,
                status = when (_uiState.value.selectedTabIndex) {
                    1 -> StatusLabels.Codes.PAY_PENDING
                    2 -> StatusLabels.Codes.PAY_PAID
                    3 -> StatusLabels.Codes.PAY_CANCELLED
                    else -> null
                },
                page = normalizedPage,
                size = pageSize,
            )
            repository.listPayOrders(filter)
                .onSuccess { orders ->
                    val items = orders.map { it.toPayOrderItem() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            orders = items
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "付款订单加载失败"
                        )
                    }
                }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword, page = 0) }
        loadOrders(0)
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index, page = 0) }
        loadOrders(0)
    }

    fun loadNextPage() {
        loadOrders(_uiState.value.page + 1)
    }

    fun loadPreviousPage() {
        if (_uiState.value.page > 0) {
            loadOrders(_uiState.value.page - 1)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
