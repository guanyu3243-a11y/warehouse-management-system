package com.warehouse.management.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String role,
        String status,
        String email,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
