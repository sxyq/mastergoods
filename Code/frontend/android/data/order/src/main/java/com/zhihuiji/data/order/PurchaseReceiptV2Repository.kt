package com.zhihuiji.data.order

import com.zhihuiji.core.model.v2.order.CreatePurchaseReceiptV2Request
import com.zhihuiji.core.model.v2.order.PurchaseReceiptV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseReceiptV2Filter
import com.zhihuiji.core.model.v2.order.UpdatePurchaseReceiptDraftV2Request
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseReceiptV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listPurchaseReceipts(filter: PurchaseReceiptV2Filter = PurchaseReceiptV2Filter()): Result<List<PurchaseReceiptV2Dto>> =
        safeApiCall { api.purchaseReceiptsV2(filter.keyword, filter.status) }

    suspend fun listByOrder(orderId: Long): Result<List<PurchaseReceiptV2Dto>> =
        safeApiCall { api.purchaseReceiptsByOrderV2(orderId) }

    suspend fun getPurchaseReceipt(id: Long): Result<PurchaseReceiptV2Dto> =
        safeApiCall { api.purchaseReceiptV2(id) }

    suspend fun createPurchaseReceipt(request: CreatePurchaseReceiptV2Request): Result<PurchaseReceiptV2Dto> =
        safeApiCall { api.createPurchaseReceiptV2(request) }

    suspend fun updateDraft(id: Long, request: UpdatePurchaseReceiptDraftV2Request): Result<PurchaseReceiptV2Dto> =
        safeApiCall { api.updatePurchaseReceiptDraftV2(id, request) }

    suspend fun confirm(id: Long): Result<PurchaseReceiptV2Dto> =
        safeApiCall { api.confirmPurchaseReceiptV2(id) }

    suspend fun cancel(id: Long): Result<PurchaseReceiptV2Dto> =
        safeApiCall { api.cancelPurchaseReceiptV2(id) }
}
