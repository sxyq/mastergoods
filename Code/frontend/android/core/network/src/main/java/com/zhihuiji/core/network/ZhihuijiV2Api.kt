package com.zhihuiji.core.network

import com.zhihuiji.core.model.ApiResponse
import com.zhihuiji.core.model.v2.agent.AgentChatRequest
import com.zhihuiji.core.model.v2.agent.AgentChatResponse
import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.AgentDraftDto
import com.zhihuiji.core.model.v2.agent.AgentImageGenerateRequest
import com.zhihuiji.core.model.v2.agent.AgentImageGenerateResponse
import com.zhihuiji.core.model.v2.agent.AgentMessageDto
import com.zhihuiji.core.model.v2.agent.AgentNotificationDto
import com.zhihuiji.core.model.v2.agent.AgentRunCancelDto
import com.zhihuiji.core.model.v2.agent.AgentRunTraceDto
import com.zhihuiji.core.model.v2.agent.AgentTaskDto
import com.zhihuiji.core.model.v2.agent.AgentWorkbenchV2Dto
import com.zhihuiji.core.model.v2.agent.CreateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentDraftRequest
import com.zhihuiji.core.model.v2.agent.CreateAgentMessageRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentConversationRequest
import com.zhihuiji.core.model.v2.agent.UpdateAgentDraftRequest
import com.zhihuiji.core.model.v2.media.CreateMediaAssetRequest
import com.zhihuiji.core.model.v2.media.CreateMediaBindingRequest
import com.zhihuiji.core.model.v2.media.MediaAssetDto
import com.zhihuiji.core.model.v2.media.MediaBindingDto
import com.zhihuiji.core.model.v2.sync.CreateImportJobV2Request
import com.zhihuiji.core.model.v2.sync.ImportJobV2Dto
import com.zhihuiji.core.model.v2.sync.RetryImportJobV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorAckV2Request
import com.zhihuiji.core.model.v2.sync.SyncCursorV2Dto
import com.zhihuiji.core.model.v2.sync.SyncHealthV2Dto
import com.zhihuiji.core.model.v2.sync.SyncPullV2Request
import com.zhihuiji.core.model.v2.sync.SyncPullV2Response
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Request
import com.zhihuiji.core.model.v2.sync.SyncUploadV2Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query



interface ZhihuijiV2Api {

    @GET("v2/sync/health")
    suspend fun syncHealthV2(): ApiResponse<SyncHealthV2Dto>

    @GET("v2/sync/cursor/{clientId}")
    suspend fun syncCursorV2(@Path("clientId") clientId: String): ApiResponse<SyncCursorV2Dto>

    @POST("v2/sync/cursor/ack")
    suspend fun acknowledgeSyncCursorV2(@Body body: SyncCursorAckV2Request): ApiResponse<SyncCursorV2Dto>

    @POST("v2/sync/upload")
    suspend fun uploadSyncChangesV2(@Body body: SyncUploadV2Request): ApiResponse<SyncUploadV2Response>

    @POST("v2/sync/pull")
    suspend fun pullSyncChangesV2(@Body body: SyncPullV2Request): ApiResponse<SyncPullV2Response>

    @GET("v2/import-jobs")
    suspend fun importJobsV2(@Query("status") status: String? = null): ApiResponse<List<ImportJobV2Dto>>

    @GET("v2/import-jobs/{id}")
    suspend fun importJobV2(@Path("id") id: Long): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs")
    suspend fun createImportJobV2(@Body body: CreateImportJobV2Request): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs/{id}/retry")
    suspend fun retryImportJobV2(@Path("id") id: Long, @Body body: RetryImportJobV2Request? = null): ApiResponse<ImportJobV2Dto>

    @POST("v2/import-jobs/{id}/cancel")
    suspend fun cancelImportJobV2(@Path("id") id: Long): ApiResponse<ImportJobV2Dto>

