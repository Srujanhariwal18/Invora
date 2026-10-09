package com.invora.controller;

import com.invora.dto.StockInRequest;
import com.invora.dto.StockOutRequest;
import com.invora.dto.StockTransactionResponse;
import com.invora.dto.StockTransferRequest;
import com.invora.service.StockTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock/transactions")
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    public StockTransactionController(StockTransactionService stockTransactionService) {
        this.stockTransactionService = stockTransactionService;
    }

    @PostMapping("/in")
    public ResponseEntity<StockTransactionResponse> receiveStock(@Valid @RequestBody StockInRequest request) {
        StockTransactionResponse response = stockTransactionService.processStockIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/out")
    public ResponseEntity<StockTransactionResponse> issueStock(@Valid @RequestBody StockOutRequest request) {
        StockTransactionResponse response = stockTransactionService.processStockOut(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<StockTransactionResponse> transferStock(@Valid @RequestBody StockTransferRequest request) {
        StockTransactionResponse response = stockTransactionService.processStockTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StockTransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(stockTransactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockTransactionResponse> getTransactionById(@PathVariable UUID id) {
        return ResponseEntity.ok(stockTransactionService.getTransactionById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockTransactionResponse>> getTransactionsByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(stockTransactionService.getTransactionsByProductId(productId));
    }
}
