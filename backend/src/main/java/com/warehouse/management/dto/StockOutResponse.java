package com.warehouse.management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StockOutResponse(
        Long id,
        String stockOutNo,
        Long warehouseId,
        String warehouseName,
        Long operatorId,
        Integer totalQuantity,
        BigDecimal totalAmount,
        String status,
        String remark,
        List<StockOutItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
