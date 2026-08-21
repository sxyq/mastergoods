package com.zhihuiji.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zhihuiji.core.database.entity.PendingAgentMessageEntity

@Dao
interface PendingAgentMessageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: PendingAgentMessageEntity)

    @Query("SELECT * FROM pending_agent_messages WHERE state = 'pending' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<PendingAgentMessageEntity>

    @Query("DELETE FROM pending_agent_messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "UPDATE pending_agent_messages SET attempts = attempts + 1, state = :state, lastError = :error " +
            "WHERE id = :id",
    )
    suspend fun markAttempt(id: String, state: String, error: String?)

    @Query("UPDATE pending_agent_messages SET conversationId = :serverConversationId WHERE conversationId = :localConversationId")
    suspend fun replaceLocalConversationId(localConversationId: Long, serverConversationId: Long)

    @Query("DELETE FROM pending_agent_messages")
    suspend fun clear()
}
