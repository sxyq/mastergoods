package com.zhihuiji.core.common

import com.zhihuiji.core.model.ApiResponse

class BusinessException(val code: Int, message: String) : Exception(message)

fun <T> ApiResponse<T>.requireData(): T {
    if (code != 0) {
        throw BusinessException(code, message)
    }
    return data ?: throw BusinessException(code, "数据为空")
}

fun <T> ApiResponse<T>.getOrThrow(): Result<T> {
    return if (code == 0 && data != null) {
        @Suppress("UNCHECKED_CAST")
        Result.success(data as T)
    } else {
        Result.failure(BusinessException(code, message))
    }
}
