package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.PayOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayOrderDao {
    @Query("SELECT * FROM pay_orders ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PayOrderEntity>>

    @Query("SELECT * FROM pay_orders WHERE id = :id")
    suspend fun findById(id: Long): PayOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PayOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PayOrderEntity>)

    @Query("DELETE FROM pay_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pay_orders")
    suspend fun clear()
}
