package com.warehouse.management.service;

import com.warehouse.management.dto.DashboardSummaryResponse;
import com.warehouse.management.dto.DashboardTrendItemResponse;
import com.warehouse.management.dto.StockResponse;

import java.util.List;

public interface DashboardService {

    DashboardSummaryResponse summary();

    List<DashboardTrendItemResponse> stockTrend(int days);

    List<StockResponse> lowStockTop(int limit);
}
