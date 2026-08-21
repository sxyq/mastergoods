package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_orders_v2",
    indices = [Index(name = "idx_sale_orders_v2_owner_order", value = ["ownerUserId", "orderId"], unique = true)],
)
data class SaleOrderV2Entity(
    @PrimaryKey val orderId: Long,
    val ownerUserId: Long,
    val orderNo: String,
    val customerId: Long?,
    val customerName: String?,
    val subtotalAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paidAmount: Double,
    val notes: String?,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
