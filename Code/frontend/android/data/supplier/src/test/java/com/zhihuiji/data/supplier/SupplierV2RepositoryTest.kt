package com.zhihuiji.data.supplier

import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.entity.SupplierEntity
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

class SupplierV2RepositoryTest {

    @Test
    fun listSuppliersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.partner.SupplierV2Dto>())
        }

        val repository = SupplierV2Repository(api, fakeSupplierDao(), fakeSyncRepository())
        val result = repository.listSuppliers(keyword = "acme", status = 1, groupId = 5L)

        assertTrue(result.isSuccess)
        assertEquals("suppliersV2", invokedMethod)
        assertEquals(listOf("acme", 1, 5L), capturedArgs.take(3))
    }

    @Test
    fun deleteSupplierMutatesLocalProjectionAndDoesNotCallRemoteApi() = runBlocking {
        var invokedMethod: String? = null
        var deletedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = SupplierV2Repository(
            api,
            fakeSupplierDao(
                existing = SupplierEntity(
                    id = 21L,
                    name = "Acme",
                    phone = "13900000000",
                    groupId = null,
                    primaryContactName = null,
                    primaryContactPhone = null,
                    address = null,
                    notes = null,
                    balance = 0.0,
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
        val result = repository.deleteSupplier(21L)

        assertTrue(result.isSuccess)
        assertNull(invokedMethod)
        assertEquals(21L, deletedId)
    }

    @Test
    fun listContactsDelegatesToSupplierContactsV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedSupplierId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedSupplierId = args?.get(0) as Long
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto>(),
            )
        }

        val repository = SupplierV2Repository(api, fakeSupplierDao(), fakeSyncRepository())
        val result = repository.listContacts(44L)

        assertTrue(result.isSuccess)
        assertEquals("supplierContactsV2", invokedMethod)
        assertEquals(44L, invokedSupplierId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SupplierV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }

    private fun fakeSupplierDao(
        existing: SupplierEntity? = null,
        onDelete: (Long) -> Unit = {},
    ): SupplierDao = Proxy.newProxyInstance(
        SupplierDao::class.java.classLoader,
        arrayOf(SupplierDao::class.java),
    ) { _, method, args ->
        when (method.name) {
            "hashCode" -> 0
            "toString" -> "SupplierDaoProxy"
            "equals" -> false
            "findById" -> existing
            "deleteById" -> {
                onDelete(args?.first() as Long)
                Unit
            }
            else -> error("Unexpected SupplierDao call: ${method.name}")
        }
    } as SupplierDao

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
