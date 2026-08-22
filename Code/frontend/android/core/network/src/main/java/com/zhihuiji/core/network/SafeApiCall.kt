package com.zhihuiji.core.network

import com.zhihuiji.core.model.ApiResponse
import retrofit2.HttpException
import java.io.IOException

class NetworkException(val code: Int, message: String) : Exception(message)

enum class HttpErrorKind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, VALIDATION, SERVER, NETWORK, UNKNOWN }

val NetworkException.kind: HttpErrorKind
    get() = when (code) {
        401 -> HttpErrorKind.UNAUTHORIZED
        403 -> HttpErrorKind.FORBIDDEN
        404 -> HttpErrorKind.NOT_FOUND
        409 -> HttpErrorKind.CONFLICT
        422 -> HttpErrorKind.VALIDATION
        in 500..599 -> HttpErrorKind.SERVER
        -1 -> HttpErrorKind.NETWORK
        else -> HttpErrorKind.UNKNOWN
    }

@Suppress("UNCHECKED_CAST")
suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> =
    runSafeApi(block) { response ->
        if (response.code == 0 && response.data != null) response.data as T else null
    }

suspend fun safeApiUnitCall(block: suspend () -> ApiResponse<*>): Result<Unit> =
    runSafeApi(block) { response ->
        if (response.code == 0) Unit else null
    }

private suspend fun <T> runSafeApi(
    block: suspend () -> ApiResponse<*>,
    onSuccess: (ApiResponse<*>) -> T?,
): Result<T> = try {
    val response = block()
    val data = onSuccess(response)
    if (data != null) {
        Result.success(data)
    } else {
        Result.failure(NetworkException(response.code, response.message))
    }
} catch (e: HttpException) {
    Result.failure(NetworkException(e.code(), httpErrorMessage(e.code(), e.message())))
} catch (e: IOException) {
    Result.failure(NetworkException(-1, "网络连接失败，请检查网络设置"))
} catch (e: Exception) {
    Result.failure(NetworkException(-1, e.message ?: "未知错误"))
}

internal fun httpErrorMessage(code: Int, fallback: String): String = when (code) {
    401 -> "登录已失效，请重新登录"
    403 -> "登录状态无效或没有权限，请重新登录后再试"
    404 -> "远程服务地址不正确或接口不存在，请检查服务器配置"
    409 -> "请求与当前数据冲突，请刷新后重试"
    422 -> "请求参数未通过校验，请检查输入内容"
    in 500..599 -> "服务器暂时不可用，请稍后重试"
    else -> fallback.ifBlank { "请求失败：$code" }
}
