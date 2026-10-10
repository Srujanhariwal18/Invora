package com.invora.service;

import com.invora.dto.SupplierRequest;
import com.invora.dto.SupplierResponse;

import java.util.List;
import java.util.UUID;

public interface SupplierService {

    SupplierResponse createSupplier(SupplierRequest request);

    List<SupplierResponse> getAllSuppliers();

    SupplierResponse getSupplierById(UUID id);

    SupplierResponse updateSupplier(UUID id, SupplierRequest request);

    List<SupplierResponse> searchSuppliersByName(String name);

    void deleteSupplier(UUID id);
}
