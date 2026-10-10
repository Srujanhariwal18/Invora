package com.invora;

import com.invora.security.JwtProperties;
import com.invora.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigurationStartupTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withUserConfiguration(JwtProperties.class, JwtTokenProvider.class);

    @Test
    @DisplayName("Production Profile: Application fails to start when JWT_SECRET is missing or blank")
    void testFailsToStart_WhenProdProfileAndSecretIsMissingOrBlank() {
        // Direct property validation
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        JwtProperties blankProps = new JwtProperties(prodEnv);
        blankProps.setSecret("   ");

        assertThatThrownBy(blankProps::validateProperties)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured and cannot be blank");

        JwtProperties nullProps = new JwtProperties(prodEnv);
        nullProps.setSecret(null);

        assertThatThrownBy(nullProps::validateProperties)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured and cannot be blank");

        // Context startup failure verification
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "invora.jwt.secret=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing secret (JWT_SECRET) must be configured and cannot be blank");
                });
    }

    @Test
    @DisplayName("Production Profile: Fails to start when known development secret is used")
    void testFailsToStart_WhenProdProfileAndDevSecretUsed() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        JwtProperties devPropsInProd = new JwtProperties(prodEnv);
        devPropsInProd.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

        assertThatThrownBy(devPropsInProd::validateProperties)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production environment cannot silently use the default development JWT secret");

        // Context startup failure verification
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "invora.jwt.secret=" + JwtProperties.DEFAULT_DEV_SECRET)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("Production environment cannot silently use the default development JWT secret");
                });
    }

    @Test
    @DisplayName("Security: Explicitly configured but invalid (blank/empty) JWT secret is rejected safely")
    void testFailsToStart_WhenSecretIsInvalidOrBlank() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        JwtProperties invalidProps = new JwtProperties(devEnv);
        invalidProps.setSecret("");

        assertThatThrownBy(invalidProps::validateProperties)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured and cannot be blank");
    }

    @Test
    @DisplayName("Development Profile: Successfully boots and operates with documented development configuration")
    void testLocalDevelopmentWorks_WithDefaultDevSecret() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        JwtProperties devProps = new JwtProperties(devEnv);
        devProps.setSecret(JwtProperties.DEFAULT_DEV_SECRET);
        devProps.setExpirationMs(86400000L);

        // Verification: No exception thrown on validation
        devProps.validateProperties();

        // Verification: JwtTokenProvider successfully generates, parses, and validates tokens
        JwtTokenProvider provider = new JwtTokenProvider(devProps);
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "dev_admin", "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("dev_admin");

        // Context startup success verification in development mode
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "invora.jwt.secret=" + JwtProperties.DEFAULT_DEV_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtProperties.class);
                    assertThat(context).hasSingleBean(JwtTokenProvider.class);
                });
    }

    @Test
    @DisplayName("Production Profile: Successfully boots when a valid custom 256-bit secret is provided")
    void testProductionStarts_WithValidCustomSecret() {
        String validProdSecret = "StrongProductionSecretKeyThatHasAtLeast256BitsLengthForHmacSha256Security1234567890";

        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        JwtProperties prodProps = new JwtProperties(prodEnv);
        prodProps.setSecret(validProdSecret);

        prodProps.validateProperties();

        JwtTokenProvider provider = new JwtTokenProvider(prodProps);
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "prod_admin", "ADMIN");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("prod_admin");

        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "invora.jwt.secret=" + validProdSecret)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtProperties.class);
                    assertThat(context).hasSingleBean(JwtTokenProvider.class);
                });
    }
}
