package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank(message = "Warehouse code is required")
        @Size(max = 50, message = "Warehouse code must be at most 50 characters")
        String code,

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 100, message = "Warehouse name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Size(max = 50, message = "Contact name must be at most 50 characters")
        String contactName,

        @Size(max = 30, message = "Contact phone must be at most 30 characters")
        String contactPhone,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
