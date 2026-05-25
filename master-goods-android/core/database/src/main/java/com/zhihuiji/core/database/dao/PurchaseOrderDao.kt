package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.PurchaseOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderDao {
    @Query("SELECT * FROM purchase_orders ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun findById(id: Long): PurchaseOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PurchaseOrderEntity>)

    @Query("DELETE FROM purchase_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM purchase_orders")
    suspend fun clear()
}
