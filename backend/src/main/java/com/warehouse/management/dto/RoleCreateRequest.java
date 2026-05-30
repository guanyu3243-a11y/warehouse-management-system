package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank(message = "Role code is required")
        @Size(max = 50, message = "Role code must be at most 50 characters")
        String code,

        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
