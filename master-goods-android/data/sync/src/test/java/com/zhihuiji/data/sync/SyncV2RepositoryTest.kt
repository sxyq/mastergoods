package com.zhihuiji.data.sync

import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.dao.FinanceRecordDao
import com.zhihuiji.core.database.dao.PayOrderDao
import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.dao.PurchaseOrderDao
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.sync.SyncChangeV2Dto
import com.zhihuiji.core.model.v2.sync.SyncCursorAckV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorV2Dto
import com.zhihuiji.core.model.v2.sync.SyncHealthV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Response
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncV2RepositoryTest {

    @Test
    fun healthDelegatesToSyncHealthV2() = runBlocking {
        var invokedMethod: String? = null
        val api = fakeApi { methodName, _ ->
            invokedMethod = methodName
            ApiResponse(code = 0, message = "ok", data = SyncHealthV2Dto(status = "ok"))
        }

        val repository = repository(api)
        val result = repository.health()

        assertTrue(result.isSuccess)
        assertEquals("syncHealthV2", invokedMethod)
        assertEquals("ok", result.getOrNull()?.status)
    }

    @Test
    fun listImportJobsForwardsStatusArgumentToApi() = runBlocking {
        var invokedMethod: String? = null
        var capturedStatus: String? = null
        val api = fakeApi { methodName, args ->
            invokedMethod = methodName
            capturedStatus = args?.get(0) as String?
            ApiResponse(code = 0, message = "ok", data = emptyList<com.zhihuiji.core.model.v2.sync.ImportJobV2Dto>())
        }

        val repository = repository(api)
        val result = repository.listImportJobs("running")

        assertTrue(result.isSuccess)
        assertEquals("importJobsV2", invokedMethod)
        assertEquals("running", capturedStatus)
    }

    @Test
    fun pullApplyAndAckUsesNextCursorAsAckCursorAndReturnsIt() = runBlocking {
        val pulledRequests = mutableListOf<Any?>()
        val ackRequests = mutableListOf<SyncCursorAckV2Request>()
        val api = fakeApi { methodName, args ->
            when (methodName) {
                "syncCursorV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncCursorV2Dto(clientId = "client-a", lastCursor = "cursor-0"),
                )
                "pullSyncChangesV2" -> {
                    pulledRequests += args?.get(0)
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncPullV2Response(
                            changes = listOf(
                                SyncChangeV2Dto(
                                    entityType = "customer",
                                    entityId = "1",
                                    operation = "delete",
                                ),
                            ),
                            nextCursor = "cursor-1",
                            hasMore = false,
                        ),
                    )
                }
                "acknowledgeSyncCursorV2" -> {
                    val request = args?.get(0) as SyncCursorAckV2Request
                    ackRequests += request
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncCursorV2Dto(clientId = request.clientId, lastCursor = request.cursor),
                    )
                }
                else -> error("Unexpected API method: $methodName")
            }
        }

        val repository = repository(api)
        val result = repository.pullApplyAndAck(clientId = "client-a", limit = 50)

        assertTrue(result.isSuccess)
        assertEquals(1, pulledRequests.size)
        assertEquals(1, ackRequests.size)
        assertEquals("client-a", ackRequests.single().clientId)
        assertEquals("cursor-1", ackRequests.single().cursor)
        assertEquals("cursor-1", result.getOrNull())
    }

    private fun repository(api: ZhihuijiV2Api): SyncV2Repository {
        return SyncV2Repository(
            api = api,
            productDao = fakeDao(ProductDao::class.java),
            customerDao = fakeDao(CustomerDao::class.java),
            supplierDao = fakeDao(SupplierDao::class.java),
            saleOrderDao = fakeDao(SaleOrderDao::class.java),
            purchaseOrderDao = fakeDao(PurchaseOrderDao::class.java),
            payOrderDao = fakeDao(PayOrderDao::class.java),
            financeRecordDao = fakeDao(FinanceRecordDao::class.java),
            json = Json { ignoreUnknownKeys = true },
        )
    }

    private fun fakeApi(handler: (methodName: String, args: Array<out Any?>?) -> Any?): ZhihuijiV2Api {
        return Proxy.newProxyInstance(
            ZhihuijiV2Api::class.java.classLoader,
            arrayOf(ZhihuijiV2Api::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SyncV2RepositoryTestApiProxy"
                "equals" -> false
                else -> handler(method.name, args)
            }
        } as ZhihuijiV2Api
    }

    private fun <T> fakeDao(clazz: Class<T>): T {
        return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz)) { _, method, _ ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "${clazz.simpleName}Proxy"
                "equals" -> false
                "deleteById",
                "upsert",
                "upsertAll",
                "upsertItems",
                "deleteItemsByOrderId",
                "deleteItemById",
                "deleteItemsByOrderIds",
                "clear" -> Unit
                else -> error("Unexpected DAO call in SyncV2RepositoryTest: ${clazz.simpleName}.${method.name}")
            }
        } as T
    }
}
