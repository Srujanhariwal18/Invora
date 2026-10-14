package com.invora.service.impl;

import com.invora.dto.DashboardSummaryResponse;
import com.invora.dto.StockTransactionResponse;
import com.invora.repository.CategoryRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.SupplierRepository;
import com.invora.repository.WarehouseRepository;
import com.invora.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import com.invora.entity.TransactionType;
import com.invora.repository.AlertRepository;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final AlertRepository alertRepository;

    public DashboardServiceImpl(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                SupplierRepository supplierRepository,
                                WarehouseRepository warehouseRepository,
                                InventoryRepository inventoryRepository,
                                StockTransactionRepository stockTransactionRepository,
                                AlertRepository alertRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockTransactionRepository = stockTransactionRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        long totalActiveProducts = productRepository.countByActiveTrue();
        long totalCategories = categoryRepository.count();
        long totalActiveSuppliers = supplierRepository.count();
        long totalWarehouses = warehouseRepository.count();
        long totalStockQuantity = inventoryRepository.sumTotalQuantity();

        BigDecimal rawValuation = inventoryRepository.calculateTotalInventoryValuation();
        BigDecimal totalInventoryValuation = (rawValuation != null)
                ? rawValuation.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long lowStockProductsCount = productRepository.countLowStockProducts();
        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        long stockInToday = stockTransactionRepository.countByTransactionTypeAndCreatedAtAfter(TransactionType.STOCK_IN, startOfDay);
        long stockOutToday = stockTransactionRepository.countByTransactionTypeAndCreatedAtAfter(TransactionType.STOCK_OUT, startOfDay);
        long criticalAlertsCount = alertRepository.countBySeverityAndStatus(AlertSeverity.CRITICAL, AlertStatus.ACTIVE)
                + alertRepository.countBySeverityAndStatus(AlertSeverity.CRITICAL, AlertStatus.ACKNOWLEDGED);
        long totalCapacity = warehouseRepository.findAll().stream().mapToLong(w -> Math.max(0, w.getCapacity())).sum();
        BigDecimal warehouseUtilization = totalCapacity == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalStockQuantity).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCapacity), 2, RoundingMode.HALF_UP);
        List<String> topMovingProducts = stockTransactionRepository.findTopMovingProducts().stream()
                .limit(5).map(row -> String.valueOf(row[0])).toList();

        List<StockTransactionResponse> recentTransactions = stockTransactionRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(StockTransactionResponse::fromEntity)
                .toList();

        return new DashboardSummaryResponse(
                totalActiveProducts,
                totalCategories,
                totalActiveSuppliers,
                totalWarehouses,
                totalStockQuantity,
                totalInventoryValuation,
                lowStockProductsCount,
                stockInToday,
                stockOutToday,
                criticalAlertsCount,
                warehouseUtilization,
                topMovingProducts,
                recentTransactions
        );
    }
}
