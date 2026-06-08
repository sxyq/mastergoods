package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AI 助手审计记录本地实体。
 * 记录每次 AI 运行的关键行为，用于本地追溯。
 */
@Entity(tableName = "agent_audit_records")
data class AgentAuditEntity(
    @PrimaryKey
    val id: String,
    val runId: String? = null,
    val conversationId: Long? = null,
    val userMessage: String,
    val safetyPassed: Boolean? = null,
    val safetyReason: String? = null,
    val toolsCalledJson: String? = null, // JSON 序列化的工具调用列表
    val draftId: Long? = null,
    val draftType: String? = null,
    val draftTitle: String? = null,
    val userConfirmed: Boolean? = null,
    val contextCompacted: Boolean = false,
    val finalAnswerSummary: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
