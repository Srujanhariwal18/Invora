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
    long stockInToday,
    long stockOutToday,
    long criticalAlertsCount,
    BigDecimal warehouseUtilization,
    List<String> topMovingProducts,
    List<StockTransactionResponse> recentTransactions
) {
    public DashboardSummaryResponse(long totalActiveProducts, long totalCategories, long totalActiveSuppliers,
                                    long totalWarehouses, long totalStockQuantity, BigDecimal totalInventoryValuation,
                                    long lowStockProductsCount, List<StockTransactionResponse> recentTransactions) {
        this(totalActiveProducts, totalCategories, totalActiveSuppliers, totalWarehouses, totalStockQuantity,
                totalInventoryValuation, lowStockProductsCount, 0, 0, 0, BigDecimal.ZERO, List.of(),
                recentTransactions);
    }
}
