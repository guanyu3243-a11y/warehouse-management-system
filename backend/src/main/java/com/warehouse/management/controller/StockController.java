package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.StockPageResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.service.BusinessExcelService;
import com.warehouse.management.service.StockService;
import com.warehouse.management.util.ExcelResponseUtil;
import org.springframework.http.ResponseEntity;
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

    private final BusinessExcelService businessExcelService;

    public StockController(StockService stockService, BusinessExcelService businessExcelService) {
        this.stockService = stockService;
        this.businessExcelService = businessExcelService;
    }

    @GetMapping
    public ApiResponse<StockPageResponse> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String productSize
    ) {
        return ApiResponse.success(stockService.page(page, pageSize, warehouseId, categoryId, keyword, lowStockOnly, color, productSize));
    }

    @GetMapping("/low")
    public ApiResponse<StockPageResponse> lowStock(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String productSize
    ) {
        return ApiResponse.success(stockService.page(page, pageSize, warehouseId, categoryId, keyword, true, color, productSize));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String productSize
    ) {
        return ExcelResponseUtil.workbook(
                "stock.xlsx",
                businessExcelService.exportStock(warehouseId, categoryId, keyword, lowStockOnly, color, productSize)
        );
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<StockResponse>> getByProductId(@PathVariable Long productId) {
        return ApiResponse.success(stockService.getByProductId(productId));
    }
}
