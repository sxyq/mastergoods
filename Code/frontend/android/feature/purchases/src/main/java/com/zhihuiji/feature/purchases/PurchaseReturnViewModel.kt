package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.common.TimeFormatter
import com.zhihuiji.core.model.v2.order.ConfirmPurchaseReturnV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseReturnV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseReturnItemV2Request
import com.zhihuiji.core.model.v2.order.PurchaseOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReturnRefundV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReturnRefundV2Request
import com.zhihuiji.core.model.v2.order.PurchaseReturnV2Dto
import com.zhihuiji.core.model.v2.order.UpdatePurchaseReturnDraftV2Request
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import com.zhihuiji.data.order.PurchaseReturnV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PurchaseReturnDraft = 0
private const val PurchaseReturnConfirmed = 1
private const val PurchaseReturnCompleted = 2
private const val PurchaseReturnCancelled = 3

private val PURCHASE_RETURN_STATUS_LABELS = mapOf(
    PurchaseReturnDraft to "草稿",
    PurchaseReturnConfirmed to "已确认",
    PurchaseReturnCompleted to "已完成",
    PurchaseReturnCancelled to "已取消",
)

enum class PurchaseReturnMode { CREATE, MANAGE }

data class PurchaseReturnUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val mode: PurchaseReturnMode = PurchaseReturnMode.CREATE,
    val sourceOrders: List<PurchaseReturnSourceOrder> = emptyList(),
    val selectedOrderId: Long? = null,
    val createNotes: String = "",
    val createItems: List<PurchaseReturnDraftLine> = emptyList(),
    val returns: List<PurchaseReturnRecord> = emptyList(),
    val selectedReturnId: Long? = null,
    val detailNotes: String = "",
    val refundAmount: String = "",
    val refundMethod: Int = StatusLabels.Codes.METHOD_BANK,
    val refundReferenceNo: String = "",
) {
    val selectedOrder: PurchaseReturnSourceOrder?
        get() = selectedOrderId?.let(sourceOrders::findById) ?: sourceOrders.firstOrNull()

    val selectedReturn: PurchaseReturnRecord?
        get() = selectedReturnId?.let(returns::findById) ?: returns.firstOrNull()
}

data class PurchaseReturnSourceOrder(
    val id: Long,
    val orderNo: String,
    val supplierId: Long?,
    val supplierName: String,
    val lines: List<PurchaseReturnSourceLine>,
    val totalAmount: Double,
    val paidAmount: Double,
    val receivedAmount: Double,
    val statusText: String,
    val createdAtText: String,
    val notes: String?,
) {
    val totalAmountText: String get() = MoneyFormatter.format(totalAmount)
    val paidAmountText: String get() = MoneyFormatter.format(paidAmount)
    val receivedAmountText: String get() = MoneyFormatter.format(receivedAmount)
}

data class PurchaseReturnSourceLine(
    val productId: Long?,
    val productName: String,
    val productCode: String,
    val quantity: Double,
    val unitCost: Double,
) {
    val quantityText: String get() = quantity.formatPurchaseReturnQuantity()
    val unitCostText: String get() = MoneyFormatter.format(unitCost)
}

data class PurchaseReturnDraftLine(
    val productId: Long?,
    val productName: String,
    val productCode: String,
    val maxQuantity: Double,
    val quantityInput: String,
    val unitCostInput: String,
)

data class PurchaseReturnRecord(
    val id: Long,
    val returnNo: String,
    val purchaseOrderId: Long?,
    val supplierName: String,
    val status: Int,
    val statusText: String,
    val totalAmount: Double,
    val refundAmount: Double,
    val notes: String?,
    val createdAtText: String,
    val items: List<PurchaseReturnRecordLine>,
    val refunds: List<PurchaseReturnRecordRefund>,
) {
    val totalAmountText: String get() = MoneyFormatter.format(totalAmount)
    val refundAmountText: String get() = MoneyFormatter.format(refundAmount)
    val remainingRefund: Double get() = (totalAmount - refundAmount).coerceAtLeast(0.0)
    val remainingRefundText: String get() = MoneyFormatter.format(remainingRefund)
}

data class PurchaseReturnRecordLine(
    val productName: String,
    val productCode: String,
    val quantity: Double,
    val unitCost: Double,
    val amount: Double,
) {
    val quantityText: String get() = quantity.formatPurchaseReturnQuantity()
    val unitCostText: String get() = MoneyFormatter.format(unitCost)
    val amountText: String get() = MoneyFormatter.format(amount)
}

