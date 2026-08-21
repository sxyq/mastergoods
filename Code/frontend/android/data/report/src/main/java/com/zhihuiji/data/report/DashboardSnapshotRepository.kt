package com.zhihuiji.data.report

import com.zhihuiji.core.database.dao.DashboardSnapshotDao
import com.zhihuiji.core.database.entity.DashboardSnapshotEntity
import com.zhihuiji.core.model.SalesTrendPointReportDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withContext

data class DashboardSnapshot(
    val salesAmount: Double,
    val salesOrderCount: Int,
    val receivableAmount: Double,
    val receivableCustomerCount: Int,
    val netCashFlow: Double,
    val salesTrend: List<SalesTrendPointReportDto>,
    val updatedAt: Long,
)

@Singleton
class DashboardSnapshotRepository @Inject constructor(
    private val dao: DashboardSnapshotDao,
    private val scopeProvider: DashboardSnapshotScopeProvider,
    private val json: Json,
) {
    suspend fun load(scope: String): DashboardSnapshot? {
        val scopeKey = scopedKey(scope) ?: return null
        return withContext(Dispatchers.IO) {
            dao.find(scopeKey)?.toSnapshotOrNull()
        }
    }

    suspend fun save(scope: String, snapshot: DashboardSnapshot) {
        val scopeKey = scopedKey(scope) ?: return
        withContext(Dispatchers.IO) {
            dao.upsert(
                DashboardSnapshotEntity(
                    scopeKey = scopeKey,
                    salesAmount = snapshot.salesAmount,
                    salesOrderCount = snapshot.salesOrderCount,
                    receivableAmount = snapshot.receivableAmount,
                    receivableCustomerCount = snapshot.receivableCustomerCount,
                    netCashFlow = snapshot.netCashFlow,
                    salesTrendJson = json.encodeToString(
                        ListSerializer(SalesTrendPointReportDto.serializer()),
                        snapshot.salesTrend,
                    ),
                    updatedAt = snapshot.updatedAt,
                ),
            )
        }
    }

    private suspend fun scopedKey(scope: String): String? {
        return scopeProvider.currentScopePrefix()?.let { "$it:$scope" }
    }

    private fun DashboardSnapshotEntity.toSnapshotOrNull(): DashboardSnapshot? = runCatching {
        DashboardSnapshot(
            salesAmount = salesAmount,
            salesOrderCount = salesOrderCount,
            receivableAmount = receivableAmount,
            receivableCustomerCount = receivableCustomerCount,
            netCashFlow = netCashFlow,
            salesTrend = json.decodeFromString(
                ListSerializer(SalesTrendPointReportDto.serializer()),
                salesTrendJson,
            ),
            updatedAt = updatedAt,
        )
    }.getOrNull()
}
