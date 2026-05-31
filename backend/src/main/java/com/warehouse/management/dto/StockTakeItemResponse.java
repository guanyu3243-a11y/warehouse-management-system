package com.warehouse.management.dto;

public record StockTakeItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer bookQuantity,
        Integer actualQuantity,
        Integer differenceQuantity,
        String remark
) {
}
