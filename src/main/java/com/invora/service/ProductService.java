package com.invora.service;

import com.invora.dto.ProductRequest;
import com.invora.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(UUID id);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    List<ProductResponse> searchProductsByName(String name);

    ProductResponse getProductBySku(String sku);

    ProductResponse deactivateProduct(UUID id);
}
