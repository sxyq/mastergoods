package com.zhihuiji.data.order

import com.zhihuiji.core.model.v2.order.ConfirmSalesReturnV2Request
import com.zhihuiji.core.model.v2.order.CreateSalesReturnV2Request
import com.zhihuiji.core.model.v2.order.SalesReturnRefundV2Request
import com.zhihuiji.core.model.v2.order.SalesReturnV2Dto
import com.zhihuiji.core.model.v2.order.SalesReturnV2Filter
import com.zhihuiji.core.model.v2.order.UpdateSalesReturnDraftV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesReturnV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listSalesReturns(filter: SalesReturnV2Filter = SalesReturnV2Filter()): Result<List<SalesReturnV2Dto>> =
        safeApiCall { api.salesReturnsV2(filter.keyword, filter.status) }

    suspend fun listByOrder(orderId: Long): Result<List<SalesReturnV2Dto>> =
        safeApiCall { api.salesReturnsByOrderV2(orderId) }

    suspend fun getSalesReturn(id: Long): Result<SalesReturnV2Dto> =
        safeApiCall { api.salesReturnV2(id) }

    suspend fun createSalesReturn(request: CreateSalesReturnV2Request): Result<SalesReturnV2Dto> =
        safeApiCall { api.createSalesReturnV2(request) }

    suspend fun updateDraft(id: Long, request: UpdateSalesReturnDraftV2Request): Result<SalesReturnV2Dto> =
        safeApiCall { api.updateSalesReturnDraftV2(id, request) }

    suspend fun confirm(id: Long, request: ConfirmSalesReturnV2Request): Result<SalesReturnV2Dto> =
        safeApiCall { api.confirmSalesReturnV2(id, request) }

    suspend fun addRefund(id: Long, request: SalesReturnRefundV2Request): Result<SalesReturnV2Dto> =
        safeApiCall { api.addSalesReturnRefundV2(id, request) }

    suspend fun cancel(id: Long): Result<SalesReturnV2Dto> =
        safeApiCall { api.cancelSalesReturnV2(id) }
}
