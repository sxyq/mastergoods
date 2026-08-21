package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.InventoryLedgerV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLedgerV2Dao {
    @Query("SELECT * FROM inventory_ledger_v2 WHERE ownerUserId = :ownerUserId ORDER BY createdAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<InventoryLedgerV2Entity>>

    @Query("SELECT * FROM inventory_ledger_v2 WHERE ownerUserId = :ownerUserId AND productId = :productId ORDER BY createdAt DESC")
    fun observeByOwnerAndProduct(ownerUserId: Long, productId: Long): Flow<List<InventoryLedgerV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InventoryLedgerV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InventoryLedgerV2Entity>)

    @Query("DELETE FROM inventory_ledger_v2 WHERE ownerUserId = :ownerUserId AND entryId = :entryId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, entryId: Long)

    @Query("DELETE FROM inventory_ledger_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
