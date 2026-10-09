package com.invora.repository;

import com.invora.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    List<Zone> findByWarehouseId(UUID warehouseId);

    Optional<Zone> findByWarehouseIdAndCode(UUID warehouseId, String code);
}
