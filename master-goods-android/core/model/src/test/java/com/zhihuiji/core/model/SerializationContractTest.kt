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

    @Test
    fun productDto_usesSnakeCaseForPriceStockAndSyncFields() {
        val encoded = json.encodeToString(
            ProductDto(
                code = "A001",
                name = "纯净水",
                salePrice = 1.5,
                purchasePrice = 1.0,
                safeStock = 20.0,
                syncStatus = 1,
                syncVersion = 2L,
            ),
        )

        assertTrue(encoded.contains("\"sale_price\":1.5"))
        assertTrue(encoded.contains("\"purchase_price\":1.0"))
        assertTrue(encoded.contains("\"safe_stock\":20.0"))
        assertTrue(encoded.contains("\"sync_status\":1"))
        assertTrue(encoded.contains("\"sync_version\":2"))
    }
}
