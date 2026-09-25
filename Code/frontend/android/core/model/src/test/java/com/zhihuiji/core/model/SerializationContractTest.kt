package com.zhihuiji.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationContractTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun authResult_usesSnakeCaseForBackendContract() {
        val encoded = json.encodeToString(
            AuthResult(userId = 8L, token = "token", refreshToken = "refresh", expiresIn = 3600),
        )

        assertTrue(encoded.contains("\"user_id\":8"))
        assertTrue(encoded.contains("\"refresh_token\":\"refresh\""))
        assertTrue(encoded.contains("\"expires_in\":3600"))
    }
}
