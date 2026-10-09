package com.invora.dto;

import com.invora.entity.Category;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String description,
    OffsetDateTime createdAt
) {
    public static CategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getCreatedAt()
        );
    }
}
