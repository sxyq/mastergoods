package com.zhihuiji.data.report

import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val api: ZhihuijiApi,
) {
    suspend fun salesSummary(startAt: Long, endAt: Long) = safeApiCall { api.salesSummary(startAt, endAt) }
    suspend fun salesTrend(startAt: Long, endAt: Long, bucket: String = "day") =
        safeApiCall { api.salesTrend(startAt, endAt, bucket) }
    suspend fun profitSummary(startAt: Long, endAt: Long) = safeApiCall { api.profitSummary(startAt, endAt) }
    suspend fun refundRecords(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.refundRecords(startAt, endAt, limit) }
    suspend fun stockOutRecords(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.stockOutRecords(startAt, endAt, limit) }
    suspend fun topProducts(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.topProducts(startAt, endAt, limit) }
    suspend fun profitByProducts(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.profitByProducts(startAt, endAt, limit) }
    suspend fun profitByCustomers(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.profitByCustomers(startAt, endAt, limit) }
    suspend fun inventoryFlow(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.inventoryFlow(startAt, endAt, limit) }
    suspend fun customerSales(startAt: Long, endAt: Long, limit: Int = 10) = safeApiCall { api.customerSales(startAt, endAt, limit) }
    suspend fun topReceivableCustomers(limit: Int = 10) = safeApiCall { api.topReceivableCustomers(limit) }
    suspend fun lowStockProducts(limit: Int = 10) = safeApiCall { api.lowStockProducts(limit) }
    suspend fun reconciliationSummary(startAt: Long, endAt: Long) = safeApiCall { api.reconciliationSummary(startAt, endAt) }
    suspend fun cashflowSummary(startAt: Long, endAt: Long) = safeApiCall { api.cashflowSummary(startAt, endAt) }
}
