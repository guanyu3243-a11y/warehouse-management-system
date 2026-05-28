package com.warehouse.management.dto;

public record AuthUserResponse(
        Long id,
        String username,
        String role,
        String status
) {
}
