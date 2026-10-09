package com.invora.dto;

import com.invora.entity.Product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String sku,
    String name,
    UUID categoryId,
    String categoryName,
    UUID supplierId,
    String supplierName,
    String unit,
    BigDecimal unitPrice,
    int threshold,
    int reorderQuantity,
    String description,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }

        UUID catId = product.getCategory() != null ? product.getCategory().getId() : null;
        String catName = product.getCategory() != null ? product.getCategory().getName() : null;
        UUID supId = product.getSupplier() != null ? product.getSupplier().getId() : null;
        String supName = product.getSupplier() != null ? product.getSupplier().getName() : null;

        return new ProductResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            catId,
            catName,
            supId,
            supName,
            product.getUnit(),
            product.getUnitPrice(),
            product.getThreshold(),
            product.getReorderQuantity(),
            product.getDescription(),
            product.isActive(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
