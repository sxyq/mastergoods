package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Preserves fields required to edit cached master data while offline. */
val Migration7To8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `categoryId` INTEGER")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `unitId` INTEGER")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `groupId` INTEGER")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `primaryContactName` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `primaryContactPhone` TEXT")
        db.execSQL("ALTER TABLE `suppliers` ADD COLUMN `groupId` INTEGER")
        db.execSQL("ALTER TABLE `suppliers` ADD COLUMN `primaryContactName` TEXT")
        db.execSQL("ALTER TABLE `suppliers` ADD COLUMN `primaryContactPhone` TEXT")
    }
}
