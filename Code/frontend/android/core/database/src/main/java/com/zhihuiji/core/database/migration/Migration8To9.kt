package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration8To9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_agent_messages` (
                `id` TEXT NOT NULL,
                `conversationId` INTEGER,
                `content` TEXT NOT NULL,
                `imageAssetIdsJson` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL DEFAULT 0,
                `state` TEXT NOT NULL DEFAULT 'pending',
                `lastError` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_agent_messages_state_createdAt` " +
                "ON `pending_agent_messages` (`state`, `createdAt`)",
        )
    }
}
