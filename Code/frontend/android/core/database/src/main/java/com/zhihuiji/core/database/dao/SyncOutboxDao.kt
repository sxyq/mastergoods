package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Query(
        "SELECT * FROM sync_outbox WHERE state IN ('pending', 'failed') " +
            "ORDER BY createdAt ASC LIMIT :limit",
    )
    suspend fun pending(limit: Int): List<SyncOutboxEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM sync_outbox " +
            "WHERE entityType = :entityType AND entityId = :entityId " +
            "AND state IN ('pending', 'failed', 'blocked'))",
    )
    suspend fun hasUnresolvedForEntity(entityType: String, entityId: String): Boolean

    @Query(
        "SELECT * FROM sync_outbox WHERE entityType = :entityType AND entityId = :entityId " +
            "AND state IN ('pending', 'failed', 'blocked') ORDER BY createdAt ASC LIMIT 1",
    )
    suspend fun firstUnresolvedForEntity(entityType: String, entityId: String): SyncOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entity: SyncOutboxEntity): Long

    @Query("DELETE FROM sync_outbox WHERE operationId = :operationId")
    suspend fun delete(operationId: String)

    @Query("SELECT * FROM sync_outbox WHERE operationId = :operationId LIMIT 1")
    suspend fun findByOperationId(operationId: String): SyncOutboxEntity?

    @Query(
        "UPDATE sync_outbox SET attempts = attempts + 1, state = :state, lastError = :error " +
            "WHERE operationId = :operationId",
    )
    suspend fun markAttempt(operationId: String, state: String, error: String?)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE state IN ('pending', 'failed')")
    suspend fun pendingCount(): Int

    @Query("DELETE FROM sync_outbox")
    suspend fun clear()
}
