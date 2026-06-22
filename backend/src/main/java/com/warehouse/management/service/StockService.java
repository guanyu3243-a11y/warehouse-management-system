package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockResponse;

import java.util.List;

public interface StockService {

    PageResponse<StockResponse> page(
            long page,
            long pageSize,
            Long warehouseId,
            Long categoryId,
            String keyword,
            Boolean lowStockOnly,
            String color,
            String productSize
    );

    List<StockResponse> getByProductId(Long productId);
}
