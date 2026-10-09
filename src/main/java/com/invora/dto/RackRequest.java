package com.invora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RackRequest(
    @NotNull(message = "Zone ID is required")
    UUID zoneId,

    @NotBlank(message = "Rack name is required")
    @Size(max = 100, message = "Rack name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Rack code is required")
    @Size(max = 50, message = "Rack code must not exceed 50 characters")
    String code,

    @Min(value = 0, message = "Capacity must be non-negative")
    int capacity
) {
}
