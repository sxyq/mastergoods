package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.common.MoneyFormatter
import com.zhihuiji.core.common.StatusLabels
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val stockStatusFilter = mapOf("normal" to "正常", "low" to "低库存", "out" to "缺货")

fun ProductV2Dto.toProductItem(): ProductItem = ProductItem(
    id = id,
    name = name,
    code = code,
    stock = stock.toInt(),
    salePrice = MoneyFormatter.format(salePrice),
    status = StatusLabels.stockStatus(stock, safeStock),
)

data class ProductItem(
    val id: Long,
    val name: String,
    val code: String,
    val stock: Int,
    val salePrice: String,
    val status: String
)

data class ProductCategory(
    val id: Long,
    val name: String
)

data class ProductListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<ProductItem> = emptyList(),
    val keyword: String = "",
    val categories: List<ProductCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedStockStatus: String = "all"
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listProducts(
                keyword = currentState.keyword.takeIf { it.isNotBlank() },
                categoryId = currentState.selectedCategoryId
            )
                .onSuccess { products ->
                    val items = products.map { it.toProductItem() }
                    val filtered = stockStatusFilter[currentState.selectedStockStatus]
                        ?.let { target -> items.filter { it.status == target } }
                        ?: items
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = filtered
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(keyword = keyword) }
        loadProducts()
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        loadProducts()
    }

    fun selectStockStatus(status: String) {
        _uiState.update { it.copy(selectedStockStatus = status) }
        loadProducts()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
