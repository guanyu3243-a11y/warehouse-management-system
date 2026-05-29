package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.service.StockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StockResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean lowStockOnly
    ) {
        return ApiResponse.success(stockService.page(page, size, warehouseId, categoryId, keyword, lowStockOnly));
    }

    @GetMapping("/low")
    public ApiResponse<PageResponse<StockResponse>> lowStock(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(stockService.page(page, size, warehouseId, categoryId, keyword, true));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<StockResponse>> getByProductId(@PathVariable Long productId) {
        return ApiResponse.success(stockService.getByProductId(productId));
    }
}