    @GET("v2/agent/conversations")
    suspend fun agentConversationsV2(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<List<AgentConversationDto>>

    @GET("v2/agent/conversations/{id}")
    suspend fun agentConversationV2(@Path("id") id: Long): ApiResponse<AgentConversationDto>

    @POST("v2/agent/conversations")
    suspend fun createAgentConversationV2(@Body body: CreateAgentConversationRequest): ApiResponse<AgentConversationDto>

    @PUT("v2/agent/conversations/{id}")
    suspend fun updateAgentConversationV2(@Path("id") id: Long, @Body body: UpdateAgentConversationRequest): ApiResponse<AgentConversationDto>

    @DELETE("v2/agent/conversations/{id}")
    suspend fun deleteAgentConversationV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/agent/conversations/{conversationId}/run-traces")
    suspend fun agentRunTracesV2(
        @Path("conversationId") conversationId: Long,
        @Query("limit") limit: Int = 80,
    ): ApiResponse<List<AgentRunTraceDto>>

    @GET("v2/agent/conversations/{conversationId}/messages")
    suspend fun agentMessagesV2(
        @Path("conversationId") conversationId: Long,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<List<AgentMessageDto>>

    @POST("v2/agent/conversations/{conversationId}/messages")
    suspend fun createAgentMessageV2(
        @Path("conversationId") conversationId: Long,
        @Body body: CreateAgentMessageRequest,
    ): ApiResponse<AgentMessageDto>

    @GET("v2/agent/drafts")
    suspend fun agentDraftsV2(
        @Query("conversation_id") conversationId: Long? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<List<AgentDraftDto>>

    @POST("v2/agent/drafts")
    suspend fun createAgentDraftV2(@Body body: CreateAgentDraftRequest): ApiResponse<AgentDraftDto>

    @POST("v2/agent/drafts/{id}/confirm")
    suspend fun confirmAgentDraftV2(
        @Path("id") id: Long,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ApiResponse<AgentDraftDto>

    @POST("v2/agent/drafts/{id}/cancel")
    suspend fun cancelAgentDraftV2(@Path("id") id: Long): ApiResponse<AgentDraftDto>

    @PUT("v2/agent/drafts/{id}")
    suspend fun updateAgentDraftV2(@Path("id") id: Long, @Body body: UpdateAgentDraftRequest): ApiResponse<AgentDraftDto>

    @DELETE("v2/agent/drafts/{id}")
    suspend fun deleteAgentDraftV2(@Path("id") id: Long): ApiResponse<Unit>

    // ========== Agent V2 Chat / Workbench ==========

    @GET("v2/agent/workbench")
    suspend fun agentWorkbenchV2(): ApiResponse<AgentWorkbenchV2Dto>

    @GET("v2/agent/tasks")
    suspend fun agentTasksV2(): ApiResponse<List<AgentTaskDto>>

    @GET("v2/agent/notifications")
    suspend fun agentNotificationsV2(
        @Query("unread_only") unreadOnly: Boolean? = null,
    ): ApiResponse<List<AgentNotificationDto>>

    @POST("v2/agent/notifications/{id}/read")
    suspend fun markAgentNotificationReadV2(@Path("id") id: Long): ApiResponse<AgentNotificationDto>

    @POST("v2/agent/chat")
    suspend fun agentChatV2(
        @Body body: AgentChatRequest,
    ): ApiResponse<AgentChatResponse>

    @POST("v2/agent/images/generate")
    suspend fun agentGenerateImageV2(
        @Body body: AgentImageGenerateRequest,
    ): ApiResponse<AgentImageGenerateResponse>

    @POST("v2/agent/runs/{runId}/cancel")
    suspend fun cancelAgentRunV2(@Path("runId") runId: String): ApiResponse<AgentRunCancelDto>

    @GET("v2/media/assets")
    suspend fun mediaAssetsV2(): ApiResponse<List<MediaAssetDto>>

    @GET("v2/media/assets/{id}")
    suspend fun mediaAssetV2(@Path("id") id: Long): ApiResponse<MediaAssetDto>

    @POST("v2/media/assets")
    suspend fun createMediaAssetV2(@Body body: CreateMediaAssetRequest): ApiResponse<MediaAssetDto>

    @Multipart
    @POST("v2/media/assets/upload")
    suspend fun uploadMediaAssetV2(
        @Part file: MultipartBody.Part,
        @Part("asset_type") assetType: RequestBody,
    ): ApiResponse<MediaAssetDto>

    @DELETE("v2/media/assets/{id}")
    suspend fun deleteMediaAssetV2(@Path("id") id: Long): ApiResponse<Unit>

    @GET("v2/media/bindings")
    suspend fun mediaBindingsV2(
        @Query("target_type") targetType: String,
        @Query("target_id") targetId: Long,
    ): ApiResponse<List<MediaBindingDto>>

    @POST("v2/media/bindings")
    suspend fun createMediaBindingV2(@Body body: CreateMediaBindingRequest): ApiResponse<MediaBindingDto>

    @DELETE("v2/media/bindings/{id}")
    suspend fun deleteMediaBindingV2(@Path("id") id: Long): ApiResponse<Unit>

}