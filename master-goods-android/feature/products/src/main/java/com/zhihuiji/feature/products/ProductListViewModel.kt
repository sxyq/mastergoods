package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.ProductDto
import com.zhihuiji.data.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<ProductDto> = emptyList(),
    val keyword: String = "",
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init { loadProducts() }

    fun loadProducts(keyword: String = _uiState.value.keyword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword)
            productRepository.refreshProducts(keyword.ifBlank { null })
            productRepository.observeProducts(keyword).collect { list ->
                _uiState.value = _uiState.value.copy(products = list, isLoading = false)
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            productRepository.deleteProduct(id)
            loadProducts()
        }
    }
}
