package com.zhihuiji.data.product

import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.entity.ProductEntity
import com.zhihuiji.core.model.v2.product.ProductCategoryV2Dto
import com.zhihuiji.core.model.v2.product.ProductCategoryWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductPriceLevelV2Dto
import com.zhihuiji.core.model.v2.product.ProductPriceLevelWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationV2Dto
import com.zhihuiji.core.model.v2.product.ProductSupplierRelationWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductUnitV2Dto
import com.zhihuiji.core.model.v2.product.ProductUnitWriteV2Request
import com.zhihuiji.core.model.v2.product.ProductV2Dto
import com.zhihuiji.core.model.v2.product.ProductWriteV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import com.zhihuiji.data.sync.LocalSyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val productDao: ProductDao,
    private val syncRepository: LocalSyncRepository,
) {
    fun observeProducts(
        keyword: String? = null,
        status: Int? = null,
        categoryId: Long? = null,
        unitId: Long? = null,
    ): Flow<List<ProductV2Dto>> {
        // The legacy local table does not retain category/unit IDs. Do not claim a
        // filtered local result for those filters until the local schema carries them.
        if (categoryId != null || unitId != null) return flowOf(emptyList())
        val source = if (keyword.isNullOrBlank()) {
            productDao.observeAll()
        } else {
            productDao.search(keyword)
        }
        return source.map { rows ->
            rows
                .asSequence()
                .filter { status == null || it.status == status }
                .map(ProductEntity::toV2Dto)
                .toList()
        }
    }

    fun observeLowStockProducts(limit: Int = 10): Flow<List<ProductV2Dto>> =
        productDao.observeLowStock(limit).map { rows -> rows.map(ProductEntity::toV2Dto) }

    suspend fun listProducts(
        keyword: String? = null,
        status: Int? = null,
        categoryId: Long? = null,
        unitId: Long? = null,
    ): Result<List<ProductV2Dto>> {
        val remote = safeApiCall { api.productsV2(keyword, status, categoryId, unitId) }
        for (product in remote.getOrNull().orEmpty()) {
            cacheRemoteProduct(product)
        }
        return remote
    }

    suspend fun listLowStockProducts(size: Int? = null): Result<List<ProductV2Dto>> =
        safeApiCall { api.lowStockProductsV2(size) }

    suspend fun getProduct(id: Long): Result<ProductV2Dto> {
        val remote = safeApiCall { api.productV2(id) }
        remote.getOrNull()?.let { product ->
            cacheRemoteProduct(product)
            return Result.success(product)
        }
        return productDao.findById(id)?.toV2Dto()?.let { Result.success(it) }
            ?: Result.failure(remote.exceptionOrNull() ?: IllegalStateException("product not found locally"))
    }

    suspend fun createProduct(request: ProductWriteV2Request): Result<ProductV2Dto> {
        val id = syncRepository.nextLocalEntityId()
        val now = System.currentTimeMillis()
        val local = request.toPendingEntity(id = id, previous = null, now = now)
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_CREATE,
            payload = syncRepository.encodePayload(ProductWriteV2Request.serializer(), request),
            baseVersion = 0L,
        ) {
            productDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun updateProduct(id: Long, request: ProductWriteV2Request): Result<ProductV2Dto> {
        val previous = productDao.findById(id)
            ?: return Result.failure(IllegalStateException("product is not available locally"))
        val local = request.toPendingEntity(id = id, previous = previous, now = System.currentTimeMillis())
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_UPDATE,
            payload = syncRepository.encodePayload(ProductWriteV2Request.serializer(), request),
            baseVersion = previous.syncVersion ?: 0L,
        ) {
            productDao.upsert(local)
            local.toV2Dto()
        }
    }

    suspend fun deleteProduct(id: Long): Result<Unit> {
        val previous = productDao.findById(id)
            ?: return Result.failure(IllegalStateException("product is not available locally"))
        return syncRepository.mutateAndEnqueue(
            entityType = ENTITY_TYPE,
            entityId = id.toString(),
            operation = OPERATION_DELETE,
            payload = null,
            baseVersion = previous.syncVersion ?: 0L,
        ) { productDao.deleteById(id) }
    }

    private suspend fun cacheRemoteProduct(product: ProductV2Dto) {
        if (syncRepository.hasUnresolvedLocalChange(ENTITY_TYPE, product.id.toString())) return
        syncRepository.reconcileRemoteProduct(product.id, product.code)
        val previous = productDao.findById(product.id)
        productDao.upsert(product.toEntity(previous))
    }

    suspend fun listCategories(): Result<List<ProductCategoryV2Dto>> =
        safeApiCall { api.productCategoriesV2() }

    suspend fun createCategory(request: ProductCategoryWriteV2Request): Result<ProductCategoryV2Dto> =
        safeApiCall { api.createProductCategoryV2(request) }

    suspend fun updateCategory(id: Long, request: ProductCategoryWriteV2Request): Result<ProductCategoryV2Dto> =
        safeApiCall { api.updateProductCategoryV2(id, request) }

    suspend fun deleteCategory(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteProductCategoryV2(id) }

    suspend fun listUnits(): Result<List<ProductUnitV2Dto>> =
        safeApiCall { api.productUnitsV2() }

    suspend fun createUnit(request: ProductUnitWriteV2Request): Result<ProductUnitV2Dto> =
        safeApiCall { api.createProductUnitV2(request) }

    suspend fun updateUnit(id: Long, request: ProductUnitWriteV2Request): Result<ProductUnitV2Dto> =
        safeApiCall { api.updateProductUnitV2(id, request) }

    suspend fun deleteUnit(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteProductUnitV2(id) }

    suspend fun listPriceLevels(): Result<List<ProductPriceLevelV2Dto>> =
        safeApiCall { api.productPriceLevelsV2() }

    suspend fun createPriceLevel(request: ProductPriceLevelWriteV2Request): Result<ProductPriceLevelV2Dto> =
        safeApiCall { api.createProductPriceLevelV2(request) }

    suspend fun updatePriceLevel(id: Long, request: ProductPriceLevelWriteV2Request): Result<ProductPriceLevelV2Dto> =
        safeApiCall { api.updateProductPriceLevelV2(id, request) }

    suspend fun deletePriceLevel(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteProductPriceLevelV2(id) }

    suspend fun listSupplierRelations(productId: Long): Result<List<ProductSupplierRelationV2Dto>> =
        safeApiCall { api.productSupplierRelationsV2(productId) }

    suspend fun createSupplierRelation(request: ProductSupplierRelationWriteV2Request): Result<ProductSupplierRelationV2Dto> =
        safeApiCall { api.createProductSupplierRelationV2(request) }

    suspend fun updateSupplierRelation(id: Long, request: ProductSupplierRelationWriteV2Request): Result<ProductSupplierRelationV2Dto> =
        safeApiCall { api.updateProductSupplierRelationV2(id, request) }

    suspend fun deleteSupplierRelation(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteProductSupplierRelationV2(id) }
}

