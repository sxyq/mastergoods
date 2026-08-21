package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_outbox` (
                `operationId` TEXT NOT NULL,
                `clientId` TEXT NOT NULL,
                `entityType` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `operation` TEXT NOT NULL,
                `payload` TEXT,
                `baseVersion` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL DEFAULT 0,
                `state` TEXT NOT NULL DEFAULT 'pending',
                `lastError` TEXT,
                PRIMARY KEY(`operationId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_outbox_state_createdAt` " +
                "ON `sync_outbox` (`state`, `createdAt`)",
        )
    }
}
