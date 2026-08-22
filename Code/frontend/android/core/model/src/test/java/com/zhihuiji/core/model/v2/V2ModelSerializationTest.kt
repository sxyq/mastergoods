package com.zhihuiji.core.model.v2

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2 model serialization contract tests.
 * Verifies @SerialName annotations produce correct snake_case JSON fields matching backend DTOs.
 */
class V2ModelSerializationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun createPayOrderRequest_serializesIdempotencyKeyAsSnakeCase() {
        val request = com.zhihuiji.core.model.v2.order.CreatePayOrderV2Request(
            idempotencyKey = "pay-key-1",
            amount = 10.0,
            method = 1,
        )
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"idempotency_key\":\"pay-key-1\""))
    }

    // --- Finance ---

    @Test
    fun accountV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.finance.AccountV2Dto(
            id = 1L, isDefault = true, sortOrder = 5, createdAt = 1000L, updatedAt = 2000L,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"is_default\":true"))
        assertTrue(encoded.contains("\"sort_order\":5"))
        assertTrue(encoded.contains("\"created_at\":1000"))
        assertTrue(encoded.contains("\"updated_at\":2000"))
    }

    @Test
    fun accountTransferV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.finance.AccountTransferV2Dto(
            transferNo = "T001", fromAccountId = 1L, fromAccountName = "Cash",
            toAccountId = 2L, toAccountName = "Bank",
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"transfer_no\":\"T001\""))
        assertTrue(encoded.contains("\"from_account_id\":1"))
        assertTrue(encoded.contains("\"from_account_name\":\"Cash\""))
        assertTrue(encoded.contains("\"to_account_id\":2"))
        assertTrue(encoded.contains("\"to_account_name\":\"Bank\""))
    }

    @Test
    fun billFundLinkV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.finance.BillFundLinkV2Dto(
            billType = "sale_order", billId = 10L, accountId = 1L,
            accountName = "Cash", linkType = 1,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"bill_type\":\"sale_order\""))
        assertTrue(encoded.contains("\"bill_id\":10"))
        assertTrue(encoded.contains("\"account_id\":1"))
        assertTrue(encoded.contains("\"account_name\":\"Cash\""))
        assertTrue(encoded.contains("\"link_type\":1"))
    }

    // --- Inventory ---

    @Test
    fun inventoryLedgerEntryV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.inventory.InventoryLedgerEntryV2Dto(
            productId = 1L, productCode = "A001", productName = "Water",
            quantityBefore = 10.0, quantityChange = -2.0, quantityAfter = 8.0,
            unitCost = 1.5, sourceType = "sale_order", sourceId = 100L, sourceNo = "SO001",
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"product_id\":1"))
        assertTrue(encoded.contains("\"product_code\":\"A001\""))
        assertTrue(encoded.contains("\"product_name\":\"Water\""))
        assertTrue(encoded.contains("\"quantity_before\":10.0"))
        assertTrue(encoded.contains("\"quantity_change\":-2.0"))
        assertTrue(encoded.contains("\"quantity_after\":8.0"))
        assertTrue(encoded.contains("\"unit_cost\":1.5"))
        assertTrue(encoded.contains("\"source_type\":\"sale_order\""))
        assertTrue(encoded.contains("\"source_id\":100"))
        assertTrue(encoded.contains("\"source_no\":\"SO001\""))
    }

    @Test
    fun inventorySnapshotV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.inventory.InventorySnapshotV2Dto(
            productId = 1L, snapshotDate = 20260101L, totalValue = 150.0,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"product_id\":1"))
        assertTrue(encoded.contains("\"snapshot_date\":20260101"))
        assertTrue(encoded.contains("\"total_value\":150.0"))
    }

    @Test
    fun inventoryMonthlyStatsV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.inventory.InventoryMonthlyStatsV2Dto(
            productId = 1L, quantityIn = 100.0, quantityOut = 80.0,
            quantityAdjust = 5.0, quantityBegin = 50.0, quantityEnd = 75.0,
            totalCostIn = 1000.0, totalCostOut = 800.0,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"product_id\":1"))
        assertTrue(encoded.contains("\"quantity_in\":100.0"))
        assertTrue(encoded.contains("\"quantity_out\":80.0"))
        assertTrue(encoded.contains("\"quantity_adjust\":5.0"))
        assertTrue(encoded.contains("\"quantity_begin\":50.0"))
        assertTrue(encoded.contains("\"quantity_end\":75.0"))
        assertTrue(encoded.contains("\"total_cost_in\":1000.0"))
        assertTrue(encoded.contains("\"total_cost_out\":800.0"))
    }

    // --- Order (most drift-prone fields) ---

    @Test
    fun saleOrderV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.order.SaleOrderV2Dto(
            orderNo = "SO001", customerId = 1L, customerName = "Alice",
            subtotalAmount = 100.0, discountAmount = 10.0,
            totalAmount = 90.0, paidAmount = 90.0,
            createdAt = 1000L, updatedAt = 2000L,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"order_no\":\"SO001\""))
        assertTrue(encoded.contains("\"customer_id\":1"))
        assertTrue(encoded.contains("\"customer_name\":\"Alice\""))
        assertTrue(encoded.contains("\"subtotal_amount\":100.0"))
        assertTrue(encoded.contains("\"discount_amount\":10.0"))
        assertTrue(encoded.contains("\"total_amount\":90.0"))
        assertTrue(encoded.contains("\"paid_amount\":90.0"))
        assertTrue(encoded.contains("\"created_at\":1000"))
        assertTrue(encoded.contains("\"updated_at\":2000"))
    }

    @Test
    fun payOrderV2Dto_serializesSnakeCaseFields() {
        val dto = com.zhihuiji.core.model.v2.order.PayOrderV2Dto(
            orderNo = "PO001", supplierId = 1L, supplierName = "Vendor",
            referenceNo = "REF001", accountId = 2L,
            createdAt = 1000L, updatedAt = 2000L,
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"order_no\":\"PO001\""))
        assertTrue(encoded.contains("\"supplier_id\":1"))
        assertTrue(encoded.contains("\"supplier_name\":\"Vendor\""))
        assertTrue(encoded.contains("\"reference_no\":\"REF001\""))
        assertTrue(encoded.contains("\"account_id\":2"))
    }

    // --- Filter classes (newly annotated with @Serializable + @SerialName) ---

    @Test
    fun saleOrderV2Filter_serializesSnakeCaseFields() {
        val filter = com.zhihuiji.core.model.v2.order.SaleOrderV2Filter(
            keyword = "test", minTotalAmount = "100", maxTotalAmount = "500",
            createdAfter = "2026-01-01", createdBefore = "2026-06-01",
            productKeyword = "water", paymentStatus = "paid",
        )
        val encoded = json.encodeToString(filter)
        assertTrue(encoded.contains("\"min_total_amount\":\"100\""))
        assertTrue(encoded.contains("\"max_total_amount\":\"500\""))
        assertTrue(encoded.contains("\"created_after\":\"2026-01-01\""))
        assertTrue(encoded.contains("\"created_before\":\"2026-06-01\""))
        assertTrue(encoded.contains("\"product_keyword\":\"water\""))
        assertTrue(encoded.contains("\"payment_status\":\"paid\""))
    }

    @Test
    fun payOrderV2Filter_serializesSnakeCaseFields() {
        val filter = com.zhihuiji.core.model.v2.order.PayOrderV2Filter(
            createdAfter = "2026-01-01", createdBefore = "2026-06-01", page = 2, size = 25,
        )
        val encoded = json.encodeToString(filter)
        assertTrue(encoded.contains("\"created_after\":\"2026-01-01\""))
        assertTrue(encoded.contains("\"created_before\":\"2026-06-01\""))
        assertTrue(encoded.contains("\"page\":2"))
        assertTrue(encoded.contains("\"size\":25"))
    }

    @Test
    fun purchaseOrderV2Filter_serializesCoreFields() {
        val filter = com.zhihuiji.core.model.v2.order.PurchaseOrderV2Filter(
            keyword = "vendor", status = 2,
        )
        val encoded = json.encodeToString(filter)
        assertTrue(encoded.contains("\"keyword\":\"vendor\""))
        assertTrue(encoded.contains("\"status\":2"))
    }

    @Test
    fun salesReturnV2Filter_serializesCoreFields() {
        val filter = com.zhihuiji.core.model.v2.order.SalesReturnV2Filter(
            keyword = "return", status = 1,
        )
        val encoded = json.encodeToString(filter)
        assertTrue(encoded.contains("\"keyword\":\"return\""))
        assertTrue(encoded.contains("\"status\":1"))
    }

    @Test
    fun purchaseReceiptV2Filter_serializesCoreFields() {
        val filter = com.zhihuiji.core.model.v2.order.PurchaseReceiptV2Filter(
            keyword = "receipt", status = 3,
        )
        val encoded = json.encodeToString(filter)
        assertTrue(encoded.contains("\"keyword\":\"receipt\""))
        assertTrue(encoded.contains("\"status\":3"))
    }
}
