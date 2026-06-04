package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.UiMessage
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<ProductV2Dto> = emptyList(),
    val keyword: String = "",
    val stockFilter: Int = 0,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
) {
    val filteredProducts: List<ProductV2Dto> get() = products.filter { product ->
        when (stockFilter) {
            1 -> product.stock < product.safeStock && product.stock > 0
            2 -> product.stock >= product.safeStock
            3 -> product.stock <= 0
            else -> true
        }
    }
}

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productV2Repository: ProductV2Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init { loadProducts() }

    fun loadProducts(keyword: String = _uiState.value.keyword) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, keyword = keyword)
            productV2Repository.listProducts(keyword.ifBlank { null }).onSuccess { list ->
                _uiState.value = _uiState.value.copy(products = list, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = UiMessage.fromThrowable(it))
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            productV2Repository.deleteProduct(id)
            loadProducts()
        }
    }

    fun setStockFilter(filter: Int) {
        _uiState.value = _uiState.value.copy(stockFilter = filter)
    }
}
