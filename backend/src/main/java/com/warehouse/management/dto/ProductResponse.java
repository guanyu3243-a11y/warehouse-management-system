package com.warehouse.management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        Long categoryId,
        String size,
        String color,
        String brand,
        String season,
        BigDecimal costPrice,
        BigDecimal salePrice,
        Integer lowStockThreshold,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
