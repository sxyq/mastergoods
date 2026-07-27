package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_cursors_v2",
    indices = [Index(name = "idx_sync_cursors_v2_owner_client", value = ["ownerUserId", "clientId"], unique = true)],
)
data class SyncCursorV2Entity(
    @PrimaryKey val clientId: String,
    val ownerUserId: Long,
    val lastCursor: String,
    val updatedAt: Long,
)
