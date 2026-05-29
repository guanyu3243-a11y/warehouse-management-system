package com.warehouse.management.service;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.SupplierRequest;
import com.warehouse.management.dto.SupplierResponse;

public interface SupplierService {

    PageResponse<SupplierResponse> page(long page, long size, String keyword, String status);

    SupplierResponse getById(Long id);

    SupplierResponse create(SupplierRequest request);

    SupplierResponse update(Long id, SupplierRequest request);

    void delete(Long id);
}
