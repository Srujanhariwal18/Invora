package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.CategoryRequest;
import com.invora.dto.StockInRequest;
import com.invora.entity.Role;
import com.invora.entity.StockTransaction;
import com.invora.entity.User;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.UserRepository;
import com.invora.security.JwtProperties;
import com.invora.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.junit.jupiter.api.Disabled("Authentication has been removed from backend")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    @Autowired
    private com.invora.service.StockTransactionService stockTransactionService;

    private static final UUID SEED_PRODUCT_ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private static final UUID SEED_RACK_A1_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID SEED_ADMIN_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private String createTokenForUser(String username, String email, Role role) {
        User user = userRepository.save(new User(
                username,
                email,
                passwordEncoder.encode("TestPass123!"),
                role
        ));
        return jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
    }

    @Test
    @DisplayName("Unauthenticated access to protected APIs returns HTTP 401 Unauthorized")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/stock/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Public endpoints (Health and Auth Register/Login) allow unauthenticated access")
    void testPublicEndpoints_AllowAnonymous() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Malformed token returns HTTP 401 Unauthorized")
    void testMalformedToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Expired token returns HTTP 401 Unauthorized")
    void testExpiredToken_Returns401() throws Exception {
        // Construct expired token
        Date pastDate = new Date(System.currentTimeMillis() - 100000);
        Date earlierDate = new Date(pastDate.getTime() - 50000);
        String expiredToken = Jwts.builder()
                .subject("expired_user")
                .claim("userId", UUID.randomUUID().toString())
                .claim("role", "VIEWER")
                .issuedAt(earlierDate)
                .expiration(pastDate)
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Role Authorization: VIEWER can read catalog & inventory but cannot mutate resources or stock")
    void testRole_Viewer_Permissions() throws Exception {
        String viewerToken = createTokenForUser("viewer_tester", "viewer_t@invora.local", Role.VIEWER);

        // Allowed: GET /api/categories
        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        // Allowed: GET /api/inventory
        mockMvc.perform(get("/api/inventory")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        // Forbidden: POST /api/categories
        CategoryRequest catReq = new CategoryRequest("Unauthorized Category", "Desc");
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Forbidden: POST /api/stock/transactions/in
        StockInRequest stockReq = new StockInRequest(SEED_PRODUCT_ID, SEED_RACK_A1_ID, 10, "REF", "Reason", null);
        mockMvc.perform(post("/api/stock/transactions/in")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Role Authorization: STAFF can perform stock operations but cannot mutate catalog resources")
    void testRole_Staff_Permissions() throws Exception {
        String staffToken = createTokenForUser("staff_tester", "staff_t@invora.local", Role.STAFF);

        // Allowed: POST /api/stock/transactions/in
        StockInRequest stockReq = new StockInRequest(SEED_PRODUCT_ID, SEED_RACK_A1_ID, 10, "PO-STAFF", "Restock", null);
        mockMvc.perform(post("/api/stock/transactions/in")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("STOCK_IN"));

        // Forbidden: POST /api/categories
        CategoryRequest catReq = new CategoryRequest("Staff Attempted Category", "Desc");
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Role Authorization: WAREHOUSE_MANAGER can mutate catalog and perform stock operations")
    void testRole_WarehouseManager_Permissions() throws Exception {
        String managerToken = createTokenForUser("manager_tester", "manager_t@invora.local", Role.WAREHOUSE_MANAGER);

        // Allowed: POST /api/categories
        CategoryRequest catReq = new CategoryRequest("Manager Category", "Created by manager");
        mockMvc.perform(post("/api/categories")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Manager Category"));

        // Allowed: POST /api/stock/transactions/in
        StockInRequest stockReq = new StockInRequest(SEED_PRODUCT_ID, SEED_RACK_A1_ID, 15, "PO-MGR", "Manager Stock In", null);
        mockMvc.perform(post("/api/stock/transactions/in")
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stockReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(15));
    }

    @Test
    @DisplayName("Stock Attribution: Transaction records authenticated user UUID and ignores client spoofed performedById")
    void testStockAttribution_UsesSecurityContext_IgnoresRequestBody() throws Exception {
        User staffActor = userRepository.save(new User(
                "actual_worker",
                "worker@invora.local",
                passwordEncoder.encode("WorkerPass123!"),
                Role.STAFF
        ));
        String workerToken = jwtTokenProvider.generateToken(
                staffActor.getId(), staffActor.getUsername(), staffActor.getRole().name());

        // Client attempts to spoof performedById with SEED_ADMIN_ID
        StockInRequest spoofedRequest = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                20,
                "PO-ATTRIBUTION",
                "Attribution verification",
                SEED_ADMIN_ID // Client attempts spoofing
        );

        MvcResult result = mockMvc.perform(post("/api/stock/transactions/in")
                .header("Authorization", "Bearer " + workerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(spoofedRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID txId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        // Fetch saved transaction from DB
        StockTransaction savedTx = stockTransactionRepository.findById(txId).orElseThrow();

        // Must be attributed to actual_worker (staffActor), NOT seed admin!
        assertThat(savedTx.getPerformedBy().getId())
                .as("Transaction performed_by must strictly match authenticated security context user")
                .isEqualTo(staffActor.getId())
                .isNotEqualTo(SEED_ADMIN_ID);
        assertThat(savedTx.getPerformedBy().getUsername()).isEqualTo("actual_worker");
    }

    @Test
    @DisplayName("Tampered token signature returns HTTP 401 Unauthorized")
    void testTamperedToken_Returns401() throws Exception {
        String validToken = createTokenForUser("tamper_test", "tamper@invora.local", Role.VIEWER);
        // Alter the signature (first character of signature part to ensure modifying signature data bits)
        int sigStart = validToken.lastIndexOf('.') + 1;
        char sigChar = validToken.charAt(sigStart);
        char replacementChar = (sigChar == 'a') ? 'b' : 'a';
        String tamperedToken = validToken.substring(0, sigStart) + replacementChar + validToken.substring(sigStart + 1);

        mockMvc.perform(get("/api/categories")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Unauthenticated stock operation invocation fails safely with AccessDeniedException without selecting seed admin")
    void testUnauthenticatedStockOperation_FailsSafely_ThrowsAccessDenied() {
        // Clear security context to ensure completely unauthenticated execution
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        StockInRequest request = new StockInRequest(
                SEED_PRODUCT_ID,
                SEED_RACK_A1_ID,
                10,
                "PO-UNAUTH",
                "Unauthenticated attempt",
                null
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> stockTransactionService.processStockIn(request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }
}
