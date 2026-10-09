package com.invora.repository;

import com.invora.entity.Alert;
import com.invora.entity.AlertSeverity;
import com.invora.entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByProductId(UUID productId);

    List<Alert> findByProductIdAndStatus(UUID productId, AlertStatus status);

    List<Alert> findBySeverity(AlertSeverity severity);
}
