package com.warehouse.management.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockTakeResponse(
        Long id,
        String stockTakeNo,
        Long warehouseId,
        String warehouseName,
        Long operatorId,
        String title,
        Integer totalBookQuantity,
        Integer totalActualQuantity,
        Integer totalDifferenceQuantity,
        String status,
        String remark,
        List<StockTakeItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
