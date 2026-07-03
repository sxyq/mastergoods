package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SaleOrderV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleOrderV2Dao {
    @Query("SELECT * FROM sale_orders_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<SaleOrderV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SaleOrderV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SaleOrderV2Entity>)

    @Query("DELETE FROM sale_orders_v2 WHERE ownerUserId = :ownerUserId AND orderId = :orderId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, orderId: Long)

    @Query("DELETE FROM sale_orders_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
