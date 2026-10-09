package com.invora.dto;

import com.invora.entity.AlertSeverity;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockReportItem(
    UUID productId,
    String productSku,
    String productName,
    String categoryName,
    String unit,
    BigDecimal unitPrice,
    int threshold,
    int currentStock,
    int shortage,
    int reorderQuantity,
    AlertSeverity severity,
    boolean active
) {
}
