package com.invora.repository;

import com.invora.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductIdAndRackId(UUID productId, UUID rackId);

    List<Inventory> findByProductId(UUID productId);

    List<Inventory> findByRackId(UUID rackId);

    boolean existsByRackId(UUID rackId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.rack.id = :rackId")
    Optional<Inventory> findByProductIdAndRackIdForUpdate(@Param("productId") UUID productId, @Param("rackId") UUID rackId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i")
    long sumTotalQuantity();

    @Query("SELECT SUM(i.quantity * p.unitPrice) FROM Inventory i JOIN i.product p")
    java.math.BigDecimal calculateTotalInventoryValuation();

    @Query("SELECT i.product.id, SUM(i.quantity) FROM Inventory i GROUP BY i.product.id")
    List<Object[]> findTotalStockGroupedByProductId();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.product.id = :productId")
    int sumQuantityByProductId(@Param("productId") UUID productId);
}
