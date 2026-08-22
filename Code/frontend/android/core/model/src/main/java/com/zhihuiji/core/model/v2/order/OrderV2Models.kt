package com.zhihuiji.core.model.v2.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleOrderV2Dto(
    val id: Long = 0L,
    @SerialName("order_no") val orderNo: String = "",
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<SaleOrderItemV2Dto> = emptyList(),
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
data class SaleOrderItemV2Dto(
    val id: Long = 0L,
    @SerialName("order_id") val orderId: Long = 0L,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateSaleOrderV2Request(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<CreateSaleOrderItemV2Request>,
    val notes: String? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
)

@Serializable
data class CreateSaleOrderItemV2Request(
    @SerialName("product_id") val productId: Long? = null,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double,
)

@Serializable
data class UpdateSaleDraftV2Request(
    @SerialName("discount_amount") val discountAmount: Double? = null,
    val notes: String? = null,
    val items: List<CreateSaleOrderItemV2Request> = emptyList(),
)

@Serializable
data class ConfirmSaleOrderV2Request(
    val notes: String? = null,
)

@Serializable
data class SalePaymentV2Dto(
    val id: Long = 0L,
    @SerialName("order_id") val orderId: Long = 0L,
    val amount: Double = 0.0,
    val method: Int = 0,
    @SerialName("reference_no") val referenceNo: String? = null,
    val type: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class SalePaymentV2Request(
    val amount: Double,
    val method: Int,
    @SerialName("reference_no") val referenceNo: String? = null,
)

@Serializable
data class SalesReturnV2Dto(
    val id: Long = 0L,
    @SerialName("return_no") val returnNo: String = "",
    @SerialName("original_order_id") val originalOrderId: Long? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<SalesReturnItemV2Dto> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("refund_amount") val refundAmount: Double = 0.0,
    val status: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class SalesReturnItemV2Dto(
    val id: Long = 0L,
    @SerialName("return_id") val returnId: Long = 0L,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateSalesReturnV2Request(
    @SerialName("original_order_id") val originalOrderId: Long? = null,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val items: List<CreateSalesReturnItemV2Request>,
    val notes: String? = null,
)

@Serializable
data class CreateSalesReturnItemV2Request(
    @SerialName("product_id") val productId: Long? = null,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double? = null,
)

@Serializable
data class UpdateSalesReturnDraftV2Request(
    val notes: String? = null,
)

@Serializable
data class ConfirmSalesReturnV2Request(
    val notes: String? = null,
)

@Serializable
data class SalesReturnRefundV2Request(
    val amount: Double? = null,
    val method: Int? = null,
    @SerialName("reference_no") val referenceNo: String? = null,
)

@Serializable
data class PurchaseOrderV2Dto(
    val id: Long = 0L,
    @SerialName("order_no") val orderNo: String = "",
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<PurchaseOrderItemV2Dto> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("paid_amount") val paidAmount: Double = 0.0,
    @SerialName("received_amount") val receivedAmount: Double = 0.0,
    val notes: String? = null,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PurchaseOrderItemV2Dto(
    val id: Long = 0L,
    @SerialName("order_id") val orderId: Long = 0L,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreatePurchaseOrderV2Request(
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<CreatePurchaseOrderItemV2Request>,
    val notes: String? = null,
    val status: Int? = null,
)

@Serializable
data class CreatePurchaseOrderItemV2Request(
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double,
    @SerialName("unit_cost") val unitCost: Double,
)

@Serializable
data class PurchaseReceiptV2Dto(
    val id: Long = 0L,
    @SerialName("receipt_no") val receiptNo: String = "",
    @SerialName("purchase_order_id") val purchaseOrderId: Long? = null,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<PurchaseReceiptItemV2Dto> = emptyList(),
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    val status: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class PurchaseReceiptItemV2Dto(
    val id: Long = 0L,
    @SerialName("receipt_id") val receiptId: Long = 0L,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double = 0.0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
    val amount: Double = 0.0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreatePurchaseReceiptV2Request(
    @SerialName("purchase_order_id") val purchaseOrderId: Long? = null,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val items: List<CreatePurchaseReceiptItemV2Request>,
    val notes: String? = null,
)

@Serializable
data class CreatePurchaseReceiptItemV2Request(
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Double,
    @SerialName("unit_cost") val unitCost: Double,
)

@Serializable
data class UpdatePurchaseReceiptDraftV2Request(
    val notes: String? = null,
)

@Serializable
data class PayOrderV2Dto(
    val id: Long = 0L,
    @SerialName("order_no") val orderNo: String = "",
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val amount: Double = 0.0,
    val method: Int = 0,
    @SerialName("reference_no") val referenceNo: String? = null,
    val notes: String? = null,
    @SerialName("account_id") val accountId: Long? = null,
    val status: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CreatePayOrderV2Request(
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("supplier_id") val supplierId: Long? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    val amount: Double,
    val method: Int,
    @SerialName("reference_no") val referenceNo: String? = null,
    val notes: String? = null,
    @SerialName("account_id") val accountId: Long? = null,
    val status: Int? = null,
)

@Serializable
data class SaleOrderV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
    @SerialName("min_total_amount") val minTotalAmount: String? = null,
    @SerialName("max_total_amount") val maxTotalAmount: String? = null,
    @SerialName("created_after") val createdAfter: String? = null,
    @SerialName("created_before") val createdBefore: String? = null,
    @SerialName("product_keyword") val productKeyword: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
)

@Serializable
data class PurchaseOrderV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
)

@Serializable
data class PayOrderV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
    @SerialName("created_after") val createdAfter: String? = null,
    @SerialName("created_before") val createdBefore: String? = null,
)

@Serializable
data class SalesReturnV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
)

@Serializable
data class PurchaseReceiptV2Filter(
    val keyword: String? = null,
    val status: Int? = null,
)
