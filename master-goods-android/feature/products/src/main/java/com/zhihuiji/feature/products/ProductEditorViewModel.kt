package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.ProductDraft
import com.zhihuiji.core.model.ProductDto
import com.zhihuiji.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductEditorUiState(
    val draft: ProductDraft = ProductDraft(),
    val existingId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class ProductEditorViewModel @Inject constructor(
    private val productRepository: ProductRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductEditorUiState())
    val uiState: StateFlow<ProductEditorUiState> = _uiState.asStateFlow()

    fun loadProduct(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            productRepository.getProduct(id).onSuccess { dto ->
                _uiState.value = _uiState.value.copy(
                    existingId = id,
                    draft = ProductDraft(
                        code = dto.code, name = dto.name, category = dto.category,
                        unit = dto.unit, salePrice = dto.salePrice, purchasePrice = dto.purchasePrice,
                        safeStock = dto.safeStock, status = dto.status,
                    ),
                    isLoading = false,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun updateDraft(update: (ProductDraft) -> ProductDraft) {
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
                productRepository.updateProduct(existingId, draft)
            } else {
                productRepository.createProduct(draft)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun adjustStock(delta: Double, reason: String?) {
        val id = _uiState.value.existingId ?: return
        viewModelScope.launch {
            productRepository.adjustStock(id, delta, reason, null).onSuccess {
                loadProduct(id)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
