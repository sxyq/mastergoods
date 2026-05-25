package com.zhihuiji.data.product

import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.toDto
import com.zhihuiji.core.database.toEntity
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val productDao: ProductDao,
) {
    fun observeProducts(keyword: String): Flow<List<ProductDto>> =
        (if (keyword.isBlank()) productDao.observeAll() else productDao.search(keyword)).map { list ->
            list.map { it.toDto() }
        }

    suspend fun refreshProducts(keyword: String?) {
        val result = safeApiCall { api.products(keyword) }
        result.onSuccess { products ->
            productDao.upsertAll(products.map { it.toEntity() })
        }
    }

    suspend fun getProduct(id: Long): Result<ProductDto> {
        val local = productDao.findById(id)?.toDto()
        val remote = safeApiCall { api.product(id) }
        remote.onSuccess { productDao.upsert(it.toEntity()) }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun findProductByCode(code: String): Result<ProductDto?> = safeApiCall { api.productByCode(code) }

    suspend fun createProduct(draft: ProductDraft): Result<ProductDto> =
        safeApiCall { api.createProduct(draft.toDto()) }.also { result ->
            result.onSuccess { productDao.upsert(it.toEntity()) }
        }

    suspend fun updateProduct(id: Long, draft: ProductDraft): Result<ProductDto> =
        safeApiCall { api.updateProduct(id, draft.toDto()) }.also { result ->
            result.onSuccess { productDao.upsert(it.toEntity()) }
        }

    suspend fun adjustStock(id: Long, delta: Double, reason: String?, operator: String?): Result<ProductDto> =
        safeApiCall { api.adjustStock(id, ProductAdjustStockRequest(delta, reason, operator)) }.also { result ->
            result.onSuccess { productDao.upsert(it.toEntity()) }
        }

    suspend fun deleteProduct(id: Long): Result<Unit> =
        safeApiCall { api.deleteProduct(id) }.also { result ->
            result.onSuccess { productDao.deleteById(id) }
        }
}