data class PurchaseReturnRecordRefund(
    val amount: Double,
    val methodText: String,
    val referenceNo: String?,
    val createdAtText: String,
) {
    val amountText: String get() = MoneyFormatter.format(amount)
}

@HiltViewModel
class PurchaseReturnViewModel @Inject constructor(
    private val purchaseOrderRepository: PurchaseOrderV2Repository,
    private val purchaseReturnRepository: PurchaseReturnV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseReturnUiState())
    val uiState: StateFlow<PurchaseReturnUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, success = null) }
            val currentState = _uiState.value
            val sourceResult = purchaseOrderRepository.listPurchaseOrders()
            val returnResult = purchaseReturnRepository.listPurchaseReturns()
            sourceResult.fold(
                onSuccess = { orders ->
                    val sortedOrders = orders.sortedByDescending { it.updatedAt.takeIf { ts -> ts > 0 } ?: it.createdAt }
                    val mappedOrders = sortedOrders.map { it.toSourceOrder() }
                    val mappedOrdersById = mappedOrders.associateBy { it.id }
                    val selectedOrderId = currentState.selectedOrderId
                        ?.takeIf(mappedOrdersById::containsKey)
                        ?: mappedOrders.firstOrNull()?.id
                    val selectedOrder = selectedOrderId?.let(mappedOrdersById::get)
                    returnResult.fold(
                        onSuccess = { returns ->
                            val sortedReturns = returns.sortedByDescending(PurchaseReturnV2Dto::updatedAt)
                            val mappedReturns = sortedReturns.map { it.toReturnRecord() }
                            val mappedReturnsById = mappedReturns.associateBy { it.id }
                            val selectedReturnId = currentState.selectedReturnId
                                ?.takeIf(mappedReturnsById::containsKey)
                                ?: mappedReturns.firstOrNull()?.id
                            val selectedReturn = selectedReturnId?.let(mappedReturnsById::get)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    sourceOrders = mappedOrders,
                                    selectedOrderId = selectedOrderId,
                                    createNotes = selectedOrder?.notes.orEmpty(),
                                    createItems = selectedOrder?.lines?.toDraftLines().orEmpty(),
                                    returns = mappedReturns,
                                    selectedReturnId = selectedReturnId,
                                    detailNotes = selectedReturn?.notes.orEmpty(),
                                    refundAmount = selectedReturn?.remainingRefund?.takeIf { value -> value > 0.0 }?.formatMoneyInput().orEmpty(),
                                    refundMethod = _uiState.value.refundMethod,
                                    error = null,
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    sourceOrders = mappedOrders,
                                    selectedOrderId = selectedOrderId,
                                    createNotes = selectedOrder?.notes.orEmpty(),
                                    createItems = selectedOrder?.lines?.toDraftLines().orEmpty(),
                                    error = error.message ?: "采购退货单加载失败",
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "采购单来源加载失败",
                        )
                    }
                }
            )
        }
    }

    fun changeMode(mode: PurchaseReturnMode) {
        _uiState.update { it.copy(mode = mode, error = null, success = null) }
    }

    fun selectSourceOrder(id: Long) {
        val order = _uiState.value.sourceOrders.findById(id) ?: return
        _uiState.update {
            it.copy(
                selectedOrderId = id,
                createNotes = order.notes.orEmpty(),
                createItems = order.lines.toDraftLines(),
                error = null,
                success = null,
            )
        }
    }

    fun updateCreateNotes(value: String) {
        _uiState.update { it.copy(createNotes = value) }
    }

    fun updateCreateQuantity(index: Int, value: String) {
        _uiState.update {
            it.copy(createItems = it.createItems.updateAt(index) { line -> line.copy(quantityInput = value) })
        }
    }

    fun updateCreateUnitCost(index: Int, value: String) {
        _uiState.update {
            it.copy(createItems = it.createItems.updateAt(index) { line -> line.copy(unitCostInput = value) })
        }
    }

    fun createReturn() {
        val state = _uiState.value
        val selectedOrder = state.selectedOrder ?: run {
            _uiState.update { it.copy(error = "请先选择来源采购单") }
            return
        }
        val items = state.createItems.mapNotNull { line ->
            val quantity = line.quantityInput.toDoubleOrNull() ?: 0.0
            val unitCost = line.unitCostInput.toDoubleOrNull() ?: 0.0
            if (quantity <= 0.0) {
                null
            } else {
                CreatePurchaseReturnItemV2Request(
                    productId = line.productId,
                    productCode = line.productCode.ifBlank { null },
                    productName = line.productName,
                    quantity = quantity.coerceAtMost(line.maxQuantity),
                    unitCost = unitCost,
                )
            }
        }
        if (items.isEmpty()) {
            _uiState.update { it.copy(error = "请至少填写一条退货商品数量") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            purchaseReturnRepository.createPurchaseReturn(
                CreatePurchaseReturnV2Request(
                    purchaseOrderId = selectedOrder.id,
                    supplierId = selectedOrder.supplierId,
                    supplierName = selectedOrder.supplierName,
                    items = items,
                    notes = state.createNotes.ifBlank { null },
                )
            ).fold(
                onSuccess = { created ->
                    val mapped = created.toReturnRecord()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            mode = PurchaseReturnMode.MANAGE,
                            returns = it.returns.upsertById(mapped) { item -> item.id },
                            selectedReturnId = mapped.id,
                            detailNotes = mapped.notes.orEmpty(),
                            refundAmount = mapped.remainingRefund.takeIf { value -> value > 0.0 }?.formatMoneyInput().orEmpty(),
                            success = "采购退货单已创建",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "采购退货单创建失败") }
                }
            )
        }
    }

    fun selectReturn(id: Long) {
        val selected = _uiState.value.returns.findById(id) ?: return
        _uiState.update {
            it.copy(
                selectedReturnId = id,
                detailNotes = selected.notes.orEmpty(),
                refundAmount = selected.remainingRefund.takeIf { value -> value > 0.0 }?.formatMoneyInput().orEmpty(),
                refundReferenceNo = "",
                error = null,
                success = null,
            )
        }
    }

    fun updateDetailNotes(value: String) {
        _uiState.update { it.copy(detailNotes = value) }
    }

    fun updateRefundAmount(value: String) {
        _uiState.update { it.copy(refundAmount = value) }
    }

    fun updateRefundMethod(value: Int) {
        _uiState.update { it.copy(refundMethod = value) }
    }

    fun updateRefundReferenceNo(value: String) {
        _uiState.update { it.copy(refundReferenceNo = value) }
    }

    fun saveDraft() {
        val selected = _uiState.value.selectedReturn ?: return
        if (selected.status != PurchaseReturnDraft) {
            _uiState.update { it.copy(error = "仅草稿状态可编辑备注") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            purchaseReturnRepository.updateDraft(
                selected.id,
                UpdatePurchaseReturnDraftV2Request(notes = _uiState.value.detailNotes.ifBlank { null }),
            ).fold(
                onSuccess = { refreshSelected(it.id, "采购退货草稿已保存") },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "采购退货草稿保存失败") }
                }
            )
        }
    }

    fun confirmReturn() {
        val selected = _uiState.value.selectedReturn ?: return
        if (selected.status != PurchaseReturnDraft) {
            _uiState.update { it.copy(error = "仅草稿状态可确认") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            purchaseReturnRepository.confirm(
                selected.id,
                ConfirmPurchaseReturnV2Request(notes = _uiState.value.detailNotes.ifBlank { null }),
            ).fold(
                onSuccess = { refreshSelected(it.id, "采购退货单已确认") },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "采购退货确认失败") }
                }
            )
        }
    }

    fun addRefund() {
        val selected = _uiState.value.selectedReturn ?: return
        if (selected.status == PurchaseReturnCancelled) {
            _uiState.update { it.copy(error = "已取消退货单不可登记退款") }
            return
        }
        val amount = _uiState.value.refundAmount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(error = "请输入有效退款金额") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            purchaseReturnRepository.addRefund(
                selected.id,
                PurchaseReturnRefundV2Request(
                    amount = amount,
                    method = _uiState.value.refundMethod,
                    referenceNo = _uiState.value.refundReferenceNo.ifBlank { null },
                )
            ).fold(
                onSuccess = { refreshSelected(it.id, "退款记录已登记") },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "退款登记失败") }
                }
            )
        }
    }

    fun cancelReturn() {
        val selected = _uiState.value.selectedReturn ?: return
        if (selected.status == PurchaseReturnCancelled) {
            _uiState.update { it.copy(error = "当前退货单已取消") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, success = null) }
            purchaseReturnRepository.cancel(selected.id).fold(
                onSuccess = { refreshSelected(it.id, "采购退货单已取消") },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "采购退货取消失败") }
                }
            )
        }
    }

    private fun refreshSelected(id: Long, success: String) {
        viewModelScope.launch {
            purchaseReturnRepository.getPurchaseReturn(id).fold(
                onSuccess = { updated ->
                    val mapped = updated.toReturnRecord()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            returns = it.returns.upsertById(mapped) { record -> record.id },
                            selectedReturnId = mapped.id,
                            detailNotes = mapped.notes.orEmpty(),
                            refundAmount = mapped.remainingRefund.takeIf { value -> value > 0.0 }?.formatMoneyInput().orEmpty(),
                            refundReferenceNo = "",
                            success = success,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.message ?: "采购退货单刷新失败") }
                }
            )
        }
    }
}

