package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.PurchaseReceiptItemV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReceiptV2Dto
import com.zhihuiji.data.order.PurchaseReceiptV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseReceiptUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
    val receipts: List<PurchaseReceiptItem> = emptyList(),
    val selectedReceiptId: Long? = null,
) {
    val selectedReceipt: PurchaseReceiptItem? get() =
        receipts.firstOrNull { it.id == selectedReceiptId } ?: receipts.firstOrNull()
}

data class PurchaseReceiptItem(
    val id: Long,
    val receiptNo: String,
    val purchaseOrderId: Long?,
    val supplierName: String,
    val items: List<PurchaseReceiptLineItem>,
    val totalAmount: Double,
    val status: Int,
    val notes: String?,
    val createdAt: String,
) {
    val canConfirm: Boolean get() = status == 0
    val statusText: String
        get() = when (status) {
            0 -> "待收货"
            1 -> "已入库"
            2 -> "已取消"
            else -> "未知"
        }
}

data class PurchaseReceiptLineItem(
    val id: Long,
    val productName: String,
    val productCode: String,
    val quantity: Double,
    val unitCost: Double,
    val amount: Double,
)

@HiltViewModel
class PurchaseReceiptViewModel @Inject constructor(
    private val repository: PurchaseReceiptV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseReceiptUiState())
    val uiState: StateFlow<PurchaseReceiptUiState> = _uiState.asStateFlow()

    init {
        loadReceipts()
    }

    fun loadReceipts() {
        viewModelScope.launch {
            loadReceiptsInternal(selectId = _uiState.value.selectedReceiptId)
        }
    }

    fun selectReceipt(id: Long) {
        _uiState.update { it.copy(selectedReceiptId = id, statusMessage = null, error = null) }
    }

    fun confirmSelectedReceipt() {
        viewModelScope.launch {
            val receipt = _uiState.value.selectedReceipt ?: return@launch
            if (!receipt.canConfirm || _uiState.value.isSubmitting) return@launch

            _uiState.update { it.copy(isSubmitting = true, error = null, statusMessage = null) }
            repository.confirm(receipt.id)
                .onSuccess {
                    loadReceiptsInternal(
                        selectId = receipt.id,
                        statusMessage = "入库确认成功，库存已按真实入库单更新。",
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = error.message ?: "入库确认失败",
                        )
                    }
                }
        }
    }

    private suspend fun loadReceiptsInternal(
        selectId: Long? = null,
        statusMessage: String? = null,
    ) {
        _uiState.update {
            it.copy(
                isLoading = true,
                isSubmitting = false,
                error = null,
                statusMessage = statusMessage,
            )
        }

        repository.listPurchaseReceipts()
            .onSuccess { receipts ->
                val items = receipts
                    .sortedWith(compareBy<PurchaseReceiptV2Dto> { it.status != 0 }.thenByDescending { it.createdAt })
                    .map(PurchaseReceiptV2Dto::toPurchaseReceiptItem)
                val selectedId = selectId
                    ?.takeIf { id -> items.any { it.id == id } }
                    ?: items.firstOrNull { it.status == 0 }?.id
                    ?: items.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        receipts = items,
                        selectedReceiptId = selectedId,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "采购入库单加载失败",
                    )
                }
            }
    }
}

private fun PurchaseReceiptV2Dto.toPurchaseReceiptItem(): PurchaseReceiptItem =
    PurchaseReceiptItem(
        id = id,
        receiptNo = receiptNo,
        purchaseOrderId = purchaseOrderId,
        supplierName = supplierName ?: "-",
        items = items.map(PurchaseReceiptItemV2Dto::toPurchaseReceiptLineItem),
        totalAmount = totalAmount,
        status = status,
        notes = notes,
        createdAt = TimeFormatter.formatDateTime(createdAt),
    )

private fun PurchaseReceiptItemV2Dto.toPurchaseReceiptLineItem(): PurchaseReceiptLineItem =
    PurchaseReceiptLineItem(
        id = id,
        productName = productName ?: "未知商品",
        productCode = productCode ?: "-",
        quantity = quantity,
        unitCost = unitCost,
        amount = amount,
    )
