package com.zhihuiji.data.agent

import com.zhihuiji.core.model.v2.media.CreateMediaAssetRequest
import com.zhihuiji.core.model.v2.media.CreateMediaBindingRequest
import com.zhihuiji.core.model.v2.media.MediaAssetDto
import com.zhihuiji.core.model.v2.media.MediaBindingDto
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
) {
    suspend fun listAssets(): Result<List<MediaAssetDto>> =
        safeApiCall { api.mediaAssetsV2() }

    suspend fun getAsset(id: Long): Result<MediaAssetDto> =
        safeApiCall { api.mediaAssetV2(id) }

    suspend fun createAsset(request: CreateMediaAssetRequest): Result<MediaAssetDto> =
        safeApiCall { api.createMediaAssetV2(request) }

    suspend fun deleteAsset(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteMediaAssetV2(id) }

    suspend fun listBindings(targetType: String, targetId: Long): Result<List<MediaBindingDto>> =
        safeApiCall { api.mediaBindingsV2(targetType, targetId) }

    suspend fun createBinding(request: CreateMediaBindingRequest): Result<MediaBindingDto> =
        safeApiCall { api.createMediaBindingV2(request) }

    suspend fun deleteBinding(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteMediaBindingV2(id) }
}
