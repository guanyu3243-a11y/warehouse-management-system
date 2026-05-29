package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.DashboardSummaryResponse;
import com.warehouse.management.dto.DashboardTrendItemResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        return ApiResponse.success(dashboardService.summary());
    }

    @GetMapping("/stock-trend")
    public ApiResponse<List<DashboardTrendItemResponse>> stockTrend(
            @RequestParam(defaultValue = "7") int days
    ) {
        return ApiResponse.success(dashboardService.stockTrend(days));
    }

    @GetMapping("/low-stock-top")
    public ApiResponse<List<StockResponse>> lowStockTop(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(dashboardService.lowStockTop(limit));
    }
}
