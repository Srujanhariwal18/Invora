package com.invora.controller;

import com.invora.dto.InventoryReportItem;
import com.invora.dto.InventoryValuationSummaryResponse;
import com.invora.dto.LowStockReportItem;
import com.invora.dto.StockTransactionResponse;
import com.invora.entity.TransactionType;
import com.invora.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<Page<InventoryReportItem>> getInventoryReport(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID rackId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reportService.getInventoryReport(warehouseId, zoneId, rackId, productId, categoryId, pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Page<LowStockReportItem>> getLowStockReport(
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.getLowStockReport(categoryId, pageable));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<Page<StockTransactionResponse>> getStockMovementsReport(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reportService.getStockMovementsReport(productId, transactionType, actorId, from, to, pageable));
    }

    @GetMapping("/valuation")
    public ResponseEntity<InventoryValuationSummaryResponse> getValuationReport(
            @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(reportService.getValuationReport(warehouseId));
    }
}
