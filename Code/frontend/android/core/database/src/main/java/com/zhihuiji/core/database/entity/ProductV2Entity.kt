package com.zhihuiji.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products_v2",
    indices = [Index(name = "idx_products_v2_owner_product", value = ["ownerUserId", "productId"], unique = true)],
)
data class ProductV2Entity(
    @PrimaryKey val productId: Long,
    val ownerUserId: Long,
    val code: String,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val unitId: Long,
    val unitName: String,
    val salePrice: Double,
    val purchasePrice: Double,
    val stock: Double,
    val safeStock: Double,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
