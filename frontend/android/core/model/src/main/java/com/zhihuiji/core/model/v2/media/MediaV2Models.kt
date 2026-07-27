package com.zhihuiji.core.model.v2.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaAssetDto(
    val id: Long = 0,
    @SerialName("asset_type") val assetType: String = "",
    @SerialName("storage_provider") val storageProvider: String = "",
    @SerialName("bucket_name") val bucketName: String? = null,
    @SerialName("object_key") val objectKey: String = "",
    @SerialName("original_file_name") val originalFileName: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    val checksum: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("metadata_json") val metadataJson: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
)

@Serializable
data class CreateMediaAssetRequest(
    @SerialName("asset_type") val assetType: String,
    @SerialName("storage_provider") val storageProvider: String,
    @SerialName("bucket_name") val bucketName: String? = null,
    @SerialName("object_key") val objectKey: String,
    @SerialName("original_file_name") val originalFileName: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val checksum: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("metadata_json") val metadataJson: String? = null,
)

@Serializable
data class MediaBindingDto(
    val id: Long = 0,
    @SerialName("asset_id") val assetId: Long = 0L,
    @SerialName("target_type") val targetType: String = "",
    @SerialName("target_id") val targetId: Long = 0L,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
)

@Serializable
data class CreateMediaBindingRequest(
    @SerialName("asset_id") val assetId: Long,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: Long,
    @SerialName("sort_order") val sortOrder: Int? = null,
)
