package com.invora.repository;

import com.invora.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {

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

    @Query("SELECT w.id, w.code, w.name, COALESCE(SUM(i.quantity), 0), SUM(i.quantity * p.unitPrice), COUNT(DISTINCT p.id) " +
           "FROM Inventory i JOIN i.product p JOIN i.rack r JOIN r.zone z JOIN z.warehouse w " +
           "GROUP BY w.id, w.code, w.name")
    List<Object[]> findValuationBreakdownByWarehouse();

    @Query("SELECT c.id, c.name, COALESCE(SUM(i.quantity), 0), SUM(i.quantity * p.unitPrice), COUNT(DISTINCT p.id) " +
           "FROM Inventory i JOIN i.product p LEFT JOIN p.category c " +
           "GROUP BY c.id, c.name")
    List<Object[]> findValuationBreakdownByCategory();

    @Query("SELECT COUNT(DISTINCT i.product.id) FROM Inventory i WHERE i.quantity > 0")
    long countDistinctProductsWithInventory();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i JOIN i.rack r JOIN r.zone z WHERE z.warehouse.id = :warehouseId")
    long sumQuantityByWarehouseId(@Param("warehouseId") UUID warehouseId);

    @Query("SELECT SUM(i.quantity * p.unitPrice) FROM Inventory i JOIN i.product p JOIN i.rack r JOIN r.zone z WHERE z.warehouse.id = :warehouseId")
    java.math.BigDecimal calculateValuationByWarehouseId(@Param("warehouseId") UUID warehouseId);
}
