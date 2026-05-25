package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val code: String,
    val name: String,
    val category: String,
    val unit: String,
    val salePrice: Double,
    val purchasePrice: Double,
    val stock: Double,
    val safeStock: Double,
    val status: Int,
    val syncStatus: Int?,
    val syncVersion: Long?,
    val createdAt: Long?,
    val updatedAt: Long?,
)