private fun PurchaseOrderV2Dto.toSourceOrder(): PurchaseReturnSourceOrder =
    PurchaseReturnSourceOrder(
        id = id,
        orderNo = orderNo,
        supplierId = supplierId,
        supplierName = supplierName?.takeIf { it.isNotBlank() } ?: "未命名供应商",
        lines = items.map(PurchaseOrderItemV2Dto::toSourceLine),
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        receivedAmount = receivedAmount,
        statusText = StatusLabels.purchaseOrderStatus(status),
        createdAtText = TimeFormatter.formatDateTime(createdAt),
        notes = notes,
    )

private fun PurchaseOrderItemV2Dto.toSourceLine(): PurchaseReturnSourceLine =
    PurchaseReturnSourceLine(
        productId = productId,
        productName = productName?.takeIf { it.isNotBlank() } ?: "未知商品",
        productCode = productCode?.takeIf { it.isNotBlank() } ?: "-",
        quantity = quantity,
        unitCost = unitCost,
    )

private fun PurchaseReturnSourceLine.toDraftLine(): PurchaseReturnDraftLine =
    PurchaseReturnDraftLine(
        productId = productId,
        productName = productName,
        productCode = productCode,
        maxQuantity = quantity,
        quantityInput = quantity.formatMoneyInput(),
        unitCostInput = unitCost.formatMoneyInput(),
    )

