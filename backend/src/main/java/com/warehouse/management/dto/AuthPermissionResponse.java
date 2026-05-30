package com.warehouse.management.dto;

import java.util.List;

public record AuthPermissionResponse(
        List<String> roles,
        List<String> permissions,
        List<Long> warehouseIds
) {
}
