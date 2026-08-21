package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_orders_v2",
    indices = [Index(name = "idx_purchase_orders_v2_owner_order", value = ["ownerUserId", "orderId"], unique = true)],
)
data class PurchaseOrderV2Entity(
    @PrimaryKey val orderId: Long,
    val ownerUserId: Long,
    val orderNo: String,
    val supplierId: Long?,
    val supplierName: String?,
    val totalAmount: Double,
    val paidAmount: Double,
    val receivedAmount: Double,
    val notes: String?,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
