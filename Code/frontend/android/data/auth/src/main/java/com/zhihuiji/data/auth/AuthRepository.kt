package com.zhihuiji.data.auth

import androidx.annotation.VisibleForTesting
import com.zhihuiji.core.datastore.SessionStore
import com.zhihuiji.core.model.*
import com.zhihuiji.core.network.ZhihuijiApi
import com.zhihuiji.core.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

// v1 only: backend has no /v2/auth endpoints planned. Auth (login/register/refresh/logout/me),
// admin user management, and store member management stay on v1 ZhihuijiApi by design.
@Singleton
class AuthRepository @Inject constructor(
    private val api: ZhihuijiApi,
    private val sessionStore: SessionStore,
    private val localDataCleaner: LocalDataCleaner,
) {
    val isLoggedIn = sessionStore.isLoggedIn

    suspend fun login(phone: String, password: String): Result<AuthResult> =
        safeApiCall { api.login(LoginRequest(phone, password)) }.onSuccess { saveSession(it) }

    suspend fun register(phone: String, password: String, verifyCode: String): Result<AuthResult> =
        safeApiCall { api.register(RegisterRequest(phone, password, verifyCode)) }.onSuccess { saveSession(it) }

    @VisibleForTesting
    suspend fun refresh(refreshToken: String): Result<AuthResult> =
        safeApiCall { api.refresh(RefreshRequest(refreshToken)) }.onSuccess { saveSession(it) }

    private suspend fun saveSession(auth: AuthResult) {
        sessionStore.saveSession(auth.token, auth.refreshToken, auth.userId, auth.expiresIn)
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

    suspend fun fetchCurrentStore(): Result<CurrentStoreProfile> {
        return safeApiCall { api.currentStore() }
    }

    suspend fun fetchStoreMembers(): Result<List<StoreStaffMember>> {
        return safeApiCall { api.storeMembers() }
    }

    suspend fun createStoreMember(
        phone: String,
        nickname: String,
        password: String,
        role: String,
        title: String? = null,
        status: Int = 1,
    ): Result<StoreStaffMember> {
        return safeApiCall {
            api.createStoreMember(
                CreateStoreStaffMemberRequest(
                    phone = phone,
                    password = password,
                    nickname = nickname,
                    role = role,
                    title = title,
                    status = status,
                )
            )
        }
    }

    suspend fun updateStoreMember(
        userId: Long,
        nickname: String? = null,
        password: String? = null,
        role: String? = null,
        title: String? = null,
        status: Int? = null,
        keepSessions: Boolean = false,
    ): Result<StoreStaffMember> {
        return safeApiCall {
            api.updateStoreMember(
                userId = userId,
                body = UpdateStoreStaffMemberRequest(
                    nickname = nickname,
                    password = password,
                    role = role,
                    title = title,
                    status = status,
                    keepSessions = keepSessions,
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
            sessionStore.requireAccessToken()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clearSession() {
        localDataCleaner.clearAll()
    }
}
