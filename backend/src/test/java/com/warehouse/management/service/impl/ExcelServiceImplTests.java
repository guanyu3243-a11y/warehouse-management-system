package com.warehouse.management.service.impl;

import com.warehouse.management.service.ExcelService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelServiceImplTests {

    private final ExcelService excelService = new ExcelServiceImpl();

    @Test
    void writesAndReadsXlsxRows() {
        byte[] workbook = excelService.writeWorkbook(
                "products",
                List.of("sku", "name", "categoryId"),
                List.of(List.of("SKU-001", "Jacket", 1))
        );

        List<ExcelService.ExcelRow> rows = excelService.readRows(new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbook
        ));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        assertThat(rows.get(0).values())
                .containsEntry("sku", "SKU-001")
                .containsEntry("name", "Jacket")
                .containsEntry("categoryId", "1");
    }
}
