package com.invora.dto;

import com.invora.entity.Warehouse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WarehouseResponse(
    UUID id,
    String name,
    String code,
    String address,
    int capacity,
    OffsetDateTime createdAt
) {
    public static WarehouseResponse fromEntity(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        return new WarehouseResponse(
            warehouse.getId(),
            warehouse.getName(),
            warehouse.getCode(),
            warehouse.getAddress(),
            warehouse.getCapacity(),
            warehouse.getCreatedAt()
        );
    }
}
