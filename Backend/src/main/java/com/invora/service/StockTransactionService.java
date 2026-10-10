package com.invora.service;

import com.invora.dto.StockInRequest;
import com.invora.dto.StockOutRequest;
import com.invora.dto.StockTransactionResponse;
import com.invora.dto.StockTransferRequest;

import java.util.List;
import java.util.UUID;

public interface StockTransactionService {

    StockTransactionResponse processStockIn(StockInRequest request);

    StockTransactionResponse processStockOut(StockOutRequest request);

    StockTransactionResponse processStockTransfer(StockTransferRequest request);

    List<StockTransactionResponse> getAllTransactions();

    StockTransactionResponse getTransactionById(UUID id);

    List<StockTransactionResponse> getTransactionsByProductId(UUID productId);
}
