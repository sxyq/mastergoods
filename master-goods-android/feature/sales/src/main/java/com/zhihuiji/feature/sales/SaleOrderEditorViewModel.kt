package com.zhihuiji.feature.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.*
import com.zhihuiji.data.order.SaleOrderRepository
import com.zhihuiji.data.product.ProductRepository
import com.zhihuiji.data.customer.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorLineItem(
    val lineId: String = java.util.UUID.randomUUID().toString(),
    val productId: Long = 0,
    val productCode: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
)

data class SaleOrderEditorUiState(
    val customerId: Long? = null,
    val customerName: String? = null,
    val lines: List<EditorLineItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val productSearchResults: List<ProductDto> = emptyList(),
    val customerSearchResults: List<CustomerDto> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class SaleOrderEditorViewModel @Inject constructor(
    private val saleOrderRepository: SaleOrderRepository,
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SaleOrderEditorUiState())
    val uiState: StateFlow<SaleOrderEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customerRepository.refreshCustomers(null)
            productRepository.refreshProducts(null)
        }
    }

    fun selectCustomer(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(customerId = id, customerName = name)
    }

    fun searchCustomers(keyword: String) {
        viewModelScope.launch {
            customerRepository.refreshCustomers(keyword.ifBlank { null })
            val list = customerRepository.observeCustomers(keyword).first()
            _uiState.value = _uiState.value.copy(customerSearchResults = list)
        }
    }

    fun searchProducts(keyword: String) {
        viewModelScope.launch {
            productRepository.refreshProducts(keyword.ifBlank { null })
            val list = productRepository.observeProducts(keyword).first()
            _uiState.value = _uiState.value.copy(productSearchResults = list)
        }
    }

    fun addItem(product: ProductDto) {
        val existing = _uiState.value.lines.find { it.productId == product.id }
        if (existing != null) {
            changeQuantity(existing.lineId, existing.quantity + 1)
            return
        }
        val line = EditorLineItem(
            productId = product.id ?: 0, productCode = product.code,
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
            val request = CreateSaleOrderRequest(
                customerId = _uiState.value.customerId,
                customerName = _uiState.value.customerName,
                items = _uiState.value.lines.map {
                    CreateSaleOrderItemRequest(productId = it.productId, quantity = it.quantity, unitPrice = it.unitPrice)
                },
                notes = _uiState.value.notes.ifBlank { null },
                discountAmount = _uiState.value.discountAmount,
            )
            saleOrderRepository.createSaleOrder(request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
