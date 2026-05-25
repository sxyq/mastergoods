package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.AuthResult
import com.zhihuiji.core.model.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val refreshToken = runBlocking {
            sessionStore.refreshToken.first()
        } ?: return null

        return runBlocking {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
                val request = Request.Builder()
                    .url("${NetworkConfig.baseUrl}auth/refresh")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val client = OkHttpClient.Builder().build()
                val refreshResponse = client.newCall(request).execute()
                if (refreshResponse.isSuccessful) {
                    val responseBody = refreshResponse.body?.string()
                    val apiResponse = json.decodeFromString<ApiResponse<AuthResult>>(responseBody ?: "")
                    val authResult = apiResponse.data
                    if (apiResponse.code == 0 && authResult != null) {
                        sessionStore.saveSession(
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
