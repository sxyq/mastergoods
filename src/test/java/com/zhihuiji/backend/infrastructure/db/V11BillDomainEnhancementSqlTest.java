package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V11BillDomainEnhancementSqlTest {
    @Test
    void migrationContainsBillEnhancementTablesAndColumns() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V11__bill_domain_enhancement.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sales_returns"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS sales_return_items"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS purchase_receipts"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS purchase_receipt_items"));
        assertTrue(sql.contains("ALTER TABLE pay_orders ADD COLUMN IF NOT EXISTS account_id BIGINT"));
        assertTrue(sql.contains("uk_sales_returns_owner_return_no"));
        assertTrue(sql.contains("uk_purchase_receipts_owner_receipt_no"));
        assertTrue(sql.contains("idx_pay_orders_account"));
    }
}
