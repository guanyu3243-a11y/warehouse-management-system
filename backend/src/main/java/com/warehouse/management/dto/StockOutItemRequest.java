package com.warehouse.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockOutItemRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity,

        @DecimalMin(value = "0.00", message = "Unit sale price must be greater than or equal to 0")
        BigDecimal unitSalePrice,

        @Size(max = 255, message = "Remark must be at most 255 characters")
        String remark
) {
}
