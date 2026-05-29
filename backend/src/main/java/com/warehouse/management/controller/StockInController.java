package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockInRequest;
import com.warehouse.management.dto.StockInResponse;
import com.warehouse.management.service.StockInService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-in")
public class StockInController {

    private final StockInService stockInService;

    public StockInController(StockInService stockInService) {
        this.stockInService = stockInService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StockInResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId
    ) {
        return ApiResponse.success(stockInService.page(page, size, status, warehouseId, supplierId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StockInResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(stockInService.getById(id));
    }

    @PostMapping
    public ApiResponse<StockInResponse> create(@Valid @RequestBody StockInRequest request) {
        return ApiResponse.success(stockInService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StockInResponse> update(@PathVariable Long id, @Valid @RequestBody StockInRequest request) {
        return ApiResponse.success(stockInService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<StockInResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(stockInService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<StockInResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(stockInService.cancel(id));
    }
}
