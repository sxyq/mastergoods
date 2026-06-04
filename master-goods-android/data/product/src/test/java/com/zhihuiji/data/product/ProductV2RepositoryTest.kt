package com.zhihuiji.data.product

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

        val repository = ProductV2Repository(api)
        val result = repository.listProducts(keyword = "milk", status = 1, categoryId = 2L, unitId = 3L)

        assertTrue(result.isSuccess)
        assertEquals("productsV2", invokedMethod)
        assertEquals(listOf("milk", 1, 2L, 3L), capturedArgs.take(4))
    }

    @Test
    fun deleteProductDelegatesToDeleteProductV2AndSucceeds() = runBlocking {
        var invokedMethod: String? = null
        var invokedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedId = args?.get(0) as Long
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = ProductV2Repository(api)
        val result = repository.deleteProduct(9L)

        assertTrue(result.isSuccess)
        assertEquals("deleteProductV2", invokedMethod)
        assertEquals(9L, invokedId)
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

        val repository = ProductV2Repository(api)
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
}
