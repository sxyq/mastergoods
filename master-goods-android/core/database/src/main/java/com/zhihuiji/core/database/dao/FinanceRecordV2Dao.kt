package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.FinanceRecordV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceRecordV2Dao {
    @Query("SELECT * FROM finance_records_v2 WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerUserId: Long): Flow<List<FinanceRecordV2Entity>>

    @Query("""
        SELECT * FROM finance_records_v2
        WHERE ownerUserId = :ownerUserId
          AND (:type IS NULL OR type = :type)
          AND (:keyword IS NULL OR category LIKE '%' || :keyword || '%' OR partnerName LIKE '%' || :keyword || '%')
          AND (:createdAfter IS NULL OR createdAt >= :createdAfter)
          AND (:createdBefore IS NULL OR createdAt <= :createdBefore)
        ORDER BY updatedAt DESC
    """)
    fun search(
        ownerUserId: Long,
        type: Int?,
        keyword: String?,
        createdAfter: Long?,
        createdBefore: Long?,
    ): Flow<List<FinanceRecordV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FinanceRecordV2Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FinanceRecordV2Entity>)

    @Query("DELETE FROM finance_records_v2 WHERE ownerUserId = :ownerUserId AND recordId = :recordId")
    suspend fun deleteByOwnerAndId(ownerUserId: Long, recordId: Long)

    @Query("DELETE FROM finance_records_v2 WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: Long)
}
