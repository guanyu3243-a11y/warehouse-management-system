package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockMovementResponse;

import java.time.LocalDateTime;

public interface StockMovementService {

    PageResponse<StockMovementResponse> page(
            long page,
            long size,
            Long productId,
            Long warehouseId,
            String movementType,
            String sourceType,
            String sourceNo,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    StockMovementResponse getById(Long id);

    PageResponse<StockMovementResponse> getByProductId(Long productId, long page, long size);

    PageResponse<StockMovementResponse> getByWarehouseId(Long warehouseId, long page, long size);

    void record(StockMovementRecordCommand command);
}
