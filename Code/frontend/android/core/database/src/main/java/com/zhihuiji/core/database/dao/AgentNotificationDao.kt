package com.zhihuiji.core.database.dao

import androidx.room.*
import com.zhihuiji.core.database.entity.AgentNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentNotificationDao {
    @Query("SELECT * FROM agent_notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AgentNotificationEntity>>

    @Query("SELECT * FROM agent_notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun observeUnread(): Flow<List<AgentNotificationEntity>>

    @Query("SELECT * FROM agent_notifications WHERE id = :id")
    suspend fun findById(id: Long): AgentNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AgentNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AgentNotificationEntity>)

    @Query("DELETE FROM agent_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM agent_notifications")
    suspend fun clear()
}
