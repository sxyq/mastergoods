package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["state", "createdAt"]),
    ],
)
data class SyncOutboxEntity(
    @androidx.room.PrimaryKey val operationId: String,
    val clientId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String?,
    val baseVersion: Long?,
    val createdAt: Long,
    val attempts: Int = 0,
    val state: String = STATE_PENDING,
    val lastError: String? = null,
) {
    companion object {
        const val STATE_PENDING = "pending"
        // Legacy failed rows remain retryable after an app upgrade. New permanent
        // server rejections use BLOCKED so they cannot loop forever in WorkManager.
        const val STATE_FAILED = "failed"
        const val STATE_BLOCKED = "blocked"
    }
}
