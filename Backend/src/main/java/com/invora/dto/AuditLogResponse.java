package com.invora.dto;

import com.invora.entity.AuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorUsername,
    String actorEmail,
    String actorRole,
    String action,
    String entityType,
    String entityId,
    String description,
    OffsetDateTime createdAt
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) {
            return null;
        }

        UUID actorId = log.getUser() != null ? log.getUser().getId() : null;
        String actorUsername = log.getUser() != null ? log.getUser().getUsername() : "SYSTEM";
        String actorEmail = log.getUser() != null ? log.getUser().getEmail() : null;
        String actorRole = (log.getUser() != null && log.getUser().getRole() != null)
                ? log.getUser().getRole().name() : null;

        return new AuditLogResponse(
            log.getId(),
            actorId,
            actorUsername,
            actorEmail,
            actorRole,
            log.getAction(),
            log.getEntity(),
            log.getEntityId(),
            log.getDescription(),
            log.getCreatedAt()
        );
    }
}
