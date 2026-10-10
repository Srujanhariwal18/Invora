package com.invora.dto;

import com.invora.entity.Supplier;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierResponse(
    UUID id,
    String name,
    String contactPerson,
    String email,
    String phone,
    String address,
    OffsetDateTime createdAt
) {
    public static SupplierResponse fromEntity(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return new SupplierResponse(
            supplier.getId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.getAddress(),
            supplier.getCreatedAt()
        );
    }
}
