package com.invora.service;

import com.invora.dto.InventoryResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    List<InventoryResponse> getAllInventory();

    InventoryResponse getInventoryById(UUID id);

    List<InventoryResponse> getInventoryByProductId(UUID productId);

    List<InventoryResponse> getInventoryByRackId(UUID rackId);
}
