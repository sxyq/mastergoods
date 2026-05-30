package com.zhihuiji.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZhihuijiDatabase {
        return Room.databaseBuilder(context, ZhihuijiDatabase::class.java, "zhihuiji.db")
            .fallbackToDestructiveMigrationFrom(1)
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
