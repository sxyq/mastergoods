package com.zhihuiji.data.order

import com.zhihuiji.core.model.StatusRequest
import com.zhihuiji.core.model.v2.order.ConfirmSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.CreateSaleOrderV2Request
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import com.zhihuiji.core.model.v2.order.SaleOrderV2Filter
import com.zhihuiji.core.model.v2.order.SalePaymentV2Dto
import com.zhihuiji.core.model.v2.order.SalePaymentV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleOrderV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listSaleOrders(filter: SaleOrderV2Filter = SaleOrderV2Filter()): Result<List<SaleOrderV2Dto>> =
        safeApiCall {
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
        }

    suspend fun getSaleOrder(id: Long): Result<SaleOrderV2Dto> =
        safeApiCall { api.saleOrderV2(id) }

    suspend fun createSaleOrder(request: CreateSaleOrderV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.createSaleOrderV2(request) }

    suspend fun updateDraft(id: Long, request: com.zhihuiji.core.model.v2.order.UpdateSaleDraftV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.updateSaleOrderDraftV2(id, request) }

    suspend fun confirm(id: Long, request: ConfirmSaleOrderV2Request): Result<SaleOrderV2Dto> =
        safeApiCall { api.confirmSaleOrderV2(id, request) }

    suspend fun addPayment(id: Long, request: SalePaymentV2Request): Result<SalePaymentV2Dto> =
        safeApiCall { api.addSaleOrderPaymentV2(id, request) }

    suspend fun listPayments(id: Long): Result<List<SalePaymentV2Dto>> =
        safeApiCall { api.saleOrderPaymentsV2(id) }

    suspend fun updateStatus(id: Long, status: Int): Result<Unit> =
        safeApiUnitCall { api.updateSaleOrderStatusV2(id, StatusRequest(status)) }

    suspend fun cancel(id: Long): Result<SaleOrderV2Dto> =
        safeApiCall { api.cancelSaleOrderV2(id) }
}
