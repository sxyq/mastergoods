package com.zhihuiji.data.order

import com.zhihuiji.core.model.v2.order.CreatePurchaseOrderV2Request
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Dto
import com.zhihuiji.core.model.v2.order.PurchaseOrderV2Filter
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseOrderV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listPurchaseOrders(filter: PurchaseOrderV2Filter = PurchaseOrderV2Filter()): Result<List<PurchaseOrderV2Dto>> =
        safeApiCall { api.purchaseOrdersV2(filter.keyword, filter.status) }

    suspend fun getPurchaseOrder(id: Long): Result<PurchaseOrderV2Dto> =
        safeApiCall { api.purchaseOrderV2(id) }

    suspend fun createPurchaseOrder(request: CreatePurchaseOrderV2Request): Result<PurchaseOrderV2Dto> =
        safeApiCall { api.createPurchaseOrderV2(request) }

    suspend fun updatePurchaseOrder(id: Long, request: CreatePurchaseOrderV2Request): Result<PurchaseOrderV2Dto> =
        safeApiCall { api.updatePurchaseOrderV2(id, request) }

    suspend fun deletePurchaseOrder(id: Long): Result<Unit> =
        safeApiCall { api.deletePurchaseOrderV2(id) }
}
