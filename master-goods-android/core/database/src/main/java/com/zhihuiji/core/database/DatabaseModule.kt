package com.zhihuiji.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sale_order_items` (
                    `id` INTEGER NOT NULL,
                    `orderId` INTEGER NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `productCode` TEXT NOT NULL,
                    `productName` TEXT NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unitPrice` REAL NOT NULL,
                    `amount` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`orderId`) REFERENCES `sale_orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_order_items_orderId` ON `sale_order_items` (`orderId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_order_items_productId` ON `sale_order_items` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_order_items_productCode` ON `sale_order_items` (`productCode`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_order_items_productName` ON `sale_order_items` (`productName`)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_audit_records` (
                    `id` TEXT NOT NULL,
                    `runId` TEXT,
                    `conversationId` INTEGER,
                    `userMessage` TEXT NOT NULL,
                    `safetyPassed` INTEGER,
                    `safetyReason` TEXT,
                    `toolsCalledJson` TEXT,
                    `draftId` INTEGER,
                    `draftType` TEXT,
                    `draftTitle` TEXT,
                    `userConfirmed` INTEGER,
                    `contextCompacted` INTEGER NOT NULL DEFAULT 0,
                    `finalAnswerSummary` TEXT,
                    `errorCode` TEXT,
                    `errorMessage` TEXT,
                    `timestamp` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_audit_records_timestamp` ON `agent_audit_records` (`timestamp`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_audit_records_conversationId` ON `agent_audit_records` (`conversationId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_agent_audit_records_runId` ON `agent_audit_records` (`runId`)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_orders_updatedAt` ON `sale_orders` (`updatedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_orders_status` ON `sale_orders` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_orders_createdAt` ON `sale_orders` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_orders_customerId` ON `sale_orders` (`customerId`)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZhihuijiDatabase {
        return Room.databaseBuilder(context, ZhihuijiDatabase::class.java, "zhihuiji.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideProductDao(db: ZhihuijiDatabase) = db.productDao()

    @Provides
    fun provideCustomerDao(db: ZhihuijiDatabase) = db.customerDao()

    @Provides
    fun provideSupplierDao(db: ZhihuijiDatabase) = db.supplierDao()

    @Provides
    fun provideSaleOrderDao(db: ZhihuijiDatabase) = db.saleOrderDao()

    @Provides
    fun providePurchaseOrderDao(db: ZhihuijiDatabase) = db.purchaseOrderDao()

    @Provides
    fun providePayOrderDao(db: ZhihuijiDatabase) = db.payOrderDao()

    @Provides
    fun provideFinanceRecordDao(db: ZhihuijiDatabase) = db.financeRecordDao()

    @Provides
    fun provideAgentNotificationDao(db: ZhihuijiDatabase) = db.agentNotificationDao()

    @Provides
    fun provideSyncCursorDao(db: ZhihuijiDatabase) = db.syncCursorDao()

    @Provides
    fun provideAgentAuditDao(db: ZhihuijiDatabase) = db.agentAuditDao()
}
