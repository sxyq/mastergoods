package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.CustomerV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerV2Dao {
    @Query("SELECT * FROM customers_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<CustomerV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomerV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CustomerV2Entity>)

    @Query("DELETE FROM customers_v2 WHERE ownerUserId = :ownerUserId AND customerId = :customerId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, customerId: Long)

    @Query("DELETE FROM customers_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
