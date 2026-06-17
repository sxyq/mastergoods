package com.zhihuiji.core.model.v2.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseReturnV2Dto(
    val id: Long = 0L,
    @SerialName("return_no") val returnNo: String = "",
    @SerialName("purchase_order_id") val purchaseOrderId: Long? = null,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<PurchaseReturnItemV2Dto> = emptyList(),
    val refunds: List<PurchaseReturnRefundV2Dto> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("refund_amount") val refundAmount: Double = 0.0,
    val status: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PurchaseReturnItemV2Dto(
    val id: Long = 0L,
    @SerialName("return_id") val returnId: Long = 0L,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class PurchaseReturnRefundV2Dto(
    val id: Long = 0L,
    @SerialName("return_id") val returnId: Long = 0L,
    val amount: Double = 0.0,
    val method: Int = 0,
    @SerialName("reference_no") val referenceNo: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreatePurchaseReturnV2Request(
    @SerialName("purchase_order_id") val purchaseOrderId: Long? = null,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<CreatePurchaseReturnItemV2Request>,
    val notes: String? = null,
)

@Serializable
data class CreatePurchaseReturnItemV2Request(
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double,
    @SerialName("unit_cost") val unitCost: Double? = null,
)

@Serializable
data class UpdatePurchaseReturnDraftV2Request(
    val notes: String? = null,
)

@Serializable
data class ConfirmPurchaseReturnV2Request(
    val notes: String? = null,
)

@Serializable
data class PurchaseReturnRefundV2Request(
    val amount: Double? = null,
    val method: Int? = null,
    @SerialName("reference_no") val referenceNo: String? = null,
)

@Serializable
data class PurchaseReturnV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
)
