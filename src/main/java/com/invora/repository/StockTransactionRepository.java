package com.invora.repository;

import com.invora.entity.StockTransaction;
import com.invora.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    List<StockTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<StockTransaction> findByReference(String reference);

    List<StockTransaction> findByTransactionType(TransactionType transactionType);

    List<StockTransaction> findByPerformedById(UUID userId);
}
