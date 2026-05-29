package com.warehouse.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Supplier code is required")
        @Size(max = 50, message = "Supplier code must be at most 50 characters")
        String code,

        @NotBlank(message = "Supplier name is required")
        @Size(max = 150, message = "Supplier name must be at most 150 characters")
        String name,

        @Size(max = 50, message = "Contact name must be at most 50 characters")
        String contactName,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @Email(message = "Email format is invalid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
