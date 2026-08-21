package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.PurchaseOrderV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderV2Dao {
    @Query("SELECT * FROM purchase_orders_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<PurchaseOrderV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PurchaseOrderV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PurchaseOrderV2Entity>)

    @Query("DELETE FROM purchase_orders_v2 WHERE ownerUserId = :ownerUserId AND orderId = :orderId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, orderId: Long)

    @Query("DELETE FROM purchase_orders_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
