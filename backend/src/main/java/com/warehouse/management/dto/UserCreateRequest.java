package com.warehouse.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must be at most 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password length must be between 6 and 100 characters")
        String password,

        @Size(max = 30, message = "Role must be at most 30 characters")
        String role,

        @Size(max = 20, message = "Status must be at most 20 characters")
        String status,

        @Email(message = "Email format is invalid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone
) {
}
