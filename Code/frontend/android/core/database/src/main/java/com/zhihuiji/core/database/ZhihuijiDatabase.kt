package com.zhihuiji.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhihuiji.core.database.dao.*
import com.zhihuiji.core.database.entity.*

@Database(
    entities = [
        AgentNotificationEntity::class,
        SyncCursorEntity::class,
        AgentAuditEntity::class,
        SyncCursorV2Entity::class,
        SyncOutboxEntity::class,
        SyncRemoteRecordEntity::class,
        PendingAgentMessageEntity::class,
        SyncConflictEntity::class,
    ],
    version = 11,
    exportSchema = false,
)
abstract class ZhihuijiDatabase : RoomDatabase() {
    abstract fun agentNotificationDao(): AgentNotificationDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun agentAuditDao(): AgentAuditDao
    abstract fun syncCursorV2Dao(): SyncCursorV2Dao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncRemoteRecordDao(): SyncRemoteRecordDao
    abstract fun pendingAgentMessageDao(): PendingAgentMessageDao
    abstract fun syncConflictDao(): SyncConflictDao
}
