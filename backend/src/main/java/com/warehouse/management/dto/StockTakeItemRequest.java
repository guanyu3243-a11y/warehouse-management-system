package com.warehouse.management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockTakeItemRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Actual quantity is required")
        @Min(value = 0, message = "Actual quantity must be greater than or equal to 0")
        Integer actualQuantity,

        @Size(max = 255, message = "Remark must be at most 255 characters")
        String remark
) {
}
