package com.invora.dto;

import com.invora.entity.StockTransaction;
import com.invora.entity.TransactionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StockTransactionResponse(
    UUID id,
    UUID productId,
    String productSku,
    String productName,
    TransactionType transactionType,
    int quantity,
    UUID sourceRackId,
    String sourceRackCode,
    UUID destinationRackId,
    String destinationRackCode,
    String reference,
    String reason,
    Integer balanceAfter,
    UUID performedById,
    String performedByUsername,
    OffsetDateTime createdAt
) {
    public static StockTransactionResponse fromEntity(StockTransaction tx) {
        if (tx == null) {
            return null;
        }

        UUID pId = tx.getProduct() != null ? tx.getProduct().getId() : null;
        String pSku = tx.getProduct() != null ? tx.getProduct().getSku() : null;
        String pName = tx.getProduct() != null ? tx.getProduct().getName() : null;

        UUID srcId = tx.getSourceRack() != null ? tx.getSourceRack().getId() : null;
        String srcCode = tx.getSourceRack() != null ? tx.getSourceRack().getCode() : null;

        UUID dstId = tx.getDestinationRack() != null ? tx.getDestinationRack().getId() : null;
        String dstCode = tx.getDestinationRack() != null ? tx.getDestinationRack().getCode() : null;

        UUID perfId = tx.getPerformedBy() != null ? tx.getPerformedBy().getId() : null;
        String perfUsername = tx.getPerformedBy() != null ? tx.getPerformedBy().getUsername() : null;

        return new StockTransactionResponse(
            tx.getId(),
            pId,
            pSku,
            pName,
            tx.getTransactionType(),
            tx.getQuantity(),
            srcId,
            srcCode,
            dstId,
            dstCode,
            tx.getReference(),
            tx.getReason(),
            tx.getBalanceAfter(),
            perfId,
            perfUsername,
            tx.getCreatedAt()
        );
    }
}
