package com.warehouse.management.controller;

import com.warehouse.management.common.ApiResponse;
import com.warehouse.management.dto.InventoryAdjustmentRequest;
import com.warehouse.management.dto.InventoryAdjustmentResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.service.InventoryAdjustmentService;
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
@RequestMapping("/api/inventory-adjustments")
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService inventoryAdjustmentService;

    public InventoryAdjustmentController(InventoryAdjustmentService inventoryAdjustmentService) {
        this.inventoryAdjustmentService = inventoryAdjustmentService;
    }

    @GetMapping
    public ApiResponse<PageResponse<InventoryAdjustmentResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ApiResponse.success(inventoryAdjustmentService.page(page, size, status, warehouseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<InventoryAdjustmentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(inventoryAdjustmentService.getById(id));
    }

    @PostMapping
    public ApiResponse<InventoryAdjustmentResponse> create(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return ApiResponse.success(inventoryAdjustmentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<InventoryAdjustmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryAdjustmentRequest request
    ) {
        return ApiResponse.success(inventoryAdjustmentService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<InventoryAdjustmentResponse> confirm(@PathVariable Long id) {
        return ApiResponse.success(inventoryAdjustmentService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<InventoryAdjustmentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success(inventoryAdjustmentService.cancel(id));
    }
}
