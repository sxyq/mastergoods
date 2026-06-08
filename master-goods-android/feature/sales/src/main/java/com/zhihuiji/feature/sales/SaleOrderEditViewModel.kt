package com.zhihuiji.feature.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.order.CreateSaleOrderItemV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.SaleOrderItemV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.data.order.SaleOrderV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditItem(
    val id: Long = 0L,
    val productId: Long? = null,
    val productName: String = "",
    val quantity: String = "1",
    val unitPrice: String = "0.00",
)

data class SaleOrderEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val customerId: String = "",
    val customerName: String = "",
    val remark: String = "",
    val discountAmount: String = "0.00",
    val items: List<EditItem> = emptyList(),
)

@HiltViewModel
class SaleOrderEditViewModel @Inject constructor(
    private val repository: SaleOrderV2Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Long? = savedStateHandle.get<Long>("orderId")

    private val _uiState = MutableStateFlow(SaleOrderEditUiState())
    val uiState: StateFlow<SaleOrderEditUiState> = _uiState.asStateFlow()

    init {
        if (orderId != null && orderId > 0) {
            loadOrder(orderId)
        }
    }

    private fun loadOrder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getSaleOrder(id)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            customerId = order.customerId?.toString() ?: "",
                            customerName = order.customerName ?: "",
                            remark = order.notes ?: "",
                            discountAmount = "%.2f".format(order.discountAmount),
                            items = order.items.map { item -> item.toEditItem() }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateCustomerId(value: String) {
        _uiState.update { it.copy(customerId = value) }
    }

    fun updateCustomerName(value: String) {
        _uiState.update { it.copy(customerName = value) }
    }

    fun updateRemark(value: String) {
        _uiState.update { it.copy(remark = value) }
    }

    fun updateDiscountAmount(value: String) {
        _uiState.update { it.copy(discountAmount = value) }
    }

    fun addItem() {
        _uiState.update {
            it.copy(items = it.items + EditItem())
        }
    }

    fun removeItem(index: Int) {
        _uiState.update {
            val newItems = it.items.toMutableList().apply { removeAt(index) }
            it.copy(items = newItems)
        }
    }

    fun updateItemProduct(index: Int, productId: String, productName: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapIndexed { i, item ->
                    if (i == index) item.copy(productId = productId.toLongOrNull(), productName = productName) else item
                }
            )
        }
    }

    fun updateItemQuantity(index: Int, quantity: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapIndexed { i, item ->
                    if (i == index) item.copy(quantity = quantity) else item
                }
            )
        }
    }

    fun updateItemUnitPrice(index: Int, unitPrice: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapIndexed { i, item ->
                    if (i == index) item.copy(unitPrice = unitPrice) else item
                }
            )
        }
    }

    fun saveOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value
            val request = CreateSaleOrderV2Request(
                customerId = state.customerId.toLongOrNull(),
                customerName = state.customerName.takeIf { it.isNotBlank() },
                items = state.items.mapNotNull { it.toCreateRequest() },
                notes = state.remark.takeIf { it.isNotBlank() },
                discountAmount = state.discountAmount.toDoubleOrNull(),
            )
            if (orderId != null && orderId > 0) {
                val updateRequest = com.zhihuiji.core.model.v2.order.UpdateSaleDraftV2Request(
                    discountAmount = state.discountAmount.toDoubleOrNull(),
                    notes = state.remark.takeIf { it.isNotBlank() },
                    items = state.items.mapNotNull { it.toCreateRequest() },
                )
                repository.updateDraft(orderId, updateRequest)
                    .onSuccess {
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isSaving = false, error = error.message) }
                    }
            } else {
                repository.createSaleOrder(request)
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

private fun SaleOrderItemV2Dto.toEditItem(): EditItem = EditItem(
    id = id,
    productId = productId,
    productName = productName ?: "",
    quantity = quantity.toString(),
    unitPrice = unitPrice.toString(),
)

private fun EditItem.toCreateRequest(): CreateSaleOrderItemV2Request? {
    val qty = quantity.toDoubleOrNull() ?: return null
    val price = unitPrice.toDoubleOrNull() ?: return null
    return CreateSaleOrderItemV2Request(
        productId = productId,
        quantity = qty,
        unitPrice = price,
    )
}
