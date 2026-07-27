package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pay_orders")
data class PayOrderEntity(
    @PrimaryKey val id: Long,
    val orderNo: String,
    val supplierId: Long?,
    val supplierName: String,
    val amount: Double,
    val method: Int,
    val referenceNo: String?,
    val notes: String?,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
