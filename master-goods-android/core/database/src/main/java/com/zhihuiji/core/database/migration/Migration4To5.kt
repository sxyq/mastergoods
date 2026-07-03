package com.zhihuiji.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `products_v2` (
                `productId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `categoryName` TEXT NOT NULL,
                `unitId` INTEGER NOT NULL,
                `unitName` TEXT NOT NULL,
                `salePrice` REAL NOT NULL,
                `purchasePrice` REAL NOT NULL,
                `stock` REAL NOT NULL,
                `safeStock` REAL NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`productId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_products_v2_owner_product` ON `products_v2` (`ownerUserId`, `productId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `customers_v2` (
                `customerId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `level` INTEGER NOT NULL,
                `groupId` INTEGER,
                `groupName` TEXT,
                `primaryContactName` TEXT,
                `primaryContactPhone` TEXT,
                `address` TEXT,
                `notes` TEXT,
                `balance` REAL NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`customerId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_customers_v2_owner_customer` ON `customers_v2` (`ownerUserId`, `customerId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `suppliers_v2` (
                `supplierId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `groupId` INTEGER,
                `groupName` TEXT,
                `primaryContactName` TEXT,
                `primaryContactPhone` TEXT,
                `address` TEXT,
                `notes` TEXT,
                `balance` REAL NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`supplierId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_suppliers_v2_owner_supplier` ON `suppliers_v2` (`ownerUserId`, `supplierId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sale_orders_v2` (
                `orderId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `orderNo` TEXT NOT NULL,
                `customerId` INTEGER,
                `customerName` TEXT,
                `subtotalAmount` REAL NOT NULL,
                `discountAmount` REAL NOT NULL,
                `totalAmount` REAL NOT NULL,
                `paidAmount` REAL NOT NULL,
                `notes` TEXT,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`orderId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_sale_orders_v2_owner_order` ON `sale_orders_v2` (`ownerUserId`, `orderId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `purchase_orders_v2` (
                `orderId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `orderNo` TEXT NOT NULL,
                `supplierId` INTEGER,
                `supplierName` TEXT,
                `totalAmount` REAL NOT NULL,
                `paidAmount` REAL NOT NULL,
                `receivedAmount` REAL NOT NULL,
                `notes` TEXT,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`orderId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_purchase_orders_v2_owner_order` ON `purchase_orders_v2` (`ownerUserId`, `orderId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pay_orders_v2` (
                `orderId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `orderNo` TEXT NOT NULL,
                `supplierId` INTEGER,
                `supplierName` TEXT,
                `amount` REAL NOT NULL,
                `method` INTEGER NOT NULL,
                `referenceNo` TEXT,
                `notes` TEXT,
                `accountId` INTEGER,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`orderId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_pay_orders_v2_owner_order` ON `pay_orders_v2` (`ownerUserId`, `orderId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `accounts_v2` (
                `accountId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` INTEGER NOT NULL,
                `balance` REAL NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`accountId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_accounts_v2_owner_account` ON `accounts_v2` (`ownerUserId`, `accountId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_records_v2` (
                `recordId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `recordNo` TEXT NOT NULL,
                `type` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `partnerName` TEXT,
                `amount` REAL NOT NULL,
                `method` INTEGER NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`recordId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_finance_records_v2_owner_record` ON `finance_records_v2` (`ownerUserId`, `recordId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_ledger_v2` (
                `entryId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `productCode` TEXT NOT NULL,
                `productName` TEXT NOT NULL,
                `warehouseId` INTEGER,
                `quantityBefore` REAL,
                `quantityChange` REAL NOT NULL,
                `quantityAfter` REAL,
                `unitCost` REAL,
                `sourceType` TEXT NOT NULL,
                `sourceId` INTEGER,
                `sourceNo` TEXT,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`entryId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_inventory_ledger_v2_owner_entry` ON `inventory_ledger_v2` (`ownerUserId`, `entryId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `inventory_snapshots_v2` (
                `snapshotId` INTEGER NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `productCode` TEXT NOT NULL,
                `productName` TEXT NOT NULL,
                `warehouseId` INTEGER,
                `quantity` REAL NOT NULL,
                `unitCost` REAL,
                `totalValue` REAL,
                `snapshotDate` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`snapshotId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_inventory_snapshots_v2_owner_snapshot` ON `inventory_snapshots_v2` (`ownerUserId`, `snapshotId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_cursors_v2` (
                `clientId` TEXT NOT NULL,
                `ownerUserId` INTEGER NOT NULL,
                `lastCursor` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`clientId`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `idx_sync_cursors_v2_owner_client` ON `sync_cursors_v2` (`ownerUserId`, `clientId`)")
    }
}
