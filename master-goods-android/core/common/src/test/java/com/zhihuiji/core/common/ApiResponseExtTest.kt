package com.zhihuiji.core.common

import com.zhihuiji.core.model.ApiResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseExtTest {
    @Test
    fun requireData_returnsPayloadWhenCodeIsZero() {
        val response = ApiResponse(code = 0, message = "ok", data = "payload")

        assertEquals("payload", response.requireData())
    }

    @Test
    fun requireData_throwsBusinessExceptionWhenCodeIsNotZero() {
        val response = ApiResponse<String>(code = 401, message = "unauthorized")
        val error = runCatching { response.requireData() }.exceptionOrNull()

        assertTrue(error is BusinessException)
        assertEquals(401, (error as BusinessException).code)
    }
}
