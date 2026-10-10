package com.invora.service.impl;

import com.invora.dto.ZoneRequest;
import com.invora.dto.ZoneResponse;
import com.invora.entity.Warehouse;
import com.invora.entity.Zone;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.RackRepository;
import com.invora.repository.WarehouseRepository;
import com.invora.repository.ZoneRepository;
import com.invora.service.ZoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;
    private final RackRepository rackRepository;

    public ZoneServiceImpl(ZoneRepository zoneRepository,
                           WarehouseRepository warehouseRepository,
                           RackRepository rackRepository) {
        this.zoneRepository = zoneRepository;
        this.warehouseRepository = warehouseRepository;
        this.rackRepository = rackRepository;
    }

    @Override
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.warehouseId()));

        String code = request.code().trim();
        if (zoneRepository.existsByWarehouseIdAndCode(request.warehouseId(), code)) {
            throw new ConflictException("Zone with code '" + code + "' already exists in warehouse '" + warehouse.getCode() + "'");
        }

        Zone zone = new Zone(warehouse, request.name().trim(), code);
        Zone saved = zoneRepository.save(zone);
        return ZoneResponse.fromEntity(saved);
    }

    @Override
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll()
                .stream()
                .map(ZoneResponse::fromEntity)
                .toList();
    }

    @Override
    public ZoneResponse getZoneById(UUID id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        return ZoneResponse.fromEntity(zone);
    }

    @Override
    public List<ZoneResponse> getZonesByWarehouseId(UUID warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", "id", warehouseId);
        }
        return zoneRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(ZoneResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public ZoneResponse updateZone(UUID id, ZoneRequest request) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", request.warehouseId()));

        String code = request.code().trim();
        if (zoneRepository.existsByWarehouseIdAndCodeAndIdNot(request.warehouseId(), code, id)) {
            throw new ConflictException("Zone with code '" + code + "' already exists in warehouse '" + warehouse.getCode() + "'");
        }

        zone.setWarehouse(warehouse);
        zone.setName(request.name().trim());
        zone.setCode(code);

        Zone updated = zoneRepository.save(zone);
        return ZoneResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteZone(UUID id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        if (rackRepository.existsByZoneId(id)) {
            throw new ConflictException("Cannot delete zone with id '" + id + "' because it contains existing racks");
        }

        zoneRepository.delete(zone);
    }
}
