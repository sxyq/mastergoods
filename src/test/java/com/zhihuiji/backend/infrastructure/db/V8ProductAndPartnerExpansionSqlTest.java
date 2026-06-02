package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V8ProductAndPartnerExpansionSqlTest {
    @Test
    void migrationContainsSecondPhaseExpansionContracts() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V8__product_and_partner_expansion.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_categories"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_units"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS partner_groups"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS partner_contacts"));
        assertTrue(sql.contains("ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id BIGINT"));
        assertTrue(sql.contains("ALTER TABLE products ADD COLUMN IF NOT EXISTS unit_id BIGINT"));
        assertTrue(sql.contains("ALTER TABLE customers ADD COLUMN IF NOT EXISTS group_id BIGINT"));
        assertTrue(sql.contains("ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS group_id BIGINT"));
        assertTrue(sql.contains("INSERT INTO product_categories"));
        assertTrue(sql.contains("INSERT INTO product_units"));
    }
}
