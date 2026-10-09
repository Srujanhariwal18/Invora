package com.invora.repository;

import com.invora.entity.Rack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RackRepository extends JpaRepository<Rack, UUID> {

    List<Rack> findByZoneId(UUID zoneId);

    Optional<Rack> findByZoneIdAndCode(UUID zoneId, String code);
}
