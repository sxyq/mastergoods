package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.SyncCursorV2Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncCursorV2Dao {
    @Query("SELECT * FROM sync_cursors_v2 WHERE ownerUserId = :ownerUserId")
    fun observeByOwner(ownerUserId: Long): Flow<List<SyncCursorV2Entity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncCursorV2Entity)

    @Query("SELECT * FROM sync_cursors_v2 WHERE ownerUserId = :ownerUserId AND clientId = :clientId")
    suspend fun getByOwnerAndClientId(ownerUserId: Long, clientId: String): SyncCursorV2Entity?
}
