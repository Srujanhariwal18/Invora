package com.invora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invora Part 2: Relational Database Schema & Constraint Verification Tests.
 * Verifies table structures, foreign keys, unique constraints, check constraints,
 * indexes, and development seed data.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseSchemaTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify that all 11 required core tables exist in the database")
    void testAllElevenTablesExist() {
        List<String> requiredTables = List.of(
            "users",
            "categories",
            "suppliers",
            "products",
            "warehouses",
            "zones",
            "racks",
            "inventory",
            "stock_transactions",
            "alerts",
            "audit_logs"
        );

        for (String table : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table, Integer.class
            );
            assertThat(count).as("Table %s should exist and be queryable", table).isNotNull();
        }
    }

    @Test
    @DisplayName("Verify seed data is present across all core entities")
    void testSeedDataPresence() {
        // 1. Users (1 ADMIN, 1 WAREHOUSE_MANAGER, 1 STAFF, 1 VIEWER)
        Integer userCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertThat(userCount).isGreaterThanOrEqualTo(4);

        Integer adminCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE role = 'ADMIN'", Integer.class
        );
        assertThat(adminCount).isEqualTo(1);

        Integer managerCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE role = 'WAREHOUSE_MANAGER'", Integer.class
        );
        assertThat(managerCount).isEqualTo(1);

        Integer staffCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE role = 'STAFF'", Integer.class
        );
        assertThat(staffCount).isEqualTo(1);

        Integer viewerCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE role = 'VIEWER'", Integer.class
        );
        assertThat(viewerCount).isEqualTo(1);

        // 2. Categories (3 categories)
        Integer categoryCount = jdbcTemplate.queryForObject("SELECT count(*) FROM categories", Integer.class);
        assertThat(categoryCount).isGreaterThanOrEqualTo(3);

        // 3. Suppliers (2 suppliers)
        Integer supplierCount = jdbcTemplate.queryForObject("SELECT count(*) FROM suppliers", Integer.class);
        assertThat(supplierCount).isGreaterThanOrEqualTo(2);

        // 4. Products (8 products)
        Integer productCount = jdbcTemplate.queryForObject("SELECT count(*) FROM products", Integer.class);
        assertThat(productCount).isBetween(5, 10);

        // 5. Warehouse (1 warehouse)
        Integer whCount = jdbcTemplate.queryForObject("SELECT count(*) FROM warehouses", Integer.class);
        assertThat(whCount).isEqualTo(1);

        // 6. Zones (2 zones)
        Integer zoneCount = jdbcTemplate.queryForObject("SELECT count(*) FROM zones", Integer.class);
        assertThat(zoneCount).isEqualTo(2);

        // 7. Racks (4 racks)
        Integer rackCount = jdbcTemplate.queryForObject("SELECT count(*) FROM racks", Integer.class);
        assertThat(rackCount).isEqualTo(4);

        // 8. Inventory (6 records)
        Integer invCount = jdbcTemplate.queryForObject("SELECT count(*) FROM inventory", Integer.class);
        assertThat(invCount).isGreaterThanOrEqualTo(6);

        // 9. Stock Transactions
        Integer txCount = jdbcTemplate.queryForObject("SELECT count(*) FROM stock_transactions", Integer.class);
        assertThat(txCount).isGreaterThanOrEqualTo(3);

        // 10. Alerts
        Integer alertCount = jdbcTemplate.queryForObject("SELECT count(*) FROM alerts", Integer.class);
        assertThat(alertCount).isGreaterThanOrEqualTo(1);

        // 11. Audit Logs
        Integer auditCount = jdbcTemplate.queryForObject("SELECT count(*) FROM audit_logs", Integer.class);
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Test 1: Insert inventory with quantity -1 (Expected: Database rejects it)")
    void test1_insertInventoryNegativeQuantityRejected() {
        String productId = "d0000000-0000-0000-0000-000000000005";
        String rackId = "e2000000-0000-0000-0000-000000000004";

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, rack_id, quantity) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(productId), UUID.fromString(rackId), -1
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Test 2: Insert duplicate SKU (Expected: Database rejects it)")
    void test2_insertDuplicateSkuRejected() {
        // 'SKU-BOLT-M10' is already seeded
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO products (id, sku, name, unit_price, threshold, reorder_quantity) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), "SKU-BOLT-M10", "Duplicate Bolt SKU", 10.00, 5, 10
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Test 3: Insert duplicate product + rack inventory (Expected: Database rejects it)")
    void test3_insertDuplicateProductAndRackInventoryRejected() {
        // Product 1 and Rack 1 combination already seeded in inventory
        String existingProductId = "d0000000-0000-0000-0000-000000000001";
        String existingRackId = "e2000000-0000-0000-0000-000000000001";

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, rack_id, quantity) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(existingProductId), UUID.fromString(existingRackId), 50
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Test 4: Insert stock transaction with quantity 0 (Expected: Database rejects it)")
    void test4_insertStockTransactionQuantityZeroRejected() {
        String productId = "d0000000-0000-0000-0000-000000000001";
        String userId = "a0000000-0000-0000-0000-000000000001";

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO stock_transactions (id, product_id, transaction_type, quantity, performed_by) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(productId), "STOCK_IN", 0, UUID.fromString(userId)
            );
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Test 5: Verify product -> category relationship")
    void test5_verifyProductCategoryRelationship() {
        // 5a. Non-existent category UUID must be rejected
        UUID fakeCategoryId = UUID.randomUUID();
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO products (id, sku, name, category_id, unit_price, threshold, reorder_quantity) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), "SKU-TEST-NONEXIST-CAT", "Test Product", fakeCategoryId, 15.00, 5, 10
            );
        }).isInstanceOf(DataIntegrityViolationException.class);

        // 5b. Valid category relationship should query successfully
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT p.name AS product_name, c.name AS category_name " +
            "FROM products p JOIN categories c ON p.category_id = c.id " +
            "WHERE p.sku = 'SKU-BOLT-M10'"
        );
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("CATEGORY_NAME")).isEqualTo("Raw Materials");
    }

    @Test
    @DisplayName("Test 6: Verify inventory -> product and rack relationships")
    void test6_verifyInventoryProductAndRackRelationships() {
        // 6a. Inventory with non-existent product must be rejected
        UUID fakeProductId = UUID.randomUUID();
        String validRackId = "e2000000-0000-0000-0000-000000000001";
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, rack_id, quantity) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), fakeProductId, UUID.fromString(validRackId), 10
            );
        }).isInstanceOf(DataIntegrityViolationException.class);

        // 6b. Inventory with non-existent rack must be rejected
        String validProductId = "d0000000-0000-0000-0000-000000000001";
        UUID fakeRackId = UUID.randomUUID();
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO inventory (id, product_id, rack_id, quantity) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(validProductId), fakeRackId, 10
            );
        }).isInstanceOf(DataIntegrityViolationException.class);

        // 6c. Valid join between inventory, product, and rack
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT i.quantity, p.sku, r.code AS rack_code " +
            "FROM inventory i " +
            "JOIN products p ON i.product_id = p.id " +
            "JOIN racks r ON i.rack_id = r.id " +
            "WHERE p.sku = 'SKU-BOLT-M10'"
        );
        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).get("QUANTITY")).isEqualTo(150);
        assertThat(rows.get(0).get("RACK_CODE")).isEqualTo("RA1");
    }

    @Test
    @DisplayName("Test 7: Verify stock transaction -> product and user relationships")
    void test7_verifyStockTransactionProductAndUserRelationships() {
        String validProductId = "d0000000-0000-0000-0000-000000000001";
        String validUserId = "a0000000-0000-0000-0000-000000000001";

        // 7a. Non-existent product in transaction must be rejected
        UUID fakeProductId = UUID.randomUUID();
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO stock_transactions (id, product_id, transaction_type, quantity, performed_by) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), fakeProductId, "STOCK_IN", 10, UUID.fromString(validUserId)
            );
        }).isInstanceOf(DataIntegrityViolationException.class);

        // 7b. Non-existent user in transaction must be rejected
        UUID fakeUserId = UUID.randomUUID();
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO stock_transactions (id, product_id, transaction_type, quantity, performed_by) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), UUID.fromString(validProductId), "STOCK_IN", 10, fakeUserId
            );
        }).isInstanceOf(DataIntegrityViolationException.class);

        // 7c. Valid join between stock_transactions, products, and users
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT st.transaction_type, st.quantity, p.sku, u.username " +
            "FROM stock_transactions st " +
            "JOIN products p ON st.product_id = p.id " +
            "JOIN users u ON st.performed_by = u.id " +
            "WHERE st.reference = 'PO-2026-001'"
        );
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("TRANSACTION_TYPE")).isEqualTo("STOCK_IN");
        assertThat(rows.get(0).get("QUANTITY")).isEqualTo(150);
        assertThat(rows.get(0).get("SKU")).isEqualTo("SKU-BOLT-M10");
        assertThat(rows.get(0).get("USERNAME")).isEqualTo("manager_demo");
    }

    @Test
    @DisplayName("Verify that all required indexes exist across tables")
    void testVerifyIndexesExist() throws Exception {
        assertThat(dataSource).as("DataSource should be available").isNotNull();
        try (var conn = dataSource.getConnection()) {
            var metaData = conn.getMetaData();

            List<String> expectedIndexNames = List.of(
                "IDX_USERS_USERNAME",
                "IDX_USERS_EMAIL",
                "IDX_PRODUCTS_SKU",
                "IDX_PRODUCTS_CATEGORY_ID",
                "IDX_ZONES_WAREHOUSE_ID",
                "IDX_RACKS_ZONE_ID",
                "IDX_INVENTORY_PRODUCT_ID",
                "IDX_INVENTORY_RACK_ID",
                "IDX_STOCK_TRANSACTIONS_PRODUCT_ID",
                "IDX_STOCK_TRANSACTIONS_CREATED_AT",
                "IDX_ALERTS_PRODUCT_ID",
                "IDX_ALERTS_STATUS",
                "IDX_AUDIT_LOGS_USER_ID",
                "IDX_AUDIT_LOGS_CREATED_AT"
            );

            List<String> tables = List.of(
                "USERS", "PRODUCTS", "ZONES", "RACKS", "INVENTORY", "STOCK_TRANSACTIONS", "ALERTS", "AUDIT_LOGS"
            );

            Set<String> actualIndexes = new HashSet<>();
            for (String table : tables) {
                try (var rs = metaData.getIndexInfo(null, null, table, false, false)) {
                    while (rs.next()) {
                        String indexName = rs.getString("INDEX_NAME");
                        if (indexName != null) {
                            actualIndexes.add(indexName.toUpperCase());
                        }
                    }
                }
            }

            for (String expectedIndex : expectedIndexNames) {
                assertThat(actualIndexes)
                    .as("Index %s should exist in database metadata", expectedIndex)
                    .contains(expectedIndex);
            }
        }
    }
}
