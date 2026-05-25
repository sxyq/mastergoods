package com.zhihuiji.data.finance

import com.zhihuiji.core.database.dao.FinanceRecordDao
import com.zhihuiji.core.database.toDto
import com.zhihuiji.core.database.toEntity
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val financeRecordDao: FinanceRecordDao,
) {
    fun observeFinanceRecords(filter: FinanceFilter): Flow<List<FinanceRecordDto>> =
        financeRecordDao.observeAll().map { list ->
            val kw = filter.keyword
            var filtered = list.map { it.toDto() }
            if (!kw.isNullOrBlank()) filtered = filtered.filter {
                it.recordNo.contains(kw, true) || it.partnerName?.contains(kw, true) == true
            }
            if (filter.type != null) filtered = filtered.filter { it.type == filter.type }
            filtered
        }

    suspend fun refreshFinanceRecords(filter: FinanceFilter) {
        val result = safeApiCall {
            api.financeRecords(keyword = filter.keyword, type = filter.type, createdAfter = filter.createdAfter, createdBefore = filter.createdBefore)
        }
        result.onSuccess { records ->
            financeRecordDao.upsertAll(records.map { it.toEntity() })
        }
    }

    suspend fun createFinanceRecord(request: CreateFinanceRecordRequest): Result<FinanceRecordDto> =
        safeApiCall { api.createFinanceRecord(request) }.also { result ->
            result.onSuccess { financeRecordDao.upsert(it.toEntity()) }
        }
}
