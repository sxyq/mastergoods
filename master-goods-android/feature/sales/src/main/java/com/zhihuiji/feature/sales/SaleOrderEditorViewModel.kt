package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.order.ConfirmSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderItemV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.UpdateSaleDraftV2Request
import com.zhihuiji.core.model.v2.partner.CustomerV2Dto
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.customer.CustomerV2Repository
import com.zhihuiji.data.order.SaleOrderV2Repository
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorLineItem(
    val lineId: String = java.util.UUID.randomUUID().toString(),
    val productId: Long? = null,
    val productCode: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
)

data class SaleOrderEditorUiState(
    val editingOrderId: Long? = null,
    val customerId: Long? = null,
    val customerName: String? = null,
    val lines: List<EditorLineItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val productSearchResults: List<ProductV2Dto> = emptyList(),
    val customerSearchResults: List<CustomerV2Dto> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SaleOrderEditorViewModel @Inject constructor(
    private val saleOrderV2Repository: SaleOrderV2Repository,
    private val productV2Repository: ProductV2Repository,
    private val customerV2Repository: CustomerV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleOrderEditorUiState())
    val uiState: StateFlow<SaleOrderEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customerV2Repository.listCustomers()
            productV2Repository.listProducts()
        }
    }

    fun loadOrderForEdit(orderId: Long) {
        if (_uiState.value.editingOrderId == orderId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = false, error = null)
            saleOrderV2Repository.getSaleOrder(orderId).onSuccess { order ->
                val lines = order.items.map { item ->
                    EditorLineItem(
                        productId = item.productId,
                        productCode = item.productCode.orEmpty(),
                        productName = item.productName.orEmpty(),
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        amount = item.amount,
                    )
                }
                _uiState.value = SaleOrderEditorUiState(
                    editingOrderId = order.id,
                    customerId = order.customerId,
                    customerName = order.customerName,
                    lines = lines,
                    discountAmount = order.discountAmount,
                    notes = order.notes.orEmpty(),
                    totalAmount = order.totalAmount,
                    productSearchResults = _uiState.value.productSearchResults,
                    customerSearchResults = _uiState.value.customerSearchResults,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun selectCustomer(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(customerId = id, customerName = name)
    }

    fun searchCustomers(keyword: String) {
        viewModelScope.launch {
            customerV2Repository.listCustomers(keyword = keyword.ifBlank { null })
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(customerSearchResults = list)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                }
        }
    }

    fun searchProducts(keyword: String) {
        viewModelScope.launch {
            productV2Repository.listProducts(keyword = keyword.ifBlank { null })
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(productSearchResults = list)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
                }
        }
    }

    fun addItem(product: ProductV2Dto) {
        val existing = _uiState.value.lines.find { it.productId == product.id }
        if (existing != null) {
            changeQuantity(existing.lineId, existing.quantity + 1)
            return
        }
        val line = EditorLineItem(
            productId = product.id, productCode = product.code,
            productName = product.name, quantity = 1.0, unitPrice = product.salePrice, amount = product.salePrice,
        )
        val newLines = _uiState.value.lines + line
        _uiState.value = _uiState.value.copy(lines = newLines, totalAmount = calcTotal(newLines))
    }

    fun removeItem(lineId: String) {
        val newLines = _uiState.value.lines.filter { it.lineId != lineId }
        _uiState.value = _uiState.value.copy(lines = newLines, totalAmount = calcTotal(newLines))
    }

    fun changeQuantity(lineId: String, quantity: Double) {
        val newLines = _uiState.value.lines.map {
            if (it.lineId == lineId) it.copy(quantity = quantity, amount = it.unitPrice * quantity) else it
        }
        _uiState.value = _uiState.value.copy(lines = newLines, totalAmount = calcTotal(newLines))
    }

    fun updateNotes(notes: String) { _uiState.value = _uiState.value.copy(notes = notes) }
    fun updateDiscount(discount: Double) {
        _uiState.value = _uiState.value.copy(discountAmount = discount, totalAmount = calcTotal(_uiState.value.lines))
    }

    private fun calcTotal(lines: List<EditorLineItem>): Double {
        return lines.sumOf { it.amount } - _uiState.value.discountAmount
    }

    fun submitOrder() {
        if (_uiState.value.lines.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("请至少添加一个商品"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val editingOrderId = _uiState.value.editingOrderId
            val result = if (editingOrderId != null) {
                saleOrderV2Repository.updateDraft(
                    editingOrderId,
                    UpdateSaleDraftV2Request(
                        discountAmount = _uiState.value.discountAmount,
                        notes = _uiState.value.notes.ifBlank { null },
                        items = _uiState.value.lines.map {
                            CreateSaleOrderItemV2Request(productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice)
                        },
                    ),
                )
            } else {
                val request = CreateSaleOrderV2Request(
                    customerId = _uiState.value.customerId,
                    customerName = _uiState.value.customerName,
                    items = _uiState.value.lines.map {
                        CreateSaleOrderItemV2Request(productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice)
                    },
                    notes = _uiState.value.notes.ifBlank { null },
                    discountAmount = _uiState.value.discountAmount,
                )
                saleOrderV2Repository.createSaleOrder(request)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
