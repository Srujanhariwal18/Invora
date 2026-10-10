package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.StockInRequest;
import com.invora.entity.AuditLog;
import com.invora.repository.AuditLogRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final UUID SEED_AUDIT_LOG_ID = UUID.fromString("f3000000-0000-0000-0000-000000000001");
    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID NON_EXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    @DisplayName("GET /api/audit-logs - ADMIN retrieves paginated audit logs")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAuditLogs_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/audit-logs?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("GET /api/audit-logs - Filter by action, entity, and actor")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testFilterAuditLogs_ByActionAndEntity() throws Exception {
        mockMvc.perform(get("/api/audit-logs?action=USER_CREATED&entity=User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].action").value("USER_CREATED"))
                .andExpect(jsonPath("$.content[0].entityType").value("User"));
    }

    @Test
    @DisplayName("GET /api/audit-logs - Filter by date range")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testFilterAuditLogs_DateRange() throws Exception {
        OffsetDateTime from = OffsetDateTime.now().minusDays(30);
        OffsetDateTime to = OffsetDateTime.now().plusDays(1);

        mockMvc.perform(get("/api/audit-logs?from=" + from + "&to=" + to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/audit-logs - Invalid date range (from > to) returns HTTP 400 Bad Request")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testFilterAuditLogs_InvalidDateRange_Returns400() throws Exception {
        OffsetDateTime from = OffsetDateTime.now().plusDays(5);
        OffsetDateTime to = OffsetDateTime.now().minusDays(5);

        mockMvc.perform(get("/api/audit-logs?from=" + from + "&to=" + to))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} - ADMIN retrieves single audit log")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAuditLogById_Success() throws Exception {
        mockMvc.perform(get("/api/audit-logs/" + SEED_AUDIT_LOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SEED_AUDIT_LOG_ID.toString()))
                .andExpect(jsonPath("$.action").value("USER_CREATED"))
                .andExpect(jsonPath("$.entityType").value("User"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/audit-logs/{id} - Non-existent ID returns HTTP 404")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testGetAuditLogById_NotFound() throws Exception {
        mockMvc.perform(get("/api/audit-logs/" + NON_EXISTENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("SENSITIVE FIELD EXCLUSION: Audit logs never expose password hashes, passwords, or secrets")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testAuditLog_SensitiveFieldExclusion() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/audit-logs/" + SEED_AUDIT_LOG_ID))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("secret");
        assertThat(responseBody).doesNotContain("token");
    }

    @Test
    @DisplayName("Audit logging of stock movement events: processStockIn automatically records STOCK_IN audit log")
    @WithMockUser(username = "admin_demo", roles = {"ADMIN"})
    void testStockIn_AutomaticallyRecordsAuditLog() throws Exception {
        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                25,
                "PO-AUDIT-TEST",
                "Verifying audit log trigger",
                null
        );

        mockMvc.perform(post("/api/stock/transactions/in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify that an audit log entry for STOCK_IN was created
        mockMvc.perform(get("/api/audit-logs?action=STOCK_IN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].action").value("STOCK_IN"))
                .andExpect(jsonPath("$.content[0].entityType").value("StockTransaction"));
    }

    @Test
    @DisplayName("Role Authorization: Non-admin roles (WAREHOUSE_MANAGER, STAFF, VIEWER) receive HTTP 403 Forbidden")
    @WithMockUser(username = "manager_user", roles = {"WAREHOUSE_MANAGER"})
    void testAuditLogs_WarehouseManager_Forbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Role Authorization: STAFF receives HTTP 403 Forbidden")
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    void testAuditLogs_Staff_Forbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Role Authorization: VIEWER receives HTTP 403 Forbidden")
    @WithMockUser(username = "viewer_user", roles = {"VIEWER"})
    void testAuditLogs_Viewer_Forbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Unauthenticated request to audit logs returns HTTP 401 Unauthorized")
    @WithAnonymousUser
    void testAuditLogs_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/audit-logs/" + SEED_AUDIT_LOG_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Public mutation prevention: Audit logs cannot be created, updated, or deleted via REST API")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void testAuditLogs_Immutable_PublicMutationPrevented() throws Exception {
        mockMvc.perform(post("/api/audit-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/audit-logs/" + SEED_AUDIT_LOG_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/audit-logs/" + SEED_AUDIT_LOG_ID))
                .andExpect(status().isMethodNotAllowed());
    }
}
