package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.ProductRequest;
import com.warehouse.management.dto.ProductResponse;

public interface ProductService {

    PageResponse<ProductResponse> page(
            long page,
            long size,
            String keyword,
            Long categoryId,
            String brand,
            String season,
            String status
    );

    ProductResponse getById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
