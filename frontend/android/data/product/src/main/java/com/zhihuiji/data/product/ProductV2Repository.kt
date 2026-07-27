package com.zhihuiji.data.product

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listProducts(
        keyword: String? = null,
        status: Int? = null,
        categoryId: Long? = null,
        unitId: Long? = null,
    ): Result<List<ProductV2Dto>> = safeApiCall { api.productsV2(keyword, status, categoryId, unitId) }

    suspend fun listLowStockProducts(size: Int? = null): Result<List<ProductV2Dto>> =
        safeApiCall { api.lowStockProductsV2(size) }

    suspend fun getProduct(id: Long): Result<ProductV2Dto> =
        safeApiCall { api.productV2(id) }

    suspend fun createProduct(request: ProductWriteV2Request): Result<ProductV2Dto> =
        safeApiCall { api.createProductV2(request) }

    suspend fun updateProduct(id: Long, request: ProductWriteV2Request): Result<ProductV2Dto> =
        safeApiCall { api.updateProductV2(id, request) }

    suspend fun deleteProduct(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteProductV2(id) }

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
