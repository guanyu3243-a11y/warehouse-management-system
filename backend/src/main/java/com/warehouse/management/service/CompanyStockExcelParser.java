package com.warehouse.management.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CompanyStockExcelParser {

    List<CompanyStockRow> parse(MultipartFile file);

    record CompanyStockRow(
            int rowNumber,
            String model,
            String color,
            String size,
            int quantity
    ) {
    }
}
