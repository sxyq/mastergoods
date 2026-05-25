package com.zhihuiji.data.order

import com.zhihuiji.core.database.dao.PayOrderDao
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
class PayOrderRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val payOrderDao: PayOrderDao,
) {
    fun observePayOrders(filter: PayOrderFilter): Flow<List<PayOrderDto>> = payOrderDao.observeAll().map { rows ->
        val kw = filter.keyword
        var filtered = rows.map { it.toDto() }
        if (!kw.isNullOrBlank()) filtered = filtered.filter {
            it.orderNo.contains(kw, true) || it.supplierName.contains(kw, true)
        }
        if (filter.status != null) filtered = filtered.filter { it.status == filter.status }
        filtered
    }

    suspend fun refreshPayOrders(filter: PayOrderFilter) {
        val result = safeApiCall {
            api.payOrders(keyword = filter.keyword, status = filter.status, createdAfter = filter.createdAfter, createdBefore = filter.createdBefore)
        }
        result.onSuccess { orders ->
            payOrderDao.upsertAll(orders.map { it.toEntity() })
        }
    }

    suspend fun getPayOrder(id: Long): Result<PayOrderDto> {
        val local = payOrderDao.findById(id)?.toDto()
        val remote = safeApiCall { api.payOrder(id) }
        remote.onSuccess { payOrderDao.upsert(it.toEntity()) }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createPayOrder(request: CreatePayOrderRequest): Result<PayOrderDto> =
        safeApiCall { api.createPayOrder(request) }.also { result ->
            result.onSuccess { payOrderDao.upsert(it.toEntity()) }
        }

    suspend fun updatePayOrderStatus(id: Long, status: Int): Result<PayOrderDto> =
        safeApiCall { api.updatePayOrderStatus(id, StatusRequest(status)) }.also { result ->
            result.onSuccess { payOrderDao.upsert(it.toEntity()) }
        }
}
