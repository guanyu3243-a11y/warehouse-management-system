package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.SupplierRequest;
import com.warehouse.management.dto.SupplierResponse;
import com.warehouse.management.service.BusinessExcelService;
import com.warehouse.management.service.SupplierService;
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
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    private final BusinessExcelService businessExcelService;

    public SupplierController(SupplierService supplierService, BusinessExcelService businessExcelService) {
        this.supplierService = supplierService;
        this.businessExcelService = businessExcelService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SupplierResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(supplierService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(supplierService.getById(id));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ExcelResponseUtil.workbook("suppliers.xlsx", businessExcelService.exportSuppliers(keyword, status));
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() {
        return ExcelResponseUtil.workbook("supplier-import-template.xlsx", businessExcelService.supplierTemplate());
    }

    @PostMapping("/import")
    public ApiResponse<ExcelImportResultResponse> importSuppliers(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(businessExcelService.importSuppliers(file));
    }

    @PostMapping
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.success(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.success(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.success();
    }
}
