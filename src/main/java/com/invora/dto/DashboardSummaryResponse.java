package com.invora.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    long totalActiveProducts,
    long totalCategories,
    long totalActiveSuppliers,
    long totalWarehouses,
    long totalStockQuantity,
    BigDecimal totalInventoryValuation,
    long lowStockProductsCount,
    List<StockTransactionResponse> recentTransactions
) {
}
