package com.warehouse.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 80, message = "SKU must be at most 80 characters")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String name,

        @NotNull(message = "Category id is required")
        Long categoryId,

        @Size(max = 30, message = "Size must be at most 30 characters")
        String size,

        @Size(max = 50, message = "Color must be at most 50 characters")
        String color,

        @Size(max = 100, message = "Brand must be at most 100 characters")
        String brand,

        @Size(max = 50, message = "Season must be at most 50 characters")
        String season,

        @DecimalMin(value = "0.00", message = "Cost price must be greater than or equal to 0")
        BigDecimal costPrice,

        @DecimalMin(value = "0.00", message = "Sale price must be greater than or equal to 0")
        BigDecimal salePrice,

        @Min(value = 0, message = "Low stock threshold must be greater than or equal to 0")
        Integer lowStockThreshold,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
