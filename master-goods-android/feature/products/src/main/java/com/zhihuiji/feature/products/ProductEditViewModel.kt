package com.zhihuiji.feature.products

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuiji.core.model.v2.media.CreateMediaBindingRequest
import com.zhihuiji.core.model.v2.product.ProductCategoryV2Dto
import com.zhihuiji.core.model.v2.product.ProductPriceValueWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductUnitV2Dto
import com.zhihuiji.core.model.v2.product.ProductWriteV2Request
import com.zhihuiji.data.agent.MediaV2Repository
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

data class ProductImageUi(
    val bindingId: Long?,
    val assetId: Long,
    val url: String,
    val fileName: String = "",
)

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
    val images: List<ProductImageUi> = emptyList(),
    val isUploading: Boolean = false,
    val authToken: String? = null,
)

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    private val repository: ProductV2Repository,
    private val mediaRepository: MediaV2Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductEditUiState())
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()

    private var currentProductId: Long? = null

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
        currentProductId = productId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, authToken = mediaRepository.peekAuthToken()) }
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
                    loadImages(productId)
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

    fun uploadImage(uri: Uri, context: Context) {
        val productId = currentProductId ?: run {
            _uiState.update { it.copy(error = "请先保存商品后再上传图片") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val fileName = readDisplayName(resolver, uri)
            val bytes = runCatching {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                _uiState.update { it.copy(isUploading = false, error = "读取图片失败") }
                return@launch
            }
            mediaRepository.uploadAsset(bytes, fileName, mimeType)
                .onSuccess { asset ->
                    val sortOrder = _uiState.value.images.size
                    mediaRepository.createBinding(
                        CreateMediaBindingRequest(
                            assetId = asset.id,
                            targetType = "product",
                            targetId = productId,
                            sortOrder = sortOrder,
                        )
                    ).onSuccess {
                        loadImages(productId)
                    }.onFailure { error ->
                        _uiState.update { it.copy(isUploading = false, error = error.message) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUploading = false, error = error.message) }
                }
        }
    }

    fun deleteImage(bindingId: Long) {
        val productId = currentProductId ?: return
        viewModelScope.launch {
            mediaRepository.deleteBinding(bindingId)
                .onSuccess { loadImages(productId) }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    private suspend fun loadImages(productId: Long) {
        mediaRepository.listBindings("product", productId)
            .onSuccess { bindings ->
                val images = bindings.map { binding ->
                    ProductImageUi(
                        bindingId = binding.id.takeIf { it > 0L },
                        assetId = binding.assetId,
                        url = mediaRepository.contentUrlFor(binding.assetId),
                    )
                }
                _uiState.update { it.copy(images = images, isUploading = false) }
            }
            .onFailure { error ->
                _uiState.update { it.copy(isUploading = false, error = error.message) }
            }
    }

    private fun readDisplayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex).ifBlank { defaultFileName() }
            }
        }
        return defaultFileName()
    }

    private fun defaultFileName(): String = "image_${System.currentTimeMillis()}"
}
