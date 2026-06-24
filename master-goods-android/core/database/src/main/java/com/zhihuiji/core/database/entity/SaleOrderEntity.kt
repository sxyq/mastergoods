package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_orders",
    indices = [
        Index("updatedAt"),
        Index("status"),
        Index("createdAt"),
        Index("customerId"),
    ],
)
data class SaleOrderEntity(
    @PrimaryKey val id: Long,
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
