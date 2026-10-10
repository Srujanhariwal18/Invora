package com.invora.service;

import com.invora.dto.RackRequest;
import com.invora.dto.RackResponse;

import java.util.List;
import java.util.UUID;

public interface RackService {

    RackResponse createRack(RackRequest request);

    List<RackResponse> getAllRacks();

    RackResponse getRackById(UUID id);

    List<RackResponse> getRacksByZoneId(UUID zoneId);

    RackResponse updateRack(UUID id, RackRequest request);

    void deleteRack(UUID id);
}
