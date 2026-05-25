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
    val id: Long,
    @SerialName("order_no") val orderNo: String,
    @SerialName("customer_name") val customerName: String,
    val amount: Double,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class StockOutRecordReportDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    val quantity: Double,
    @SerialName("sale_amount") val saleAmount: Double,
    @SerialName("created_at") val createdAt: Long,
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
    @SerialName("total_revenue") val totalRevenue: Double,
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("total_profit") val totalProfit: Double,
)

@Serializable
data class ProfitByCustomerReportDto(
    @SerialName("customer_id") val customerId: Long,
    @SerialName("customer_name") val customerName: String,
    @SerialName("total_revenue") val totalRevenue: Double,
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("total_profit") val totalProfit: Double,
)

@Serializable
data class InventoryFlowRecordDto(
    val id: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("product_code") val productCode: String,
    @SerialName("product_name") val productName: String,
    @SerialName("flow_type") val flowType: Int,
    val quantity: Double,
    @SerialName("source_type") val sourceType: Int,
    @SerialName("source_id") val sourceId: Long? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class CustomerSalesReportDto(
    @SerialName("customer_id") val customerId: Long,
    @SerialName("customer_name") val customerName: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("order_count") val orderCount: Int,
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
