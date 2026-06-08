package com.zhihuiji.feature.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.CreatePurchaseOrderItemV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseOrderV2Request
import com.zhihuiji.core.model.v2.order.PurchaseOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.data.order.PurchaseOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseEditItem(
    val id: Long = 0L,
    val productId: Long? = null,
    val productCode: String = "",
    val productName: String = "",
    val quantity: String = "1",
    val unitCost: String = "0.00",
)

data class PurchaseOrderEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val supplierId: String = "",
    val supplierName: String = "",
    val remark: String = "",
    val items: List<PurchaseEditItem> = emptyList(),
) {
    val totalAmount: Double
        get() = items.sumOf {
            val qty = it.quantity.toDoubleOrNull() ?: 0.0
            val cost = it.unitCost.toDoubleOrNull() ?: 0.0
            qty * cost
        }
}

@HiltViewModel
class PurchaseOrderEditViewModel @Inject constructor(
    private val repository: PurchaseOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long? = savedStateHandle.get<Long>("orderId")

    private val _uiState = MutableStateFlow(PurchaseOrderEditUiState())
    val uiState: StateFlow<PurchaseOrderEditUiState> = _uiState.asStateFlow()

    init {
        if (orderId != null && orderId > 0) {
            loadOrder(orderId)
        }
    }

    private fun loadOrder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getPurchaseOrder(id)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            supplierId = order.supplierId?.toString() ?: "",
                            supplierName = order.supplierName ?: "",
                            remark = order.notes ?: "",
                            items = order.items.map { item -> item.toEditItem() }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateSupplierId(value: String) {
        _uiState.update { it.copy(supplierId = value) }
    }

    fun updateSupplierName(value: String) {
        _uiState.update { it.copy(supplierName = value) }
    }

    fun updateRemark(value: String) {
        _uiState.update { it.copy(remark = value) }
    }

    fun addItem() {
        _uiState.update {
            it.copy(items = it.items + PurchaseEditItem())
        }
    }

    fun removeItem(index: Int) {
        _uiState.update {
            val newItems = it.items.toMutableList().apply { removeAt(index) }
            it.copy(items = newItems)
        }
    }

    fun updateItemProduct(index: Int, productId: String, productName: String, productCode: String? = null) {
        _uiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(
                productId = productId.toLongOrNull(),
                productName = productName,
                productCode = productCode ?: newItems[index].productCode,
            )
            state.copy(items = newItems)
        }
    }

    fun updateItemQuantity(index: Int, quantity: String) {
        _uiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(quantity = quantity)
            state.copy(items = newItems)
        }
    }

    fun updateItemUnitCost(index: Int, unitCost: String) {
        _uiState.update { state ->
            val newItems = state.items.toMutableList()
            newItems[index] = newItems[index].copy(unitCost = unitCost)
            state.copy(items = newItems)
        }
    }

    fun saveOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value
            val request = CreatePurchaseOrderV2Request(
                supplierId = state.supplierId.toLongOrNull(),
                supplierName = state.supplierName.takeIf { it.isNotBlank() },
                items = state.items.mapNotNull { it.toCreateRequest() },
                notes = state.remark.takeIf { it.isNotBlank() },
            )
            if (orderId != null && orderId > 0) {
                repository.updatePurchaseOrder(orderId, request)
                    .onSuccess {
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isSaving = false, error = error.message) }
                    }
            } else {
                repository.createPurchaseOrder(request)
                    .onSuccess {
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isSaving = false, error = error.message) }
                    }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onSaveSuccessHandled() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

private fun PurchaseOrderItemV2Dto.toEditItem(): PurchaseEditItem = PurchaseEditItem(
    id = id,
    productId = productId,
    productCode = productCode ?: "",
    productName = productName ?: "",
    quantity = quantity.toString(),
    unitCost = unitCost.toString(),
)

private fun PurchaseEditItem.toCreateRequest(): CreatePurchaseOrderItemV2Request? {
    val qty = quantity.toDoubleOrNull() ?: return null
    val cost = unitCost.toDoubleOrNull() ?: return null
    return CreatePurchaseOrderItemV2Request(
        productId = productId,
        productCode = productCode.takeIf { it.isNotBlank() },
        productName = productName.takeIf { it.isNotBlank() },
        quantity = qty,
        unitCost = cost,
    )
}
