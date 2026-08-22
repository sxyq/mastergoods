package com.zhihuiji.data.sync

import com.zhihuiji.core.model.v2.inventory.CreateInventoryLedgerEntryV2Request
import com.zhihuiji.core.model.v2.inventory.CreateInventorySnapshotV2Request
import com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto
import com.zhihuiji.core.model.v2.inventory.InventoryMonthlyStatsV2Dto
import com.zhihuiji.core.model.v2.inventory.InventorySnapshotV2Dto
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listInventoryLedger(
        productId: Long? = null,
        startAt: Long? = null,
        endAt: Long? = null,
        page: Int = 0,
        size: Int = 50,
    ): Result<List<InventoryLedgerEntryV2Dto>> =
        safeApiCall { api.inventoryLedgerV2(productId, startAt, endAt, page, size) }.map { it.content }

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
        page: Int = 0,
        size: Int = 50,
    ): Result<List<InventorySnapshotV2Dto>> =
        safeApiCall { api.inventorySnapshotsV2(snapshotDate, startDate, endDate, page, size) }.map { it.content }

    suspend fun createInventorySnapshot(request: CreateInventorySnapshotV2Request): Result<InventorySnapshotV2Dto> =
        safeApiCall { api.createInventorySnapshotV2(request) }

    suspend fun listInventoryMonthlyStats(
        year: Int,
        month: Int,
        page: Int = 0,
        size: Int = 50,
    ): Result<List<InventoryMonthlyStatsV2Dto>> =
        safeApiCall { api.inventoryMonthlyStatsV2(year, month, page, size) }.map { it.content }
}
