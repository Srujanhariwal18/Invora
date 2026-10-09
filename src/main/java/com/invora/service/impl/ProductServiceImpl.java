package com.invora.service.impl;

import com.invora.dto.ProductRequest;
import com.invora.dto.ProductResponse;
import com.invora.entity.Category;
import com.invora.entity.Product;
import com.invora.entity.Supplier;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.CategoryRepository;
import com.invora.repository.ProductRepository;
import com.invora.repository.SupplierRepository;
import com.invora.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new ConflictException("Product with SKU '" + sku + "' already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.supplierId()));
        }

        String unit = (request.unit() != null && !request.unit().isBlank()) ? request.unit().trim() : "PIECE";

        Product product = new Product(
                sku,
                request.name().trim(),
                category,
                supplier,
                unit,
                request.unitPrice(),
                request.threshold(),
                request.reorderQuantity(),
                request.description()
        );

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        String sku = request.sku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("Product with SKU '" + sku + "' already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", request.supplierId()));
        }

        product.setSku(sku);
        product.setName(request.name().trim());
        product.setCategory(category);
        product.setSupplier(supplier);
        if (request.unit() != null && !request.unit().isBlank()) {
            product.setUnit(request.unit().trim());
        }
        product.setUnitPrice(request.unitPrice());
        product.setThreshold(request.threshold());
        product.setReorderQuantity(request.reorderQuantity());
        product.setDescription(request.description());

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Override
    public List<ProductResponse> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Override
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return ProductResponse.fromEntity(product);
    }

    @Override
    @Transactional
    public ProductResponse deactivateProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(false);
        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }
}
