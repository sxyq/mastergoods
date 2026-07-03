package com.zhihuiji.data.report

import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun salesSummary(startAt: Long, endAt: Long) = safeApiCall { api.salesSummaryV2(startAt, endAt) }
    suspend fun salesTrend(startAt: Long, endAt: Long, bucket: String = "day") =
        safeApiCall { api.salesTrendV2(startAt, endAt, bucket) }
    suspend fun profitSummary(startAt: Long, endAt: Long) = safeApiCall { api.profitSummaryV2(startAt, endAt) }
    suspend fun refundRecords(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.refundRecordsV2(startAt, endAt, limit) }
    suspend fun stockOutRecords(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.stockOutRecordsV2(startAt, endAt, limit) }
    suspend fun topProducts(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.topProductsV2(startAt, endAt, limit) }
    suspend fun profitByProducts(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.profitByProductsV2(startAt, endAt, limit) }
    suspend fun profitByCustomers(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.profitByCustomersV2(startAt, endAt, limit) }
    suspend fun inventoryFlow(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.inventoryFlowV2(startAt, endAt, limit) }
    suspend fun customerSales(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.customerSalesV2(startAt, endAt, limit) }
    suspend fun topReceivableCustomers(limit: Int = 10) = safeApiCall { api.topReceivableCustomersV2(limit) }
    suspend fun lowStockProducts(limit: Int = 10) = safeApiCall { api.lowStockProductsReportV2(limit) }
    suspend fun reconciliationSummary(startAt: Long, endAt: Long) = safeApiCall { api.reconciliationSummaryV2(startAt, endAt) }
    suspend fun cashflowSummary(startAt: Long, endAt: Long) = safeApiCall { api.cashflowSummaryV2(startAt, endAt) }
}
