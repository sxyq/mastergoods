package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Lossless local mirror for sync entities that do not yet have a dedicated Room table.
 *
 * The raw server payload is retained so acknowledging a cursor never discards a change
 * merely because a screen-specific projection has not been implemented yet.
 */
@Entity(
    tableName = "sync_remote_records",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index(value = ["updatedAt"]), Index(value = ["isDeleted", "updatedAt"])],
)
data class SyncRemoteRecordEntity(
    val entityType: String,
    val entityId: String,
    val operationId: String?,
    val operation: String,
    val payload: String?,
    val baseVersion: Long?,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val receivedAt: Long,
)
