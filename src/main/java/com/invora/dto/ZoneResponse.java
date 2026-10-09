package com.invora.dto;

import com.invora.entity.Zone;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ZoneResponse(
    UUID id,
    UUID warehouseId,
    String warehouseName,
    String warehouseCode,
    String name,
    String code,
    OffsetDateTime createdAt
) {
    public static ZoneResponse fromEntity(Zone zone) {
        if (zone == null) {
            return null;
        }
        UUID whId = zone.getWarehouse() != null ? zone.getWarehouse().getId() : null;
        String whName = zone.getWarehouse() != null ? zone.getWarehouse().getName() : null;
        String whCode = zone.getWarehouse() != null ? zone.getWarehouse().getCode() : null;

        return new ZoneResponse(
            zone.getId(),
            whId,
            whName,
            whCode,
            zone.getName(),
            zone.getCode(),
            zone.getCreatedAt()
        );
    }
}
