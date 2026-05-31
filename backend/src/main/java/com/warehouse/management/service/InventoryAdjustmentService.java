package com.warehouse.management.service;

import com.warehouse.management.dto.InventoryAdjustmentRequest;
import com.warehouse.management.dto.InventoryAdjustmentResponse;
import com.warehouse.management.dto.PageResponse;

public interface InventoryAdjustmentService {

    PageResponse<InventoryAdjustmentResponse> page(long page, long size, String status, Long warehouseId);

    InventoryAdjustmentResponse getById(Long id);

    InventoryAdjustmentResponse create(InventoryAdjustmentRequest request);

    InventoryAdjustmentResponse update(Long id, InventoryAdjustmentRequest request);

    InventoryAdjustmentResponse confirm(Long id);

    InventoryAdjustmentResponse cancel(Long id);
}
