package com.invora.controller;

import com.invora.dto.AcknowledgeAlertRequest;
import com.invora.dto.AlertDetectionResponse;
import com.invora.dto.AlertResponse;
import com.invora.dto.ResolveAlertRequest;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import com.invora.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(alertService.getAlerts(status, severity, productId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledgeAlertPut(
            @PathVariable UUID id,
            @RequestBody(required = false) AcknowledgeAlertRequest request) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, request));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledgeAlertPost(
            @PathVariable UUID id,
            @RequestBody(required = false) AcknowledgeAlertRequest request) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, request));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolveAlertPut(
            @PathVariable UUID id,
            @RequestBody(required = false) ResolveAlertRequest request) {
        return ResponseEntity.ok(alertService.resolveAlert(id, request));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolveAlertPost(
            @PathVariable UUID id,
            @RequestBody(required = false) ResolveAlertRequest request) {
        return ResponseEntity.ok(alertService.resolveAlert(id, request));
    }

    @PostMapping("/check")
    public ResponseEntity<AlertDetectionResponse> checkLowStockAlerts() {
        return ResponseEntity.ok(alertService.detectLowStockAlerts());
    }

    @PostMapping("/detect")
    public ResponseEntity<AlertDetectionResponse> detectLowStockAlerts() {
        return ResponseEntity.ok(alertService.detectLowStockAlerts());
    }
}
