package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SupplierV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierV2Dao {
    @Query("SELECT * FROM suppliers_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<SupplierV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SupplierV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SupplierV2Entity>)

    @Query("DELETE FROM suppliers_v2 WHERE ownerUserId = :ownerUserId AND supplierId = :supplierId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, supplierId: Long)

    @Query("DELETE FROM suppliers_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
