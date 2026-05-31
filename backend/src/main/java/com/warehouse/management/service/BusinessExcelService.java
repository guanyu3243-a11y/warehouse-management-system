package com.warehouse.management.service;

import com.warehouse.management.dto.ExcelImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public interface BusinessExcelService {

    byte[] productTemplate();

    byte[] exportProducts(String keyword, Long categoryId, String brand, String season, String status);

    ExcelImportResultResponse importProducts(MultipartFile file);

    byte[] warehouseTemplate();

    byte[] exportWarehouses(String keyword, String status);

    ExcelImportResultResponse importWarehouses(MultipartFile file);

    byte[] supplierTemplate();

    byte[] exportSuppliers(String keyword, String status);

    ExcelImportResultResponse importSuppliers(MultipartFile file);

    byte[] exportStock(Long warehouseId, Long categoryId, String keyword, Boolean lowStockOnly);

    byte[] exportStockMovements(
            Long productId,
            Long warehouseId,
            String movementType,
            String sourceType,
            String sourceNo,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    byte[] exportStockIn(String status, Long warehouseId, Long supplierId);

    byte[] exportStockOut(String status, Long warehouseId);
}
