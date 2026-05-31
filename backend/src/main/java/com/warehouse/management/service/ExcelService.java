package com.warehouse.management.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ExcelService {

    byte[] writeWorkbook(String sheetName, List<String> headers, List<? extends List<?>> rows);

    List<ExcelRow> readRows(MultipartFile file);

    record ExcelRow(
            Integer rowNumber,
            Map<String, String> values
    ) {
    }
}
