package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration6To7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_remote_records` (
                `entityType` TEXT NOT NULL,
                `entityId` TEXT NOT NULL,
                `operationId` TEXT,
                `operation` TEXT NOT NULL,
                `payload` TEXT,
                `baseVersion` INTEGER,
                `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL,
                `receivedAt` INTEGER NOT NULL,
                PRIMARY KEY(`entityType`, `entityId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_remote_records_updatedAt` " +
                "ON `sync_remote_records` (`updatedAt`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sync_remote_records_isDeleted_updatedAt` " +
                "ON `sync_remote_records` (`isDeleted`, `updatedAt`)",
        )
    }
}
