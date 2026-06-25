package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Filter
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseOrderListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val orders: List<PurchaseOrderItem> = emptyList(),
    val keyword: String = "",
    val selectedTabIndex: Int = 0
)

data class PurchaseOrderItem(
    val id: Long,
    val orderNo: String,
    val supplier: String,
    val amount: String,
    val status: String,
    val date: String
)

fun PurchaseOrderV2Dto.toPurchaseOrderItem(): PurchaseOrderItem = PurchaseOrderItem(
    id = id,
    orderNo = orderNo,
    supplier = supplierName ?: "",
    amount = MoneyFormatter.format(totalAmount),
    status = StatusLabels.purchaseOrderStatus(status),
    date = TimeFormatter.formatDate(createdAt)
)

@HiltViewModel
class PurchaseOrderListViewModel @Inject constructor(
    private val repository: PurchaseOrderV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseOrderListUiState())
    val uiState: StateFlow<PurchaseOrderListUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val keyword = _uiState.value.keyword.takeIf { it.isNotBlank() }
            val filter = PurchaseOrderV2Filter(
                keyword = keyword,
                status = when (_uiState.value.selectedTabIndex) {
                    1 -> StatusLabels.Codes.PURCHASE_DRAFT
                    2 -> StatusLabels.Codes.PURCHASE_RECEIVED
                    else -> null
                }
            )
            repository.listPurchaseOrders(filter)
                .onSuccess { orders ->
                    val items = orders.map { it.toPurchaseOrderItem() }
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
                            error = error.message ?: "采购订单加载失败"
                        )
                    }
                }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadOrders()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadOrders()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
