package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Long? = null,
    val code: String = "",
    val name: String = "",
    val category: String = "",
    val unit: String = "",
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("purchase_price") val purchasePrice: Double = 0.0,
    val stock: Double = 0.0,
    @SerialName("safe_stock") val safeStock: Double = 0.0,
    val status: Int = 1,
    @SerialName("sync_status") val syncStatus: Int? = null,
    @SerialName("sync_version") val syncVersion: Long? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)

@Serializable
data class ProductAdjustStockRequest(
    val delta: Double,
    val reason: String? = null,
    val operator: String? = null,
)

data class ProductDraft(
    val code: String = "",
    val name: String = "",
    val category: String = "",
    val unit: String = "",
    val salePrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val safeStock: Double = 0.0,
    val status: Int = 1,
) {
    fun toDto() = ProductDto(
        code = code,
        name = name,
        category = category,
        unit = unit,
        salePrice = salePrice,
        purchasePrice = purchasePrice,
        stock = 0.0,
        safeStock = safeStock,
        status = status,
    )
}
