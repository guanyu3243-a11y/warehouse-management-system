package com.warehouse.management.dto;

public record DashboardSummaryResponse(
        Long productTotal,
        Long categoryTotal,
        Long warehouseTotal,
        Long supplierTotal,
        Integer totalStockQuantity,
        Long lowStockItemCount,
        Integer todayStockInQuantity,
        Integer todayStockOutQuantity
) {
}
