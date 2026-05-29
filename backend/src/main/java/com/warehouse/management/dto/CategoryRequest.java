package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must be at most 100 characters")
        String name,

        @NotBlank(message = "Category code is required")
        @Size(max = 50, message = "Category code must be at most 50 characters")
        String code,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        Integer sortOrder,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
