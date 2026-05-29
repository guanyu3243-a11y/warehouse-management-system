package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record SupplierResponse(
        Long id,
        String code,
        String name,
        String contactName,
        String phone,
        String email,
        String address,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
