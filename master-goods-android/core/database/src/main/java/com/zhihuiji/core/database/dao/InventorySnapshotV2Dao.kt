package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.InventorySnapshotV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventorySnapshotV2Dao {
    @Query("SELECT * FROM inventory_snapshots_v2 WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<InventorySnapshotV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InventorySnapshotV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InventorySnapshotV2Entity>)

    @Query("DELETE FROM inventory_snapshots_v2 WHERE ownerUserId = :ownerUserId AND snapshotId = :snapshotId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, snapshotId: Long)

    @Query("DELETE FROM inventory_snapshots_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
