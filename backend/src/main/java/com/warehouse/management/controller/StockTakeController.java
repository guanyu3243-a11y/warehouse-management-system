package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockTakeRequest;
import com.warehouse.management.dto.StockTakeResponse;
import com.warehouse.management.service.StockTakeService;
import com.warehouse.management.util.ExcelResponseUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stock-takes")
public class StockTakeController {

    private final StockTakeService stockTakeService;

    public StockTakeController(StockTakeService stockTakeService) {
        this.stockTakeService = stockTakeService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StockTakeResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ApiResponse.success(stockTakeService.page(page, size, status, warehouseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StockTakeResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(stockTakeService.getById(id));
    }

    @PostMapping
    public ApiResponse<StockTakeResponse> create(@Valid @RequestBody StockTakeRequest request) {
        return ApiResponse.success(stockTakeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StockTakeResponse> update(@PathVariable Long id, @Valid @RequestBody StockTakeRequest request) {
        return ApiResponse.success(stockTakeService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<StockTakeResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(stockTakeService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<StockTakeResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(stockTakeService.cancel(id));
    }

    @PostMapping("/{id}/import")
    public ApiResponse<ExcelImportResultResponse> importItems(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(stockTakeService.importItems(id, file));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportItems(@PathVariable Long id) {
        return ExcelResponseUtil.workbook("stock-take-" + id + ".xlsx", stockTakeService.exportItems(id));
    }
}
