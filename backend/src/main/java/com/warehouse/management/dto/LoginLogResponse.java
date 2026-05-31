package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record LoginLogResponse(
        Long id,
        Long userId,
        String username,
        Boolean success,
        String failureReason,
        String requestIp,
        String userAgent,
        LocalDateTime createdAt
) {
}
