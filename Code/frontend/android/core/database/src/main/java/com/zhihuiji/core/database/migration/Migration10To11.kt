package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration10To11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_conflicts` (
                `entityType` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `localOperationId` TEXT,
                `localPayload` TEXT,
                `remoteOperationId` TEXT,
                `remotePayload` TEXT,
                `remoteVersion` INTEGER,
                `reason` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `resolvedAt` INTEGER,
                PRIMARY KEY(`entityType`, `entityId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_conflicts_state_createdAt` " +
                "ON `sync_conflicts` (`state`, `createdAt`)",
        )
    }
}
