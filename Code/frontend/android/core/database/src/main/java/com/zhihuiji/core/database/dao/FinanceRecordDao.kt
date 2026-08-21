package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.FinanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceRecordDao {
    @Query("SELECT * FROM finance_records ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FinanceRecordEntity>>

    @Query("""
        SELECT * FROM finance_records
        WHERE (:type IS NULL OR type = :type)
          AND (:keyword IS NULL OR category LIKE '%' || :keyword || '%' OR partnerName LIKE '%' || :keyword || '%')
          AND (:createdAfter IS NULL OR createdAt >= :createdAfter)
          AND (:createdBefore IS NULL OR createdAt <= :createdBefore)
        ORDER BY updatedAt DESC
    """)
    fun search(
        type: Int?,
        keyword: String?,
        createdAfter: Long?,
        createdBefore: Long?,
    ): Flow<List<FinanceRecordEntity>>

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
