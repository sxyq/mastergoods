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
        ProductV2Entity::class,
        CustomerV2Entity::class,
        SupplierV2Entity::class,
        SaleOrderV2Entity::class,
        PurchaseOrderV2Entity::class,
        PayOrderV2Entity::class,
        AccountV2Entity::class,
        FinanceRecordV2Entity::class,
        InventoryLedgerV2Entity::class,
        InventorySnapshotV2Entity::class,
        SyncCursorV2Entity::class,
    ],
    version = 5,
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
    abstract fun productV2Dao(): ProductV2Dao
    abstract fun customerV2Dao(): CustomerV2Dao
    abstract fun supplierV2Dao(): SupplierV2Dao
    abstract fun saleOrderV2Dao(): SaleOrderV2Dao
    abstract fun purchaseOrderV2Dao(): PurchaseOrderV2Dao
    abstract fun payOrderV2Dao(): PayOrderV2Dao
    abstract fun accountV2Dao(): AccountV2Dao
    abstract fun financeRecordV2Dao(): FinanceRecordV2Dao
    abstract fun inventoryLedgerV2Dao(): InventoryLedgerV2Dao
    abstract fun inventorySnapshotV2Dao(): InventorySnapshotV2Dao
    abstract fun syncCursorV2Dao(): SyncCursorV2Dao
}
