package com.zhihuiji.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val phone: String,
    val password: String,
    @SerialName("verify_code") val verifyCode: String,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
enum class VerificationType(@SerialName("type") val value: String) {
    @SerialName("register") REGISTER("register"),
    @SerialName("login") LOGIN("login"),
    @SerialName("reset_password") RESET_PASSWORD("reset_password"),
}

@Serializable
data class VerifyCodeRequest(
    val phone: String,
    val type: VerificationType,
)

@Serializable
data class VerifyCodeResponse(
    val success: Boolean,
    @SerialName("expire_seconds") val expireSeconds: Int,
)

@Serializable
data class AuthResult(
    @SerialName("user_id") val userId: Long,
    val token: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
)

@Serializable
data class UserProfile(
    val id: Long,
    val phone: String,
    val nickname: String,
    val status: Int,
)
