package com.zhihuiji.data.sync

import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.dao.FinanceRecordDao
import com.zhihuiji.core.database.dao.PayOrderDao
import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.dao.PurchaseOrderDao
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.entity.CustomerEntity
import com.zhihuiji.core.database.entity.FinanceRecordEntity
import com.zhihuiji.core.database.entity.PayOrderEntity
import com.zhihuiji.core.database.entity.ProductEntity
import com.zhihuiji.core.database.entity.PurchaseOrderEntity
import com.zhihuiji.core.database.entity.SaleOrderEntity
import com.zhihuiji.core.database.entity.SaleOrderItemEntity
import com.zhihuiji.core.database.entity.SupplierEntity
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
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.KSerializer
import java.security.SecureRandom

private val EMPTY_JSON_OBJECT = JsonObject(emptyMap())
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
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val saleOrderDao: SaleOrderDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val payOrderDao: PayOrderDao,
    private val financeRecordDao: FinanceRecordDao,
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
    ): Result<String> = runCatching {
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
    ): Result<T> = runCatching {
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
    ): Result<Unit> = runCatching {
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

    /**
     * Server-side create operations may allocate a positive ID for a local negative
     * temporary ID. Once the matching local Outbox row is settled, retain the
     * authoritative server row and remove the resolved temporary projection.
     */
    override suspend fun reconcileRemoteProduct(remoteId: Long, code: String) {
        if (remoteId <= 0L || code.isBlank()) return
        val temporary = productDao.findTemporaryByCode(code) ?: return
        if (hasUnresolvedLocalChange("product", temporary.id.toString())) return
        productDao.deleteById(temporary.id)
    }

    suspend fun syncPendingAndPull(clientId: String, limit: Int = 50): Result<String> = runCatching {
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

    suspend fun pullApplyAndAck(clientId: String, limit: Int? = null): Result<String> = runCatching {
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

    suspend fun applyPulledChanges(result: SyncPullV2Response): Result<Unit> = runCatching {
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
        when (change.entityType) {
            "customer" -> applyCustomerChange(change)
            "supplier" -> applySupplierChange(change)
            "product" -> applyProductChange(change)
            "sale_order" -> applySaleOrderChange(change)
            "sale_order_item" -> applySaleOrderItemChange(change)
            "purchase_order" -> applyPurchaseOrderChange(change)
            "pay_order" -> applyPayOrderChange(change)
            "finance_record" -> applyFinanceRecordChange(change)
        }
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

    private suspend fun applyCustomerChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            customerDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        customerDao.upsert(
            CustomerEntity(
                id = id,
                name = payload.string("name"),
                phone = payload.string("phone"),
                level = payload.int("level"),
                groupId = payload.longOrNull("group_id"),
                primaryContactName = payload.stringOrNull("contact_name")
                    ?: payload.stringOrNull("primary_contact_name"),
                primaryContactPhone = payload.stringOrNull("contact_phone")
                    ?: payload.stringOrNull("primary_contact_phone"),
                address = payload.stringOrNull("address"),
                notes = payload.stringOrNull("notes"),
                balance = payload.double("balance"),
                status = payload.int("status", default = 1),
                syncStatus = payload.intOrNull("sync_status"),
                syncVersion = payload.longOrNull("sync_version"),
                createdAt = payload.longOrNull("created_at"),
                updatedAt = payload.longOrNull("updated_at"),
            ),
        )
    }

    private suspend fun applySupplierChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            supplierDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        supplierDao.upsert(
            SupplierEntity(
                id = id,
                name = payload.string("name"),
                phone = payload.string("phone"),
                groupId = payload.longOrNull("group_id"),
                primaryContactName = payload.stringOrNull("contact_name")
                    ?: payload.stringOrNull("primary_contact_name"),
                primaryContactPhone = payload.stringOrNull("contact_phone")
                    ?: payload.stringOrNull("primary_contact_phone"),
                address = payload.stringOrNull("address"),
                notes = payload.stringOrNull("notes"),
                balance = payload.double("balance"),
                status = payload.int("status", default = 1),
                syncStatus = payload.intOrNull("sync_status"),
                syncVersion = payload.longOrNull("sync_version"),
                createdAt = payload.longOrNull("created_at"),
                updatedAt = payload.longOrNull("updated_at"),
            ),
        )
    }

    private suspend fun applyProductChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            productDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        val code = payload.string("code")
        reconcileRemoteProduct(id, code)
        productDao.upsert(
            ProductEntity(
                id = id,
                code = code,
                name = payload.string("name"),
                categoryId = payload.longOrNull("category_id"),
                category = payload.stringOrNull("category").orEmpty(),
                unitId = payload.longOrNull("unit_id"),
                unit = payload.stringOrNull("unit").orEmpty(),
                salePrice = payload.double("sale_price"),
                purchasePrice = payload.double("purchase_price"),
                stock = payload.double("stock"),
                safeStock = payload.double("safe_stock"),
                status = payload.int("status", default = 1),
                syncStatus = payload.intOrNull("sync_status"),
                syncVersion = payload.longOrNull("sync_version"),
                createdAt = payload.longOrNull("created_at"),
                updatedAt = payload.longOrNull("updated_at"),
            ),
        )
    }

    private suspend fun applySaleOrderChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            saleOrderDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        saleOrderDao.upsert(
            SaleOrderEntity(
                id = id,
                orderNo = payload.string("order_no"),
                customerId = payload.longOrNull("customer_id"),
                customerName = payload.stringOrNull("customer_name"),
                subtotalAmount = payload.double("subtotal_amount"),
                discountAmount = payload.double("discount_amount"),
                totalAmount = payload.double("total_amount"),
                paidAmount = payload.double("paid_amount"),
                notes = payload.stringOrNull("notes"),
                status = payload.int("status"),
                createdAt = payload.long("created_at"),
                updatedAt = payload.long("updated_at"),
            ),
        )
    }

    private suspend fun applySaleOrderItemChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            saleOrderDao.deleteItemById(id)
            return
        }
        val payload = parsePayload(change.payload)
        saleOrderDao.upsertItems(
            listOf(
                SaleOrderItemEntity(
                    id = id,
                    orderId = payload.long("order_id"),
                    productId = payload.long("product_id"),
                    productCode = payload.stringOrNull("product_code").orEmpty(),
                    productName = payload.stringOrNull("product_name").orEmpty(),
                    quantity = payload.double("quantity"),
                    unitPrice = payload.double("unit_price"),
                    amount = payload.double("amount"),
                    createdAt = payload.long("created_at"),
                ),
            ),
        )
    }

    private suspend fun applyPurchaseOrderChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            purchaseOrderDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        purchaseOrderDao.upsert(
            PurchaseOrderEntity(
                id = id,
                orderNo = payload.string("order_no"),
                supplierName = payload.string("supplier_name"),
                totalAmount = payload.double("total_amount"),
                notes = payload.stringOrNull("notes"),
                status = payload.int("status"),
                createdAt = payload.long("created_at"),
                updatedAt = payload.long("updated_at"),
            ),
        )
    }

    private suspend fun applyPayOrderChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            payOrderDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        payOrderDao.upsert(
            PayOrderEntity(
                id = id,
                orderNo = payload.string("order_no"),
                supplierId = payload.longOrNull("supplier_id"),
                supplierName = payload.string("supplier_name"),
                amount = payload.double("amount"),
                method = payload.int("method", default = 1),
                referenceNo = payload.stringOrNull("reference_no"),
                notes = payload.stringOrNull("notes"),
                status = payload.int("status"),
                createdAt = payload.long("created_at"),
                updatedAt = payload.long("updated_at"),
            ),
        )
    }

    private suspend fun applyFinanceRecordChange(change: SyncChangeV2Dto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.isDelete()) {
            financeRecordDao.deleteById(id)
            return
        }
        val payload = parsePayload(change.payload)
        financeRecordDao.upsert(
            FinanceRecordEntity(
                id = id,
                recordNo = payload.string("record_no"),
                type = payload.int("type", default = 0),
                category = payload.string("category"),
                partnerName = payload.stringOrNull("partner_name"),
                amount = payload.double("amount"),
                method = payload.int("method", default = 1),
                notes = payload.stringOrNull("notes"),
                createdAt = payload.long("created_at"),
                updatedAt = payload.long("updated_at"),
            ),
        )
    }

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

    private fun parsePayload(payload: String?): JsonObject =
        if (payload.isNullOrBlank()) {
            EMPTY_JSON_OBJECT
        } else {
            runCatching { json.parseToJsonElement(payload).jsonObject }
                .getOrDefault(EMPTY_JSON_OBJECT)
        }

    private fun JsonObject.string(key: String): String = stringOrNull(key).orEmpty()

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.int(key: String, default: Int = 0): Int =
        intOrNull(key) ?: default

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun JsonObject.long(key: String, default: Long = 0L): Long =
        longOrNull(key) ?: default

    private fun JsonObject.longOrNull(key: String): Long? =
        this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()

    private fun JsonObject.double(key: String, default: Double = 0.0): Double =
        doubleOrNull(key) ?: default

    private fun JsonObject.doubleOrNull(key: String): Double? =
        this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
}
