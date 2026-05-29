package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        String description,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
