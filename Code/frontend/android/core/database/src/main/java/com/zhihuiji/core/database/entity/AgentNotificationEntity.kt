// 测试账号范围：8220 测试服务器上的所有账号都是测试账号，均可用于本项目测试。
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
