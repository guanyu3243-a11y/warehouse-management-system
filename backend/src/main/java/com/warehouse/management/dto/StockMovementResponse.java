package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        String movementNo,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseName,
        String movementType,
        String sourceType,
        Long sourceId,
        String sourceNo,
        Integer quantityBefore,
        Integer changeQuantity,
        Integer quantityAfter,
        Long operatorId,
        String operatorUsername,
        String remark,
        LocalDateTime createdAt
) {
}
