package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_snapshots_v2",
    indices = [Index(name = "idx_inventory_snapshots_v2_owner_snapshot", value = ["ownerUserId", "snapshotId"], unique = true)],
)
data class InventorySnapshotV2Entity(
    @PrimaryKey val snapshotId: Long,
    val ownerUserId: Long,
    val productId: Long,
    val productCode: String,
    val productName: String,
    val warehouseId: Long?,
    val quantity: Double,
    val unitCost: Double?,
    val totalValue: Double?,
    val snapshotDate: Long,
    val createdAt: Long,
)
