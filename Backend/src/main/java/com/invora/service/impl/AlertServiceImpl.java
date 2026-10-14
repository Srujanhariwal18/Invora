package com.invora.service.impl;

import com.invora.dto.AcknowledgeAlertRequest;
import com.invora.dto.AlertDetectionResponse;
import com.invora.dto.AlertResponse;
import com.invora.dto.ResolveAlertRequest;
import com.invora.entity.Alert;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import com.invora.entity.AuditLog;
import com.invora.entity.Product;
import com.invora.entity.User;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.AlertRepository;
import com.invora.repository.AuditLogRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.UserRepository;
import com.invora.security.UserPrincipal;
import com.invora.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AlertServiceImpl(AlertRepository alertRepository,
                            ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            AuditLogRepository auditLogRepository,
                            UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<AlertResponse> getAlerts(AlertStatus status, AlertSeverity severity, UUID productId, Pageable pageable) {
        Specification<Alert> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        return alertRepository.findAll(spec, pageable).map(AlertResponse::fromEntity);
    }

    @Override
    public AlertResponse getAlertById(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));
        return AlertResponse.fromEntity(alert);
    }

    @Override
    @Transactional
    public AlertResponse acknowledgeAlert(UUID id, AcknowledgeAlertRequest request) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ConflictException("Cannot acknowledge an already resolved alert with id: " + id);
        }

        String note = (request != null && request.note() != null) ? request.note().trim() : null;
        String currentMsg = alert.getMessage() != null ? alert.getMessage() : "";

        if (note != null && !note.isBlank()) {
            if (!currentMsg.contains("[ACKNOWLEDGED]")) {
                alert.setMessage("[ACKNOWLEDGED] " + currentMsg + " (Note: " + note + ")");
            } else {
                alert.setMessage(currentMsg + " (Note: " + note + ")");
            }
        } else if (!currentMsg.startsWith("[ACKNOWLEDGED]")) {
            alert.setMessage("[ACKNOWLEDGED] " + currentMsg);
        }

        Alert savedAlert = alertRepository.save(alert);

        User actingUser = resolveActingUser();
        String description = note != null && !note.isBlank()
                ? "Alert acknowledged for product " + alert.getProduct().getSku() + ": " + note
                : "Alert acknowledged for product " + alert.getProduct().getSku();
        auditLogRepository.save(new AuditLog(actingUser, "ALERT_ACKNOWLEDGED", "Alert", alert.getId().toString(), description));

        return AlertResponse.fromEntity(savedAlert);
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(UUID id, ResolveAlertRequest request) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ConflictException("Alert with id: " + id + " is already resolved");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(OffsetDateTime.now());

        String note = (request != null && request.note() != null) ? request.note().trim() : null;
        if (note != null && !note.isBlank()) {
            String currentMsg = alert.getMessage() != null ? alert.getMessage() : "";
            alert.setMessage(currentMsg + " [Resolved: " + note + "]");
        }

        Alert savedAlert = alertRepository.save(alert);

        User actingUser = resolveActingUser();
        String description = note != null && !note.isBlank()
                ? "Alert resolved for product " + alert.getProduct().getSku() + ": " + note
                : "Alert resolved for product " + alert.getProduct().getSku();
        auditLogRepository.save(new AuditLog(actingUser, "ALERT_RESOLVED", "Alert", alert.getId().toString(), description));

        return AlertResponse.fromEntity(savedAlert);
    }

    private static final Object DETECTION_LOCK = new Object();

    @Override
    @Transactional
    public AlertDetectionResponse detectLowStockAlerts() {
        synchronized (DETECTION_LOCK) {
            List<Product> activeProducts = productRepository.findByActiveTrue();

            List<Object[]> groupedStock = inventoryRepository.findTotalStockGroupedByProductId();
            Map<UUID, Integer> stockMap = new HashMap<>();
            for (Object[] row : groupedStock) {
                UUID pId = (UUID) row[0];
                int total = ((Number) row[1]).intValue();
                stockMap.put(pId, total);
            }

            int alertsCreated = 0;
            int alertsUpdated = 0;
            int alertsResolved = 0;

            for (Product product : activeProducts) {
                int currentStock = stockMap.getOrDefault(product.getId(), 0);
                int threshold = product.getThreshold();

                if (currentStock <= threshold) {
                    AlertSeverity severity;
                    if (currentStock == 0) {
                        severity = AlertSeverity.OUT_OF_STOCK;
                    } else if (currentStock <= threshold * 0.5) {
                        severity = AlertSeverity.CRITICAL;
                    } else {
                        severity = AlertSeverity.WARNING;
                    }

                    String message;
                    if (severity == AlertSeverity.OUT_OF_STOCK) {
                        message = String.format("Product '%s' (%s) is out of stock (threshold: %d)",
                                product.getName(), product.getSku(), threshold);
                    } else {
                        message = String.format("Stock (%d) is below safety threshold (%d) for %s",
                                currentStock, threshold, product.getName());
                    }

                    Optional<Alert> existingOpt = alertRepository.findFirstByProductIdAndStatus(product.getId(), AlertStatus.ACTIVE);
                    if (existingOpt.isPresent()) {
                        Alert existing = existingOpt.get();
                        boolean changed = false;
                        if (existing.getCurrentStock() != currentStock) {
                            existing.setCurrentStock(currentStock);
                            changed = true;
                        }
                        if (existing.getThreshold() != threshold) {
                            existing.setThreshold(threshold);
                            changed = true;
                        }
                        if (existing.getSeverity() != severity) {
                            existing.setSeverity(severity);
                            changed = true;
                        }
                        if (changed) {
                            existing.setMessage(message);
                            alertRepository.save(existing);
                            alertsUpdated++;
                        }
                    } else {
                        Alert newAlert = new Alert(product, currentStock, threshold, severity, AlertStatus.ACTIVE, message);
                        alertRepository.save(newAlert);
                        alertsCreated++;
                    }
                } else {
                    Optional<Alert> existingOpt = alertRepository.findFirstByProductIdAndStatus(product.getId(), AlertStatus.ACTIVE);
                    if (existingOpt.isPresent()) {
                        Alert existing = existingOpt.get();
                        existing.setStatus(AlertStatus.RESOLVED);
                        existing.setResolvedAt(OffsetDateTime.now());
                        existing.setCurrentStock(currentStock);
                        existing.setMessage(String.format("Stock replenished (%d > %d) for %s",
                                currentStock, threshold, product.getName()));
                        alertRepository.save(existing);
                        alertsResolved++;
                    }
                }
            }

            long totalActive = alertRepository.countByStatus(AlertStatus.ACTIVE);
            List<AlertResponse> activeAlerts = alertRepository.findByStatus(AlertStatus.ACTIVE)
                    .stream()
                    .map(AlertResponse::fromEntity)
                    .toList();

            return new AlertDetectionResponse(
                    activeProducts.size(),
                    alertsCreated,
                    alertsUpdated,
                    alertsResolved,
                    totalActive,
                    activeAlerts
            );
        }
    }

    private User resolveActingUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof UserPrincipal principal) {
                return userRepository.findById(principal.getId()).orElse(null);
            }
            String username = auth.getName();
            if (username != null && !username.isBlank()) {
                return userRepository.findByUsername(username).orElse(null);
            }
        }
        return null;
    }
}
