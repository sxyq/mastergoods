package com.zhihuiji.feature.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.CreatePurchaseOrderItemRequest
import com.zhihuiji.core.model.CreatePurchaseOrderRequest
import com.zhihuiji.core.model.ProductDto
import com.zhihuiji.core.model.SupplierDto
import com.zhihuiji.data.order.PurchaseOrderRepository
import com.zhihuiji.data.product.ProductRepository
import com.zhihuiji.data.supplier.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseLineItem(
    val lineId: String = java.util.UUID.randomUUID().toString(),
    val productId: Long? = null,
    val productCode: String = "",
    val productName: String = "",
    val quantity: Double = 1.0,
    val unitCost: Double = 0.0,
    val amount: Double = 0.0,
)

data class PurchaseOrderEditorUiState(
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val lines: List<PurchaseLineItem> = emptyList(),
    val notes: String = "",
    val totalAmount: Double = 0.0,
    val productSearchResults: List<ProductDto> = emptyList(),
    val supplierSearchResults: List<SupplierDto> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class PurchaseOrderEditorViewModel @Inject constructor(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseOrderEditorUiState())
    val uiState: StateFlow<PurchaseOrderEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierRepository.refreshSuppliers(null, null)
            productRepository.refreshProducts(null)
        }
    }

    fun selectSupplier(id: Long, name: String) {
        _uiState.value = _uiState.value.copy(supplierId = id, supplierName = name)
    }

    fun searchSuppliers(keyword: String) {
        viewModelScope.launch {
            supplierRepository.refreshSuppliers(keyword.ifBlank { null }, null)
            val list = supplierRepository.observeSuppliers(keyword, null).first()
            _uiState.value = _uiState.value.copy(supplierSearchResults = list)
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
        val line = PurchaseLineItem(
            productId = product.id,
            productCode = product.code,
            productName = product.name,
            quantity = 1.0,
            unitCost = product.purchasePrice,
            amount = product.purchasePrice,
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
            if (it.lineId == lineId) it.copy(quantity = quantity, amount = it.unitCost * quantity) else it
        }
        _uiState.value = _uiState.value.copy(lines = newLines, totalAmount = calcTotal(newLines))
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    private fun calcTotal(lines: List<PurchaseLineItem>): Double = lines.sumOf { it.amount }

    fun submitOrder() {
        if (_uiState.value.supplierName.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("请选择供应商"))
            return
        }
        if (_uiState.value.lines.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("请至少添加一个商品"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val request = CreatePurchaseOrderRequest(
                supplierName = _uiState.value.supplierName!!,
                items = _uiState.value.lines.map {
                    CreatePurchaseOrderItemRequest(
                        productId = it.productId,
                        productCode = it.productCode.ifBlank { null },
                        productName = it.productName.ifBlank { null },
                        quantity = it.quantity,
                        unitCost = it.unitCost,
                    )
                },
                notes = _uiState.value.notes.ifBlank { null },
            )
            purchaseOrderRepository.createPurchaseOrder(request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
