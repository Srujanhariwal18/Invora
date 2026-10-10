package com.invora.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    String sku,

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    String name,

    @NotNull(message = "Category ID is required")
    UUID categoryId,

    UUID supplierId,

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    String unit,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be non-negative")
    BigDecimal unitPrice,

    @Min(value = 0, message = "Threshold must be non-negative")
    int threshold,

    @Min(value = 0, message = "Reorder quantity must be non-negative")
    int reorderQuantity,

    String description
) {
}
