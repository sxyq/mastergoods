package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V9ProductPriceLevelAndSupplierRelationSqlTest {
    @Test
    void migrationContainsThirdPhaseProductExpansionContracts() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V9__product_price_levels_and_supplier_relations.sql"));

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_price_levels"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS product_supplier_relations"));
        assertTrue(sql.contains("ALTER TABLE products"));
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS price_level_values_json VARCHAR(4000)"));
        assertTrue(sql.contains("uq_product_price_levels_owner_code"));
        assertTrue(sql.contains("uq_product_price_levels_owner_name"));
        assertTrue(sql.contains("uq_product_supplier_relations_owner_product_supplier"));
        assertTrue(sql.contains("uq_product_supplier_relations_owner_product_default"));
    }
}
