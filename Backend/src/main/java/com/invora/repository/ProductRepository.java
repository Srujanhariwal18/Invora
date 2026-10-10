package com.invora.repository;

import com.invora.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findBySupplierId(UUID supplierId);

    List<Product> findByActiveTrue();

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsBySupplierId(UUID supplierId);

    long countByActiveTrue();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND (SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.product = p) <= p.threshold")
    long countLowStockProducts();

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.active = true AND (SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.product = p) <= p.threshold")
    List<Product> findLowStockProducts();
}
