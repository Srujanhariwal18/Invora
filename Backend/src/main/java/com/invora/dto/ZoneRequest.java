package com.invora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ZoneRequest(
    @NotNull(message = "Warehouse ID is required")
    UUID warehouseId,

    @NotBlank(message = "Zone name is required")
    @Size(max = 100, message = "Zone name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Zone code is required")
    @Size(max = 50, message = "Zone code must not exceed 50 characters")
    String code
) {
}
