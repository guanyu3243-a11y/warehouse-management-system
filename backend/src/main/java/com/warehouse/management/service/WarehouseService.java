package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.WarehouseRequest;
import com.warehouse.management.dto.WarehouseResponse;

public interface WarehouseService {

    PageResponse<WarehouseResponse> page(long page, long size, String keyword, String status);

    WarehouseResponse getById(Long id);

    WarehouseResponse create(WarehouseRequest request);

    WarehouseResponse update(Long id, WarehouseRequest request);

    void delete(Long id);
}
