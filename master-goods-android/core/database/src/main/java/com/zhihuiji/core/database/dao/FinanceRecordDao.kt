package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.FinanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceRecordDao {
    @Query("SELECT * FROM finance_records ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FinanceRecordEntity>>

    @Query("SELECT * FROM finance_records WHERE id = :id")
    suspend fun findById(id: Long): FinanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FinanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FinanceRecordEntity>)

    @Query("DELETE FROM finance_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM finance_records")
    suspend fun clear()
}
