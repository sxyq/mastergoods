package com.zhihuiji.data.order

import com.zhihuiji.core.model.v2.order.ConfirmPurchaseReturnV2Request
import com.zhihuiji.core.model.v2.order.CreatePurchaseReturnV2Request
import com.zhihuiji.core.model.v2.order.PurchaseReturnRefundV2Request
import com.zhihuiji.core.model.v2.order.PurchaseReturnV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReturnV2Filter
import com.zhihuiji.core.model.v2.order.UpdatePurchaseReturnDraftV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseReturnV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listPurchaseReturns(filter: PurchaseReturnV2Filter = PurchaseReturnV2Filter()): Result<List<PurchaseReturnV2Dto>> =
        safeApiCall { api.purchaseReturnsV2(filter.keyword, filter.status) }

    suspend fun listByOrder(orderId: Long): Result<List<PurchaseReturnV2Dto>> =
        safeApiCall { api.purchaseReturnsByOrderV2(orderId) }

    suspend fun getPurchaseReturn(id: Long): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.purchaseReturnV2(id) }

    suspend fun createPurchaseReturn(request: CreatePurchaseReturnV2Request): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.createPurchaseReturnV2(request) }

    suspend fun updateDraft(id: Long, request: UpdatePurchaseReturnDraftV2Request): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.updatePurchaseReturnDraftV2(id, request) }

    suspend fun confirm(id: Long, request: ConfirmPurchaseReturnV2Request): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.confirmPurchaseReturnV2(id, request) }

    suspend fun addRefund(id: Long, request: PurchaseReturnRefundV2Request): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.addPurchaseReturnRefundV2(id, request) }

    suspend fun cancel(id: Long): Result<PurchaseReturnV2Dto> =
        safeApiCall { api.cancelPurchaseReturnV2(id) }
}
