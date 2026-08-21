package com.zhihuiji.data.customer

import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.entity.CustomerEntity
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

class CustomerV2RepositoryTest {

    @Test
    fun listCustomersForwardsFilterArgumentsToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedArgs: List<Any?> = emptyList()
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedArgs = args?.toList().orEmpty()
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.partner.CustomerV2Dto>())
        }

        val repository = CustomerV2Repository(api, fakeCustomerDao(), fakeSyncRepository())
        val result = repository.listCustomers(keyword = "alice", status = 1, groupId = 7L)

        assertTrue(result.isSuccess)
        assertEquals("customersV2", invokedMethod)
        assertEquals(listOf("alice", 1, 7L), capturedArgs.take(3))
    }

    @Test
    fun deleteCustomerMutatesLocalProjectionAndDoesNotCallRemoteApi() = runBlocking {
        var invokedMethod: String? = null
        var deletedId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            ApiResponse<Unit>(code = 0, message = "ok", data = null)
        }

        val repository = CustomerV2Repository(
            api,
            fakeCustomerDao(
                existing = CustomerEntity(
                    id = 12L,
                    name = "Alice",
                    phone = "13800000000",
                    level = 1,
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
        val result = repository.deleteCustomer(12L)

        assertTrue(result.isSuccess)
        assertNull(invokedMethod)
        assertEquals(12L, deletedId)
    }

    @Test
    fun listContactsDelegatesToCustomerContactsV2() = runBlocking {
        var invokedMethod: String? = null
        var invokedCustomerId: Long? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            invokedCustomerId = args?.get(0) as Long
            ApiResponse(
                code = 0,
                message = "ok",
                data = emptyList<com.zhihuiji.core.model.v2.partner.PartnerContactV2Dto>(),
            )
        }

        val repository = CustomerV2Repository(api, fakeCustomerDao(), fakeSyncRepository())
        val result = repository.listContacts(33L)

        assertTrue(result.isSuccess)
        assertEquals("customerContactsV2", invokedMethod)
        assertEquals(33L, invokedCustomerId)
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "CustomerV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }

    private fun fakeCustomerDao(
        existing: CustomerEntity? = null,
        onDelete: (Long) -> Unit = {},
    ): CustomerDao = Proxy.newProxyInstance(
        CustomerDao::class.java.classLoader,
        arrayOf(CustomerDao::class.java),
    ) { _, method, args ->
        when (method.name) {
            "hashCode" -> 0
            "toString" -> "CustomerDaoProxy"
            "equals" -> false
            "findById" -> existing
            "deleteById" -> {
                onDelete(args?.first() as Long)
                Unit
            }
            else -> error("Unexpected CustomerDao call: ${method.name}")
        }
    } as CustomerDao

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
