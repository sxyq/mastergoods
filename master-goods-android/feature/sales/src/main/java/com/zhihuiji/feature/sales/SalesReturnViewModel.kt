package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.ConfirmSalesReturnV2Request
import com.zhihuiji.core.model.v2.order.SalesReturnItemV2Dto
import com.zhihuiji.core.model.v2.order.SalesReturnV2Dto
import com.zhihuiji.data.order.SalesReturnV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SalesReturnUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
    val returns: List<SalesReturnItem> = emptyList(),
    val selectedReturnId: Long? = null,
) {
    val selectedReturn: SalesReturnItem? get() =
        returns.firstOrNull { it.id == selectedReturnId } ?: returns.firstOrNull()
}

data class SalesReturnItem(
    val id: Long,
    val returnNo: String,
    val originalOrderId: Long?,
    val customerName: String,
    val lines: List<SalesReturnLineItem>,
    val totalAmount: Double,
    val refundAmount: Double,
    val status: Int,
    val notes: String?,
    val createdAt: String,
) {
    val canConfirm: Boolean get() = status == 0
    val remainingRefund: Double get() = (totalAmount - refundAmount).coerceAtLeast(0.0)
    val statusText: String
        get() = when (status) {
            0 -> "待确认"
            1 -> "已确认"
            2 -> "已退款"
            3 -> "已取消"
            else -> "未知"
        }
}

data class SalesReturnLineItem(
    val id: Long,
    val productName: String,
    val productCode: String,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
)

@HiltViewModel
class SalesReturnViewModel @Inject constructor(
    private val repository: SalesReturnV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SalesReturnUiState())
    val uiState: StateFlow<SalesReturnUiState> = _uiState.asStateFlow()

    init {
        loadReturns()
    }

    fun loadReturns() {
        viewModelScope.launch {
            loadReturnsInternal(selectId = _uiState.value.selectedReturnId)
        }
    }

    fun selectReturn(id: Long) {
        _uiState.update { it.copy(selectedReturnId = id, error = null, statusMessage = null) }
    }

    fun confirmSelectedReturn() {
        viewModelScope.launch {
            val salesReturn = _uiState.value.selectedReturn ?: return@launch
            if (!salesReturn.canConfirm || _uiState.value.isSubmitting) return@launch

            _uiState.update { it.copy(isSubmitting = true, error = null, statusMessage = null) }
            repository.confirm(salesReturn.id, ConfirmSalesReturnV2Request())
                .onSuccess {
                    loadReturnsInternal(
                        selectId = salesReturn.id,
                        statusMessage = "销售退货确认成功，库存与客户应收已按真实退货单更新。",
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = error.message ?: "销售退货确认失败",
                        )
                    }
                }
        }
    }

    private suspend fun loadReturnsInternal(
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

        repository.listSalesReturns()
            .onSuccess { rows ->
                val sortedRows = rows.sortedWith(compareBy<SalesReturnV2Dto> { it.status != 0 }.thenByDescending { it.createdAt })
                val items = ArrayList<SalesReturnItem>(sortedRows.size)
                for (row in sortedRows) {
                    items.add(row.toSalesReturnItem())
                }
                var selectedFound = false
                var firstOpenId: Long? = null
                var firstAnyId: Long? = null
                for (item in items) {
                    if (firstAnyId == null) {
                        firstAnyId = item.id
                    }
                    if (firstOpenId == null && item.status == 0) {
                        firstOpenId = item.id
                    }
                    if (selectId != null && item.id == selectId) {
                        selectedFound = true
                    }
                }
                val selectedId = when {
                    selectId != null && selectedFound -> selectId
                    else -> firstOpenId ?: firstAnyId
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        returns = items,
                        selectedReturnId = selectedId,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "销售退货单加载失败",
                    )
                }
            }
    }
}

private fun SalesReturnV2Dto.toSalesReturnItem(): SalesReturnItem =
    SalesReturnItem(
        id = id,
        returnNo = returnNo,
        originalOrderId = originalOrderId,
        customerName = customerName ?: "-",
        lines = items.map(SalesReturnItemV2Dto::toSalesReturnLineItem),
        totalAmount = totalAmount,
        refundAmount = refundAmount,
        status = status,
        notes = notes,
        createdAt = TimeFormatter.formatDateTime(createdAt),
    )

private fun SalesReturnItemV2Dto.toSalesReturnLineItem(): SalesReturnLineItem =
    SalesReturnLineItem(
        id = id,
        productName = productName ?: "未知商品",
        productCode = productCode ?: "-",
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
    )
