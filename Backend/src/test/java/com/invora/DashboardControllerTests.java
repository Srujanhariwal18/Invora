package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.entity.Category;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.Rack;
import com.invora.repository.CategoryRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.RackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RackRepository rackRepository;

    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A2_ID = UUID.fromString("e2000000-0000-0000-0000-000000000002");
    private static final UUID SEED_CATEGORY_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("GET /api/dashboard/summary - ADMIN gets complete summary statistics")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testDashboardSummary_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveProducts").value(8))
                .andExpect(jsonPath("$.totalCategories").value(3))
                .andExpect(jsonPath("$.totalActiveSuppliers").value(2))
                .andExpect(jsonPath("$.totalWarehouses").value(1))
                .andExpect(jsonPath("$.totalStockQuantity").value(490))
                .andExpect(jsonPath("$.totalInventoryValuation").value(13647.38))
                .andExpect(jsonPath("$.lowStockProductsCount").value(3))
                .andExpect(jsonPath("$.recentTransactions", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - WAREHOUSE_MANAGER has read access")
    @WithMockUser(username = "wm_user", roles = {"WAREHOUSE_MANAGER"})
    void testDashboardSummary_WarehouseManagerSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveProducts").isNumber());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - STAFF has read access")
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    void testDashboardSummary_StaffSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveProducts").isNumber());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - VIEWER has read-only access")
    @WithMockUser(username = "viewer_user", roles = {"VIEWER"})
    void testDashboardSummary_ViewerSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveProducts").isNumber());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary - Unauthenticated request returns HTTP 401")
    @WithAnonymousUser
    void testDashboardSummary_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Dashboard correctly aggregates product stock across multiple warehouse racks")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testStockAggregationAcrossMultipleRacks() throws Exception {
        Category category = categoryRepository.findById(SEED_CATEGORY_ID).orElseThrow();
        Rack rack1 = rackRepository.findById(SEED_RACK_A1_ID).orElseThrow();
        Rack rack2 = rackRepository.findById(SEED_RACK_A2_ID).orElseThrow();

        // Create new product with threshold 50 and price 10.00
        Product multiRackProduct = new Product(
                "SKU-MULTI-RACK-01",
                "Multi Rack Test Product",
                category,
                null,
                "PIECE",
                new BigDecimal("10.00"),
                50,
                100,
                "Testing multi rack aggregation"
        );
        multiRackProduct = productRepository.save(multiRackProduct);

        // Rack 1 has 20 units, Rack 2 has 25 units -> Total 45 units (<= threshold 50)
        inventoryRepository.save(new Inventory(multiRackProduct, rack1, 20));
        inventoryRepository.save(new Inventory(multiRackProduct, rack2, 25));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                // Original stock was 490 + 45 = 535
                .andExpect(jsonPath("$.totalStockQuantity").value(535))
                // Original valuation 13647.38 + 450.00 = 14097.38
                .andExpect(jsonPath("$.totalInventoryValuation").value(14097.38))
                // Low stock products count: 3 original + 1 new (45 <= 50) = 4
                .andExpect(jsonPath("$.lowStockProductsCount").value(4));
    }

    @Test
    @DisplayName("Dashboard evaluates low-stock threshold equality and above/below boundaries")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testLowStockThreshold_EqualityAndBoundary() throws Exception {
        Category category = categoryRepository.findById(SEED_CATEGORY_ID).orElseThrow();
        Rack rack1 = rackRepository.findById(SEED_RACK_A1_ID).orElseThrow();

        // Product 1: Stock EXACTLY equals threshold (30 == 30) -> should be counted as low stock
        Product equalProduct = new Product(
                "SKU-THRESHOLD-EQUAL",
                "Threshold Equal Product",
                category,
                null,
                "PIECE",
                new BigDecimal("5.00"),
                30,
                50,
                "Equal boundary"
        );
        equalProduct = productRepository.save(equalProduct);
        inventoryRepository.save(new Inventory(equalProduct, rack1, 30));

        // Product 2: Stock is strictly greater than threshold (31 > 30) -> should NOT be counted as low stock
        Product aboveProduct = new Product(
                "SKU-THRESHOLD-ABOVE",
                "Threshold Above Product",
                category,
                null,
                "PIECE",
                new BigDecimal("5.00"),
                30,
                50,
                "Above boundary"
        );
        aboveProduct = productRepository.save(aboveProduct);
        inventoryRepository.save(new Inventory(aboveProduct, rack1, 31));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                // Low stock products: 3 original + 1 equal = 4 (aboveProduct is not low stock)
                .andExpect(jsonPath("$.lowStockProductsCount").value(4));
    }

    @Test
    @DisplayName("Products with no inventory records are safely treated as having zero stock")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testProductsWithNoInventory_TreatedAsZeroStock() throws Exception {
        Category category = categoryRepository.findById(SEED_CATEGORY_ID).orElseThrow();

        // Product with threshold 10 and NO inventory records created
        Product zeroStockProduct = new Product(
                "SKU-ZERO-INVENTORY",
                "Zero Stock Product",
                category,
                null,
                "PIECE",
                new BigDecimal("20.00"),
                10,
                50,
                "No inventory records"
        );
        productRepository.save(zeroStockProduct);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                // 3 original low-stock + 1 zero inventory product = 4
                .andExpect(jsonPath("$.lowStockProductsCount").value(4));
    }

    @Test
    @DisplayName("Empty inventory table boundary returns 0 total stock and 0.00 valuation safely")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testEmptyInventory_BoundaryReturnsZeros() throws Exception {
        inventoryRepository.deleteAll();

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStockQuantity").value(0))
                .andExpect(jsonPath("$.totalInventoryValuation").value(0.00))
                // All 8 active products now have 0 stock <= threshold, so all 8 are low stock
                .andExpect(jsonPath("$.lowStockProductsCount").value(8));
    }
}
