package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_notifications")
data class AgentNotificationEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val isDelivered: Boolean,
    val createdAt: Long,
)
