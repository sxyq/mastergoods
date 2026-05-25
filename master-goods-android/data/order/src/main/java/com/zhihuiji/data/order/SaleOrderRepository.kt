package com.zhihuiji.data.order

import com.zhihuiji.core.database.dao.SaleOrderDao
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
class SaleOrderRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val saleOrderDao: SaleOrderDao,
) {
    fun observeSaleOrders(filter: SaleOrderFilter): Flow<List<SaleOrderDto>> = saleOrderDao.observeAll().map { rows ->
        val kw = filter.keyword
        var filtered = rows.map { it.toDto() }
        if (!kw.isNullOrBlank()) filtered = filtered.filter {
            it.orderNo.contains(kw, true) || it.customerName?.contains(kw, true) == true
        }
        if (filter.status != null) filtered = filtered.filter { it.status == filter.status }
        filtered
    }

    suspend fun refreshSaleOrders(filter: SaleOrderFilter) {
        val result = safeApiCall {
            api.saleOrders(
                keyword = filter.keyword,
                status = filter.status,
                minTotalAmount = filter.minTotalAmount,
                maxTotalAmount = filter.maxTotalAmount,
                createdAfter = filter.createdAfter,
                createdBefore = filter.createdBefore,
                productKeyword = filter.productKeyword,
                paymentStatus = filter.paymentStatus,
            )
        }
        result.onSuccess { orders ->
            saleOrderDao.upsertAll(orders.map { it.toEntity() })
        }
    }

    suspend fun getSaleOrder(id: Long): Result<SaleOrderDto> {
        val local = saleOrderDao.findById(id)?.toDto()
        val remote = safeApiCall { api.saleOrder(id) }
        remote.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createSaleOrder(request: CreateSaleOrderRequest): Result<SaleOrderDto> =
        safeApiCall { api.createSaleOrder(request) }.also { result ->
            result.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        }

    suspend fun updateSaleDraft(id: Long, request: UpdateSaleDraftRequest): Result<SaleOrderDto> =
        safeApiCall { api.updateSaleDraft(id, request) }.also { result ->
            result.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        }

    suspend fun addSalePayment(id: Long, request: PaymentRequest): Result<PaymentDto> =
        safeApiCall { api.addSalePayment(id, request) }

    suspend fun listSalePayments(id: Long): Result<List<PaymentDto>> =
        safeApiCall { api.salePayments(id) }

    suspend fun updateSaleStatus(id: Long, status: Int): Result<Unit> {
        val result = safeApiCall { api.updateSaleStatus(id, StatusRequest(status)) }
        result.onSuccess {
            safeApiCall { api.saleOrder(id) }.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        }
        return result
    }

    suspend fun cancelSaleOrder(id: Long): Result<SaleOrderDto> =
        safeApiCall { api.cancelSaleOrder(id) }.also { result ->
            result.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        }
}
