package com.invora.dto;

import com.invora.entity.Inventory;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryResponse(
    UUID id,
    UUID productId,
    String productSku,
    String productName,
    UUID rackId,
    String rackCode,
    String rackName,
    UUID zoneId,
    String zoneCode,
    String zoneName,
    UUID warehouseId,
    String warehouseCode,
    String warehouseName,
    int quantity,
    OffsetDateTime updatedAt
) {
    public static InventoryResponse fromEntity(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        UUID pId = inventory.getProduct() != null ? inventory.getProduct().getId() : null;
        String pSku = inventory.getProduct() != null ? inventory.getProduct().getSku() : null;
        String pName = inventory.getProduct() != null ? inventory.getProduct().getName() : null;

        UUID rId = inventory.getRack() != null ? inventory.getRack().getId() : null;
        String rCode = inventory.getRack() != null ? inventory.getRack().getCode() : null;
        String rName = inventory.getRack() != null ? inventory.getRack().getName() : null;

        UUID zId = (inventory.getRack() != null && inventory.getRack().getZone() != null)
                ? inventory.getRack().getZone().getId() : null;
        String zCode = (inventory.getRack() != null && inventory.getRack().getZone() != null)
                ? inventory.getRack().getZone().getCode() : null;
        String zName = (inventory.getRack() != null && inventory.getRack().getZone() != null)
                ? inventory.getRack().getZone().getName() : null;

        UUID whId = (inventory.getRack() != null && inventory.getRack().getZone() != null
                && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getId() : null;
        String whCode = (inventory.getRack() != null && inventory.getRack().getZone() != null
                && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getCode() : null;
        String whName = (inventory.getRack() != null && inventory.getRack().getZone() != null
                && inventory.getRack().getZone().getWarehouse() != null)
                ? inventory.getRack().getZone().getWarehouse().getName() : null;

        return new InventoryResponse(
            inventory.getId(),
            pId,
            pSku,
            pName,
            rId,
            rCode,
            rName,
            zId,
            zCode,
            zName,
            whId,
            whCode,
            whName,
            inventory.getQuantity(),
            inventory.getUpdatedAt()
        );
    }
}
