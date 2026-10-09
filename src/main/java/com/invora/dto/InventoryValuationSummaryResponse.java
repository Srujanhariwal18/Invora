package com.invora.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationSummaryResponse(
    long totalProductsWithInventory,
    long totalStockQuantity,
    BigDecimal totalInventoryValuation,
    List<WarehouseValuationBreakdown> warehouseBreakdown,
    List<CategoryValuationBreakdown> categoryBreakdown
) {
}
