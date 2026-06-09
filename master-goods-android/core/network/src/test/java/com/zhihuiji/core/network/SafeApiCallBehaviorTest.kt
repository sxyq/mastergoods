package com.zhihuiji.core.network

import com.zhihuiji.core.model.ApiResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * Tests for safeApiCall and safeApiUnitCall behavior.
 * Verifies the critical distinction: safeApiCall requires data != null,
 * while safeApiUnitCall only checks code == 0.
 */
class SafeApiCallBehaviorTest {

    @Test
    fun safeApiCall_succeedsWhenDataIsNonNull() = runBlocking {
        val response = ApiResponse(code = 0, message = "ok", data = listOf("item"))
        val result = safeApiCall { response }
        assertTrue(result.isSuccess)
        assertEquals(listOf("item"), result.getOrNull())
    }

    @Test
    fun safeApiCall_failsWhenDataIsNullEvenIfCodeIsZero() = runBlocking {
        val response: ApiResponse<List<String>> = ApiResponse(code = 0, message = "ok", data = null)
        val result = safeApiCall { response }
        assertTrue(result.isFailure)
    }

    @Test
    fun safeApiCall_failsWhenCodeIsNonZero() = runBlocking {
        val response = ApiResponse(code = 400, message = "bad request", data = listOf("item"))
        val result = safeApiCall { response }
        assertTrue(result.isFailure)
    }

    @Test
    fun safeApiUnitCall_succeedsWhenCodeIsZeroEvenIfDataIsNull() = runBlocking {
        val response: ApiResponse<Unit> = ApiResponse(code = 0, message = "ok", data = null)
        val result = safeApiUnitCall { response }
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }

    @Test
    fun safeApiUnitCall_failsWhenCodeIsNonZero() = runBlocking {
        val response: ApiResponse<Unit> = ApiResponse(code = 500, message = "error", data = null)
        val result = safeApiUnitCall { response }
        assertTrue(result.isFailure)
    }

    @Test
    fun safeApiUnitCall_succeedsForDeleteApiResponse() = runBlocking {
        // Simulates the exact pattern used by deleteAccount/deleteBillFundLink/deleteDraft/deleteAsset/deleteBinding
        val response: ApiResponse<Unit> = ApiResponse(code = 0, message = "", data = null)
        val result = safeApiUnitCall { response }
        assertTrue("safeApiUnitCall should succeed for ApiResponse<Unit> with null data", result.isSuccess)
    }

    @Test
    fun safeApiCall_wouldFailForDeleteApiResponse() = runBlocking {
        // Demonstrates why safeApiUnitCall is required for delete operations
        val response: ApiResponse<Unit> = ApiResponse(code = 0, message = "", data = null)
        val result = safeApiCall { response }
        assertTrue("safeApiCall should fail for ApiResponse<Unit> with null data (this is why safeApiUnitCall exists)", result.isFailure)
    }

    @Test
    fun safeApiCall_mapsForbiddenToLoginStateMessage() = runBlocking {
        val result = safeApiCall<String> {
            throw HttpException(Response.error<String>(403, "".toResponseBody(null)))
        }

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as NetworkException
        assertEquals(403, error.code)
        assertEquals("登录状态无效或没有权限，请重新登录后再试", error.message)
    }
}
