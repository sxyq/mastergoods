package com.zhihuiji.backend.infrastructure.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class V42OwnerScopedBillNumbersSqlTest {
    private static final String MIGRATION = "src/main/resources/db/migration/V42__owner_scoped_bill_numbers.sql";

    @Test
    void migrationDropsGlobalKeysAndCreatesOwnerScopedIndexes() throws Exception {
        String sql = Files.readString(Path.of(MIGRATION)).toLowerCase();

        assertTrue(sql.contains("alter table sale_orders drop constraint if exists sale_orders_order_no_key"));
        assertTrue(sql.contains("alter table purchase_orders drop constraint if exists purchase_orders_order_no_key"));
        assertTrue(sql.contains("alter table pay_orders drop constraint if exists pay_orders_order_no_key"));
        assertTrue(sql.contains("alter table finance_records drop constraint if exists finance_records_record_no_key"));
        assertTrue(sql.contains("uq_sale_orders_owner_order_no"));
        assertTrue(sql.contains("uq_purchase_orders_owner_order_no"));
        assertTrue(sql.contains("uq_pay_orders_owner_order_no"));
        assertTrue(sql.contains("uq_finance_records_owner_record_no"));
    }

    @Test
    void sameNumberCanBeReusedAcrossOwnersButNotWithinOneOwner() throws Exception {
        String dbName = "owner_scoped_bill_numbers_v42_" + System.nanoTime();
        try (Connection connection = DriverManager.getConnection(
            "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                for (String table : new String[] {"sale_orders", "purchase_orders", "pay_orders"}) {
                    installOwnerScopedNumberIndex(statement, table, "order_no");
                }
                installOwnerScopedNumberIndex(statement, "finance_records", "record_no");

                for (String table : new String[] {"sale_orders", "purchase_orders", "pay_orders"}) {
                    assertOwnerScopedUniqueness(statement, table, "order_no");
                }
                assertOwnerScopedUniqueness(statement, "finance_records", "record_no");
            }
        }
    }

    private void installOwnerScopedNumberIndex(Statement statement, String table, String numberColumn)
        throws SQLException {
        statement.execute("CREATE TABLE " + table + " ("
            + "owner_user_id BIGINT NOT NULL, "
            + numberColumn + " VARCHAR(64) NOT NULL, "
            + "CONSTRAINT " + table + "_" + numberColumn + "_key UNIQUE (" + numberColumn + "))");
        statement.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS "
            + table + "_" + numberColumn + "_key");
        statement.execute("CREATE UNIQUE INDEX uq_" + table + "_owner_" + numberColumn
            + " ON " + table + "(owner_user_id, " + numberColumn + ")");
    }

    private void assertOwnerScopedUniqueness(Statement statement, String table, String numberColumn)
        throws SQLException {
        assertDoesNotThrow(() -> statement.execute(
            "INSERT INTO " + table + " (owner_user_id, " + numberColumn + ") VALUES (1, 'REUSED-001')"));
        assertDoesNotThrow(() -> statement.execute(
            "INSERT INTO " + table + " (owner_user_id, " + numberColumn + ") VALUES (2, 'REUSED-001')"));
        assertThrows(SQLException.class, () -> statement.execute(
            "INSERT INTO " + table + " (owner_user_id, " + numberColumn + ") VALUES (1, 'REUSED-001')"));
    }
}
