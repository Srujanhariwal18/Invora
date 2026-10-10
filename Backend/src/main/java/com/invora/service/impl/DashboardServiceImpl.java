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

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public DashboardServiceImpl(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                SupplierRepository supplierRepository,
                                WarehouseRepository warehouseRepository,
                                InventoryRepository inventoryRepository,
                                StockTransactionRepository stockTransactionRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockTransactionRepository = stockTransactionRepository;
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
                recentTransactions
        );
    }
}
