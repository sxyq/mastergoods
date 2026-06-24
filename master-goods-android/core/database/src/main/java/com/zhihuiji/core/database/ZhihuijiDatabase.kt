package com.zhihuiji.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zhihuiji.core.database.dao.*
import com.zhihuiji.core.database.entity.*

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleOrderEntity::class,
        SaleOrderItemEntity::class,
        PurchaseOrderEntity::class,
        PayOrderEntity::class,
        FinanceRecordEntity::class,
        AgentNotificationEntity::class,
        SyncCursorEntity::class,
        AgentAuditEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class ZhihuijiDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun saleOrderDao(): SaleOrderDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun payOrderDao(): PayOrderDao
    abstract fun financeRecordDao(): FinanceRecordDao
    abstract fun agentNotificationDao(): AgentNotificationDao
    abstract fun syncCursorDao(): SyncCursorDao
    abstract fun agentAuditDao(): AgentAuditDao
}
