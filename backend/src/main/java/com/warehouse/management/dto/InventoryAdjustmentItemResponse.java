package com.warehouse.management.dto;

public record InventoryAdjustmentItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantityBefore,
        Integer adjustQuantity,
        Integer quantityAfter,
        String remark
) {
}
