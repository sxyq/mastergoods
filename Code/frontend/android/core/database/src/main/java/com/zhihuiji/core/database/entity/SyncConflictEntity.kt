package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/** Keeps a remote version visible without overwriting a local unconfirmed edit. */
@Entity(
    tableName = "sync_conflicts",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index(value = ["state", "createdAt"])],
)
data class SyncConflictEntity(
    val entityType: String,
    val entityId: String,
    val localOperationId: String?,
    val localPayload: String?,
    val remoteOperationId: String?,
    val remotePayload: String?,
    val remoteVersion: Long?,
    val reason: String,
    val state: String = STATE_OPEN,
    val createdAt: Long,
    val resolvedAt: Long? = null,
) {
    companion object {
        const val STATE_OPEN = "open"
        const val STATE_RESOLVED = "resolved"
    }
}
