package com.zhihuiji.data.order

import com.zhihuiji.core.model.StatusRequest
import com.zhihuiji.core.model.v2.order.ConfirmSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Filter
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Request
import com.zhihuiji.core.network.CacheScopeProvider
import com.zhihuiji.core.network.MemoryCache
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleOrderV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val cacheScopeProvider: CacheScopeProvider,
) {
    private val cache = MemoryCache()
    private val listTtl = 60_000L
    private val detailTtl = 300_000L

    suspend fun listSaleOrders(filter: SaleOrderV2Filter = SaleOrderV2Filter()): Result<List<SaleOrderV2Dto>> {
        val cacheKey = scopedCacheKey("sale_orders_${filter.hashCode()}")
        cache.get<List<SaleOrderV2Dto>>(cacheKey, listTtl)?.let { return Result.success(it) }

        return safeApiCall {
            api.saleOrdersV2(
                keyword = filter.keyword,
                status = filter.status,
                minTotalAmount = filter.minTotalAmount,
                maxTotalAmount = filter.maxTotalAmount,
                createdAfter = filter.createdAfter,
                createdBefore = filter.createdBefore,
                productKeyword = filter.productKeyword,
                paymentStatus = filter.paymentStatus,
            )
        }.also { result ->
            result.getOrNull()?.let { cache.put(cacheKey, it) }
        }
    }

    suspend fun getSaleOrder(id: Long): Result<SaleOrderV2Dto> {
        val cacheKey = scopedCacheKey("sale_order_$id")
        cache.get<SaleOrderV2Dto>(cacheKey, detailTtl)?.let { return Result.success(it) }

        return safeApiCall { api.saleOrderV2(id) }.also { result ->
            result.getOrNull()?.let { cache.put(cacheKey, it) }
        }
    }

    suspend fun createSaleOrder(request: CreateSaleOrderV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.createSaleOrderV2(request) }
            .also { invalidateCurrentSaleOrderLists() }

    suspend fun updateDraft(id: Long, request: com.zhihuiji.core.model.v2.order.UpdateSaleDraftV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.updateSaleOrderDraftV2(id, request) }
            .also { cache.invalidate(scopedCacheKey("sale_order_$id")) }

    suspend fun confirm(id: Long, request: ConfirmSaleOrderV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.confirmSaleOrderV2(id, request) }
            .also {
                cache.invalidate(scopedCacheKey("sale_order_$id"))
                invalidateCurrentSaleOrderLists()
            }

    suspend fun addPayment(id: Long, request: SalePaymentV2Request): Result<SalePaymentV2Dto> =
        safeApiCall { api.addSaleOrderPaymentV2(id, request) }
            .also { cache.invalidate(scopedCacheKey("sale_order_$id")) }

    suspend fun listPayments(id: Long): Result<List<SalePaymentV2Dto>> =
        safeApiCall { api.saleOrderPaymentsV2(id) }

    suspend fun updateStatus(id: Long, status: Int): Result<Unit> =
        safeApiUnitCall { api.updateSaleOrderStatusV2(id, StatusRequest(status)) }
            .also {
                cache.invalidate(scopedCacheKey("sale_order_$id"))
                invalidateCurrentSaleOrderLists()
            }

    suspend fun cancel(id: Long): Result<SaleOrderV2Dto> =
        safeApiCall { api.cancelSaleOrderV2(id) }
            .also {
                cache.invalidate(scopedCacheKey("sale_order_$id"))
                invalidateCurrentSaleOrderLists()
            }

    private fun scopedCacheKey(key: String): String = "${cacheScopeProvider.scopeKey()}|$key"

    private fun invalidateCurrentSaleOrderLists() {
        cache.invalidatePrefix(scopedCacheKey("sale_orders_"))
    }
}
