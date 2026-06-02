package com.zhihuiji.core.network

import com.zhihuiji.core.datastore.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (isAnonymousRequest(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = sessionStore.peekAccessToken()
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
