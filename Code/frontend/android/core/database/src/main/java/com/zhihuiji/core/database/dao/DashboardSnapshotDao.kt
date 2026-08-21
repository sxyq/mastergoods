package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.DashboardSnapshotEntity

@Dao
interface DashboardSnapshotDao {
    @Query("SELECT * FROM dashboard_snapshots WHERE scopeKey = :scopeKey LIMIT 1")
    suspend fun find(scopeKey: String): DashboardSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: DashboardSnapshotEntity)

    @Query("DELETE FROM dashboard_snapshots")
    suspend fun clear()
}
