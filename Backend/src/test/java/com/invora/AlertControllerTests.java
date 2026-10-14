package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.AcknowledgeAlertRequest;
import com.invora.dto.ResolveAlertRequest;
import com.invora.entity.Alert;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import com.invora.entity.AuditLog;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.Rack;
import com.invora.repository.AlertRepository;
import com.invora.repository.AuditLogRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.RackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private RackRepository rackRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final UUID SEED_ALERT_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");
    private static final UUID SEED_PRODUCT_FLANGE_ID = UUID.fromString("d0000000-0000-0000-0000-000000000003");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("GET /api/alerts - Paginated list returns alerts for ADMIN")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAlerts_Paginated() throws Exception {
        mockMvc.perform(get("/api/alerts?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/alerts - Filter by status returns only matching alerts")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testFilterAlerts_ByStatus() throws Exception {
        // Query ACTIVE alerts
        mockMvc.perform(get("/api/alerts?status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        // Query RESOLVED alerts (none initially in seed)
        mockMvc.perform(get("/api/alerts?status=RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/alerts - Filter by severity and productId")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testFilterAlerts_BySeverityAndProduct() throws Exception {
        mockMvc.perform(get("/api/alerts?severity=WARNING&productId=" + SEED_PRODUCT_FLANGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-FLANGE-04"))
                .andExpect(jsonPath("$.content[0].severity").value("WARNING"));
    }

    @Test
    @DisplayName("GET /api/alerts/{id} - Success returns alert details")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAlertById_Success() throws Exception {
        mockMvc.perform(get("/api/alerts/" + SEED_ALERT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ALERT_ID.toString()))
                .andExpect(jsonPath("$.productSku").value("SKU-FLANGE-04"))
                .andExpect(jsonPath("$.currentStock").value(8))
                .andExpect(jsonPath("$.threshold").value(15))
                .andExpect(jsonPath("$.severity").value("WARNING"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/alerts/{id} - Non-existent ID returns HTTP 404")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAlertById_NotFound() throws Exception {
        mockMvc.perform(get("/api/alerts/" + NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/alerts/check - Idempotent detection creates no duplicates on re-run")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testLowStockDetection_IdempotencyAndNoDuplicates() throws Exception {
        // Initial state has 1 seed alert for SKU-FLANGE-04
        long initialActiveCount = alertRepository.countByStatus(AlertStatus.ACTIVE);
        assertThat(initialActiveCount).isEqualTo(1);

        // Run detection: 3 products are low stock (Flange: stock 8 <= 15, Valve: stock 0 <= 12, Gasket: stock 0 <= 10)
        // Flange already has active alert -> updated/reused, Valve and Gasket have 0 inventory -> 2 new created
        mockMvc.perform(post("/api/alerts/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productsEvaluated").value(8))
                .andExpect(jsonPath("$.alertsCreated").value(2))
                .andExpect(jsonPath("$.totalActiveAlerts").value(3));

        // Re-run detection immediately: MUST BE COMPLETELY IDEMPOTENT, 0 NEW ALERTS CREATED
        mockMvc.perform(post("/api/alerts/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertsCreated").value(0))
                .andExpect(jsonPath("$.totalActiveAlerts").value(3));

        // Total active alerts in repository remains strictly 3 (no duplicate alerts for same product)
        List<Alert> activeAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE);
        assertThat(activeAlerts).hasSize(3);

        // Verify that zero-inventory products received OUT_OF_STOCK severity
        boolean hasOutOfStock = activeAlerts.stream()
                .anyMatch(a -> a.getSeverity() == AlertSeverity.OUT_OF_STOCK && a.getCurrentStock() == 0);
        assertThat(hasOutOfStock).isTrue();
    }

    @Test
    @DisplayName("POST /api/alerts/check - Automatically resolves alert when product is replenished")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testLowStockDetection_SelfHealingReplenishment() throws Exception {
        // Product SKU-FLANGE-04 currently has 8 stock with threshold 15 (ACTIVE alert exists)
        Product flange = productRepository.findById(SEED_PRODUCT_FLANGE_ID).orElseThrow();
        Rack rack1 = rackRepository.findById(SEED_RACK_A1_ID).orElseThrow();

        // Replenish stock by adding 50 units (8 + 50 = 58 > threshold 15)
        Inventory inventory = inventoryRepository.findByProductIdAndRackId(SEED_PRODUCT_FLANGE_ID, SEED_RACK_A1_ID).orElseThrow();
        inventory.setQuantity(58);
        inventoryRepository.save(inventory);

        // Run low stock detection
        mockMvc.perform(post("/api/alerts/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertsResolved", greaterThanOrEqualTo(1)));

        // Verify that the Flange alert is now RESOLVED
        Alert alert = alertRepository.findById(SEED_ALERT_ID).orElseThrow();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedAt()).isNotNull();
        assertThat(alert.getCurrentStock()).isEqualTo(58);
    }

    @Test
    @DisplayName("PUT /api/alerts/{id}/acknowledge - ADMIN and WAREHOUSE_MANAGER acknowledge alert and audit log is recorded")
    @WithMockUser(username = "admin_demo", roles = {"ADMIN"})
    void testAcknowledgeAlert_AdminSuccess() throws Exception {
        AcknowledgeAlertRequest request = new AcknowledgeAlertRequest("Purchase order placed with supplier");

        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/acknowledge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ALERT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Purchase order placed with supplier")));

        // Verify Audit Log entry created
        List<AuditLog> auditLogs = auditLogRepository.findByAction("ALERT_ACKNOWLEDGED");
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getEntityId()).isEqualTo(SEED_ALERT_ID.toString());
    }

    @Test
    @DisplayName("PUT /api/alerts/{id}/acknowledge - WAREHOUSE_MANAGER success")
    @WithMockUser(username = "wm_demo", roles = {"WAREHOUSE_MANAGER"})
    void testAcknowledgeAlert_WarehouseManagerSuccess() throws Exception {
        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ALERT_ID.toString()));
    }

    @Test
    @DisplayName("PUT /api/alerts/{id}/acknowledge - STAFF and VIEWER forbidden (HTTP 403)")
    @WithMockUser(username = "staff_demo", roles = {"STAFF"})
    void testAcknowledgeAlert_StaffForbidden() throws Exception {
        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/acknowledge"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PUT /api/alerts/{id}/resolve - ADMIN successfully resolves alert with timestamp")
    @WithMockUser(username = "admin_demo", roles = {"ADMIN"})
    void testResolveAlert_AdminSuccess() throws Exception {
        ResolveAlertRequest request = new ResolveAlertRequest("Shipment received and inspected");

        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_ALERT_ID.toString()))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Shipment received and inspected")));

        // Verify Alert state in database
        Alert resolvedAlert = alertRepository.findById(SEED_ALERT_ID).orElseThrow();
        assertThat(resolvedAlert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(resolvedAlert.getResolvedAt()).isNotNull();

        // Verify Audit Log entry created
        List<AuditLog> auditLogs = auditLogRepository.findByAction("ALERT_RESOLVED");
        assertThat(auditLogs).isNotEmpty();

        // Attempting to resolve again returns HTTP 409 Conflict
        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/resolve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Attempting to acknowledge a resolved alert returns HTTP 409 Conflict
        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/acknowledge"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("PUT /api/alerts/{id}/resolve - STAFF and VIEWER forbidden (HTTP 403)")
    @WithMockUser(username = "viewer_demo", roles = {"VIEWER"})
    void testResolveAlert_ViewerForbidden() throws Exception {
        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/resolve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Unauthenticated alert requests return HTTP 401 Unauthorized")
    @WithAnonymousUser
    void testAlerts_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/alerts/" + SEED_ALERT_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/acknowledge"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(put("/api/alerts/" + SEED_ALERT_ID + "/resolve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/alerts/check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
