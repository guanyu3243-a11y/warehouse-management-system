package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.ProductRequest;
import com.warehouse.management.dto.ProductResponse;

public interface ProductService {

    PageResponse<ProductResponse> page(
            long page,
            long pageSize,
            String keyword,
            Long categoryId,
            String brand,
            String season,
            String status,
            String color,
            String productSize
    );

    ProductResponse getById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
