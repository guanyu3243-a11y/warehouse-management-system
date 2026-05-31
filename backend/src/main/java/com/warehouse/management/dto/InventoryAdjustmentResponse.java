package com.warehouse.management.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryAdjustmentResponse(
        Long id,
        String adjustmentNo,
        Long warehouseId,
        String warehouseName,
        Long operatorId,
        Integer totalAdjustQuantity,
        String reason,
        String status,
        String remark,
        List<InventoryAdjustmentItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
