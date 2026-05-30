package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password length must be between 6 and 100 characters")
        String password
) {
}
