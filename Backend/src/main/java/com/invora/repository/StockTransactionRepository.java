package com.invora.repository;

import com.invora.entity.StockTransaction;
import com.invora.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID>, JpaSpecificationExecutor<StockTransaction> {

    List<StockTransaction> findAllByOrderByCreatedAtDesc();

    List<StockTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<StockTransaction> findByReference(String reference);

    List<StockTransaction> findByTransactionType(TransactionType transactionType);

    List<StockTransaction> findByPerformedById(UUID userId);

    @Query("SELECT (COUNT(st) > 0) FROM StockTransaction st WHERE st.sourceRack.id = :rackId OR st.destinationRack.id = :rackId")
    boolean existsByRackId(@Param("rackId") UUID rackId);

    List<StockTransaction> findTop10ByOrderByCreatedAtDesc();

    long countByTransactionTypeAndCreatedAtAfter(TransactionType transactionType, OffsetDateTime after);

    @Query("SELECT st.product.name, SUM(st.quantity) FROM StockTransaction st " +
           "WHERE st.transactionType = com.invora.entity.TransactionType.STOCK_OUT " +
           "GROUP BY st.product.name ORDER BY SUM(st.quantity) DESC")
    List<Object[]> findTopMovingProducts();

    @Query("SELECT COALESCE(SUM(st.quantity), 0) FROM StockTransaction st " +
           "WHERE st.product.id = :productId AND st.transactionType = com.invora.entity.TransactionType.STOCK_OUT " +
           "AND st.createdAt >= :since")
    long sumStockOutByProductSince(@Param("productId") UUID productId, @Param("since") OffsetDateTime since);
}
