package com.zhihuiji.data.sync

import android.util.Log
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
import com.zhihuiji.core.model.v2.inventory.CreateInventoryLedgerEntryV2Request
import com.zhihuiji.core.model.v2.inventory.CreateInventorySnapshotV2Request
import com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto
import com.zhihuiji.core.model.v2.inventory.InventoryMonthlyStatsV2Dto
import com.zhihuiji.core.model.v2.inventory.InventorySnapshotV2Dto
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
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val EMPTY_JSON_OBJECT = JsonObject(emptyMap())

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
    private val json: Json,
) {
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

    suspend fun pullApplyAndAck(clientId: String, limit: Int? = null): Result<String> = runCatching {
        var sinceCursor = cursor(clientId).getOrThrow().lastCursor.takeIf { it.isNotBlank() }
        var latestCursor = sinceCursor.orEmpty()
        var hasMore: Boolean

        do {
            val response = pull(
                SyncPullV2Request(
                    clientId = clientId,
                    sinceCursor = sinceCursor,
                    limit = limit,
                ),
            ).getOrThrow()

            applyPulledChanges(response).getOrThrow()

            val ackCursor = response.nextCursor.takeIf { it.isNotBlank() }
            if (ackCursor != null && response.changes.isNotEmpty()) {
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
        } while (hasMore && !sinceCursor.isNullOrBlank())

        latestCursor
    }

    suspend fun applyPulledChanges(result: SyncPullV2Response): Result<Unit> = runCatching {
        for (change in result.changes) {
            when (change.entityType) {
                "customer" -> applyCustomerChange(change)
                "supplier" -> applySupplierChange(change)
                "product" -> applyProductChange(change)
                "sale_order" -> applySaleOrderChange(change)
                "sale_order_item" -> applySaleOrderItemChange(change)
                "purchase_order" -> applyPurchaseOrderChange(change)
                "pay_order" -> applyPayOrderChange(change)
                "finance_record" -> applyFinanceRecordChange(change)
                else -> Log.d("SyncV2Repository", "Skip unsupported local sync entityType=${change.entityType}")
            }
        }
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

    suspend fun listInventoryLedger(
        productId: Long? = null,
        startAt: Long? = null,
        endAt: Long? = null,
    ): Result<List<InventoryLedgerEntryV2Dto>> = safeApiCall { api.inventoryLedgerV2(productId, startAt, endAt) }

    suspend fun listInventoryLedgerBySource(
        sourceType: String,
        sourceId: Long,
    ): Result<List<InventoryLedgerEntryV2Dto>> = safeApiCall { api.inventoryLedgerBySourceV2(sourceType, sourceId) }

    suspend fun createInventoryLedgerEntry(request: CreateInventoryLedgerEntryV2Request): Result<InventoryLedgerEntryV2Dto> =
        safeApiCall { api.createInventoryLedgerEntryV2(request) }

    suspend fun listInventorySnapshots(
        snapshotDate: Long? = null,
        startDate: Long? = null,
        endDate: Long? = null,
    ): Result<List<InventorySnapshotV2Dto>> =
        safeApiCall { api.inventorySnapshotsV2(snapshotDate, startDate, endDate) }

    suspend fun createInventorySnapshot(request: CreateInventorySnapshotV2Request): Result<InventorySnapshotV2Dto> =
        safeApiCall { api.createInventorySnapshotV2(request) }

    suspend fun listInventoryMonthlyStats(year: Int, month: Int): Result<List<InventoryMonthlyStatsV2Dto>> =
        safeApiCall { api.inventoryMonthlyStatsV2(year, month) }

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
        productDao.upsert(
            ProductEntity(
                id = id,
                code = payload.string("code"),
                name = payload.string("name"),
                category = payload.stringOrNull("category").orEmpty(),
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
