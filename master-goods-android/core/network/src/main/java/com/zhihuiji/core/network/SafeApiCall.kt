package com.zhihuiji.core.network

import com.zhihuiji.core.model.ApiResponse
import retrofit2.HttpException
import java.io.IOException

class NetworkException(val code: Int, message: String) : Exception(message)

suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> {
    return try {
        val response = block()
        if (response.code == 0 && response.data != null) {
            @Suppress("UNCHECKED_CAST")
            Result.success(response.data as T)
        } else {
            Result.failure(NetworkException(response.code, response.message))
        }
    } catch (e: HttpException) {
        Result.failure(NetworkException(e.code(), e.message()))
    } catch (e: IOException) {
        Result.failure(NetworkException(-1, "网络连接失败，请检查网络设置"))
    } catch (e: Exception) {
        Result.failure(NetworkException(-1, e.message ?: "未知错误"))
    }
}
