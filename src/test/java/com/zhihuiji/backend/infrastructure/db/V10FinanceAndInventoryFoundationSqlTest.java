package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V10FinanceAndInventoryFoundationSqlTest {
    @Test
    void migrationContainsFinanceAndInventoryFoundationContracts() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V10__finance_and_inventory_foundation.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS accounts"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS account_transfers"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS bill_fund_links"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS cash_change_records"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS inventory_ledger"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS inventory_snapshots"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS inventory_monthly_stats"));
        assertTrue(sql.contains("uk_account_transfers_owner_no"));
        assertTrue(sql.contains("fk_bill_fund_links_owner_account"));
        assertTrue(sql.contains("fk_account_transfers_owner_from"));
        assertTrue(sql.contains("fk_account_transfers_owner_to"));
        assertTrue(sql.contains("uk_inventory_snapshots_owner_product_date"));
        assertTrue(sql.contains("uk_inventory_monthly_stats_owner_product_period"));
    }
}
