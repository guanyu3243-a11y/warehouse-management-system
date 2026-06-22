package com.warehouse.management.dto;

public record StockSummaryResponse(
        long skuCount,
        long totalQuantity,
        long totalLockedQuantity,
        long totalAvailableQuantity
) {
}
