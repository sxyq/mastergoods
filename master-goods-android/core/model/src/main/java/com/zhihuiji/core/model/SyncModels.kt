package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncHealthResult(
    val status: String = "",
    val message: String = "",
    @SerialName("serverTime") val serverTime: Long = 0L,
)

@Serializable
enum class SyncOperation {
    @SerialName("create") CREATE,
    @SerialName("update") UPDATE,
    @SerialName("delete") DELETE,
}

@Serializable
data class SyncChangeDto(
    @SerialName("entityType") val entityType: String,
    @SerialName("entityId") val entityId: String,
    val operation: SyncOperation,
    val payload: String,
    @SerialName("updatedAt") val updatedAt: Long,
)

@Serializable
data class PullRequest(
    @SerialName("sinceCursor") val sinceCursor: String? = null,
    val limit: Int? = 200,
)

@Serializable
data class PullResult(
    val changes: List<SyncChangeDto> = emptyList(),
    @SerialName("nextCursor") val nextCursor: String = "",
    @SerialName("hasMore") val hasMore: Boolean = false,
)

@Serializable
data class UploadRequest(
    @SerialName("clientId") val clientId: String,
    val changes: List<SyncChangeDto> = emptyList(),
    @SerialName("lastSyncCursor") val lastSyncCursor: String? = null,
)

@Serializable
data class UploadResult(
    @SerialName("acceptedCount") val acceptedCount: Int = 0,
    @SerialName("failedCount") val failedCount: Int = 0,
    val message: String = "",
    @SerialName("nextCursor") val nextCursor: String = "",
)
