package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentStoreProfile(
    @SerialName("store_id") val storeId: Long,
    @SerialName("store_name") val storeName: String,
    @SerialName("owner_user_id") val ownerUserId: Long,
    @SerialName("current_user_id") val currentUserId: Long,
    @SerialName("current_user_name") val currentUserName: String,
    @SerialName("current_user_phone") val currentUserPhone: String,
    val role: String,
    val title: String,
    val status: Int,
    val permissions: List<String> = emptyList(),
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("enabled_member_count") val enabledMemberCount: Int = 0,
    @SerialName("disabled_member_count") val disabledMemberCount: Int = 0,
)

@Serializable
data class StoreStaffMember(
    @SerialName("user_id") val id: Long,
    val phone: String,
    val nickname: String,
    val role: String,
    val title: String,
    val status: Int,
    val permissions: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("active_sessions") val activeSessions: Long,
    @SerialName("store_id") val storeId: Long,
    @SerialName("store_name") val storeName: String,
)

@Serializable
data class CreateStoreStaffMemberRequest(
    val phone: String,
    val password: String,
    val nickname: String,
    val role: String,
    val title: String? = null,
    val status: Int = 1,
)

@Serializable
data class UpdateStoreStaffMemberRequest(
    val nickname: String? = null,
    val password: String? = null,
    val role: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val keepSessions: Boolean = false,
)
