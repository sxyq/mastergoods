package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PurchaseOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseReturnUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val sourceOrders: List<PurchaseReturnSourceOrder> = emptyList(),
    val selectedOrderId: Long? = null,
) {
    val selectedOrder: PurchaseReturnSourceOrder? get() =
        sourceOrders.firstOrNull { it.id == selectedOrderId } ?: sourceOrders.firstOrNull()
}

data class PurchaseReturnSourceOrder(
    val id: Long,
    val orderNo: String,
    val supplierName: String,
    val lines: List<PurchaseReturnSourceLine>,
    val totalAmount: Double,
    val paidAmount: Double,
    val receivedAmount: Double,
    val statusText: String,
    val createdAtText: String,
    val notes: String?,
) {
    val payableBalance: Double get() = (totalAmount - paidAmount).coerceAtLeast(0.0)
    val totalQuantity: Double get() = lines.sumOf { it.quantity }
    val totalQuantityText: String get() = totalQuantity.formatPurchaseReturnQuantity()
    val payableBalanceText: String get() = MoneyFormatter.format(payableBalance)
    val totalAmountText: String get() = MoneyFormatter.format(totalAmount)
    val paidAmountText: String get() = MoneyFormatter.format(paidAmount)
}

data class PurchaseReturnSourceLine(
    val id: Long,
    val productName: String,
    val productCode: String,
    val quantity: Double,
    val unitCost: Double,
    val amount: Double,
) {
    val quantityText: String get() = quantity.formatPurchaseReturnQuantity()
    val unitCostText: String get() = "${MoneyFormatter.format(unitCost)}/件"
    val amountText: String get() = MoneyFormatter.format(amount)
}

@HiltViewModel
class PurchaseReturnViewModel @Inject constructor(
    private val repository: PurchaseOrderV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseReturnUiState())
    val uiState: StateFlow<PurchaseReturnUiState> = _uiState.asStateFlow()

    init {
        loadSourceOrders()
    }

    fun loadSourceOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listPurchaseOrders()
                .onSuccess { orders ->
                    val mapped = orders
                        .sortedWith(
                            compareByDescending<PurchaseOrderV2Dto> { it.items.isNotEmpty() }
                                .thenByDescending { it.updatedAt.takeIf { updatedAt -> updatedAt > 0 } ?: it.createdAt }
                        )
                        .map(PurchaseOrderV2Dto::toPurchaseReturnSourceOrder)
                    val selectedId = _uiState.value.selectedOrderId
                        ?.takeIf { id -> mapped.any { it.id == id } }
                        ?: mapped.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sourceOrders = mapped,
                            selectedOrderId = selectedId,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "采购退货来源加载失败",
                        )
                    }
                }
        }
    }

    fun selectSourceOrder(id: Long) {
        _uiState.update { it.copy(selectedOrderId = id, error = null) }
    }
}

private fun PurchaseOrderV2Dto.toPurchaseReturnSourceOrder(): PurchaseReturnSourceOrder =
    PurchaseReturnSourceOrder(
        id = id,
        orderNo = orderNo,
        supplierName = supplierName?.takeIf { it.isNotBlank() } ?: "未命名供应商",
        lines = items.map(PurchaseOrderItemV2Dto::toPurchaseReturnSourceLine),
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        receivedAmount = receivedAmount,
        statusText = StatusLabels.purchaseOrderStatus(status),
        createdAtText = TimeFormatter.formatDate(createdAt),
        notes = notes,
    )

private fun PurchaseOrderItemV2Dto.toPurchaseReturnSourceLine(): PurchaseReturnSourceLine =
    PurchaseReturnSourceLine(
        id = id,
        productName = productName?.takeIf { it.isNotBlank() } ?: "未知商品",
        productCode = productCode?.takeIf { it.isNotBlank() } ?: "-",
        quantity = quantity,
        unitCost = unitCost,
        amount = amount,
    )

private fun Double.formatPurchaseReturnQuantity(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this) else "%.2f".format(this)
