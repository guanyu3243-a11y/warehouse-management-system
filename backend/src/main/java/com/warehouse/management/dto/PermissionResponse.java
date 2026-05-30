package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String type,
        String module,
        String path,
        String method,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
