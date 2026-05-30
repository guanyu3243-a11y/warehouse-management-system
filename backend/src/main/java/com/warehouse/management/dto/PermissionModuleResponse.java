package com.warehouse.management.dto;

import java.util.List;

public record PermissionModuleResponse(
        String module,
        List<PermissionResponse> permissions
) {
}
