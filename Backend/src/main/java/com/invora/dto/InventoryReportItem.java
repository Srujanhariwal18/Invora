package com.invora.dto;

import com.invora.entity.Inventory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryReportItem(
    UUID inventoryId,
    UUID productId,
    String productSku,
    String productName,
    String categoryName,
    UUID warehouseId,
    String warehouseCode,
    String warehouseName,
    UUID zoneId,
    String zoneCode,
    UUID rackId,
    String rackCode,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalValue,
    OffsetDateTime updatedAt
) {
    public static InventoryReportItem fromEntity(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        UUID pId = inventory.getProduct() != null ? inventory.getProduct().getId() : null;
        String pSku = inventory.getProduct() != null ? inventory.getProduct().getSku() : null;
        String pName = inventory.getProduct() != null ? inventory.getProduct().getName() : null;
        String catName = (inventory.getProduct() != null && inventory.getProduct().getCategory() != null)
                ? inventory.getProduct().getCategory().getName() : null;
        BigDecimal uPrice = (inventory.getProduct() != null && inventory.getProduct().getUnitPrice() != null)
                ? inventory.getProduct().getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal val = uPrice.multiply(BigDecimal.valueOf(inventory.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        UUID rId = inventory.getRack() != null ? inventory.getRack().getId() : null;
        String rCode = inventory.getRack() != null ? inventory.getRack().getCode() : null;

        UUID zId = (inventory.getRack() != null && inventory.getRack().getZone() != null)
                ? inventory.getRack().getZone().getId() : null;
        String zCode = (inventory.getRack() != null && inventory.getRack().getZone() != null)
                ? inventory.getRack().getZone().getCode() : null;

        UUID wId = (inventory.getRack() != null && inventory.getRack().getZone() != null && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getId() : null;
        String wCode = (inventory.getRack() != null && inventory.getRack().getZone() != null && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getCode() : null;
        String wName = (inventory.getRack() != null && inventory.getRack().getZone() != null && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getName() : null;

        return new InventoryReportItem(
            inventory.getId(),
            pId,
            pSku,
            pName,
            catName,
            wId,
            wCode,
            wName,
            zId,
            zCode,
            rId,
            rCode,
            inventory.getQuantity(),
            uPrice,
            val,
            inventory.getUpdatedAt()
        );
    }
}
