package com.zhihuiji.data.order

import androidx.annotation.VisibleForTesting
import com.zhihuiji.core.database.dao.SaleOrderDao
import com.zhihuiji.core.database.toDto
import com.zhihuiji.core.database.toEntity
import com.zhihuiji.core.database.toItemEntities
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
    fun observeSaleOrders(filter: SaleOrderFilter): Flow<List<SaleOrderDto>> =
        saleOrderDao.search(
            keyword = filter.keyword,
            status = filter.status,
            minTotalAmount = filter.minTotalAmount,
            maxTotalAmount = filter.maxTotalAmount,
            createdAfter = filter.createdAfter?.toLongOrNull(),
            createdBefore = filter.createdBefore?.toLongOrNull(),
            productKeyword = filter.productKeyword,
            paymentStatus = filter.paymentStatus,
        ).map { rows ->
            rows.map { it.toDto() }
        }

    suspend fun refreshSaleOrders(filter: SaleOrderFilter) {
        val params = mutableMapOf<String, String?>()
        filter.keyword?.let { params["keyword"] = it }
        filter.status?.let { params["status"] = it.toString() }
        filter.minTotalAmount?.let { params["min_total_amount"] = it.toString() }
        filter.maxTotalAmount?.let { params["max_total_amount"] = it.toString() }
        filter.createdAfter?.let { params["created_after"] = it }
        filter.createdBefore?.let { params["created_before"] = it }
        filter.productKeyword?.let { params["product_keyword"] = it }
        filter.paymentStatus?.let { params["payment_status"] = it.toString() }
        val result = safeApiCall { api.saleOrders(params) }
        result.onSuccess { orders ->
            saleOrderDao.replaceOrderGraphs(
                orders = orders.map { it.toEntity() },
                items = orders.flatMap { it.toItemEntities() },
            )
        }
    }

    suspend fun getSaleOrder(id: Long): Result<SaleOrderDto> {
        val local = saleOrderDao.findWithItemsById(id)?.toDto()
        val remote = safeApiCall { api.saleOrder(id) }
        remote.onSuccess {
            saleOrderDao.replaceOrderGraph(
                order = it.toEntity(),
                items = it.toItemEntities(),
            )
        }
        return remote.fold(
            onSuccess = { Result.success(it) },
            onFailure = { error -> local?.let(Result.Companion::success) ?: Result.failure(error) },
        )
    }

    suspend fun createSaleOrder(request: CreateSaleOrderRequest): Result<SaleOrderDto> =
        safeApiCall { api.createSaleOrder(request) }.also { result ->
            result.onSuccess {
                saleOrderDao.replaceOrderGraph(
                    order = it.toEntity(),
                    items = it.toItemEntities(),
                )
            }
        }

    @VisibleForTesting
    suspend fun updateSaleDraft(id: Long, request: UpdateSaleDraftRequest): Result<SaleOrderDto> =
        safeApiCall { api.updateSaleDraft(id, request) }.also { result ->
            result.onSuccess {
                saleOrderDao.replaceOrderGraph(
                    order = it.toEntity(),
                    items = it.toItemEntities(),
                )
            }
        }

    suspend fun addSalePayment(id: Long, request: PaymentRequest): Result<PaymentDto> {
        val result = safeApiCall { api.addSalePayment(id, request) }
        result.onSuccess {
            safeApiCall { api.saleOrder(id) }.onSuccess { saleOrderDao.upsert(it.toEntity()) }
        }
        return result
    }

    suspend fun listSalePayments(id: Long): Result<List<PaymentDto>> =
        safeApiCall { api.salePayments(id) }

    suspend fun updateSaleStatus(id: Long, status: Int): Result<Unit> {
        val result = safeApiCall { api.updateSaleStatus(id, StatusRequest(status)) }
        result.onSuccess {
            safeApiCall { api.saleOrder(id) }.onSuccess {
                saleOrderDao.replaceOrderGraph(
                    order = it.toEntity(),
                    items = it.toItemEntities(),
                )
            }
        }
        return result
    }

    suspend fun cancelSaleOrder(id: Long): Result<SaleOrderDto> =
        safeApiCall { api.cancelSaleOrder(id) }.also { result ->
            result.onSuccess {
                saleOrderDao.replaceOrderGraph(
                    order = it.toEntity(),
                    items = it.toItemEntities(),
                )
            }
        }
}
