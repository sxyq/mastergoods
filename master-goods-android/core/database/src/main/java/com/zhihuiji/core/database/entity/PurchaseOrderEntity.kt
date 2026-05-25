package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey val id: Long,
    val orderNo: String,
    val supplierName: String,
    val totalAmount: Double,
    val notes: String?,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
