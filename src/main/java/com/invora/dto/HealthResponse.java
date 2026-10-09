package com.invora.dto;

import java.time.Instant;

public record HealthResponse(
    String status,
    String application,
    String version,
    Instant timestamp
) {}
