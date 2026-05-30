package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
        @NotBlank(message = "Permission code is required")
        @Size(max = 100, message = "Permission code must be at most 100 characters")
        String code,

        @NotBlank(message = "Permission name is required")
        @Size(max = 100, message = "Permission name must be at most 100 characters")
        String name,

        @NotBlank(message = "Permission type is required")
        @Size(max = 20, message = "Permission type must be at most 20 characters")
        String type,

        @NotBlank(message = "Permission module is required")
        @Size(max = 50, message = "Permission module must be at most 50 characters")
        String module,

        @Size(max = 255, message = "Path must be at most 255 characters")
        String path,

        @Size(max = 10, message = "Method must be at most 10 characters")
        String method,

        Integer sortOrder,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
