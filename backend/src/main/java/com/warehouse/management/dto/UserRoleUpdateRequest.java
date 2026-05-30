package com.warehouse.management.dto;

import java.util.List;

public record UserRoleUpdateRequest(
        List<Long> roleIds
) {
}
