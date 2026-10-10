package com.invora.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "invora.jwt")
public class JwtProperties {

    public static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private String secret;
    private long expirationMs = 86400000L; // default 24h

    private final Environment environment;

    public JwtProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProperties() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT signing secret (JWT_SECRET) must be configured and cannot be blank");
        }
        if (environment != null && environment.matchesProfiles("prod") && DEFAULT_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException("Production environment cannot silently use the default development JWT secret");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
