package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.product.ProductPriceValueWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.core.model.v2.product.ProductWriteV2Request
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductV2Draft(
    val code: String = "",
    val name: String = "",
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val unitId: Long = 0L,
    val unitName: String = "",
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val safeStock: Double = 0.0,
    val stock: Double = 0.0,
    val status: Int = 1,
    val priceLevels: List<ProductPriceValueWriteV2Request> = emptyList(),
    val supplierRelations: List<ProductSupplierRelationWriteV2Request> = emptyList(),
) {
    fun toWriteRequest() = ProductWriteV2Request(
        code = code,
        name = name,
        categoryId = categoryId,
        unitId = unitId,
        salePrice = salePrice,
        purchasePrice = purchasePrice,
        priceLevels = priceLevels,
        supplierRelations = supplierRelations,
        stock = stock,
        safeStock = safeStock,
        status = status,
    )
}

data class ProductEditorUiState(
    val draft: ProductV2Draft = ProductV2Draft(),
    val existingId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class ProductEditorViewModel @Inject constructor(
    private val productV2Repository: ProductV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductEditorUiState())
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    fun loadProduct(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            productV2Repository.getProduct(id).onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    existingId = id,
                    draft = dto.toDraft(),
                    isLoading = false,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateDraft(update: (ProductV2Draft) -> ProductV2Draft) {
        _uiState.value = _uiState.value.copy(draft = update(_uiState.value.draft))
    }

    fun saveProduct() {
        val draft = _uiState.value.draft
        if (draft.name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("商品名称不能为空"))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val existingId = _uiState.value.existingId
            val result = if (existingId != null) {
                productV2Repository.updateProduct(existingId, draft.toWriteRequest())
            } else {
                productV2Repository.createProduct(draft.toWriteRequest())
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    // TODO: Stock adjust via /v2/inventory/ledger is planned for future
    fun adjustStock(delta: Double, reason: String?) {
        val id = _uiState.value.existingId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = UiMessage.error("库存调整功能暂未迁移至V2"))
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}

private fun ProductV2Dto.toDraft() = ProductV2Draft(
    code = code,
    name = name,
    categoryId = categoryId,
    categoryName = categoryName,
    unitId = unitId,
    unitName = unitName,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    safeStock = safeStock,
    stock = stock,
    status = status,
    priceLevels = priceLevels.map { ProductPriceValueWriteV2Request(levelId = it.levelId, price = it.price) },
    supplierRelations = supplierRelations.map {
        ProductSupplierRelationWriteV2Request(
            productId = it.productId,
            supplierId = it.supplierId,
            isDefault = it.isDefault,
            purchasePriority = it.purchasePriority,
            lastPurchasePrice = it.lastPurchasePrice,
            notes = it.notes,
        )
    },
)
