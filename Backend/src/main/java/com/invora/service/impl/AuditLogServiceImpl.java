package com.invora.service.impl;

import com.invora.dto.AuditLogResponse;
import com.invora.entity.AuditLog;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.AuditLogRepository;
import com.invora.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(String actor, String action, String entity,
                                               OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();

        if (actor != null && !actor.isBlank()) {
            String cleanActor = actor.trim();
            try {
                UUID actorUuid = UUID.fromString(cleanActor);
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.equal(root.get("user").get("id"), actorUuid),
                        cb.equal(cb.lower(root.get("user").get("username")), cleanActor.toLowerCase())
                ));
            } catch (IllegalArgumentException e) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(cb.lower(root.get("user").get("username")), cleanActor.toLowerCase()));
            }
        }

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
        }

        if (entity != null && !entity.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("entity")), entity.trim().toLowerCase()));
        }

        if (from != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }

        if (to != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::fromEntity);
    }

    @Override
    public AuditLogResponse getAuditLogById(UUID id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return AuditLogResponse.fromEntity(auditLog);
    }
}
