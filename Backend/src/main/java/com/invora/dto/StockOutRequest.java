package com.invora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockOutRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Source rack ID is required")
    UUID sourceRackId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    int quantity,

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    String reference,

    String reason,

    @Size(max = 200, message = "Destination must not exceed 200 characters")
    String destination,

    UUID performedById
) {
    public StockOutRequest(UUID productId, UUID sourceRackId, int quantity, String reference,
                           String reason, UUID performedById) {
        this(productId, sourceRackId, quantity, reference, reason, null, performedById);
    }
}
