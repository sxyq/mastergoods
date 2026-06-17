package com.zhihuiji.data.auth

import androidx.annotation.VisibleForTesting
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val sessionStore: SessionStore,
    private val localDataCleaner: LocalDataCleaner,
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

    @VisibleForTesting
    suspend fun refresh(refreshToken: String): Result<AuthResult> {
        return safeApiCall { api.refresh(RefreshRequest(refreshToken)) }.onSuccess { auth ->
            sessionStore.saveSession(auth.token, auth.refreshToken, auth.userId, auth.expiresIn)
        }
    }

    suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
        } finally {
            localDataCleaner.clearAll()
        }
    }

    suspend fun fetchCurrentUser(): Result<UserProfile> {
        return safeApiCall { api.me() }
    }

    suspend fun fetchAdminUsers(
        keyword: String? = null,
        page: Int? = null,
        size: Int? = null,
    ): Result<List<AdminUser>> {
        return safeApiCall { api.adminUsers(keyword = keyword, page = page, size = size) }
    }

    suspend fun createAdminUser(
        phone: String,
        nickname: String,
        password: String,
        status: Int = 1,
    ): Result<AdminUser> {
        return safeApiCall {
            api.createAdminUser(
                CreateAdminUserRequest(
                    phone = phone,
                    password = password,
                    nickname = nickname,
                    status = status,
                )
            )
        }
    }

    suspend fun updateAdminUser(
        userId: Long,
        nickname: String? = null,
        password: String? = null,
        status: Int? = null,
        keepSessions: Boolean = false,
    ): Result<AdminUser> {
        return safeApiCall {
            api.updateAdminUser(
                userId = userId,
                body = UpdateAdminUserRequest(
                    nickname = nickname,
                    status = status,
                    password = password,
                    keepSessions = keepSessions,
                )
            )
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

    suspend fun clearSession() {
        localDataCleaner.clearAll()
    }
}
