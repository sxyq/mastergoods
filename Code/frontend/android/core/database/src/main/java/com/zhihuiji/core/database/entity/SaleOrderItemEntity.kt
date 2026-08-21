package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_order_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("orderId"),
        Index("productId"),
        Index("productCode"),
        Index("productName"),
    ],
)
data class SaleOrderItemEntity(
    @PrimaryKey val id: Long,
    val orderId: Long,
    val productId: Long,
    val productCode: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
    val createdAt: Long,
)
