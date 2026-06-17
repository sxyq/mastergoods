package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(
    val id: Long,
    val phone: String,
    val nickname: String,
    val status: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("active_sessions") val activeSessions: Long,
)

@Serializable
data class CreateAdminUserRequest(
    val phone: String,
    val password: String,
    val nickname: String,
    val status: Int = 1,
)

@Serializable
data class UpdateAdminUserRequest(
    val nickname: String? = null,
    val status: Int? = null,
    val password: String? = null,
    @SerialName("keepSessions") val keepSessions: Boolean = false,
)
