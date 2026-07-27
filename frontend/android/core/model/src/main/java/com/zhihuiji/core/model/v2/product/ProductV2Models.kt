package com.zhihuiji.core.model.v2.product

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductV2Dto(
    val id: Long = 0L,
    val code: String = "",
    val name: String = "",
    @SerialName("category_id") val categoryId: Long = 0L,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("unit_id") val unitId: Long = 0L,
    @SerialName("unit_name") val unitName: String = "",
    @SerialName("sale_price") val salePrice: Double = 0.0,
    @SerialName("purchase_price") val purchasePrice: Double = 0.0,
    @SerialName("price_levels") val priceLevels: List<ProductPriceValueV2Dto> = emptyList(),
    @SerialName("default_supplier") val defaultSupplier: ProductSupplierRelationV2Dto? = null,
    @SerialName("supplier_relations") val supplierRelations: List<ProductSupplierRelationV2Dto> = emptyList(),
    val stock: Double = 0.0,
    @SerialName("safe_stock") val safeStock: Double = 0.0,
    val status: Int = 1,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class ProductWriteV2Request(
    val code: String,
    val name: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("unit_id") val unitId: Long,
    @SerialName("sale_price") val salePrice: Double,
    @SerialName("purchase_price") val purchasePrice: Double,
    @SerialName("price_levels") val priceLevels: List<ProductPriceValueWriteV2Request> = emptyList(),
    @SerialName("supplier_relations") val supplierRelations: List<ProductSupplierRelationWriteV2Request> = emptyList(),
    val stock: Double,
    @SerialName("safe_stock") val safeStock: Double,
    val status: Int,
)

@Serializable
data class ProductCategoryV2Dto(
    val id: Long = 0L,
    val name: String = "",
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class ProductCategoryWriteV2Request(
    val name: String,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class ProductUnitV2Dto(
    val id: Long = 0L,
    val name: String = "",
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class ProductUnitWriteV2Request(
    val name: String,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class ProductPriceLevelV2Dto(
    val id: Long = 0L,
    val code: String = "",
    val name: String = "",
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class ProductPriceLevelWriteV2Request(
    val code: String,
    val name: String,
    val status: Int? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class ProductPriceValueV2Dto(
    @SerialName("level_id") val levelId: Long = 0L,
    val code: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val status: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class ProductPriceValueWriteV2Request(
    @SerialName("level_id") val levelId: Long,
    val price: Double,
)

@Serializable
data class ProductSupplierRelationV2Dto(
    val id: Long = 0L,
    @SerialName("product_id") val productId: Long = 0L,
    @SerialName("supplier_id") val supplierId: Long = 0L,
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("supplier_phone") val supplierPhone: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("purchase_priority") val purchasePriority: Int? = null,
    @SerialName("last_purchase_price") val lastPurchasePrice: Double? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class ProductSupplierRelationWriteV2Request(
    @SerialName("product_id") val productId: Long,
    @SerialName("supplier_id") val supplierId: Long,
    @SerialName("is_default") val isDefault: Boolean? = null,
    @SerialName("purchase_priority") val purchasePriority: Int? = null,
    @SerialName("last_purchase_price") val lastPurchasePrice: Double? = null,
    val notes: String? = null,
)
