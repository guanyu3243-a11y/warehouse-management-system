package com.warehouse.management.dto;

import java.util.List;

public record UserWarehousePermissionUpdateRequest(
        List<Long> warehouseIds
) {
}
