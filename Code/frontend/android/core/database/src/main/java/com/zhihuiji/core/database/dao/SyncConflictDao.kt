package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.SyncConflictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query(
        "SELECT * FROM sync_conflicts " +
            "WHERE entityType = :entityType AND entityId = :entityId AND state = 'open'",
    )
    suspend fun findOpen(entityType: String, entityId: String): SyncConflictEntity?

    @Query("SELECT * FROM sync_conflicts WHERE state = 'open' ORDER BY createdAt DESC")
    fun observeOpen(): Flow<List<SyncConflictEntity>>

    @Query(
        "UPDATE sync_conflicts SET state = 'resolved', resolvedAt = :resolvedAt " +
            "WHERE entityType = :entityType AND entityId = :entityId",
    )
    suspend fun markResolved(entityType: String, entityId: String, resolvedAt: Long)

    @Query("DELETE FROM sync_conflicts")
    suspend fun clear()
}
