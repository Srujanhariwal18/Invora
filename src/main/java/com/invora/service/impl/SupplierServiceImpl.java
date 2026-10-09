package com.invora.service.impl;

import com.invora.dto.SupplierRequest;
import com.invora.dto.SupplierResponse;
import com.invora.entity.Supplier;
import com.invora.exception.ConflictException;
import com.invora.exception.ResourceNotFoundException;
import com.invora.repository.ProductRepository;
import com.invora.repository.SupplierRepository;
import com.invora.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository, ProductRepository productRepository) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        String name = request.name().trim();
        String contactPerson = request.contactPerson() != null && !request.contactPerson().isBlank()
                ? request.contactPerson().trim() : null;
        String email = request.email() != null && !request.email().isBlank()
                ? request.email().trim() : null;
        String phone = request.phone() != null && !request.phone().isBlank()
                ? request.phone().trim() : null;
        String address = request.address() != null && !request.address().isBlank()
                ? request.address().trim() : null;

        Supplier supplier = new Supplier(name, contactPerson, email, phone, address);
        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponse.fromEntity(saved);
    }

    @Override
    public List<SupplierResponse> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    public SupplierResponse getSupplierById(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return SupplierResponse.fromEntity(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(UUID id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson() != null && !request.contactPerson().isBlank()
                ? request.contactPerson().trim() : null);
        supplier.setEmail(request.email() != null && !request.email().isBlank()
                ? request.email().trim() : null);
        supplier.setPhone(request.phone() != null && !request.phone().isBlank()
                ? request.phone().trim() : null);
        supplier.setAddress(request.address() != null && !request.address().isBlank()
                ? request.address().trim() : null);

        Supplier updated = supplierRepository.save(supplier);
        return SupplierResponse.fromEntity(updated);
    }

    @Override
    public List<SupplierResponse> searchSuppliersByName(String name) {
        if (name == null || name.isBlank()) {
            return getAllSuppliers();
        }
        return supplierRepository.findByNameContainingIgnoreCase(name.trim())
                .stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSupplier(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        if (productRepository.existsBySupplierId(id)) {
            throw new ConflictException("Cannot delete supplier with id '" + id + "' because it is referenced by existing products");
        }

        supplierRepository.delete(supplier);
    }
}
