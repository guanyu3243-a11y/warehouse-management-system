package com.warehouse.management.dto;

import java.util.List;

public record StockPageResponse(
        List<StockResponse> records,
        long total,
        long page,
        long size,
        StockSummaryResponse summary
) {
}
