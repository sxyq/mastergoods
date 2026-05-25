package com.zhihuiji.data.auth

import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.datastore.SettingsStore
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore,
) {
    val isLoggedIn = sessionStore.isLoggedIn

    suspend fun login(phone: String, password: String): Result<AuthResult> {
        return safeApiCall { api.login(LoginRequest(phone, password)) }.onSuccess { auth ->
            sessionStore.saveSession(auth.token, auth.refreshToken, auth.userId, auth.expiresIn)
        }
    }

    suspend fun register(phone: String, password: String, verifyCode: String): Result<AuthResult> {
        return safeApiCall { api.register(RegisterRequest(phone, password, verifyCode)) }.onSuccess { auth ->
            sessionStore.saveSession(auth.token, auth.refreshToken, auth.userId, auth.expiresIn)
        }
    }

    suspend fun refresh(refreshToken: String): Result<AuthResult> {
        return safeApiCall { api.refresh(RefreshRequest(refreshToken)) }.onSuccess { auth ->
            sessionStore.saveSession(auth.token, auth.refreshToken, auth.userId, auth.expiresIn)
        }
    }

    suspend fun logout() {
        try {
            val token = sessionStore.requireAccessToken()
            api.logout("Bearer $token")
        } catch (_: Exception) {
        } finally {
            sessionStore.clearSession()
        }
    }

    suspend fun fetchCurrentUser(): Result<UserProfile> {
        return try {
            val token = sessionStore.requireAccessToken()
            safeApiCall { api.me("Bearer $token") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSessionIfNeeded(): Boolean {
        return try {
            val token = sessionStore.requireAccessToken()
            val result = fetchCurrentUser()
            result.isSuccess
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearSessionAndCache() {
        sessionStore.clearSession()
    }
}
