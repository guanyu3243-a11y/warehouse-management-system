package com.warehouse.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InventoryAdjustmentRequest(
        @NotNull(message = "Warehouse id is required")
        Long warehouseId,

        @NotBlank(message = "Reason is required")
        @Size(max = 100, message = "Reason must be at most 100 characters")
        String reason,

        @Size(max = 255, message = "Remark must be at most 255 characters")
        String remark,

        @Valid
        @NotEmpty(message = "Inventory adjustment items are required")
        List<InventoryAdjustmentItemRequest> items
) {
}
