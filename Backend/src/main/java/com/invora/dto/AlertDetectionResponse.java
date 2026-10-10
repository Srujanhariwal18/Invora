package com.invora.dto;

import java.util.List;

public record AlertDetectionResponse(
    int productsEvaluated,
    int alertsCreated,
    int alertsUpdated,
    int alertsResolved,
    long totalActiveAlerts,
    List<AlertResponse> alerts
) {
}
