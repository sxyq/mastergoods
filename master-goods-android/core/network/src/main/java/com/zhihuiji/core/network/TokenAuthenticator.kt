package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.AuthResult
import com.zhihuiji.core.model.RefreshRequest
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore,
    private val json: Json,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        if (sessionStore.isTokenExpired()) return null
        val refreshToken = sessionStore.peekRefreshToken() ?: return null

        return try {
            val refreshBaseUrl = settingsStore.peekBaseUrl()
            if (!BuildConfig.ALLOW_CLEARTEXT_BASE_URL && !SettingsStore.isTrustedReleaseBaseUrl(refreshBaseUrl)) {
                return null
            }
            val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            val request = Request.Builder()
                .url("${refreshBaseUrl}auth/refresh")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            val client = OkHttpClient.Builder()
                .connectTimeout(NetworkConfig.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(NetworkConfig.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val refreshResponse = client.newCall(request).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string()
                val apiResponse = json.decodeFromString<ApiResponse<AuthResult>>(responseBody ?: "")
                val authResult = apiResponse.data
                if (apiResponse.code == 0 && authResult != null) {
                    sessionStore.saveSessionAsync(
                        token = authResult.token,
                        refreshToken = authResult.refreshToken,
                        userId = authResult.userId,
                        expiresIn = authResult.expiresIn,
                    )
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${authResult.token}")
                        .build()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
