package com.warehouse.management.dto;

import java.math.BigDecimal;

public record StockInItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal amount,
        String remark
) {
}
