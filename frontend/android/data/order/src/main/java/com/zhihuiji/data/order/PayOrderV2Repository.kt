package com.zhihuiji.data.order

import com.zhihuiji.core.model.StatusRequest
import com.zhihuiji.core.model.v2.order.CreatePayOrderV2Request
import com.zhihuiji.core.model.v2.order.PayOrderV2Dto
import com.zhihuiji.core.model.v2.order.PayOrderV2Filter
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayOrderV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listPayOrders(filter: PayOrderV2Filter = PayOrderV2Filter()): Result<List<PayOrderV2Dto>> =
        safeApiCall {
            api.payOrdersV2(
                keyword = filter.keyword,
                status = filter.status,
                createdAfter = filter.createdAfter,
                createdBefore = filter.createdBefore,
            )
        }

    suspend fun getPayOrder(id: Long): Result<PayOrderV2Dto> =
        safeApiCall { api.payOrderV2(id) }

    suspend fun createPayOrder(request: CreatePayOrderV2Request): Result<PayOrderV2Dto> =
        safeApiCall { api.createPayOrderV2(request) }

    suspend fun updateStatus(id: Long, status: Int): Result<PayOrderV2Dto> =
        safeApiCall { api.updatePayOrderStatusV2(id, StatusRequest(status)) }
}
