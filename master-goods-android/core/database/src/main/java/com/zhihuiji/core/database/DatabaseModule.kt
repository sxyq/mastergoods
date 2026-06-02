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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZhihuijiDatabase {
        return Room.databaseBuilder(context, ZhihuijiDatabase::class.java, "zhihuiji.db")
            .addMigrations(MIGRATION_1_2)
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
}
