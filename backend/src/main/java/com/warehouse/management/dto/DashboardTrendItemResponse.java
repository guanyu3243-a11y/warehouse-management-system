package com.warehouse.management.dto;

public record DashboardTrendItemResponse(
        String date,
        Integer stockInQuantity,
        Integer stockOutQuantity
) {
}
