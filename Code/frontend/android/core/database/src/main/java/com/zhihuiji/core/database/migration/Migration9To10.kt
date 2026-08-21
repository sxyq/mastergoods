package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration9To10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dashboard_snapshots` (
                `scopeKey` TEXT NOT NULL,
                `salesAmount` REAL NOT NULL,
                `salesOrderCount` INTEGER NOT NULL,
                `receivableAmount` REAL NOT NULL,
                `receivableCustomerCount` INTEGER NOT NULL,
                `netCashFlow` REAL NOT NULL,
                `salesTrendJson` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`scopeKey`)
            )
            """.trimIndent(),
        )
    }
}
