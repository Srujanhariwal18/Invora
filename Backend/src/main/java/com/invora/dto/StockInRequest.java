package com.invora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockInRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    @NotNull(message = "Destination rack ID is required")
    UUID destinationRackId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    int quantity,

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    String reference,

    String reason,

    UUID supplierId,

    @Size(max = 100, message = "Batch must not exceed 100 characters")
    String batch,

    String notes,

    UUID performedById
) {
    public StockInRequest(UUID productId, UUID destinationRackId, int quantity, String reference,
                          String reason, UUID performedById) {
        this(productId, destinationRackId, quantity, reference, reason, null, null, null, performedById);
    }
}
