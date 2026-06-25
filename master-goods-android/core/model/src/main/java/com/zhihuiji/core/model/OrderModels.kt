package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleOrderDto(
    val id: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<SaleOrderItemDto> = emptyList(),
    @SerialName("subtotal_amount") val subtotalAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    val notes: String? = null,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class SaleOrderItemDto(
    val id: Long,
    @SerialName("order_id") val orderId: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateSaleOrderRequest(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<CreateSaleOrderItemRequest>,
    val notes: String? = null,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
)

@Serializable
data class CreateSaleOrderItemRequest(
    @SerialName("product_id") val productId: Long,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
)

@Serializable
data class UpdateSaleDraftRequest(
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val notes: String? = null,
)

@Serializable
data class PaymentDto(
    val id: Long,
    @SerialName("order_id") val orderId: Long,
    val amount: Double,
    val method: Int,
    @SerialName("reference_no") val referenceNo: String? = null,
    val type: Int,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class PaymentRequest(
    val amount: Double,
    val method: Int,
    @SerialName("reference_no") val referenceNo: String? = null,
)

@Serializable
data class StatusRequest(
    val status: Int,
)

@Serializable
data class PurchaseOrderDto(
    val id: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("supplier_name") val supplierName: String,
    val items: List<PurchaseOrderItemDto> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    val notes: String? = null,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PurchaseOrderItemDto(
    val id: Long,
    @SerialName("order_id") val orderId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreatePurchaseOrderRequest(
    @SerialName("supplier_name") val supplierName: String,
    val items: List<CreatePurchaseOrderItemRequest>,
    val notes: String? = null,
    val status: Int? = null,
)

@Serializable
data class CreatePurchaseOrderItemRequest(
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double,
    @SerialName("unit_cost") val unitCost: Double,
)

@Serializable
data class PayOrderDto(
    val id: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String,
    val amount: Double = 0.0,
    val method: Int = 1,
    @SerialName("reference_no") val referenceNo: String? = null,
    val notes: String? = null,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CreatePayOrderRequest(
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val amount: Double,
    val method: Int,
    @SerialName("reference_no") val referenceNo: String? = null,
    val notes: String? = null,
    val status: Int? = null,
)

data class SaleOrderFilter(
    val keyword: String? = null,
    val status: Int? = null,
    val minTotalAmount: Double? = null,
    val maxTotalAmount: Double? = null,
    val createdAfter: String? = null,
    val createdBefore: String? = null,
    val productKeyword: String? = null,
    val paymentStatus: Int? = null,
)

data class PurchaseOrderFilter(
    val keyword: String? = null,
    val status: Int? = null,
)

data class PayOrderFilter(
    val keyword: String? = null,
    val status: Int? = null,
    val createdAfter: String? = null,
    val createdBefore: String? = null,
)
