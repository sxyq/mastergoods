package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class V6AddForeignKeysSqlTest {
    @Test
    void migrationMatchesSnakeCaseSchema() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V6__add_foreign_keys.sql"));

        assertTrue(sql.contains("FOREIGN KEY (order_id)"));
        assertTrue(sql.contains("FOREIGN KEY (product_id)"));
        assertTrue(sql.contains("FOREIGN KEY (customer_id)"));
        assertTrue(sql.contains("FOREIGN KEY (supplier_id)"));
        assertTrue(sql.contains("ALTER TABLE inventory_adjustments"));
        assertFalse(sql.contains("ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_supplier"));

        assertFalse(sql.contains("orderId"));
        assertFalse(sql.contains("productId"));
        assertFalse(sql.contains("customerId"));
        assertFalse(sql.contains("supplierId"));
        assertFalse(sql.contains("stock_adjustments"));
    }
}
