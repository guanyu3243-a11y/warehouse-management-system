package com.warehouse.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserStatusUpdateRequest(
        @NotBlank(message = "Status is required")
        @Size(max = 20, message = "Status must be at most 20 characters")
        String status
) {
}
