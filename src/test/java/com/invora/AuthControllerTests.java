package com.invora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invora.dto.LoginRequest;
import com.invora.dto.RegisterRequest;
import com.invora.entity.Role;
import com.invora.entity.User;
import com.invora.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/auth/register - Successfully registers user with BCrypt hash and default VIEWER role")
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "new_operator",
                "operator@invora.local",
                "StrongPassword123!"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("new_operator"))
                .andExpect(jsonPath("$.email").value("operator@invora.local"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andReturn();

        // Verify database state and BCrypt hashing
        User savedUser = userRepository.findByUsername("new_operator").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.VIEWER);
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("StrongPassword123!");
        assertThat(passwordEncoder.matches("StrongPassword123!", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate username returns HTTP 409 Conflict")
    void testRegister_DuplicateUsername_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "admin_demo", // already exists in seed data
                "unique_email@invora.local",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate email returns HTTP 409 Conflict")
    void testRegister_DuplicateEmail_Returns409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "brand_new_user",
                "admin@invora.local", // already exists in seed data
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid email and short password return HTTP 400 Bad Request")
    void testRegister_ValidationErrors_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "ab", // too short (min 3)
                "not-an-email",
                "123" // too short (min 6)
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - Authenticates valid credentials and returns JWT")
    void testLogin_Success() throws Exception {
        // Register a user first to have guaranteed credentials
        userRepository.save(new User(
                "login_test_user",
                "login_test@invora.local",
                passwordEncoder.encode("SecretPass123!"),
                Role.STAFF
        ));

        LoginRequest request = new LoginRequest("login_test_user", "SecretPass123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username").value("login_test_user"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid password returns HTTP 401 Unauthorized")
    void testLogin_InvalidPassword_Returns401() throws Exception {
        userRepository.save(new User(
                "pass_test_user",
                "pass_test@invora.local",
                passwordEncoder.encode("RealPassword123!"),
                Role.STAFF
        ));

        LoginRequest request = new LoginRequest("pass_test_user", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Non-existent user returns HTTP 401 Unauthorized")
    void testLogin_NonExistentUser_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("ghost_user", "SomePassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/auth/login - Inactive user is rejected with HTTP 401 Unauthorized")
    void testLogin_InactiveUser_Returns401() throws Exception {
        User inactiveUser = new User(
                "inactive_user",
                "inactive@invora.local",
                passwordEncoder.encode("ActivePass123!"),
                Role.STAFF
        );
        inactiveUser.setActive(false);
        userRepository.save(inactiveUser);

        LoginRequest request = new LoginRequest("inactive_user", "ActivePass123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Returns current authenticated user profile")
    void testGetMe_Authenticated_ReturnsProfile() throws Exception {
        // Register user and obtain token
        RegisterRequest registerReq = new RegisterRequest("profile_user", "profile@invora.local", "MyPass123!");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profile_user"))
                .andExpect(jsonPath("$.email").value("profile@invora.local"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/register - Client-supplied role field is ignored and always defaults to VIEWER")
    void testRegister_IgnoresSuppliedRole_AlwaysAssignsViewer() throws Exception {
        String payloadWithAdmin = """
                {
                    "username": "role_hacker",
                    "email": "hacker@invora.local",
                    "password": "Password123!",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadWithAdmin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        User savedUser = userRepository.findByUsername("role_hacker").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.VIEWER);
    }

    @Test
    @DisplayName("JWT Authentication - Deactivated user with previously valid JWT token is rejected with HTTP 401")
    void testJwtAuthentication_DeactivatedUser_Rejected() throws Exception {
        // Register active user and obtain token
        RegisterRequest registerReq = new RegisterRequest("temp_active_user", "temp@invora.local", "MyPass123!");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Deactivate user in database
        User user = userRepository.findByUsername("temp_active_user").orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        // Subsequent request using valid token must be rejected with 401
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/auth/me - Unauthenticated request returns HTTP 401 Unauthorized")
    void testGetMe_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
