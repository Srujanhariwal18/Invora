package com.invora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies that the Spring application context loads successfully
 * using the test profile (H2 in-memory, Flyway disabled).
 */
@SpringBootTest
@ActiveProfiles("test")
class InvoraApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the application context started without errors.
    }
}
