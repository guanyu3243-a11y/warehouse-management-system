package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockOutRequest;
import com.warehouse.management.dto.StockOutResponse;

public interface StockOutService {

    PageResponse<StockOutResponse> page(long page, long size, String status, Long warehouseId);

    StockOutResponse getById(Long id);

    StockOutResponse create(StockOutRequest request);

    StockOutResponse update(Long id, StockOutRequest request);

    StockOutResponse confirm(Long id);

    StockOutResponse cancel(Long id);
}
