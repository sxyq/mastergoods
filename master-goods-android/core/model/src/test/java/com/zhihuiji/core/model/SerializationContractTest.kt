package com.zhihuiji.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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

    @Test
    fun reconciliationSummary_usesSnakeCaseForPartnerCounts() {
        val encoded = json.encodeToString(
            ReconciliationSummaryReportDto(
                startAt = 1L,
                endAt = 2L,
                totalReceivableAmount = 300.0,
                totalPayableAmount = 120.0,
                totalReceivableCustomerCount = 4L,
                totalPayableSupplierCount = 2L,
            ),
        )

        assertTrue(encoded.contains("\"total_receivable_customer_count\":4"))
        assertTrue(encoded.contains("\"total_payable_supplier_count\":2"))
    }

    @Test
    fun salesTrendPoint_usesSnakeCaseForDashboardAggregateContract() {
        val encoded = json.encodeToString(
            SalesTrendPointReportDto(
                startAt = 1L,
                endAt = 2L,
                totalSalesAmount = 88.5,
                totalOrderCount = 3,
            ),
        )

        assertTrue(encoded.contains("\"start_at\":1"))
        assertTrue(encoded.contains("\"end_at\":2"))
        assertTrue(encoded.contains("\"total_sales_amount\":88.5"))
        assertTrue(encoded.contains("\"total_order_count\":3"))
    }

    @Test
    fun reconciliationSummary_decodesOldResponsesWithoutPartnerCounts() {
        val decoded = json.decodeFromString<ReconciliationSummaryReportDto>(
            """
            {
              "start_at": 1,
              "end_at": 2,
              "total_receivable_amount": 300.0,
              "total_payable_amount": 120.0,
              "total_received_amount": 80.0,
              "total_paid_amount": 30.0,
              "net_cash_flow": 50.0
            }
            """.trimIndent(),
        )

        assertEquals(0L, decoded.totalReceivableCustomerCount)
        assertEquals(0L, decoded.totalPayableSupplierCount)
    }
}
