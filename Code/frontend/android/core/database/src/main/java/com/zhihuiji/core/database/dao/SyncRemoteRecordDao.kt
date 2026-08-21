package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.SyncRemoteRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRemoteRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SyncRemoteRecordEntity)

    @Query("SELECT * FROM sync_remote_records WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun find(entityType: String, entityId: String): SyncRemoteRecordEntity?

    @Query("SELECT * FROM sync_remote_records WHERE entityType = :entityType AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeActiveByType(entityType: String): Flow<List<SyncRemoteRecordEntity>>

    @Query("DELETE FROM sync_remote_records")
    suspend fun clear()
}
