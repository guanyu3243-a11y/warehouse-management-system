package com.warehouse.management.service;

import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockTakeRequest;
import com.warehouse.management.dto.StockTakeResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StockTakeService {

    PageResponse<StockTakeResponse> page(long page, long size, String status, Long warehouseId);

    StockTakeResponse getById(Long id);

    StockTakeResponse create(StockTakeRequest request);

    StockTakeResponse update(Long id, StockTakeRequest request);

    StockTakeResponse confirm(Long id);

    StockTakeResponse cancel(Long id);

    ExcelImportResultResponse importItems(Long id, MultipartFile file);

    byte[] exportItems(Long id);
}
