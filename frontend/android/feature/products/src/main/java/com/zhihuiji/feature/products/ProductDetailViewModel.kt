package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetail(
    val id: Long,
    val name: String,
    val code: String,
    val salePrice: Double,
    val purchasePrice: Double,
    val stock: Double,
    val safeStock: Double,
    val unit: String,
)

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val product: ProductDetail? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: ProductV2Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getProduct(productId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = ProductDetail(
                                id = dto.id,
                                name = dto.name,
                                code = dto.code,
                                salePrice = dto.salePrice,
                                purchasePrice = dto.purchasePrice,
                                stock = dto.stock,
                                safeStock = dto.safeStock,
                                unit = dto.unitName ?: "件"
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteProduct(productId)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, product = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
