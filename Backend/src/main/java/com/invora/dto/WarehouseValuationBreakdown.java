package com.invora.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WarehouseValuationBreakdown(
    UUID warehouseId,
    String warehouseCode,
    String warehouseName,
    long totalQuantity,
    BigDecimal totalValuation,
    long itemCount
) {
}
