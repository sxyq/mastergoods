package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

fun ProductV2Dto.toProductItem(): ProductItem = ProductItem(
    id = id,
    name = name,
    code = code,
    stock = stock.toInt(),
    salePrice = "¥%.2f".format(salePrice),
    status = when {
        stock <= 0 -> "缺货"
        stock < safeStock -> "低库存"
        else -> "正常"
    }
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
                    val filtered = ArrayList<ProductItem>(products.size)
                    when (currentState.selectedStockStatus) {
                        "normal" -> {
                            for (dto in products) {
                                val item = dto.toProductItem()
                                if (item.status == "正常") filtered.add(item)
                            }
                        }
                        "low" -> {
                            for (dto in products) {
                                val item = dto.toProductItem()
                                if (item.status == "低库存") filtered.add(item)
                            }
                        }
                        "out" -> {
                            for (dto in products) {
                                val item = dto.toProductItem()
                                if (item.status == "缺货") filtered.add(item)
                            }
                        }
                        else -> {
                            for (dto in products) {
                                filtered.add(dto.toProductItem())
                            }
                        }
                    }
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
