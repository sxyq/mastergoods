package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.AgentAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AgentAuditEntity)

    @Query("SELECT * FROM agent_audit_records ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AgentAuditEntity>>

    @Query("SELECT * FROM agent_audit_records WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun observeByConversation(conversationId: Long): Flow<List<AgentAuditEntity>>

    @Query("SELECT * FROM agent_audit_records WHERE runId = :runId LIMIT 1")
    suspend fun findByRunId(runId: String): AgentAuditEntity?

    @Query("DELETE FROM agent_audit_records WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM agent_audit_records")
    suspend fun count(): Int
}
