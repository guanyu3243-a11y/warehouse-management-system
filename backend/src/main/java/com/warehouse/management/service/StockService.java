package com.warehouse.management.service;

import com.warehouse.management.dto.StockPageResponse;
import com.warehouse.management.dto.StockResponse;

import java.util.List;

public interface StockService {

    StockPageResponse page(
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
