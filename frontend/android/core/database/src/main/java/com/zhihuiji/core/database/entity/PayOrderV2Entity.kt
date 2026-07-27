package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pay_orders_v2",
    indices = [Index(name = "idx_pay_orders_v2_owner_order", value = ["ownerUserId", "orderId"], unique = true)],
)
data class PayOrderV2Entity(
    @PrimaryKey val orderId: Long,
    val ownerUserId: Long,
    val orderNo: String,
    val supplierId: Long?,
    val supplierName: String?,
    val amount: Double,
    val method: Int,
    val referenceNo: String?,
    val notes: String?,
    val accountId: Long?,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
