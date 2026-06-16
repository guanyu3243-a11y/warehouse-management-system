package com.warehouse.management.dto;

public record CompanyProductImportResponse(
        String batchNo,
        Long categoryId,
        String categoryName,
        Long warehouseId,
        String warehouseName,
        int specificationCount,
        int createdProductCount,
        int reusedProductCount,
        int createdStockCount,
        int updatedStockCount,
        int unchangedStockCount,
        int zeroStockCount
) {
}
