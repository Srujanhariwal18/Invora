package com.invora.service;

import com.invora.dto.WarehouseRequest;
import com.invora.dto.WarehouseResponse;

import java.util.List;
import java.util.UUID;

public interface WarehouseService {

    WarehouseResponse createWarehouse(WarehouseRequest request);

    List<WarehouseResponse> getAllWarehouses();

    WarehouseResponse getWarehouseById(UUID id);

    WarehouseResponse updateWarehouse(UUID id, WarehouseRequest request);

    void deleteWarehouse(UUID id);
}
