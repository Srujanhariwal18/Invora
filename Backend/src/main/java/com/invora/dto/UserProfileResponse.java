package com.invora.dto;

import com.invora.entity.Role;
import com.invora.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        Role role,
        boolean active,
        OffsetDateTime createdAt
) {
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
