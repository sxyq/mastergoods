package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SaleOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleOrderDao {
    @Query("SELECT * FROM sale_orders ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SaleOrderEntity>>

    @Query("""
        SELECT * FROM sale_orders
        WHERE (:keyword IS NULL OR orderNo LIKE '%' || :keyword || '%' OR customerName LIKE '%' || :keyword || '%')
          AND (:status IS NULL OR status = :status)
        ORDER BY updatedAt DESC
    """)
    fun search(keyword: String?, status: Int?): Flow<List<SaleOrderEntity>>

    @Query("SELECT * FROM sale_orders WHERE id = :id")
    suspend fun findById(id: Long): SaleOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SaleOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SaleOrderEntity>)

    @Query("DELETE FROM sale_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sale_orders")
    suspend fun clear()
}
