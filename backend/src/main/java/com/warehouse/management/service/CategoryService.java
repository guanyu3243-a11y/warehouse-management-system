package com.warehouse.management.service;

import com.warehouse.management.dto.CategoryRequest;
import com.warehouse.management.dto.CategoryResponse;
import com.warehouse.management.dto.PageResponse;

public interface CategoryService {

    PageResponse<CategoryResponse> page(long page, long size, String keyword, String status);

    CategoryResponse getById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
