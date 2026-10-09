package com.invora.repository;

import com.invora.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductIdAndRackId(UUID productId, UUID rackId);

    List<Inventory> findByProductId(UUID productId);

    List<Inventory> findByRackId(UUID rackId);

    boolean existsByRackId(UUID rackId);
}
