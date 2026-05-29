package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long userId,
        String module,
        String action,
        String method,
        String requestUri,
        String requestIp,
        String description,
        LocalDateTime createdAt
) {
}
