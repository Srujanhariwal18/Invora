package com.invora.service.impl;

import com.invora.dto.StockInRequest;
import com.invora.dto.StockOutRequest;
import com.invora.dto.StockTransactionResponse;
import com.invora.dto.StockTransferRequest;
import com.invora.entity.AuditLog;
import com.invora.entity.Inventory;
import com.invora.entity.Product;
import com.invora.entity.Rack;
import com.invora.entity.Role;
import com.invora.entity.StockTransaction;
import com.invora.entity.TransactionType;
import com.invora.entity.User;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.AuditLogRepository;
import com.invora.repository.InventoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.RackRepository;
import com.invora.repository.StockTransactionRepository;
import com.invora.repository.UserRepository;
import com.invora.service.StockTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final RackRepository rackRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public StockTransactionServiceImpl(StockTransactionRepository stockTransactionRepository,
                                       InventoryRepository inventoryRepository,
                                       ProductRepository productRepository,
                                       RackRepository rackRepository,
                                       UserRepository userRepository,
                                       AuditLogRepository auditLogRepository) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.rackRepository = rackRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    private User resolveActingUser(UUID legacyPerformedById) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof com.invora.security.UserPrincipal principal) {
                return userRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
            }

            String username = auth.getName();
            java.util.Optional<User> userOpt = userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username));
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
            throw new ResourceNotFoundException("User", "username", username);
        }

        // Unauthenticated or invalid context: fail safely rather than selecting a seed admin
        if (legacyPerformedById != null) {
            return userRepository.findById(legacyPerformedById)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", legacyPerformedById));
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "Authentication required: No authenticated user available to record stock transaction");
    }

    @Override
    @Transactional
    public StockTransactionResponse processStockIn(StockInRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId()));

        Rack destinationRack = rackRepository.findById(request.destinationRackId())
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", request.destinationRackId()));

        Inventory inventory = inventoryRepository
                .findByProductIdAndRackIdForUpdate(request.productId(), request.destinationRackId())
                .orElse(null);

        if (inventory == null) {
            inventory = new Inventory(product, destinationRack, 0);
        }

        int newBalance = inventory.getQuantity() + request.quantity();
        inventory.setQuantity(newBalance);
        inventoryRepository.save(inventory);

        User user = resolveActingUser(request.performedById());
        StockTransaction tx = new StockTransaction(
                product,
                TransactionType.STOCK_IN,
                request.quantity(),
                null,
                destinationRack,
                request.reference(),
                request.reason(),
                newBalance,
                user
        );

        StockTransaction saved = stockTransactionRepository.save(tx);
        auditLogRepository.save(new AuditLog(
                user,
                "STOCK_IN",
                "StockTransaction",
                saved.getId().toString(),
                String.format("Stock IN of %d units for SKU %s into rack %s", saved.getQuantity(), product.getSku(), destinationRack.getCode())
        ));
        return StockTransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public StockTransactionResponse processStockOut(StockOutRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId()));

        Rack sourceRack = rackRepository.findById(request.sourceRackId())
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", request.sourceRackId()));

        Inventory inventory = inventoryRepository
                .findByProductIdAndRackIdForUpdate(request.productId(), request.sourceRackId())
                .orElse(null);

        if (inventory == null || inventory.getQuantity() < request.quantity()) {
            int available = inventory == null ? 0 : inventory.getQuantity();
            throw new ConflictException("Insufficient stock in rack '" + sourceRack.getCode()
                    + "'. Available: " + available + ", requested: " + request.quantity());
        }

        int newBalance = inventory.getQuantity() - request.quantity();
        inventory.setQuantity(newBalance);
        inventoryRepository.save(inventory);

        User user = resolveActingUser(request.performedById());
        StockTransaction tx = new StockTransaction(
                product,
                TransactionType.STOCK_OUT,
                request.quantity(),
                sourceRack,
                null,
                request.reference(),
                request.reason(),
                newBalance,
                user
        );

        StockTransaction saved = stockTransactionRepository.save(tx);
        auditLogRepository.save(new AuditLog(
                user,
                "STOCK_OUT",
                "StockTransaction",
                saved.getId().toString(),
                String.format("Stock OUT of %d units for SKU %s from rack %s", saved.getQuantity(), product.getSku(), sourceRack.getCode())
        ));
        return StockTransactionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public StockTransactionResponse processStockTransfer(StockTransferRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (request.sourceRackId().equals(request.destinationRackId())) {
            throw new ConflictException("Source rack and destination rack cannot be the same");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId()));

        Rack sourceRack = rackRepository.findById(request.sourceRackId())
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", request.sourceRackId()));

        Rack destinationRack = rackRepository.findById(request.destinationRackId())
                .orElseThrow(() -> new ResourceNotFoundException("Rack", "id", request.destinationRackId()));

        // Deadlock prevention: lock inventories in deterministic order based on rack UUID
        UUID firstRackId = request.sourceRackId().compareTo(request.destinationRackId()) < 0
                ? request.sourceRackId() : request.destinationRackId();
        UUID secondRackId = request.sourceRackId().compareTo(request.destinationRackId()) < 0
                ? request.destinationRackId() : request.sourceRackId();

        Inventory firstInv = inventoryRepository
                .findByProductIdAndRackIdForUpdate(request.productId(), firstRackId).orElse(null);
        Inventory secondInv = inventoryRepository
                .findByProductIdAndRackIdForUpdate(request.productId(), secondRackId).orElse(null);

        Inventory sourceInv = request.sourceRackId().equals(firstRackId) ? firstInv : secondInv;
        Inventory destInv = request.destinationRackId().equals(firstRackId) ? firstInv : secondInv;

        if (sourceInv == null || sourceInv.getQuantity() < request.quantity()) {
            int available = sourceInv == null ? 0 : sourceInv.getQuantity();
            throw new ConflictException("Insufficient stock in source rack '" + sourceRack.getCode()
                    + "'. Available: " + available + ", requested: " + request.quantity());
        }

        if (destInv == null) {
            destInv = new Inventory(product, destinationRack, 0);
        }

        int newSourceBalance = sourceInv.getQuantity() - request.quantity();
        sourceInv.setQuantity(newSourceBalance);
        destInv.setQuantity(destInv.getQuantity() + request.quantity());

        inventoryRepository.save(sourceInv);
        inventoryRepository.save(destInv);

        User user = resolveActingUser(request.performedById());
        StockTransaction tx = new StockTransaction(
                product,
                TransactionType.TRANSFER,
                request.quantity(),
                sourceRack,
                destinationRack,
                request.reference(),
                request.reason(),
                newSourceBalance,
                user
        );

        StockTransaction saved = stockTransactionRepository.save(tx);
        auditLogRepository.save(new AuditLog(
                user,
                "STOCK_TRANSFER",
                "StockTransaction",
                saved.getId().toString(),
                String.format("Stock TRANSFER of %d units for SKU %s from rack %s to rack %s", saved.getQuantity(), product.getSku(), sourceRack.getCode(), destinationRack.getCode())
        ));
        return StockTransactionResponse.fromEntity(saved);
    }

    @Override
    public List<StockTransactionResponse> getAllTransactions() {
        return stockTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(StockTransactionResponse::fromEntity)
                .toList();
    }

    @Override
    public StockTransactionResponse getTransactionById(UUID id) {
        StockTransaction tx = stockTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTransaction", "id", id));
        return StockTransactionResponse.fromEntity(tx);
    }

    @Override
    public List<StockTransactionResponse> getTransactionsByProductId(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        return stockTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(StockTransactionResponse::fromEntity)
                .toList();
    }
}
