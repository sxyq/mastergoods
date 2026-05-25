package com.zhihuiji.data.order

import com.zhihuiji.core.database.dao.PurchaseOrderDao
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
class PurchaseOrderRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val purchaseOrderDao: PurchaseOrderDao,
) {
    fun observePurchaseOrders(filter: PurchaseOrderFilter): Flow<List<PurchaseOrderDto>> = purchaseOrderDao.observeAll().map { rows ->
        val kw = filter.keyword
        var filtered = rows.map { it.toDto() }
        if (!kw.isNullOrBlank()) filtered = filtered.filter {
            it.orderNo.contains(kw, true) || it.supplierName.contains(kw, true)
        }
        if (filter.status != null) filtered = filtered.filter { it.status == filter.status }
        filtered
    }

    suspend fun refreshPurchaseOrders(filter: PurchaseOrderFilter) {
        val result = safeApiCall { api.purchaseOrders(keyword = filter.keyword, status = filter.status) }
        result.onSuccess { orders ->
            purchaseOrderDao.upsertAll(orders.map { it.toEntity() })
        }
    }

    suspend fun getPurchaseOrder(id: Long): Result<PurchaseOrderDto> {
        val local = purchaseOrderDao.findById(id)?.toDto()
        val remote = safeApiCall { api.purchaseOrder(id) }
        remote.onSuccess { purchaseOrderDao.upsert(it.toEntity()) }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createPurchaseOrder(request: CreatePurchaseOrderRequest): Result<PurchaseOrderDto> =
        safeApiCall { api.createPurchaseOrder(request) }.also { result ->
            result.onSuccess { purchaseOrderDao.upsert(it.toEntity()) }
        }
}
