package com.invora.service.impl;

import com.invora.dto.CategoryValuationBreakdown;
import com.invora.dto.InventoryReportItem;
import com.invora.dto.InventoryValuationSummaryResponse;
import com.invora.dto.LowStockReportItem;
import com.invora.dto.StockTransactionResponse;
import com.invora.dto.WarehouseValuationBreakdown;
import com.invora.entity.AlertSeverity;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.StockTransaction;
import com.invora.entity.TransactionType;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public ReportServiceImpl(InventoryRepository inventoryRepository,
                             ProductRepository productRepository,
                             StockTransactionRepository stockTransactionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Override
    public Page<InventoryReportItem> getInventoryReport(UUID warehouseId, UUID zoneId, UUID rackId,
                                                        UUID productId, UUID categoryId, Pageable pageable) {
        Specification<Inventory> spec = (root, query, cb) -> cb.conjunction();

        if (warehouseId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("rack").get("zone").get("warehouse").get("id"), warehouseId));
        }

        if (zoneId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("rack").get("zone").get("id"), zoneId));
        }

        if (rackId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("rack").get("id"), rackId));
        }

        if (productId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("product").get("id"), productId));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("product").get("category").get("id"), categoryId));
        }

        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        return inventoryRepository.findAll(spec, pageable)
                .map(item -> InventoryReportItem.fromEntity(item,
                        stockTransactionRepository.sumStockOutByProductSince(item.getProduct().getId(), since)));
    }

    @Override
    public Page<LowStockReportItem> getLowStockReport(UUID categoryId, Pageable pageable) {
        List<Product> activeProducts = productRepository.findByActiveTrue();

        if (categoryId != null) {
            activeProducts = activeProducts.stream()
                    .filter(p -> p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                    .toList();
        }

        List<Object[]> groupedStock = inventoryRepository.findTotalStockGroupedByProductId();
        Map<UUID, Integer> stockMap = new HashMap<>();
        for (Object[] row : groupedStock) {
            UUID pId = (UUID) row[0];
            int total = ((Number) row[1]).intValue();
            stockMap.put(pId, total);
        }

        List<LowStockReportItem> lowStockItems = new ArrayList<>();
        for (Product product : activeProducts) {
            int currentStock = stockMap.getOrDefault(product.getId(), 0);
            int threshold = product.getThreshold();

            if (currentStock <= threshold) {
                int shortage = Math.max(0, threshold - currentStock);
                AlertSeverity severity;
                if (currentStock == 0) {
                    severity = AlertSeverity.OUT_OF_STOCK;
                } else if (currentStock <= threshold * 0.5) {
                    severity = AlertSeverity.CRITICAL;
                } else {
                    severity = AlertSeverity.WARNING;
                }

                String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
                BigDecimal unitPrice = product.getUnitPrice() != null
                        ? product.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

                lowStockItems.add(new LowStockReportItem(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        categoryName,
                        product.getUnit(),
                        unitPrice,
                        threshold,
                        currentStock,
                        shortage,
                        product.getReorderQuantity(),
                        severity,
                        product.isActive()
                ));
            }
        }

        int start = (int) Math.min(pageable.getOffset(), lowStockItems.size());
        int end = Math.min((start + pageable.getPageSize()), lowStockItems.size());
        List<LowStockReportItem> pagedContent = lowStockItems.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, lowStockItems.size());
    }

    @Override
    public Page<StockTransactionResponse> getStockMovementsReport(UUID productId, TransactionType transactionType,
                                                                  UUID actorId, OffsetDateTime from,
                                                                  OffsetDateTime to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Specification<StockTransaction> spec = (root, query, cb) -> cb.conjunction();

        if (productId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("product").get("id"), productId));
        }

        if (transactionType != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("transactionType"), transactionType));
        }

        if (actorId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("performedBy").get("id"), actorId));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return stockTransactionRepository.findAll(spec, pageable).map(StockTransactionResponse::fromEntity);
    }

    @Override
    public InventoryValuationSummaryResponse getValuationReport(UUID warehouseId) {
        if (warehouseId != null) {
            long totalQuantity = inventoryRepository.sumQuantityByWarehouseId(warehouseId);
            BigDecimal rawValuation = inventoryRepository.calculateValuationByWarehouseId(warehouseId);
            BigDecimal totalValuation = rawValuation != null
                    ? rawValuation.setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            List<WarehouseValuationBreakdown> allWh = mapWarehouseBreakdowns(
                    inventoryRepository.findValuationBreakdownByWarehouse());
            List<WarehouseValuationBreakdown> filteredWh = allWh.stream()
                    .filter(w -> warehouseId.equals(w.warehouseId()))
                    .toList();

            long totalProducts = filteredWh.isEmpty() ? 0 : filteredWh.get(0).itemCount();

            return new InventoryValuationSummaryResponse(
                    totalProducts,
                    totalQuantity,
                    totalValuation,
                    filteredWh,
                    List.of()
            );
        }

        long totalProductsWithInventory = inventoryRepository.countDistinctProductsWithInventory();
        long totalStockQuantity = inventoryRepository.sumTotalQuantity();
        BigDecimal rawValuation = inventoryRepository.calculateTotalInventoryValuation();
        BigDecimal totalInventoryValuation = rawValuation != null
                ? rawValuation.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<WarehouseValuationBreakdown> warehouseBreakdown = mapWarehouseBreakdowns(
                inventoryRepository.findValuationBreakdownByWarehouse());
        List<CategoryValuationBreakdown> categoryBreakdown = mapCategoryBreakdowns(
                inventoryRepository.findValuationBreakdownByCategory());

        return new InventoryValuationSummaryResponse(
                totalProductsWithInventory,
                totalStockQuantity,
                totalInventoryValuation,
                warehouseBreakdown,
                categoryBreakdown
        );
    }

    private List<WarehouseValuationBreakdown> mapWarehouseBreakdowns(List<Object[]> rows) {
        List<WarehouseValuationBreakdown> list = new ArrayList<>();
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            String code = (String) r[1];
            String name = (String) r[2];
            long qty = ((Number) r[3]).longValue();
            BigDecimal val = r[4] != null
                    ? ((BigDecimal) r[4]).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long items = ((Number) r[5]).longValue();
            list.add(new WarehouseValuationBreakdown(id, code, name, qty, val, items));
        }
        return list;
    }

    private List<CategoryValuationBreakdown> mapCategoryBreakdowns(List<Object[]> rows) {
        List<CategoryValuationBreakdown> list = new ArrayList<>();
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            String name = r[1] != null ? (String) r[1] : "Uncategorized";
            long qty = ((Number) r[2]).longValue();
            BigDecimal val = r[3] != null
                    ? ((BigDecimal) r[3]).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long items = ((Number) r[4]).longValue();
            list.add(new CategoryValuationBreakdown(id, name, qty, val, items));
        }
        return list;
    }
}
