package com.zhihuiji.core.network

import com.zhihuiji.core.model.*
import retrofit2.http.*

interface ZhihuijiApi {
    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthResult>

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthResult>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): ApiResponse<AuthResult>

    @POST("v1/auth/logout")
    suspend fun logout(): ApiResponse<Unit>

    @POST("v1/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeRequest): ApiResponse<VerifyCodeResponse>

    @GET("v1/auth/users/me")
    suspend fun me(): ApiResponse<UserProfile>

    @GET("v1/admin/users")
    suspend fun adminUsers(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<List<AdminUser>>

    @POST("v1/admin/users")
    suspend fun createAdminUser(@Body body: CreateAdminUserRequest): ApiResponse<AdminUser>

    @PUT("v1/admin/users/{userId}")
    suspend fun updateAdminUser(
        @Path("userId") userId: Long,
        @Body body: UpdateAdminUserRequest,
    ): ApiResponse<AdminUser>

    @GET("v2/stores/current")
    suspend fun currentStore(): ApiResponse<CurrentStoreProfile>

    @GET("v2/stores/current/members")
    suspend fun storeMembers(): ApiResponse<List<StoreStaffMember>>

    @POST("v2/stores/current/members")
    suspend fun createStoreMember(@Body body: CreateStoreStaffMemberRequest): ApiResponse<StoreStaffMember>

    @PUT("v2/stores/current/members/{userId}")
    suspend fun updateStoreMember(
        @Path("userId") userId: Long,
        @Body body: UpdateStoreStaffMemberRequest,
    ): ApiResponse<StoreStaffMember>

    @GET("v1/sync/health")
    suspend fun syncHealth(): ApiResponse<SyncHealthResult>

    @POST("v1/sync/pull")
    suspend fun pull(@Body body: PullRequest): ApiResponse<PullResult>

    @POST("v1/sync/upload")
    suspend fun upload(@Body body: UploadRequest): ApiResponse<UploadResult>
}
