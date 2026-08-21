package com.zhihuiji.core.model.v2.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncHealthV2Dto(
    val status: String = "",
    val message: String = "",
    @SerialName("owner_scoped") val ownerScoped: Boolean = false,
    @SerialName("server_time") val serverTime: Long = 0L,
    @SerialName("supported_entity_types") val supportedEntityTypes: List<String> = emptyList(),
    @SerialName("uploadable_entity_types") val uploadableEntityTypes: List<String> = emptyList(),
)

@Serializable
data class SyncCursorV2Dto(
    @SerialName("client_id") val clientId: String = "",
    @SerialName("last_cursor") val lastCursor: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class SyncCursorAckV2Request(
    @SerialName("client_id") val clientId: String,
    val cursor: String,
)

@Serializable
data class SyncChangeV2Dto(
    @SerialName("operation_id") val operationId: String? = null,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    val operation: String,
    val payload: String? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
    @SerialName("base_version") val baseVersion: Long? = null,
)

@Serializable
data class SyncUploadV2Request(
    @SerialName("client_id") val clientId: String,
    val changes: List<SyncChangeV2Dto>,
    @SerialName("last_sync_cursor") val lastSyncCursor: String? = null,
)

@Serializable
data class SyncUploadV2Response(
    @SerialName("accepted_count") val acceptedCount: Int = 0,
    @SerialName("failed_count") val failedCount: Int = 0,
    val status: String = "",
    @SerialName("next_cursor") val nextCursor: String = "",
    @SerialName("accepted_operation_ids") val acceptedOperationIds: List<String> = emptyList(),
    @SerialName("failed_operation_ids") val failedOperationIds: List<String> = emptyList(),
    val failures: List<SyncOperationFailureV2Dto> = emptyList(),
    @SerialName("operation_results") val operationResults: List<SyncOperationResultV2Dto> = emptyList(),
)

@Serializable
data class SyncOperationFailureV2Dto(
    @SerialName("operation_id") val operationId: String? = null,
    val code: String = "",
    val message: String = "",
)

@Serializable
data class SyncOperationResultV2Dto(
    @SerialName("operation_id") val operationId: String? = null,
    val status: String = "",
    val code: String? = null,
    val message: String? = null,
    @SerialName("server_version") val serverVersion: Long? = null,
    @SerialName("conflict_fields") val conflictFields: List<String> = emptyList(),
    @SerialName("server_payload") val serverPayload: String? = null,
)

@Serializable
data class SyncPullV2Request(
    @SerialName("client_id") val clientId: String,
    @SerialName("since_cursor") val sinceCursor: String? = null,
    val limit: Int? = null,
)

@Serializable
data class SyncPullV2Response(
    val changes: List<SyncChangeV2Dto> = emptyList(),
    @SerialName("effective_cursor") val effectiveCursor: String? = null,
    @SerialName("next_cursor") val nextCursor: String = "",
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class ImportJobV2Dto(
    val id: Long = 0L,
    @SerialName("client_id") val clientId: String = "",
    @SerialName("source_type") val sourceType: String = "",
    @SerialName("source_uri") val sourceUri: String? = null,
    @SerialName("source_checksum") val sourceChecksum: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    val status: String = "",
    val stage: String? = null,
    @SerialName("retry_count") val retryCount: Int = 0,
    @SerialName("replay_cursor") val replayCursor: String? = null,
    @SerialName("summary_json") val summaryJson: String? = null,
    @SerialName("options_json") val optionsJson: String? = null,
    @SerialName("failure_code") val failureCode: String? = null,
    @SerialName("failure_message") val failureMessage: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("finished_at") val finishedAt: Long? = null,
    @SerialName("last_heartbeat_at") val lastHeartbeatAt: Long? = null,
)

@Serializable
data class CreateImportJobV2Request(
    @SerialName("client_id") val clientId: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_uri") val sourceUri: String? = null,
    @SerialName("source_checksum") val sourceChecksum: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    @SerialName("replay_cursor") val replayCursor: String? = null,
    @SerialName("options_json") val optionsJson: String? = null,
)

@Serializable
data class RetryImportJobV2Request(
    @SerialName("replay_cursor") val replayCursor: String? = null,
)
