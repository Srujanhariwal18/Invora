package com.invora.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryValuationBreakdown(
    UUID categoryId,
    String categoryName,
    long totalQuantity,
    BigDecimal totalValuation,
    long itemCount
) {
}
