package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.WarehouseRequest;
import com.warehouse.management.dto.WarehouseResponse;
import com.warehouse.management.service.BusinessExcelService;
import com.warehouse.management.service.WarehouseService;
import com.warehouse.management.util.ExcelResponseUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    private final BusinessExcelService businessExcelService;

    public WarehouseController(WarehouseService warehouseService, BusinessExcelService businessExcelService) {
        this.warehouseService = warehouseService;
        this.businessExcelService = businessExcelService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WarehouseResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(warehouseService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getById(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ExcelResponseUtil.workbook("warehouses.xlsx", businessExcelService.exportWarehouses(keyword, status));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return ExcelResponseUtil.workbook("warehouse-import-template.xlsx", businessExcelService.warehouseTemplate());
    }

    @PostMapping("/import")
    public ApiResponse<ExcelImportResultResponse> importWarehouses(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(businessExcelService.importWarehouses(file));
    }

    @PostMapping
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return ApiResponse.success(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WarehouseResponse> update(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
        return ApiResponse.success(warehouseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ApiResponse.success();
    }
}
