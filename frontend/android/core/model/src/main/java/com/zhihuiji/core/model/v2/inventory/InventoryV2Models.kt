package com.zhihuiji.core.model.v2.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryLedgerEntryV2Dto(
    val id: Long = 0L,
    @SerialName("product_id") val productId: Long = 0L,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("warehouse_id") val warehouseId: Long? = null,
    @SerialName("quantity_before") val quantityBefore: Double? = null,
    @SerialName("quantity_change") val quantityChange: Double = 0.0,
    @SerialName("quantity_after") val quantityAfter: Double? = null,
    @SerialName("unit_cost") val unitCost: Double? = null,
    @SerialName("source_type") val sourceType: String = "",
    @SerialName("source_id") val sourceId: Long? = null,
    @SerialName("source_no") val sourceNo: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateInventoryLedgerEntryV2Request(
    @SerialName("product_id") val productId: Long,
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_id") val sourceId: Long? = null,
    @SerialName("source_no") val sourceNo: String? = null,
    @SerialName("quantity_change") val quantityChange: Double,
    @SerialName("unit_cost") val unitCost: Double? = null,
    @SerialName("warehouse_id") val warehouseId: Long? = null,
    val notes: String? = null,
)

@Serializable
data class InventorySnapshotV2Dto(
    val id: Long = 0L,
    @SerialName("product_id") val productId: Long = 0L,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("warehouse_id") val warehouseId: Long? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double? = null,
    @SerialName("total_value") val totalValue: Double? = null,
    @SerialName("snapshot_date") val snapshotDate: Long = 0L,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateInventorySnapshotV2Request(
    @SerialName("product_id") val productId: Long,
    @SerialName("snapshot_date") val snapshotDate: Long,
    @SerialName("warehouse_id") val warehouseId: Long? = null,
)

@Serializable
data class InventoryMonthlyStatsV2Dto(
    val id: Long = 0L,
    @SerialName("product_id") val productId: Long = 0L,
    @SerialName("product_code") val productCode: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("warehouse_id") val warehouseId: Long? = null,
    val month: Int = 0,
    val year: Int = 0,
    @SerialName("quantity_in") val quantityIn: Double = 0.0,
    @SerialName("quantity_out") val quantityOut: Double = 0.0,
    @SerialName("quantity_adjust") val quantityAdjust: Double = 0.0,
    @SerialName("quantity_begin") val quantityBegin: Double = 0.0,
    @SerialName("quantity_end") val quantityEnd: Double = 0.0,
    @SerialName("total_cost_in") val totalCostIn: Double = 0.0,
    @SerialName("total_cost_out") val totalCostOut: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)