private fun List<PurchaseReturnSourceLine>.toDraftLines(): List<PurchaseReturnDraftLine> =
    map { it.toDraftLine() }

private fun PurchaseReturnV2Dto.toReturnRecord(): PurchaseReturnRecord =
    PurchaseReturnRecord(
        id = id,
        returnNo = returnNo,
        purchaseOrderId = purchaseOrderId,
        supplierName = supplierName?.takeIf { it.isNotBlank() } ?: "未命名供应商",
        status = status,
        statusText = purchaseReturnStatusLabel(status),
        totalAmount = totalAmount,
        refundAmount = refundAmount,
        notes = notes,
        createdAtText = TimeFormatter.formatDateTime(createdAt),
        items = items.map {
            PurchaseReturnRecordLine(
                productName = it.productName?.takeIf(String::isNotBlank) ?: "未知商品",
                productCode = it.productCode?.takeIf(String::isNotBlank) ?: "-",
                quantity = it.quantity,
                unitCost = it.unitCost,
                amount = it.amount,
            )
        },
        refunds = refunds.map(PurchaseReturnRefundV2Dto::toRefundRecord),
    )

private fun PurchaseReturnRefundV2Dto.toRefundRecord(): PurchaseReturnRecordRefund =
    PurchaseReturnRecordRefund(
        amount = amount,
        methodText = StatusLabels.paymentMethod(method),
        referenceNo = referenceNo,
        createdAtText = TimeFormatter.formatDateTime(createdAt),
    )

private fun List<PurchaseReturnDraftLine>.updateAt(index: Int, update: (PurchaseReturnDraftLine) -> PurchaseReturnDraftLine): List<PurchaseReturnDraftLine> =
    mapIndexed { i, item -> if (i == index) update(item) else item }

private inline fun <T> List<T>.upsertById(
    item: T,
    idSelector: (T) -> Long,
): List<T> {
    val targetId = idSelector(item)
    val existingIndex = indexOfFirst { idSelector(it) == targetId }
    return if (existingIndex >= 0) {
        mapIndexed { i, current -> if (i == existingIndex) item else current }
    } else {
        listOf(item) + this
    }
}

private fun List<PurchaseReturnSourceOrder>.findById(id: Long): PurchaseReturnSourceOrder? =
    firstOrNull { it.id == id }

private fun List<PurchaseReturnRecord>.findById(id: Long): PurchaseReturnRecord? =
    firstOrNull { it.id == id }

private fun Double.formatPurchaseReturnQuantity(): String = formatMoneyInput()

private fun Double.formatMoneyInput(): String =
    if (this % 1.0 == 0.0) "%.0f".format(this) else "%.2f".format(this)

private fun purchaseReturnStatusLabel(status: Int): String =
    PURCHASE_RETURN_STATUS_LABELS[status] ?: "未知"
