package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.PayOrderV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayOrderV2Dao {
    @Query("SELECT * FROM pay_orders_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<PayOrderV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PayOrderV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PayOrderV2Entity>)

    @Query("DELETE FROM pay_orders_v2 WHERE ownerUserId = :ownerUserId AND orderId = :orderId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, orderId: Long)

    @Query("DELETE FROM pay_orders_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
