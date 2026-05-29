package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        String contactName,
        String contactPhone,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
