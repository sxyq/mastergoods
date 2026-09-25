package com.zhihuiji.data.sync

import com.zhihuiji.core.database.dao.SyncOutboxDao
import com.zhihuiji.core.database.dao.SyncConflictDao
import com.zhihuiji.core.database.entity.SyncOutboxEntity
import com.zhihuiji.core.database.dao.SyncRemoteRecordDao
import com.zhihuiji.core.database.entity.SyncRemoteRecordEntity
import com.zhihuiji.core.database.entity.SyncConflictEntity
import com.zhihuiji.core.model.v2.sync.CreateImportJobV2Request
import com.zhihuiji.core.model.v2.sync.ImportJobV2Dto
import com.zhihuiji.core.model.v2.sync.RetryImportJobV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorAckV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorV2Dto
import com.zhihuiji.core.model.v2.sync.SyncHealthV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Request
import com.zhihuiji.core.model.v2.sync.SyncChangeV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Response
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Request
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Response
import com.zhihuiji.core.model.v2.sync.SyncOperationFailureV2Dto
import com.zhihuiji.core.model.v2.sync.SyncOperationResultV2Dto
import com.zhihuiji.core.common.runCatchingCancellable
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import java.security.SecureRandom

private val LOCAL_ENTITY_ID_RANDOM = SecureRandom()

private fun String?.toOutboxState(): String = when (this) {
    "version_conflict",
    "permission_denied",
    "validation_failed",
    "unsupported_entity_type",
    "server_command_required" -> SyncOutboxEntity.STATE_BLOCKED
    else -> SyncOutboxEntity.STATE_FAILED
}

