package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.service.CompanyStockExcelParser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyStockExcelParserImplTests {

    private static final List<String> SIZES = List.of(
            "XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL", "5XL", "6XL"
    );

    private final CompanyStockExcelParser parser = new CompanyStockExcelParserImpl();

    @Test
    void parsesXlsAndCarriesForwardModelWithBlankQuantityAsZero() throws IOException {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = createTemplate(workbook);
            addDataRow(sheet, 2, "V3", "白色", List.of(0, 42, 87, 2, 1, 4, 71, 36, 27, 28), 298);
            addDataRow(
                    sheet,
                    3,
                    "",
                    "藏青",
                    Arrays.asList(null, 51, 293, 155, 41, 66, 31, 26, 19, 28),
                    710
            );
            addSummaryRow(sheet, 4);

            List<CompanyStockExcelParser.CompanyStockRow> rows = parser.parse(file(workbook, "stock.xls"));

            assertThat(rows).hasSize(20);
            assertThat(find(rows, "V3", "白色", "S").quantity()).isEqualTo(42);
            assertThat(find(rows, "V3", "藏青", "XS").quantity()).isZero();
            assertThat(find(rows, "V3", "藏青", "M").quantity()).isEqualTo(293);
        }
    }

    @Test
    void parsesXlsxWithTheSameCompanyTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = createTemplate(workbook);
            addDataRow(sheet, 2, "V6", "黑色", List.of(0, 4, 137, 72, 33, 69, 105, 0, 21, 19), 460);

            List<CompanyStockExcelParser.CompanyStockRow> rows = parser.parse(file(workbook, "stock.xlsx"));

            assertThat(rows).hasSize(10);
            assertThat(find(rows, "V6", "黑色", "3XL").quantity()).isEqualTo(105);
        }
    }

    @Test
    void parsesSchoolUniformInventoryMatrix() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("校服库存");
            sheet.createRow(0).createCell(1).setCellValue("校服赣");
            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("名称");
            header.createCell(1).setCellValue("115#");
            header.createCell(2).setCellValue("120#");
            header.createCell(3).setCellValue("125#");
            Row row = sheet.createRow(4);
            row.createCell(0).setCellValue("短袖T恤");
            row.createCell(1).setCellValue(20);
            row.createCell(2).setCellValue(154);

            List<CompanyStockExcelParser.CompanyStockRow> rows = parser.parse(file(workbook, "school-stock.xlsx"));

            assertThat(rows).hasSize(3);
            assertThat(find(rows, "短袖T恤", "校服赣", "115#").quantity()).isEqualTo(20);
            assertThat(find(rows, "短袖T恤", "校服赣", "120#").quantity()).isEqualTo(154);
            assertThat(find(rows, "短袖T恤", "校服赣", "125#").quantity()).isZero();
        }
    }

    @Test
    void rejectsRowWhenTotalDoesNotMatchSizeQuantities() throws IOException {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = createTemplate(workbook);
            addDataRow(sheet, 2, "V3", "白色", List.of(0, 42, 87, 2, 1, 4, 71, 36, 27, 28), 999);

            assertThatThrownBy(() -> parser.parse(file(workbook, "invalid.xls")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Row total does not match")
                    .hasMessageContaining("row 3");
        }
    }

    private Sheet createTemplate(Workbook workbook) {
        Sheet sheet = workbook.createSheet("2026.6.5");
        sheet.createRow(0).createCell(0).setCellValue("剩余数量统计表(6月5日更新)");
        Row header = sheet.createRow(1);
        header.createCell(0).setCellValue("型号");
        header.createCell(1).setCellValue("颜色");
        for (int index = 0; index < SIZES.size(); index++) {
            header.createCell(index + 2).setCellValue(SIZES.get(index));
        }
        header.createCell(12).setCellValue("合计");
        return sheet;
    }

    private void addDataRow(
            Sheet sheet,
            int rowIndex,
            String model,
            String color,
            List<Integer> quantities,
            int total
    ) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(model);
        row.createCell(1).setCellValue(color);
        for (int index = 0; index < quantities.size(); index++) {
            Integer quantity = quantities.get(index);
            if (quantity != null) {
                row.createCell(index + 2).setCellValue(quantity);
            }
        }
        row.createCell(12).setCellValue(total);
    }

    private void addSummaryRow(Sheet sheet, int rowIndex) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue("合计总量");
        row.createCell(12).setCellValue(1008);
    }

    private MockMultipartFile file(Workbook workbook, String filename) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.ms-excel",
                output.toByteArray()
        );
    }

    private CompanyStockExcelParser.CompanyStockRow find(
            List<CompanyStockExcelParser.CompanyStockRow> rows,
            String model,
            String color,
            String size
    ) {
        return rows.stream()
                .filter(row -> model.equals(row.model()))
                .filter(row -> color.equals(row.color()))
                .filter(row -> size.equals(row.size()))
                .findFirst()
                .orElseThrow();
    }
}
