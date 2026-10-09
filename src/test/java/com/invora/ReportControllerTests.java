package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.entity.Alert;
import com.invora.entity.AlertStatus;
import com.invora.repository.AlertRepository;
import com.invora.repository.InventoryRepository;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private com.invora.service.AlertService alertService;

    private static final UUID SEED_WAREHOUSE_ID = UUID.fromString("e0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_PRODUCT_BOLT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_CATEGORY_RAW_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("GET /api/reports/inventory - Returns paginated inventory with valuation per item")
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    void testInventoryReport_Success() throws Exception {
        mockMvc.perform(get("/api/reports/inventory?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].productSku").isNotEmpty())
                .andExpect(jsonPath("$.content[0].warehouseCode").isNotEmpty())
                .andExpect(jsonPath("$.content[0].rackCode").isNotEmpty())
                .andExpect(jsonPath("$.content[0].quantity").isNumber())
                .andExpect(jsonPath("$.content[0].totalValue").isNumber());
    }

    @Test
    @DisplayName("GET /api/reports/inventory - Filter by warehouse and product")
    @WithMockUser(username = "manager_user", roles = {"WAREHOUSE_MANAGER"})
    void testInventoryReport_FilterWarehouseAndProduct() throws Exception {
        mockMvc.perform(get("/api/reports/inventory?warehouseId=" + SEED_WAREHOUSE_ID + "&productId=" + SEED_PRODUCT_BOLT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-BOLT-M10"))
                .andExpect(jsonPath("$.content[0].warehouseCode").value("WH-MAIN"));
    }

    @Test
    @DisplayName("GET /api/reports/low-stock - Evaluates products below threshold and zero inventory products")
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    void testLowStockReport_EvaluatesThresholdAndZeroStock() throws Exception {
        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(status().isOk())
                // 3 low stock products in seed (Flange, Valve with 0 stock, Gasket with 0 stock)
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));

        // Filter by category
        mockMvc.perform(get("/api/reports/low-stock?categoryId=" + SEED_CATEGORY_RAW_ID))
                .andExpect(status().isOk())
                // Flange and Gasket belong to RAW_MATERIALS category
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/reports/stock-movements - Returns movement history with filters")
    @WithMockUser(username = "viewer_user", roles = {"VIEWER"})
    void testStockMovementsReport_Success() throws Exception {
        mockMvc.perform(get("/api/reports/stock-movements?transactionType=STOCK_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].transactionType").value("STOCK_IN"));
    }

    @Test
    @DisplayName("GET /api/reports/stock-movements - Invalid date range returns HTTP 400")
    @WithMockUser(username = "manager_user", roles = {"WAREHOUSE_MANAGER"})
    void testStockMovementsReport_InvalidDateRange_Returns400() throws Exception {
        OffsetDateTime from = OffsetDateTime.now().plusDays(2);
        OffsetDateTime to = OffsetDateTime.now().minusDays(2);

        mockMvc.perform(get("/api/reports/stock-movements?from=" + from + "&to=" + to))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/reports/valuation - Valuation summary with warehouse and category breakdowns")
    @WithMockUser(username = "manager_user", roles = {"WAREHOUSE_MANAGER"})
    void testValuationReport_GlobalAndBreakdown() throws Exception {
        mockMvc.perform(get("/api/reports/valuation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductsWithInventory").value(6))
                .andExpect(jsonPath("$.totalStockQuantity").value(490))
                .andExpect(jsonPath("$.totalInventoryValuation").value(13647.38))
                .andExpect(jsonPath("$.warehouseBreakdown", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.warehouseBreakdown[0].warehouseCode").value("WH-MAIN"))
                .andExpect(jsonPath("$.categoryBreakdown", hasSize(greaterThanOrEqualTo(1))));

        // Valuation filtered by warehouse
        mockMvc.perform(get("/api/reports/valuation?warehouseId=" + SEED_WAREHOUSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStockQuantity").value(490))
                .andExpect(jsonPath("$.totalInventoryValuation").value(13647.38));
    }

    @Test
    @DisplayName("Role Authorization: ADMIN, WAREHOUSE_MANAGER, STAFF, VIEWER can view read-only reports")
    @WithMockUser(username = "viewer_user", roles = {"VIEWER"})
    void testReports_RoleAuthorization_ViewerAllowed() throws Exception {
        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/stock-movements"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/valuation"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request to reports returns HTTP 401 Unauthorized")
    @WithAnonymousUser
    void testReports_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/reports/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/reports/low-stock"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/reports/valuation"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Part 9 Follow-up: Concurrent low-stock detection does not generate duplicate active alerts")
    void testConcurrentLowStockDetection_PreventsDuplicates() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                alertService.detectLowStockAlerts();
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Verify that duplicate active alerts were NOT generated:
        // There are 3 low stock products in seed (Flange, Valve, Gasket).
        // Each low-stock product must have EXACTLY ONE active alert.
        List<Alert> activeAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);
        assertThat(activeAlerts).hasSize(3);

        // Group by product id to assert uniqueness
        long uniqueProductsWithActiveAlert = activeAlerts.stream()
                .map(a -> a.getProduct().getId())
                .distinct()
                .count();
        assertThat(uniqueProductsWithActiveAlert)
                .as("Each product must have at most 1 active alert even after concurrent execution")
                .isEqualTo(3);
    }
}
