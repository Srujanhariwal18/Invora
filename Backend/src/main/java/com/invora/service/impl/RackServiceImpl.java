package com.invora.service.impl;

import com.invora.dto.RackRequest;
import com.invora.dto.RackResponse;
import com.invora.entity.Rack;
import com.invora.entity.Zone;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.InventoryRepository;
import com.invora.repository.RackRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.ZoneRepository;
import com.invora.service.RackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RackServiceImpl implements RackService {

    private final RackRepository rackRepository;
    private final ZoneRepository zoneRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository stockTransactionRepository;

    public RackServiceImpl(RackRepository rackRepository,
                           ZoneRepository zoneRepository,
                           InventoryRepository inventoryRepository,
                           StockTransactionRepository stockTransactionRepository) {
        this.rackRepository = rackRepository;
        this.zoneRepository = zoneRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockTransactionRepository = stockTransactionRepository;
    }

    @Override
    @Transactional
    public RackResponse createRack(RackRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.zoneId()));

        String code = request.code().trim();
        if (rackRepository.existsByZoneIdAndCode(request.zoneId(), code)) {
            throw new ConflictException("Rack with code '" + code + "' already exists in zone '" + zone.getCode() + "'");
        }

        Rack rack = new Rack(zone, request.name().trim(), code, request.capacity());
        Rack saved = rackRepository.save(rack);
        return RackResponse.fromEntity(saved);
    }

    @Override
    public List<RackResponse> getAllRacks() {
        return rackRepository.findAll()
                .stream()
                .map(RackResponse::fromEntity)
                .toList();
    }

    @Override
    public RackResponse getRackById(UUID id) {
        Rack rack = rackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", id));
        return RackResponse.fromEntity(rack);
    }

    @Override
    public List<RackResponse> getRacksByZoneId(UUID zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ResourceNotFoundException("Zone", "id", zoneId);
        }
        return rackRepository.findByZoneId(zoneId)
                .stream()
                .map(RackResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public RackResponse updateRack(UUID id, RackRequest request) {
        Rack rack = rackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", id));

        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.zoneId()));

        String code = request.code().trim();
        if (rackRepository.existsByZoneIdAndCodeAndIdNot(request.zoneId(), code, id)) {
            throw new ConflictException("Rack with code '" + code + "' already exists in zone '" + zone.getCode() + "'");
        }

        rack.setZone(zone);
        rack.setName(request.name().trim());
        rack.setCode(code);
        rack.setCapacity(request.capacity());

        Rack updated = rackRepository.save(rack);
        return RackResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteRack(UUID id) {
        Rack rack = rackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", id));

        if (inventoryRepository.existsByRackId(id)) {
            throw new ConflictException("Cannot delete rack with id '" + id + "' because it contains active inventory");
        }

        if (stockTransactionRepository.existsByRackId(id)) {
            throw new ConflictException("Cannot delete rack with id '" + id + "' because it is referenced in stock transaction history");
        }

        rackRepository.delete(rack);
    }
}
