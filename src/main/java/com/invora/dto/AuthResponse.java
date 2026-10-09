package com.invora.dto;

import com.invora.entity.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID id,
        String username,
        String email,
        Role role
) {
    public AuthResponse(String accessToken, UUID id, String username, String email, Role role) {
        this(accessToken, "Bearer", id, username, email, role);
    }
}
