package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/** User-authored Agent input that must wait for a network connection. */
@Entity(
    tableName = "pending_agent_messages",
    indices = [Index(value = ["state", "createdAt"])],
)
data class PendingAgentMessageEntity(
    @androidx.room.PrimaryKey val id: String,
    val conversationId: Long?,
    val content: String,
    val imageAssetIdsJson: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val state: String = STATE_PENDING,
    val lastError: String? = null,
) {
    companion object {
        const val STATE_PENDING = "pending"
        const val STATE_BLOCKED = "blocked"
    }
}
