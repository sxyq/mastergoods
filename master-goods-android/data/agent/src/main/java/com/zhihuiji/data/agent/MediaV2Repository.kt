package com.zhihuiji.data.agent

import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.model.v2.media.CreateMediaAssetRequest
import com.zhihuiji.core.model.v2.media.CreateMediaBindingRequest
import com.zhihuiji.core.model.v2.media.MediaAssetDto
import com.zhihuiji.core.model.v2.media.MediaBindingDto
import com.zhihuiji.core.network.NetworkConfig
import com.zhihuiji.core.network.ZhihuijiV2Api
import com.zhihuiji.core.network.safeApiCall
import com.zhihuiji.core.network.safeApiUnitCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaV2Repository @Inject constructor(
    private val api: ZhihuijiV2Api,
    private val settingsStore: SettingsStore,
    private val sessionStore: SessionStore,
) {
    suspend fun listAssets(): Result<List<MediaAssetDto>> =
        safeApiCall { api.mediaAssetsV2() }

    suspend fun getAsset(id: Long): Result<MediaAssetDto> =
        safeApiCall { api.mediaAssetV2(id) }

    suspend fun createAsset(request: CreateMediaAssetRequest): Result<MediaAssetDto> =
        safeApiCall { api.createMediaAssetV2(request) }

    suspend fun uploadAsset(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        assetType: String = "product_image",
    ): Result<MediaAssetDto> {
        val filePart = MultipartBody.Part.createFormData(
            name = "file",
            filename = fileName,
            body = bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
        )
        val assetTypePart = assetType.toRequestBody("text/plain".toMediaTypeOrNull())
        return safeApiCall { api.uploadMediaAssetV2(filePart, assetTypePart) }
    }

    suspend fun deleteAsset(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteMediaAssetV2(id) }

    suspend fun listBindings(targetType: String, targetId: Long): Result<List<MediaBindingDto>> =
        safeApiCall { api.mediaBindingsV2(targetType, targetId) }

    suspend fun createBinding(request: CreateMediaBindingRequest): Result<MediaBindingDto> =
        safeApiCall { api.createMediaBindingV2(request) }

    suspend fun deleteBinding(id: Long): Result<Unit> =
        safeApiUnitCall { api.deleteMediaBindingV2(id) }

    /** 拼接媒体内容访问 URL，配合 [peekAuthToken] 一起用于带认证的图片加载。 */
    fun contentUrlFor(assetId: Long): String =
        NetworkConfig.endpointUrl(settingsStore.peekBaseUrl(), "v2/media/assets/$assetId/content")

    /** 读取当前缓存的访问令牌，供图片加载器附加 Authorization 头。 */
    fun peekAuthToken(): String? = sessionStore.peekAccessToken()
}
