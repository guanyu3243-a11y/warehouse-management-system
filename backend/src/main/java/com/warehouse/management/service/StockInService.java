package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockInRequest;
import com.warehouse.management.dto.StockInResponse;

public interface StockInService {

    PageResponse<StockInResponse> page(long page, long size, String status, Long warehouseId, Long supplierId);

    StockInResponse getById(Long id);

    StockInResponse create(StockInRequest request);

    StockInResponse update(Long id, StockInRequest request);

    StockInResponse confirm(Long id);

    StockInResponse cancel(Long id);
}
