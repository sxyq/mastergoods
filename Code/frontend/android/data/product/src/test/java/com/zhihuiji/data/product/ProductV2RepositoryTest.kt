package com.zhihuiji.data.product

import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.entity.ProductEntity
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.data.sync.LocalSyncRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductV2RepositoryTest {

    @Test
    fun listProductsForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.product.ProductV2Dto>())
        }

        val repository = ProductV2Repository(api, fakeProductDao(), fakeSyncRepository())
        val result = repository.listProducts(keyword = "milk", status = 1, categoryId = 2L, unitId = 3L)

        assertTrue(result.isSuccess)
        assertEquals("productsV2", invokedMethod)
        assertEquals(listOf("milk", 1, 2L, 3L), capturedArgs.take(4))
    }

    @Test
    fun deleteProductMutatesLocalProjectionAndDoesNotCallRemoteApi() = runBlocking {
        var invokedMethod: String? = null
        var deletedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = ProductV2Repository(
            api,
            fakeProductDao(
                existing = ProductEntity(
                    id = 9L,
                    code = "P-9",
                    name = "Milk",
                    categoryId = null,
                    category = "",
                    unitId = null,
                    unit = "",
                    salePrice = 10.0,
                    purchasePrice = 5.0,
                    stock = 2.0,
                    safeStock = 1.0,
                    status = 1,
                    syncStatus = 0,
                    syncVersion = 3L,
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
                onDelete = { deletedId = it },
            ),
            fakeSyncRepository(),
        )
        val result = repository.deleteProduct(9L)

        assertTrue(result.isSuccess)
        assertNull(invokedMethod)
        assertEquals(9L, deletedId)
    }

    @Test
    fun listSupplierRelationsDelegatesToProductSupplierRelationsV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedProductId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedProductId = args?.get(0) as Long
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.product.ProductSupplierRelationV2Dto>(),
            )
        }

        val repository = ProductV2Repository(api, fakeProductDao(), fakeSyncRepository())
        val result = repository.listSupplierRelations(18L)

        assertTrue(result.isSuccess)
        assertEquals("productSupplierRelationsV2", invokedMethod)
        assertEquals(18L, invokedProductId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "ProductV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }

    private fun fakeProductDao(
        existing: ProductEntity? = null,
        onDelete: (Long) -> Unit = {},
    ): ProductDao = Proxy.newProxyInstance(
        ProductDao::class.java.classLoader,
        arrayOf(ProductDao::class.java),
    ) { _, method, args ->
        when (method.name) {
            "hashCode" -> 0
            "toString" -> "ProductDaoProxy"
            "equals" -> false
            "findById" -> existing
            "deleteById" -> {
                onDelete(args?.first() as Long)
                Unit
            }
            else -> error("Unexpected ProductDao call: ${method.name}")
        }
    } as ProductDao

    private fun fakeSyncRepository(): LocalSyncRepository = object : LocalSyncRepository {
        override fun <T> encodePayload(serializer: KSerializer<T>, value: T): String = "{}"

        override fun nextLocalEntityId(): Long = -1L

        override suspend fun <T> mutateAndEnqueue(
            entityType: String,
            entityId: String,
            operation: String,
            payload: String?,
            baseVersion: Long?,
            mutation: suspend () -> T,
        ): Result<T> = runCatching { mutation() }

        override suspend fun hasUnresolvedLocalChange(entityType: String, entityId: String): Boolean = false

        override suspend fun reconcileRemoteProduct(remoteId: Long, code: String) = Unit
    }
}
