package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesSummaryReportDto(
    @SerialName("start_at") val startAt: Long,
    @SerialName("end_at") val endAt: Long,
    @SerialName("total_sales_amount") val totalSalesAmount: Double = 0.0,
    @SerialName("total_paid_amount") val totalPaidAmount: Double = 0.0,
    @SerialName("total_refund_amount") val totalRefundAmount: Double = 0.0,
    @SerialName("total_unpaid_amount") val totalUnpaidAmount: Double = 0.0,
    @SerialName("total_order_count") val totalOrderCount: Int = 0,
)

@Serializable
data class ProfitSummaryReportDto(
    @SerialName("start_at") val startAt: Long,
    @SerialName("end_at") val endAt: Long,
    @SerialName("estimated_cost_amount") val estimatedCostAmount: Double = 0.0,
    @SerialName("estimated_profit_amount") val estimatedProfitAmount: Double = 0.0,
    @SerialName("estimated_profit_rate") val estimatedProfitRate: Double = 0.0,
)

@Serializable
data class RefundRecordReportDto(
    @SerialName("payment_id") val paymentId: Long,
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("refund_amount") val refundAmount: Double,
    val method: Int = 0,
    @SerialName("reference_no") val referenceNo: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class StockOutRecordReportDto(
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val amount: Double,
    @SerialName("item_created_at") val itemCreatedAt: Long,
    @SerialName("order_created_at") val orderCreatedAt: Long,
)

@Serializable
data class TopSellingProductReportDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    @SerialName("total_quantity") val totalQuantity: Double,
    @SerialName("total_amount") val totalAmount: Double,
)

@Serializable
data class ProfitByProductReportDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    @SerialName("total_sales_amount") val totalSalesAmount: Double,
    @SerialName("total_cost_amount") val totalCostAmount: Double,
    @SerialName("total_profit_amount") val totalProfitAmount: Double,
    @SerialName("profit_rate") val profitRate: Double = 0.0,
)

@Serializable
data class ProfitByCustomerReportDto(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String,
    @SerialName("total_sales_amount") val totalSalesAmount: Double,
    @SerialName("total_cost_amount") val totalCostAmount: Double,
    @SerialName("total_profit_amount") val totalProfitAmount: Double,
    @SerialName("profit_rate") val profitRate: Double = 0.0,
)

@Serializable
data class InventoryFlowRecordDto(
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("flow_type") val flowType: Int,
    @SerialName("flow_time") val flowTime: Long,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("source_type") val sourceType: Int,
    @SerialName("source_label") val sourceLabel: String? = null,
    @SerialName("adjust_reason") val adjustReason: String? = null,
    @SerialName("operator_name") val operatorName: String? = null,
)

@Serializable
data class CustomerSalesReportDto(
    @SerialName("customer_id") val customerId: Long? = null,
    @SerialName("customer_name") val customerName: String,
    @SerialName("total_orders") val totalOrders: Int,
    @SerialName("total_amount") val totalAmount: Double,
)

@Serializable
data class CustomerReceivableReportDto(
    @SerialName("customer_id") val customerId: Long,
    @SerialName("customer_name") val customerName: String,
    val phone: String,
    val balance: Double,
)

@Serializable
data class LowStockProductReportDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val stock: Double,
    @SerialName("safe_stock") val safeStock: Double,
)

@Serializable
data class ReconciliationSummaryReportDto(
    @SerialName("start_at") val startAt: Long,
    @SerialName("end_at") val endAt: Long,
    @SerialName("total_receivable_amount") val totalReceivableAmount: Double = 0.0,
    @SerialName("total_payable_amount") val totalPayableAmount: Double = 0.0,
    @SerialName("total_received_amount") val totalReceivedAmount: Double = 0.0,
    @SerialName("total_paid_amount") val totalPaidAmount: Double = 0.0,
    @SerialName("net_cash_flow") val netCashFlow: Double = 0.0,
)
