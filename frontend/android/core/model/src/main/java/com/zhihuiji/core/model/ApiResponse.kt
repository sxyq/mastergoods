package com.zhihuiji.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    val timestamp: Long = 0L,
)
