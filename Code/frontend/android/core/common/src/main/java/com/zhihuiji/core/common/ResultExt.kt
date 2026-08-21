package com.zhihuiji.core.common

import com.zhihuiji.core.model.ApiResponse

class BusinessException(val code: Int, message: String) : Exception(message)

@Deprecated(
    message = "Legacy ApiResponse helper. Prefer safeApiCall and Kotlin Result chaining in repositories.",
)
fun <T> ApiResponse<T>.requireData(): T {
    if (code != 0) {
        throw BusinessException(code, message)
    }
    return data ?: throw BusinessException(code, "数据为空")
}
