package com.invora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
    @NotBlank(message = "Supplier name is required")
    @Size(max = 150, message = "Supplier name must not exceed 150 characters")
    String name,

    @Size(max = 100, message = "Contact person must not exceed 100 characters")
    String contactPerson,

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email,

    @Size(max = 30, message = "Phone must not exceed 30 characters")
    String phone,

    String address
) {
}
