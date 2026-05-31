package com.warehouse.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentItemRequest(
        @NotNull(message = "Product id is required")
        Long productId,

        @NotNull(message = "Adjust quantity is required")
        Integer adjustQuantity,

        @Size(max = 255, message = "Remark must be at most 255 characters")
        String remark
) {
}
