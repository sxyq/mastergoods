package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_ledger_v2",
    indices = [Index(name = "idx_inventory_ledger_v2_owner_entry", value = ["ownerUserId", "entryId"], unique = true)],
)
data class InventoryLedgerV2Entity(
    @PrimaryKey val entryId: Long,
    val ownerUserId: Long,
    val productId: Long,
    val productCode: String,
    val productName: String,
    val warehouseId: Long?,
    val quantityBefore: Double?,
    val quantityChange: Double,
    val quantityAfter: Double?,
    val unitCost: Double?,
    val sourceType: String,
    val sourceId: Long?,
    val sourceNo: String?,
    val notes: String?,
    val createdAt: Long,
)
