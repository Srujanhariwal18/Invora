package com.invora;

import com.invora.entity.Alert;
import com.invora.entity.AlertStatus;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.Role;
import com.invora.entity.StockTransaction;
import com.invora.entity.User;
import com.invora.repository.AlertRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find a product by SKU")
    void testFindProductBySku() {
        Optional<Product> productOpt = productRepository.findBySku("SKU-BOLT-M10");
        assertThat(productOpt).isPresent();

        Product product = productOpt.get();
        assertThat(product.getName()).contains("Industrial Hex Bolt");
        assertThat(product.getUnitPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(product.getThreshold()).isEqualTo(20);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("Search products by name (case-insensitive substring)")
    void testSearchProductsByName() {
        List<Product> products = productRepository.findByNameContainingIgnoreCase("valve");
        assertThat(products).isNotEmpty();
        assertThat(products.get(0).getSku()).isEqualTo("SKU-VALVE-BALL02");
    }

    @Test
    @DisplayName("Find inventory by product ID and rack ID")
    void testFindInventoryByProductAndRack() {
        UUID productId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        UUID rackId = UUID.fromString("e2000000-0000-0000-0000-000000000001");

        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductIdAndRackId(productId, rackId);
        assertThat(inventoryOpt).isPresent();
        assertThat(inventoryOpt.get().getQuantity()).isEqualTo(150);
    }

    @Test
    @DisplayName("Find active threshold alerts")
    void testFindActiveAlerts() {
        List<Alert> activeAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);
        assertThat(activeAlerts).isNotEmpty();

        Alert alert = activeAlerts.get(0);
        assertThat(alert.getProduct().getSku()).isEqualTo("SKU-FLANGE-04");
        assertThat(alert.getCurrentStock()).isLessThan(alert.getThreshold());
    }

    @Test
    @DisplayName("Find transactions for a product ordered by creation time descending")
    void testFindTransactionsForProductOrdered() {
        UUID productId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        List<StockTransaction> transactions = stockTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);

        assertThat(transactions).isNotEmpty();
        assertThat(transactions.get(0).getReference()).isEqualTo("PO-2026-001");
    }

    @Test
    @DisplayName("Find user by username and verify role")
    void testFindUserByUsername() {
        Optional<User> adminOpt = userRepository.findByUsername("admin_demo");
        assertThat(adminOpt).isPresent();
        assertThat(adminOpt.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(adminOpt.get().getPasswordHash()).startsWith("$2a$");
    }
}
