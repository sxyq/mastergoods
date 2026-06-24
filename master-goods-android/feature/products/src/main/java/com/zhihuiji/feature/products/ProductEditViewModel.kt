package com.zhihuiji.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.product.ProductCategoryV2Dto
import com.zhihuiji.core.model.v2.product.ProductPriceValueWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductUnitV2Dto
import com.zhihuiji.core.model.v2.product.ProductWriteV2Request
import com.zhihuiji.data.product.ProductV2Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductEditUiState(
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val name: String = "",
    val code: String = "",
    val salePrice: String = "",
    val purchasePrice: String = "",
    val wholesalePrice: String = "",
    val stock: String = "",
    val safeStock: String = "",
    val categoryId: Long? = null,
    val categoryName: String = "",
    val unitId: Long? = null,
    val unitName: String = "",
    val supplierName: String = "",
    val categories: List<ProductCategoryV2Dto> = emptyList(),
    val units: List<ProductUnitV2Dto> = emptyList(),
    val preservedPriceLevels: List<ProductPriceValueWriteV2Request> = emptyList(),
    val preservedSupplierRelations: List<ProductSupplierRelationWriteV2Request> = emptyList(),
)

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    private val repository: ProductV2Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductEditUiState())
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()

    init {
        loadReferenceData()
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            val (categoriesResult, unitsResult) = coroutineScope {
                val categoriesDeferred = async { repository.listCategories() }
                val unitsDeferred = async { repository.listUnits() }
                categoriesDeferred.await() to unitsDeferred.await()
            }

            val categories = categoriesResult.getOrNull()
                ?.filter { item -> item.status == 1 }
                .orEmpty()
            val units = unitsResult.getOrNull()
                ?.filter { item -> item.status == 1 }
                .orEmpty()
            val errorMessage = listOfNotNull(
                categoriesResult.exceptionOrNull()?.message,
                unitsResult.exceptionOrNull()?.message,
            ).takeIf { it.isNotEmpty() }?.joinToString("\n")

            _uiState.update {
                it.copy(
                    categories = categories,
                    units = units,
                    error = errorMessage,
                )
            }
        }
    }

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getProduct(productId)
                .onSuccess { dto ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditMode = true,
                            name = dto.name,
                            code = dto.code,
                            salePrice = dto.salePrice.toString(),
                            purchasePrice = dto.purchasePrice.toString(),
                            stock = dto.stock.toString(),
                            safeStock = dto.safeStock.toString(),
                            categoryId = dto.categoryId.takeIf { id -> id > 0L },
                            categoryName = dto.categoryName,
                            unitId = dto.unitId.takeIf { id -> id > 0L },
                            unitName = dto.unitName,
                            supplierName = dto.defaultSupplier?.supplierName.orEmpty(),
                            preservedPriceLevels = dto.priceLevels.map { price ->
                                ProductPriceValueWriteV2Request(
                                    levelId = price.levelId,
                                    price = price.price,
                                )
                            },
                            preservedSupplierRelations = dto.supplierRelations.map { relation ->
                                ProductSupplierRelationWriteV2Request(
                                    productId = relation.productId,
                                    supplierId = relation.supplierId,
                                    isDefault = relation.isDefault,
                                    purchasePriority = relation.purchasePriority,
                                    lastPurchasePrice = relation.lastPurchasePrice,
                                    notes = relation.notes,
                                )
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun selectCategory(category: ProductCategoryV2Dto) {
        _uiState.update { it.copy(categoryId = category.id, categoryName = category.name) }
    }

    fun selectUnit(unit: ProductUnitV2Dto) {
        _uiState.update { it.copy(unitId = unit.id, unitName = unit.name) }
    }

    fun createProduct(
        name: String,
        code: String,
        categoryId: Long,
        unitId: Long,
        salePrice: Double,
        purchasePrice: Double,
        stock: Double,
        safeStock: Double,
        continueAdding: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = ProductWriteV2Request(
                name = name,
                code = code,
                categoryId = categoryId,
                unitId = unitId,
                salePrice = salePrice,
                purchasePrice = purchasePrice,
                stock = stock,
                safeStock = safeStock,
                status = 1,
            )
            repository.createProduct(request)
                .onSuccess {
                    if (continueAdding) {
                        _uiState.value = ProductEditUiState()
                    } else {
                        _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateProduct(
        productId: Long,
        name: String,
        code: String,
        categoryId: Long,
        unitId: Long,
        salePrice: Double,
        purchasePrice: Double,
        stock: Double,
        safeStock: Double,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val request = ProductWriteV2Request(
                name = name,
                code = code,
                categoryId = categoryId,
                unitId = unitId,
                salePrice = salePrice,
                purchasePrice = purchasePrice,
                priceLevels = _uiState.value.preservedPriceLevels,
                supplierRelations = _uiState.value.preservedSupplierRelations,
                stock = stock,
                safeStock = safeStock,
                status = 1,
            )
            repository.updateProduct(productId, request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
