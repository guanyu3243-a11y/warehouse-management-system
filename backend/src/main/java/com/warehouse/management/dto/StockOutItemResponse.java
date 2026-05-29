package com.warehouse.management.dto;

import java.math.BigDecimal;

public record StockOutItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal unitSalePrice,
        BigDecimal amount,
        String remark
) {
}
