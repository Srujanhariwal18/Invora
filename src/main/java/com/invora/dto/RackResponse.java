package com.invora.dto;

import com.invora.entity.Rack;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RackResponse(
    UUID id,
    UUID zoneId,
    String zoneName,
    String zoneCode,
    UUID warehouseId,
    String warehouseCode,
    String name,
    String code,
    int capacity,
    OffsetDateTime createdAt
) {
    public static RackResponse fromEntity(Rack rack) {
        if (rack == null) {
            return null;
        }

        UUID zId = rack.getZone() != null ? rack.getZone().getId() : null;
        String zName = rack.getZone() != null ? rack.getZone().getName() : null;
        String zCode = rack.getZone() != null ? rack.getZone().getCode() : null;

        UUID whId = (rack.getZone() != null && rack.getZone().getWarehouse() != null)
                ? rack.getZone().getWarehouse().getId() : null;
        String whCode = (rack.getZone() != null && rack.getZone().getWarehouse() != null)
                ? rack.getZone().getWarehouse().getCode() : null;

        return new RackResponse(
            rack.getId(),
            zId,
            zName,
            zCode,
            whId,
            whCode,
            rack.getName(),
            rack.getCode(),
            rack.getCapacity(),
            rack.getCreatedAt()
        );
    }
}
