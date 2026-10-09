package com.invora.service;

import com.invora.dto.ZoneRequest;
import com.invora.dto.ZoneResponse;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

    ZoneResponse createZone(ZoneRequest request);

    List<ZoneResponse> getAllZones();

    ZoneResponse getZoneById(UUID id);

    List<ZoneResponse> getZonesByWarehouseId(UUID warehouseId);

    ZoneResponse updateZone(UUID id, ZoneRequest request);

    void deleteZone(UUID id);
}
