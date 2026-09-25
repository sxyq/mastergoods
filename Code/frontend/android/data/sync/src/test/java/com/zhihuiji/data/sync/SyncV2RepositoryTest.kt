package com.zhihuiji.data.sync

import com.zhihuiji.core.database.dao.SyncOutboxDao
import com.zhihuiji.core.database.dao.SyncRemoteRecordDao
import com.zhihuiji.core.database.entity.SyncConflictEntity
import com.zhihuiji.core.database.entity.SyncRemoteRecordEntity
import com.zhihuiji.core.database.entity.SyncOutboxEntity
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.sync.SyncChangeV2Dto
import com.zhihuiji.core.model.v2.sync.SyncCursorAckV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorV2Dto
import com.zhihuiji.core.model.v2.sync.SyncHealthV2Dto
import com.zhihuiji.core.model.v2.sync.SyncOperationFailureV2Dto
import com.zhihuiji.core.model.v2.sync.SyncOperationResultV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Response
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Request
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Response
import com.zhihuiji.core.network.ZhihuijiV2Api
import java.io.File
import java.io.IOException
import java.lang.reflect.Proxy
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
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

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
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
                                    entityType = "note",
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

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(1, pulledRequests.size)
        assertEquals(1, ackRequests.size)
        assertEquals("client-a", ackRequests.single().clientId)
        assertEquals("cursor-1", ackRequests.single().cursor)
        assertEquals("cursor-1", result.getOrNull())
    }

    @Test
    fun pullApplyAndAckContinuesWhenEachPageAdvancesTheCursor() = runBlocking {
        var pullCount = 0
        val acknowledgements = mutableListOf<String>()
        val api = fakeApi { methodName, args ->
            when (methodName) {
                "syncCursorV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncCursorV2Dto(clientId = "client-a", lastCursor = "cursor-0"),
                )
                "pullSyncChangesV2" -> {
                    pullCount++
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncPullV2Response(
                            changes = emptyList(),
                            nextCursor = "cursor-$pullCount",
                            hasMore = pullCount == 1,
                        ),
                    )
                }
                "acknowledgeSyncCursorV2" -> {
                    val request = args?.get(0) as SyncCursorAckV2Request
                    acknowledgements += request.cursor
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncCursorV2Dto(clientId = request.clientId, lastCursor = request.cursor),
                    )
                }
                else -> error("Unexpected API method: $methodName")
            }
        }

        val result = repository(api).pullApplyAndAck(clientId = "client-a")

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(2, pullCount)
        assertEquals(listOf("cursor-1", "cursor-2"), acknowledgements)
        assertEquals("cursor-2", result.getOrNull())
    }

    @Test
    fun syncPendingAndPullSettlesEachServerOperationResultByOperationId() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "op-applied",
                clientId = "client-a",
                entityType = "note",
                entityId = "1",
                operation = "create",
                payload = "{}",
                baseVersion = 0L,
                createdAt = 1L,
            ),
            SyncOutboxEntity(
                operationId = "op-duplicate",
                clientId = "client-a",
                entityType = "note",
                entityId = "2",
                operation = "create",
                payload = "{}",
                baseVersion = 0L,
                createdAt = 2L,
            ),
            SyncOutboxEntity(
                operationId = "op-conflict",
                clientId = "client-a",
                entityType = "note",
                entityId = "3",
                operation = "update",
                payload = "{\"title\":\"本地\"}",
                baseVersion = 1L,
                createdAt = 3L,
            ),
        )
        var uploadedRequest: SyncUploadV2Request? = null
        val api = fakeApi { methodName, args ->
            when (methodName) {
                "uploadSyncChangesV2" -> {
                    uploadedRequest = args?.get(0) as SyncUploadV2Request
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncUploadV2Response(
                            acceptedOperationIds = listOf("op-applied"),
                            operationResults = listOf(
                                SyncOperationResultV2Dto(
                                    operationId = "op-duplicate",
                                    status = "duplicate",
                                ),
                                SyncOperationResultV2Dto(
                                    operationId = "op-conflict",
                                    status = "conflict",
                                    code = "version_conflict",
                                    message = "server version advanced",
                                ),
                            ),
                            failures = listOf(
                                SyncOperationFailureV2Dto(
                                    operationId = "op-conflict",
                                    code = "version_conflict",
                                    message = "server version advanced",
                                ),
                            ),
                        ),
                    )
                }
                "syncCursorV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncCursorV2Dto(clientId = "client-a", lastCursor = ""),
                )
                "pullSyncChangesV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncPullV2Response(nextCursor = ""),
                )
                else -> error("Unexpected API method: $methodName")
            }
        }

        val result = repository(
            api = api,
            outboxDao = recordingOutboxDao(pending),
        ).syncPendingAndPull(clientId = "client-a")

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(
            listOf("op-applied", "op-duplicate", "op-conflict"),
            uploadedRequest?.changes?.mapNotNull { it.operationId },
        )
        assertEquals(1, pending.size)
        assertEquals("op-conflict", pending.single().operationId)
        assertEquals(SyncOutboxEntity.STATE_BLOCKED, pending.single().state)
        assertEquals("server version advanced", pending.single().lastError)
    }

    @Test
    fun syncPendingAndPullUploadsDeleteAndSettlesIt() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "op-delete",
                clientId = "client-a",
                entityType = "note",
                entityId = "88",
                operation = "delete",
                payload = null,
                baseVersion = 3L,
                createdAt = 4L,
            ),
        )
        var uploadedRequest: SyncUploadV2Request? = null
        val api = fakeApi { methodName, args ->
            when (methodName) {
                "uploadSyncChangesV2" -> {
                    uploadedRequest = args?.get(0) as SyncUploadV2Request
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncUploadV2Response(
                            acceptedOperationIds = listOf("op-delete"),
                            operationResults = listOf(
                                SyncOperationResultV2Dto(
                                    operationId = "op-delete",
                                    status = "applied",
                                ),
                            ),
                        ),
                    )
                }
                "syncCursorV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncCursorV2Dto(clientId = "client-a", lastCursor = ""),
                )
                "pullSyncChangesV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncPullV2Response(nextCursor = ""),
                )
                else -> error("Unexpected API method: $methodName")
            }
        }

        val result = repository(
            api = api,
            outboxDao = recordingOutboxDao(pending),
        ).syncPendingAndPull(clientId = "client-a")

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        val uploadedChange = uploadedRequest?.changes?.singleOrNull()
            ?: error("delete was not uploaded")
        assertEquals("note", uploadedChange.entityType)
        assertEquals("88", uploadedChange.entityId)
        assertEquals("delete", uploadedChange.operation)
        assertEquals(null, uploadedChange.payload)
        assertEquals(3L, uploadedChange.baseVersion)
        assertTrue(pending.isEmpty())
    }

    @Test
    fun failedDeleteRemainsPendingForTheNextSyncAttempt() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "op-delete-retry",
                clientId = "client-a",
                entityType = "note",
                entityId = "89",
                operation = "delete",
                payload = null,
                baseVersion = 2L,
                createdAt = 5L,
            ),
        )
        var uploadAttempts = 0
        val api = fakeApi { methodName, args ->
            when (methodName) {
                "uploadSyncChangesV2" -> {
                    uploadAttempts++
                    if (uploadAttempts == 1) throw IOException("offline")
                    ApiResponse(
                        code = 0,
                        message = "ok",
                        data = SyncUploadV2Response(
                            acceptedOperationIds = listOf("op-delete-retry"),
                        ),
                    )
                }
                "syncCursorV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncCursorV2Dto(clientId = "client-a", lastCursor = ""),
                )
                "pullSyncChangesV2" -> ApiResponse(
                    code = 0,
                    message = "ok",
                    data = SyncPullV2Response(nextCursor = ""),
                )
                else -> error("Unexpected API method: $methodName")
            }
        }
        val repository = repository(
            api = api,
            outboxDao = recordingOutboxDao(pending),
        )

        val firstAttempt = repository.syncPendingAndPull(clientId = "client-a")

        assertTrue(firstAttempt.isFailure)
        assertEquals(1, uploadAttempts)
        assertEquals(1, pending.size)
        assertEquals(SyncOutboxEntity.STATE_PENDING, pending.single().state)
        assertEquals(0, pending.single().attempts)

        val secondAttempt = repository.syncPendingAndPull(clientId = "client-a")

        assertTrue("failure: ${secondAttempt.exceptionOrNull()}", secondAttempt.isSuccess)
        assertEquals(2, uploadAttempts)
        assertTrue(pending.isEmpty())
    }

    @Test
    fun applyPulledChangesPersistsUnknownEntitiesBeforeTheirCursorCanBeAcknowledged() = runBlocking {
        val remoteRecords = mutableListOf<SyncRemoteRecordEntity>()
        var transactionCount = 0
        val repository = repository(
            api = fakeApi { methodName, _ -> error("Unexpected API method: $methodName") },
            remoteRecordDao = recordingRemoteRecordDao(remoteRecords),
            transactionRunner = object : SyncTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T {
                    transactionCount++
                    return block()
                }
            },
        )

        val result = repository.applyPulledChanges(
            SyncPullV2Response(
                changes = listOf(
                    SyncChangeV2Dto(
                        operationId = "remote-op-1",
                        entityType = "agent_draft",
                        entityId = "42",
                        operation = "update",
                        payload = "{\"title\":\"真实草稿\"}",
                        updatedAt = 123L,
                        baseVersion = 4L,
                    ),
                ),
            ),
        )

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(1, transactionCount)
        assertEquals(1, remoteRecords.size)
        assertEquals("agent_draft", remoteRecords.single().entityType)
        assertEquals("42", remoteRecords.single().entityId)
        assertFalse(remoteRecords.single().isDeleted)
        assertEquals("{\"title\":\"真实草稿\"}", remoteRecords.single().payload)
    }

    @Test
    fun mutateAndEnqueuePersistsTheOutboxRecordInsideTheLocalTransaction() = runBlocking {
        val queued = mutableListOf<SyncOutboxEntity>()
        var transactionCount = 0
        val repository = repository(
            api = fakeApi { methodName, _ -> error("Unexpected API method: $methodName") },
            outboxDao = recordingOutboxDao(queued),
            transactionRunner = object : SyncTransactionRunner {
                override suspend fun <T> run(block: suspend () -> T): T {
                    transactionCount++
                    return block()
                }
            },
        )

        var localMutationApplied = false
        val result = repository.mutateAndEnqueue(
            entityType = "note",
            entityId = "-42",
            operation = "create",
            payload = "{\"title\":\"离线记录\"}",
            baseVersion = 0L,
        ) {
            localMutationApplied = true
            "local-result"
        }

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals("local-result", result.getOrNull())
        assertTrue(localMutationApplied)
        assertEquals(1, transactionCount)
        assertEquals(1, queued.size)
        assertEquals("note", queued.single().entityType)
        assertEquals("-42", queued.single().entityId)
        assertEquals("create", queued.single().operation)
        assertEquals(0L, queued.single().baseVersion)
    }

    @Test
    fun remoteChangeWithPendingLocalMutationIsStoredAsConflictWithoutOverwritingProjection() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "local-op-1",
                clientId = "client-a",
                entityType = "note",
                entityId = "42",
                operation = "update",
                payload = "{\"title\":\"本地修改\"}",
                baseVersion = 1L,
                createdAt = 1L,
            ),
        )
        val conflicts = mutableListOf<com.zhihuiji.core.database.entity.SyncConflictEntity>()

        val result = repository(
            api = fakeApi { methodName, _ -> error("Unexpected API method: $methodName") },
            outboxDao = recordingOutboxDao(pending),
            syncConflictDao = recordingConflictDao(conflicts),
        ).applyPulledChanges(
            SyncPullV2Response(
                changes = listOf(
                    SyncChangeV2Dto(
                        operationId = "remote-op-1",
                        entityType = "note",
                        entityId = "42",
                        operation = "update",
                        payload = "{\"title\":\"服务器修改\"}",
                        baseVersion = 2L,
                    ),
                ),
            ),
        )

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(1, conflicts.size)
        assertEquals("local-op-1", conflicts.single().localOperationId)
        assertEquals("remote-op-1", conflicts.single().remoteOperationId)
        assertEquals("open", conflicts.single().state)
    }

    @Test
    fun keepLocalConflictDeletesBlockedOperationAndQueuesAgainstRemoteVersion() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "local-op-1",
                clientId = "client-a",
                entityType = "note",
                entityId = "42",
                operation = "update",
                payload = "{\"title\":\"本地修改\"}",
                baseVersion = 1L,
                createdAt = 1L,
                state = SyncOutboxEntity.STATE_BLOCKED,
            ),
        )
        val conflicts = mutableListOf(
            SyncConflictEntity(
                entityType = "note",
                entityId = "42",
                localOperationId = "local-op-1",
                localPayload = "{\"title\":\"本地修改\"}",
                remoteOperationId = "remote-op-1",
                remotePayload = "{\"title\":\"服务器修改\"}",
                remoteVersion = 2L,
                reason = "remote_change_while_local_pending",
                createdAt = 2L,
            ),
        )

        val result = repository(
            api = fakeApi { methodName, _ -> error("Unexpected API method: $methodName") },
            outboxDao = recordingOutboxDao(pending),
            syncConflictDao = recordingConflictDao(conflicts),
        ).resolveConflict("note", "42", keepLocal = true)

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(1, pending.size)
        assertEquals(SyncOutboxEntity.STATE_PENDING, pending.single().state)
        assertEquals(2L, pending.single().baseVersion)
        assertEquals("{\"title\":\"本地修改\"}", pending.single().payload)
        assertEquals(SyncConflictEntity.STATE_RESOLVED, conflicts.single().state)
    }

    @Test
    fun acceptRemoteConflictSettlesOutboxAndMarksConflictResolvedWithoutRequeue() = runBlocking {
        val pending = mutableListOf(
            SyncOutboxEntity(
                operationId = "local-op-1",
                clientId = "client-a",
                entityType = "note",
                entityId = "42",
                operation = "update",
                payload = "{\"title\":\"本地修改\"}",
                baseVersion = 1L,
                createdAt = 1L,
                state = SyncOutboxEntity.STATE_BLOCKED,
            ),
        )
        val conflicts = mutableListOf(
            SyncConflictEntity(
                entityType = "note",
                entityId = "42",
                localOperationId = "local-op-1",
                localPayload = "{\"title\":\"本地修改\"}",
                remoteOperationId = "remote-op-1",
                remotePayload = "{\"title\":\"服务器修改\"}",
                remoteVersion = 2L,
                reason = "remote_change_while_local_pending",
                createdAt = 2L,
            ),
        )
        val remoteRecords = mutableListOf(
            SyncRemoteRecordEntity(
                entityType = "note",
                entityId = "42",
                operationId = "remote-op-1",
                operation = "update",
                payload = "{\"id\":42,\"title\":\"服务器修改\"}",
                baseVersion = 2L,
                updatedAt = 2L,
                isDeleted = false,
                receivedAt = 2L,
            ),
        )

        val result = repository(
            api = fakeApi { methodName, _ -> error("Unexpected API method: $methodName") },
            outboxDao = recordingOutboxDao(pending),
            remoteRecordDao = recordingRemoteRecordDao(remoteRecords),
            syncConflictDao = recordingConflictDao(conflicts),
        ).resolveConflict("note", "42", keepLocal = false)

        assertTrue("failure: ${result.exceptionOrNull()}", result.isSuccess)
        assertTrue(pending.isEmpty())
        assertEquals(SyncConflictEntity.STATE_RESOLVED, conflicts.single().state)
    }

    private fun repository(
        api: ZhihuijiV2Api,
        remoteRecordDao: SyncRemoteRecordDao = fakeDao(SyncRemoteRecordDao::class.java),
        outboxDao: SyncOutboxDao = fakeDao(SyncOutboxDao::class.java),
        syncConflictDao: com.zhihuiji.core.database.dao.SyncConflictDao =
            fakeDao(com.zhihuiji.core.database.dao.SyncConflictDao::class.java),
        transactionRunner: SyncTransactionRunner = immediateTransactionRunner,
    ): SyncV2Repository {
        return SyncV2Repository(
            api = api,
            syncOutboxDao = outboxDao,
            syncRemoteRecordDao = remoteRecordDao,
            syncConflictDao = syncConflictDao,
            syncPreferenceStore = com.zhihuiji.core.datastore.SyncPreferenceStore(
                PreferenceDataStoreFactory.create { File.createTempFile("sync-test-", ".preferences_pb") },
            ),
            json = Json { ignoreUnknownKeys = true },
            transactionRunner = transactionRunner,
        )
    }

    private val immediateTransactionRunner = object : SyncTransactionRunner {
        override suspend fun <T> run(block: suspend () -> T): T = block()
    }

    private fun recordingRemoteRecordDao(records: MutableList<SyncRemoteRecordEntity>): SyncRemoteRecordDao {
        return Proxy.newProxyInstance(
            SyncRemoteRecordDao::class.java.classLoader,
            arrayOf(SyncRemoteRecordDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SyncRemoteRecordDaoProxy"
                "equals" -> false
                "upsert" -> {
                    records += args?.first() as SyncRemoteRecordEntity
                    Unit
                }
                "find" -> records.firstOrNull {
                    it.entityType == args?.get(0) && it.entityId == args?.get(1)
                }
                else -> error("Unexpected remote record DAO call: ${method.name}")
            }
        } as SyncRemoteRecordDao
    }

    private fun recordingOutboxDao(records: MutableList<SyncOutboxEntity>): SyncOutboxDao {
        return Proxy.newProxyInstance(
            SyncOutboxDao::class.java.classLoader,
            arrayOf(SyncOutboxDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SyncOutboxDaoProxy"
                "equals" -> false
                "enqueue" -> {
                    records += args?.first() as SyncOutboxEntity
                    1L
                }
                "hasUnresolvedForEntity" -> records.any {
                    it.entityType == args?.get(0) && it.entityId == args?.get(1) &&
                        it.state in setOf(
                            SyncOutboxEntity.STATE_PENDING,
                            SyncOutboxEntity.STATE_FAILED,
                            SyncOutboxEntity.STATE_BLOCKED,
                        )
                }
                "firstUnresolvedForEntity" -> records.firstOrNull {
                    it.entityType == args?.get(0) && it.entityId == args?.get(1) &&
                        it.state in setOf(
                            SyncOutboxEntity.STATE_PENDING,
                            SyncOutboxEntity.STATE_FAILED,
                            SyncOutboxEntity.STATE_BLOCKED,
                        )
                }
                "findByOperationId" -> records.firstOrNull { it.operationId == args?.first() }
                "pending" -> records.toList()
                "markAttempt" -> {
                    val operationId = args?.get(0) as String
                    val index = records.indexOfFirst { it.operationId == operationId }
                    if (index >= 0) {
                        records[index] = records[index].copy(
                            attempts = records[index].attempts + 1,
                            state = args[1] as String,
                            lastError = args[2] as String?,
                        )
                    }
                    Unit
                }
                "delete" -> {
                    records.removeIf { it.operationId == args?.first() }
                    Unit
                }
                else -> error("Unexpected outbox DAO call: ${method.name}")
            }
        } as SyncOutboxDao
    }

    private fun recordingConflictDao(
        conflicts: MutableList<com.zhihuiji.core.database.entity.SyncConflictEntity>,
    ): com.zhihuiji.core.database.dao.SyncConflictDao {
        return Proxy.newProxyInstance(
            com.zhihuiji.core.database.dao.SyncConflictDao::class.java.classLoader,
            arrayOf(com.zhihuiji.core.database.dao.SyncConflictDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "hashCode" -> 0
                "toString" -> "SyncConflictDaoProxy"
                "equals" -> false
                "upsert" -> {
                    conflicts += args?.first() as com.zhihuiji.core.database.entity.SyncConflictEntity
                    Unit
                }
                "findOpen" -> conflicts.firstOrNull {
                    it.entityType == args?.get(0) && it.entityId == args?.get(1) &&
                        it.state == SyncConflictEntity.STATE_OPEN
                }
                "markResolved" -> {
                    val index = conflicts.indexOfFirst {
                        it.entityType == args?.get(0) && it.entityId == args?.get(1)
                    }
                    if (index >= 0) {
                        conflicts[index] = conflicts[index].copy(
                            state = SyncConflictEntity.STATE_RESOLVED,
                            resolvedAt = args?.get(2) as Long,
                        )
                    }
                    Unit
                }
                "clear" -> Unit
                else -> error("Unexpected conflict DAO call: ${method.name}")
            }
        } as com.zhihuiji.core.database.dao.SyncConflictDao
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
                "markResolved",
                "delete",
                "clear" -> Unit
                "hasUnresolvedForEntity" -> false
                "firstUnresolvedForEntity" -> null
                "find" -> null
                else -> error("Unexpected DAO call in SyncV2RepositoryTest: ${clazz.simpleName}.${method.name}")
            }
        } as T
    }
}
