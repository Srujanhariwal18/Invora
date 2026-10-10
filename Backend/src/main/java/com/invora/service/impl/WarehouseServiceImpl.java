package com.invora.service.impl;

import com.invora.dto.WarehouseRequest;
import com.invora.dto.WarehouseResponse;
import com.invora.entity.Warehouse;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.WarehouseRepository;
import com.invora.repository.ZoneRepository;
import com.invora.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ZoneRepository zoneRepository;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository, ZoneRepository zoneRepository) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        String code = request.code().trim();
        if (warehouseRepository.existsByCode(code)) {
            throw new ConflictException("Warehouse with code '" + code + "' already exists");
        }

        String address = request.address() != null && !request.address().isBlank()
                ? request.address().trim() : null;

        Warehouse warehouse = new Warehouse(request.name().trim(), code, address, request.capacity());
        Warehouse saved = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(saved);
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream()
                .map(WarehouseResponse::fromEntity)
                .toList();
    }

    @Override
    public WarehouseResponse getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        return WarehouseResponse.fromEntity(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(UUID id, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        String code = request.code().trim();
        if (warehouseRepository.existsByCodeAndIdNot(code, id)) {
            throw new ConflictException("Warehouse with code '" + code + "' already exists");
        }

        String address = request.address() != null && !request.address().isBlank()
                ? request.address().trim() : null;

        warehouse.setName(request.name().trim());
        warehouse.setCode(code);
        warehouse.setAddress(address);
        warehouse.setCapacity(request.capacity());

        Warehouse updated = warehouseRepository.save(warehouse);
        return WarehouseResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteWarehouse(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        if (zoneRepository.existsByWarehouseId(id)) {
            throw new ConflictException("Cannot delete warehouse with id '" + id + "' because it contains existing zones");
        }

        warehouseRepository.delete(warehouse);
    }
}
