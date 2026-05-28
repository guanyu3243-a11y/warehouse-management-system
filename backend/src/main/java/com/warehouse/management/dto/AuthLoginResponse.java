package com.warehouse.management.dto;

public record AuthLoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
}
