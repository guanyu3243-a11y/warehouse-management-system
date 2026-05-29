package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record StockResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Long categoryId,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer lockedQuantity,
        Integer availableQuantity,
        Integer lowStockThreshold,
        Boolean lowStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
