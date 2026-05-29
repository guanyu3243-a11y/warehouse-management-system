package com.warehouse.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StockOutRequest(
        @NotNull(message = "Warehouse id is required")
        Long warehouseId,

        @Size(max = 255, message = "Remark must be at most 255 characters")
        String remark,

        @Valid
        @NotEmpty(message = "Stock-out items are required")
        List<StockOutItemRequest> items
) {
}
