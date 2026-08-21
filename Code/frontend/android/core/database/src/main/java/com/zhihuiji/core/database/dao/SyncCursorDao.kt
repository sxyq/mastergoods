package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SyncCursorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncCursorDao {
    @Query("SELECT * FROM sync_cursors")
    fun observeAll(): Flow<List<SyncCursorEntity>>

    @Query("SELECT * FROM sync_cursors WHERE entityType = :entityType")
    suspend fun findByEntityType(entityType: String): SyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncCursorEntity)

    @Query("DELETE FROM sync_cursors")
    suspend fun clear()
}
