package com.invora.service.impl;

import com.invora.dto.InventoryResponse;
import com.invora.entity.Inventory;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.RackRepository;
import com.invora.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final RackRepository rackRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                ProductRepository productRepository,
                                RackRepository rackRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.rackRepository = rackRepository;
    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(InventoryResponse::fromEntity)
                .toList();
    }

    @Override
    public InventoryResponse getInventoryById(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));
        return InventoryResponse.fromEntity(inventory);
    }

    @Override
    public List<InventoryResponse> getInventoryByProductId(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return inventoryRepository.findByProductId(productId)
                .stream()
                .map(InventoryResponse::fromEntity)
                .toList();
    }

    @Override
    public List<InventoryResponse> getInventoryByRackId(UUID rackId) {
        if (!rackRepository.existsById(rackId)) {
            throw new ResourceNotFoundException("Rack", "id", rackId);
        }
        return inventoryRepository.findByRackId(rackId)
                .stream()
                .map(InventoryResponse::fromEntity)
                .toList();
    }
}
