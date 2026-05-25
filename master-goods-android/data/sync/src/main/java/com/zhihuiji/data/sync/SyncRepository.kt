package com.zhihuiji.data.sync

import com.zhihuiji.core.database.dao.CustomerDao
import com.zhihuiji.core.database.dao.PayOrderDao
import com.zhihuiji.core.database.dao.ProductDao
import com.zhihuiji.core.database.dao.PurchaseOrderDao
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.dao.SupplierDao
import com.zhihuiji.core.database.dao.SyncCursorDao
import com.zhihuiji.core.database.entity.CustomerEntity
import com.zhihuiji.core.database.entity.PayOrderEntity
import com.zhihuiji.core.database.entity.ProductEntity
import com.zhihuiji.core.database.entity.PurchaseOrderEntity
import com.zhihuiji.core.database.entity.SaleOrderEntity
import com.zhihuiji.core.database.entity.SupplierEntity
import com.zhihuiji.core.database.entity.SyncCursorEntity
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.datastore.SyncPreferenceStore
import com.zhihuiji.core.model.PullRequest
import com.zhihuiji.core.model.PullResult
import com.zhihuiji.core.model.SyncChangeDto
import com.zhihuiji.core.model.SyncHealthResult
import com.zhihuiji.core.model.UploadRequest
import com.zhihuiji.core.model.UploadResult
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class SyncRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val syncPreferenceStore: SyncPreferenceStore,
    private val settingsStore: SettingsStore,
    private val syncCursorDao: SyncCursorDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val saleOrderDao: SaleOrderDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val payOrderDao: PayOrderDao,
    private val json: Json,
) {
    companion object {
        private const val GLOBAL_CURSOR_KEY = "server_pull"
        private const val DEFAULT_PULL_LIMIT = 200
    }

    suspend fun healthCheck(): Result<SyncHealthResult> = safeApiCall { api.syncHealth() }

    suspend fun pull(): Result<PullResult> {
        var sinceCursor = loadCursor().takeIf { it.isNotBlank() }
        var lastPage = PullResult()

        while (true) {
            val pageResult = safeApiCall {
                api.pull(PullRequest(sinceCursor = sinceCursor, limit = DEFAULT_PULL_LIMIT))
            }
            val page = pageResult.getOrElse { return Result.failure(it) }

            applyPulledChanges(page)
            persistCursor(page.nextCursor)

            lastPage = page
            sinceCursor = page.nextCursor.takeIf { it.isNotBlank() }

            if (!page.hasMore || page.changes.isEmpty()) break
        }

        return Result.success(lastPage)
    }

    suspend fun runManualSync(): Result<PullResult> = pull()

    suspend fun upload(changes: List<SyncChangeDto>): Result<UploadResult> {
        val clientId = settingsStore.ensureClientId()
        val currentCursor = loadCursor().takeIf { it.isNotBlank() }
        val result = safeApiCall { api.upload(UploadRequest(clientId, changes, currentCursor)) }
        result.onSuccess { uploadResult ->
            if (uploadResult.nextCursor.isNotBlank()) {
                persistCursor(uploadResult.nextCursor)
            }
        }
        return result
    }

    suspend fun applyPulledChanges(result: PullResult) {
        result.changes.forEach { change ->
            when (change.entityType) {
                "customer" -> applyCustomerChange(change)
                "supplier" -> applySupplierChange(change)
                "product" -> applyProductChange(change)
                "sale_order" -> applySaleOrderChange(change)
                "purchase_order" -> applyPurchaseOrderChange(change)
                "pay_order" -> applyPayOrderChange(change)
            }
        }
    }

    suspend fun clearSyncState() {
        syncPreferenceStore.clearAll()
        syncCursorDao.clear()
    }

    private suspend fun loadCursor(): String {
        val dbCursor = syncCursorDao.findByEntityType(GLOBAL_CURSOR_KEY)?.cursor
        if (!dbCursor.isNullOrBlank()) return dbCursor
        return syncPreferenceStore.observeCursor(GLOBAL_CURSOR_KEY).first()
    }

    private suspend fun persistCursor(cursor: String) {
        if (cursor.isBlank()) return
        syncCursorDao.upsert(
            SyncCursorEntity(
                entityType = GLOBAL_CURSOR_KEY,
                cursor = cursor,
                updatedAt = System.currentTimeMillis(),
            )
        )
        syncPreferenceStore.saveCursor(GLOBAL_CURSOR_KEY, cursor)
    }

    private suspend fun applyCustomerChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
            )
        )
    }

    private suspend fun applySupplierChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
            )
        )
    }

    private suspend fun applyProductChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
                unit = payload.string("unit"),
                salePrice = payload.double("sale_price"),
                purchasePrice = payload.double("purchase_price"),
                stock = payload.double("stock"),
                safeStock = payload.double("safe_stock"),
                status = payload.int("status", default = 1),
                syncStatus = payload.intOrNull("sync_status"),
                syncVersion = payload.longOrNull("sync_version"),
                createdAt = payload.longOrNull("created_at"),
                updatedAt = payload.longOrNull("updated_at"),
            )
        )
    }

    private suspend fun applySaleOrderChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
            )
        )
    }

    private suspend fun applyPurchaseOrderChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
            )
        )
    }

    private suspend fun applyPayOrderChange(change: SyncChangeDto) {
        val id = change.entityId.toLongOrNull() ?: return
        if (change.operation == "delete") {
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
            )
        )
    }

    private fun parsePayload(payload: String): JsonObject =
        runCatching { json.parseToJsonElement(payload).jsonObject }.getOrDefault(JsonObject(emptyMap()))

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
        this[key]?.toLongValue()

    private fun JsonObject.double(key: String, default: Double = 0.0): Double =
        doubleOrNull(key) ?: default

    private fun JsonObject.doubleOrNull(key: String): Double? =
        this[key]?.toDoubleValue()

    private fun JsonElement.toLongValue(): Long? = jsonPrimitive.contentOrNull?.toLongOrNull()

    private fun JsonElement.toDoubleValue(): Double? = jsonPrimitive.contentOrNull?.toDoubleOrNull()
}