@Singleton
class SyncV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val syncOutboxDao: SyncOutboxDao,
    private val syncRemoteRecordDao: SyncRemoteRecordDao,
    private val syncConflictDao: SyncConflictDao,
    private val syncPreferenceStore: com.zhihuiji.core.datastore.SyncPreferenceStore,
    private val json: Json,
    private val transactionRunner: SyncTransactionRunner,
    private val syncWorkScheduler: SyncWorkScheduler = NoOpSyncWorkScheduler,
) : LocalSyncRepository {
    suspend fun health(): Result<SyncHealthV2Dto> =
        safeApiCall { api.syncHealthV2() }

    suspend fun cursor(clientId: String): Result<SyncCursorV2Dto> =
        safeApiCall { api.syncCursorV2(clientId) }

    suspend fun acknowledgeCursor(request: SyncCursorAckV2Request): Result<SyncCursorV2Dto> =
        safeApiCall { api.acknowledgeSyncCursorV2(request) }

    suspend fun upload(request: SyncUploadV2Request): Result<SyncUploadV2Response> =
        safeApiCall { api.uploadSyncChangesV2(request) }

    suspend fun pull(request: SyncPullV2Request): Result<SyncPullV2Response> =
        safeApiCall { api.pullSyncChangesV2(request) }

    suspend fun enqueue(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String?,
        baseVersion: Long?,
    ): Result<String> = runCatchingCancellable {
        val operationId = enqueueInCurrentTransaction(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            baseVersion = baseVersion,
        )
        syncWorkScheduler.scheduleNow()
        operationId
    }

    /**
     * Runs the local projection update and its matching Outbox insert in one Room
     * transaction. A write is therefore never visible without a durable retry item.
     */
    override suspend fun <T> mutateAndEnqueue(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String?,
        baseVersion: Long?,
        mutation: suspend () -> T,
    ): Result<T> = runCatchingCancellable {
        val result = transactionRunner.run {
            val mutationResult = mutation()
            enqueueInCurrentTransaction(entityType, entityId, operation, payload, baseVersion)
            mutationResult
        }
        syncWorkScheduler.scheduleNow()
        result
    }

    override fun <T> encodePayload(serializer: KSerializer<T>, value: T): String =
        json.encodeToString(serializer, value)

    override fun nextLocalEntityId(): Long {
        while (true) {
            val candidate = LOCAL_ENTITY_ID_RANDOM.nextLong() and Long.MAX_VALUE
            if (candidate != 0L) return -candidate
        }
    }

    override suspend fun hasUnresolvedLocalChange(entityType: String, entityId: String): Boolean =
        syncOutboxDao.hasUnresolvedForEntity(entityType, entityId)

    suspend fun pendingCount(): Int = syncOutboxDao.pendingCount()

    /** Resolves a recorded conflict without silently overwriting either side. */
    suspend fun resolveConflict(
        entityType: String,
        entityId: String,
        keepLocal: Boolean,
    ): Result<Unit> = runCatchingCancellable {
        transactionRunner.run {
            val conflict = syncConflictDao.findOpen(entityType, entityId)
                ?: return@run
            val localOperation = conflict.localOperationId?.let { operationId ->
                syncOutboxDao.findByOperationId(operationId)
            }
            if (keepLocal) {
                if (localOperation != null) {
                    syncOutboxDao.delete(localOperation.operationId)
                }
                enqueueInCurrentTransaction(
                    entityType = entityType,
                    entityId = entityId,
                    operation = localOperation?.operation ?: "update",
                    payload = conflict.localPayload ?: localOperation?.payload,
                    baseVersion = conflict.remoteVersion,
                )
            } else {
                syncRemoteRecordDao.find(entityType, entityId)?.let { remote ->
                    applyProjection(
                        SyncChangeV2Dto(
                            operationId = remote.operationId,
                            entityType = remote.entityType,
                            entityId = remote.entityId,
                            operation = remote.operation,
                            payload = remote.payload,
                            updatedAt = remote.updatedAt,
                            baseVersion = remote.baseVersion,
                        ),
                    )
                }
                if (localOperation != null) {
                    syncOutboxDao.delete(localOperation.operationId)
                }
            }
            syncConflictDao.markResolved(entityType, entityId, System.currentTimeMillis())
        }
        if (keepLocal) syncWorkScheduler.scheduleNow()
    }

    suspend fun syncPendingAndPull(clientId: String, limit: Int = 50): Result<String> = runCatchingCancellable {
        val pending = syncOutboxDao.pending(limit)
        if (pending.isNotEmpty()) {
            val result = upload(
                SyncUploadV2Request(
                    clientId = clientId,
                    changes = pending.map { item ->
                        SyncChangeV2Dto(
                            operationId = item.operationId,
                            entityType = item.entityType,
                            entityId = item.entityId,
                            operation = item.operation,
                            payload = item.payload,
                            updatedAt = item.createdAt,
                            baseVersion = item.baseVersion,
                        )
                    },
                ),
            ).getOrThrow()
            val operationResultsById = result.operationResults.mapNotNull { operationResult ->
                operationResult.operationId?.let { it to operationResult }
            }.toMap()
            val failuresById = result.failures.mapNotNull { failure ->
                failure.operationId?.let { it to failure }
            }.toMap()
            val acceptedOperationIds = result.acceptedOperationIds.toSet()
            pending.forEach { item ->
                settleOutboxItem(
                    item = item,
                    operationResult = operationResultsById[item.operationId],
                    failure = failuresById[item.operationId],
                    acceptedById = item.operationId in acceptedOperationIds,
                )
            }
        }
        pullApplyAndAck(clientId).getOrThrow()
    }

    private suspend fun settleOutboxItem(
        item: SyncOutboxEntity,
        operationResult: SyncOperationResultV2Dto?,
        failure: SyncOperationFailureV2Dto?,
        acceptedById: Boolean,
    ) {
        val accepted = operationResult?.status == "applied" || operationResult?.status == "duplicate" ||
            acceptedById
        if (accepted) {
            syncOutboxDao.delete(item.operationId)
            syncConflictDao.markResolved(item.entityType, item.entityId, System.currentTimeMillis())
            return
        }
        syncOutboxDao.markAttempt(
            operationId = item.operationId,
            state = (operationResult?.code ?: failure?.code).toOutboxState(),
            error = (operationResult?.message ?: failure?.message)?.takeIf(String::isNotBlank)
                ?: "server rejected sync operation",
        )
    }

    suspend fun pullApplyAndAck(clientId: String, limit: Int? = null): Result<String> = runCatchingCancellable {
        var sinceCursor = cursor(clientId).getOrThrow().lastCursor.takeIf { it.isNotBlank() }
        var latestCursor = sinceCursor.orEmpty()
        var hasMore: Boolean

        do {
            val cursorBeforePull = sinceCursor
            val response = pull(
                SyncPullV2Request(
                    clientId = clientId,
                    sinceCursor = sinceCursor,
                    limit = limit,
                ),
            ).getOrThrow()

            applyPulledChanges(response).getOrThrow()

            val ackCursor = response.nextCursor.takeIf { it.isNotBlank() }
            val cursorAdvanced = ackCursor != null && ackCursor != cursorBeforePull
            if (cursorAdvanced) {
                acknowledgeCursor(
                    SyncCursorAckV2Request(
                        clientId = clientId,
                        cursor = ackCursor,
                    ),
                ).getOrThrow()
                latestCursor = ackCursor
                sinceCursor = ackCursor
            }

            hasMore = response.hasMore
            if (hasMore && !cursorAdvanced) {
                throw IllegalStateException("sync pull reported another page without advancing the cursor")
            }
        } while (hasMore && !sinceCursor.isNullOrBlank())

        latestCursor
    }

    suspend fun applyPulledChanges(result: SyncPullV2Response): Result<Unit> = runCatchingCancellable {
        transactionRunner.run {
            result.changes.forEach { change ->
                syncRemoteRecordDao.upsert(change.toRemoteRecord())
                val localChange = syncOutboxDao.firstUnresolvedForEntity(change.entityType, change.entityId)
                if (localChange != null) {
                    syncConflictDao.upsert(
                        SyncConflictEntity(
                            entityType = change.entityType,
                            entityId = change.entityId,
                            localOperationId = localChange.operationId,
                            localPayload = localChange.payload,
                            remoteOperationId = change.operationId,
                            remotePayload = change.payload,
                            remoteVersion = change.baseVersion,
                            reason = "remote_change_while_local_pending",
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    return@forEach
                }
                applyProjection(change)
            }
        }
    }

    private suspend fun applyProjection(change: SyncChangeV2Dto) {
        // 旧业务实体的远端变更落库已随 REWRITE-CLEAN-SLATE-001B 移除；远端记录与冲突流水线保持通用，
        // 新领域接入后在此登记自己的 entityType 落库处理。
    }

    private suspend fun enqueueInCurrentTransaction(
        entityType: String,
        entityId: String,
        operation: String,
        payload: String?,
        baseVersion: Long?,
    ): String {
        val clientId = syncPreferenceStore.requireClientId()
        val operationId = "${clientId}-${java.util.UUID.randomUUID()}"
        check(syncOutboxDao.enqueue(
            SyncOutboxEntity(
                operationId = operationId,
                clientId = clientId,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload,
                baseVersion = baseVersion,
                createdAt = System.currentTimeMillis(),
            ),
        ) != -1L) { "failed to persist sync operation" }
        return operationId
    }

    suspend fun listImportJobs(status: String? = null): Result<List<ImportJobV2Dto>> =
        safeApiCall { api.importJobsV2(status) }

    suspend fun getImportJob(id: Long): Result<ImportJobV2Dto> =
        safeApiCall { api.importJobV2(id) }

    suspend fun createImportJob(request: CreateImportJobV2Request): Result<ImportJobV2Dto> =
        safeApiCall { api.createImportJobV2(request) }

    suspend fun retryImportJob(id: Long, request: RetryImportJobV2Request? = null): Result<ImportJobV2Dto> =
        safeApiCall { api.retryImportJobV2(id, request) }

    suspend fun cancelImportJob(id: Long): Result<ImportJobV2Dto> =
        safeApiCall { api.cancelImportJobV2(id) }

    private fun SyncChangeV2Dto.isDelete(): Boolean =
        operation.equals("delete", ignoreCase = true)

    private fun SyncChangeV2Dto.toRemoteRecord(): SyncRemoteRecordEntity =
        SyncRemoteRecordEntity(
            entityType = entityType,
            entityId = entityId,
            operationId = operationId,
            operation = operation,
            payload = payload,
            baseVersion = baseVersion,
            updatedAt = updatedAt ?: System.currentTimeMillis(),
            isDeleted = isDelete(),
            receivedAt = System.currentTimeMillis(),
        )

}
