package com.warehouse.management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StockInResponse(
        Long id,
        String stockInNo,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        Long operatorId,
        Integer totalQuantity,
        BigDecimal totalAmount,
        String status,
        String remark,
        List<StockInItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
