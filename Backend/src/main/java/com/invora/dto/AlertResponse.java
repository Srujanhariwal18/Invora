package com.invora.dto;

import com.invora.entity.Alert;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
    UUID id,
    UUID productId,
    String productSku,
    String productName,
    int currentStock,
    int threshold,
    AlertSeverity severity,
    AlertStatus status,
    String message,
    OffsetDateTime createdAt,
    OffsetDateTime resolvedAt
) {
    public static AlertResponse fromEntity(Alert alert) {
        if (alert == null) {
            return null;
        }

        UUID productId = alert.getProduct() != null ? alert.getProduct().getId() : null;
        String productSku = alert.getProduct() != null ? alert.getProduct().getSku() : null;
        String productName = alert.getProduct() != null ? alert.getProduct().getName() : null;

        return new AlertResponse(
            alert.getId(),
            productId,
            productSku,
            productName,
            alert.getCurrentStock(),
            alert.getThreshold(),
            alert.getSeverity(),
            alert.getStatus(),
            alert.getMessage(),
            alert.getCreatedAt(),
            alert.getResolvedAt()
        );
    }
}
