package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
