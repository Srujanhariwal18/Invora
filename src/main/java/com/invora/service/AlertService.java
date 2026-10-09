package com.invora.service;

import com.invora.dto.AcknowledgeAlertRequest;
import com.invora.dto.AlertDetectionResponse;
import com.invora.dto.AlertResponse;
import com.invora.dto.ResolveAlertRequest;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AlertService {

    Page<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, UUID productId, Pageable pageable);

    AlertResponse getAlertById(UUID id);

    AlertResponse acknowledgeAlert(UUID id, AcknowledgeAlertRequest request);

    AlertResponse resolveAlert(UUID id, ResolveAlertRequest request);

    AlertDetectionResponse detectLowStockAlerts();
}
