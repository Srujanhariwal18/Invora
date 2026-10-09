package com.invora.service;

import com.invora.dto.InventoryReportItem;
import com.invora.dto.InventoryValuationSummaryResponse;
import com.invora.dto.LowStockReportItem;
import com.invora.dto.StockTransactionResponse;
import com.invora.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ReportService {

    Page<InventoryReportItem> getInventoryReport(UUID warehouseId, UUID zoneId, UUID rackId,
                                                UUID productId, UUID categoryId, Pageable pageable);

    Page<LowStockReportItem> getLowStockReport(UUID categoryId, Pageable pageable);

    Page<StockTransactionResponse> getStockMovementsReport(UUID productId, TransactionType transactionType,
                                                          UUID actorId, OffsetDateTime from,
                                                          OffsetDateTime to, Pageable pageable);

    InventoryValuationSummaryResponse getValuationReport(UUID warehouseId);
}
