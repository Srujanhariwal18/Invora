package com.invora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100, message = "Warehouse name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 50, message = "Warehouse code must not exceed 50 characters")
    String code,

    String address,

    @Min(value = 0, message = "Capacity must be non-negative")
    int capacity
) {
}
