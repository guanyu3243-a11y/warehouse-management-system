package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockOutRequest;
import com.warehouse.management.dto.StockOutResponse;
import com.warehouse.management.service.BusinessExcelService;
import com.warehouse.management.service.StockOutService;
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

@RestController
@RequestMapping("/api/stock-out")
public class StockOutController {

    private final StockOutService stockOutService;

    private final BusinessExcelService businessExcelService;

    public StockOutController(StockOutService stockOutService, BusinessExcelService businessExcelService) {
        this.stockOutService = stockOutService;
        this.businessExcelService = businessExcelService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StockOutResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ApiResponse.success(stockOutService.page(page, size, status, warehouseId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ExcelResponseUtil.workbook(
                "stock-out.xlsx",
                businessExcelService.exportStockOut(status, warehouseId)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<StockOutResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(stockOutService.getById(id));
    }

    @PostMapping
    public ApiResponse<StockOutResponse> create(@Valid @RequestBody StockOutRequest request) {
        return ApiResponse.success(stockOutService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StockOutResponse> update(@PathVariable Long id, @Valid @RequestBody StockOutRequest request) {
        return ApiResponse.success(stockOutService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<StockOutResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(stockOutService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<StockOutResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(stockOutService.cancel(id));
    }
}
