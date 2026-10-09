package com.invora.service;

import com.invora.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditLogService {

    Page<AuditLogResponse> getAuditLogs(String actor, String action, String entity,
                                        OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    AuditLogResponse getAuditLogById(UUID id);
}
