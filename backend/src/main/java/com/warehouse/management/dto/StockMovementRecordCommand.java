package com.warehouse.management.dto;

public record StockMovementRecordCommand(
        Long productId,
        Long warehouseId,
        String movementType,
        String sourceType,
        Long sourceId,
        String sourceNo,
        Integer quantityBefore,
        Integer changeQuantity,
        Integer quantityAfter,
        Long operatorId,
        String remark
) {
}
