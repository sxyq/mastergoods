package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    @Volatile
    private var cachedToken: String? = null

    fun updateToken(token: String?) {
        cachedToken = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (isAnonymousRequest(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = cachedToken ?: runBlocking { sessionStore.token.first() }
        val request = originalRequest.newBuilder().apply {
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }

    private fun isAnonymousRequest(path: String): Boolean {
        return path.endsWith("/auth/login") ||
            path.endsWith("/auth/register") ||
            path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/verify-code")
    }
}
