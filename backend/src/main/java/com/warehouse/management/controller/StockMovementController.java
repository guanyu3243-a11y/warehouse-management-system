package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementResponse;
import com.warehouse.management.service.StockMovementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StockMovementResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String sourceNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime
    ) {
        return ApiResponse.success(stockMovementService.page(
                page,
                size,
                productId,
                warehouseId,
                movementType,
                sourceType,
                sourceNo,
                startTime,
                endTime
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<StockMovementResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(stockMovementService.getById(id));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<StockMovementResponse>> getByProductId(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(stockMovementService.getByProductId(productId, page, size));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ApiResponse<PageResponse<StockMovementResponse>> getByWarehouseId(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return ApiResponse.success(stockMovementService.getByWarehouseId(warehouseId, page, size));
    }
}