private fun ProductV2Dto.toEntity(previous: ProductEntity? = null) = ProductEntity(
    id = id,
    code = code,
    name = name,
    categoryId = categoryId,
    category = categoryName,
    unitId = unitId,
    unit = unitName,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    safeStock = safeStock,
    status = status,
    syncStatus = previous?.syncStatus,
    syncVersion = previous?.syncVersion,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ProductWriteV2Request.toPendingEntity(
    id: Long,
    previous: ProductEntity?,
    now: Long,
) = ProductEntity(
    id = id,
    code = code,
    name = name,
    categoryId = categoryId,
    category = previous?.category.orEmpty(),
    unitId = unitId,
    unit = previous?.unit.orEmpty(),
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    safeStock = safeStock,
    status = status,
    syncStatus = previous?.syncStatus ?: 1,
    syncVersion = previous?.syncVersion ?: 0L,
    createdAt = previous?.createdAt ?: now,
    updatedAt = now,
)

private fun ProductEntity.toV2Dto() = ProductV2Dto(
    id = id,
    code = code,
    name = name,
    categoryId = categoryId ?: 0L,
    categoryName = category,
    unitId = unitId ?: 0L,
    unitName = unit,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    stock = stock,
    safeStock = safeStock,
    status = status,
    createdAt = createdAt ?: 0L,
    updatedAt = updatedAt ?: 0L,
)

private const val ENTITY_TYPE = "product"
private const val OPERATION_CREATE = "create"
private const val OPERATION_UPDATE = "update"
private const val OPERATION_DELETE = "delete"
