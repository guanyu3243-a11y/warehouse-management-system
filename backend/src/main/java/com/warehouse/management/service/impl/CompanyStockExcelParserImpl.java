package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.service.CompanyStockExcelParser;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CompanyStockExcelParserImpl implements CompanyStockExcelParser {

    @Override
    public List<CompanyStockRow> parse(MultipartFile file) {
        validateFile(file);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            HeaderLocation header = findHeader(workbook, formatter, evaluator);
            return parseRows(header, formatter, evaluator);
        } catch (BusinessException exception) {
            throw exception;
        } catch (EncryptedDocumentException exception) {
            throw BusinessException.badRequest("Encrypted Excel files are not supported");
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest("Failed to read company stock Excel file");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Excel file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw BusinessException.badRequest("Excel filename is required");
        }
        String normalized = filename.trim().toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".xls") && !normalized.endsWith(".xlsx")) {
            throw BusinessException.badRequest("Only .xls and .xlsx files are supported");
        }
    }

    private HeaderLocation findHeader(
            Workbook workbook,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                if ("型号".equals(cellText(row, 0, formatter, evaluator))
                        && "颜色".equals(cellText(row, 1, formatter, evaluator))) {
                    Map<String, Integer> sizeColumns = new LinkedHashMap<>();
                    Integer totalColumn = null;
                    for (int columnIndex = 2; columnIndex < row.getLastCellNum(); columnIndex++) {
                        String header = normalizeSize(cellText(row, columnIndex, formatter, evaluator));
                        if (isTotalHeader(header)) {
                            totalColumn = columnIndex;
                            break;
                        }
                        if (isSizeHeader(header) && !sizeColumns.containsKey(header)) {
                            sizeColumns.put(header, columnIndex);
                        }
                    }

                    if (!sizeColumns.isEmpty() && totalColumn != null) {
                        return new HeaderLocation(
                                sheet,
                                rowIndex,
                                sizeColumns,
                                totalColumn,
                                TemplateType.COMPANY_STOCK,
                                null
                        );
                    }
                }

                if ("名称".equals(cellText(row, 0, formatter, evaluator))) {
                    Map<String, Integer> schoolSizeColumns = new LinkedHashMap<>();
                    for (int columnIndex = 1; columnIndex < row.getLastCellNum(); columnIndex++) {
                        String size = normalizeSize(cellText(row, columnIndex, formatter, evaluator));
                        if (isTotalHeader(size)) {
                            break;
                        }
                        if (isSizeHeader(size) && !schoolSizeColumns.containsKey(size)) {
                            schoolSizeColumns.put(size, columnIndex);
                        }
                    }

                    if (!schoolSizeColumns.isEmpty()) {
                        return new HeaderLocation(
                                sheet,
                                rowIndex,
                                schoolSizeColumns,
                                null,
                                TemplateType.SCHOOL_UNIFORM_STOCK,
                                findSchoolUniformName(sheet, rowIndex, formatter, evaluator)
                        );
                    }
                }
            }
        }
        throw BusinessException.badRequest(
                "Company stock template header was not found; expected 型号/颜色 or 名称 with size columns"
        );
    }

    private List<CompanyStockRow> parseRows(
            HeaderLocation header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        if (header.type() == TemplateType.SCHOOL_UNIFORM_STOCK) {
            return parseSchoolUniformRows(header, formatter, evaluator);
        }

        List<CompanyStockRow> result = new ArrayList<>();
        String currentModel = null;

        for (int rowIndex = header.headerRowIndex() + 1;
             rowIndex <= header.sheet().getLastRowNum();
             rowIndex++) {
            Row row = header.sheet().getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String modelCell = cellText(row, 0, formatter, evaluator);
            String color = cellText(row, 1, formatter, evaluator);
            if (isSummary(modelCell)) {
                continue;
            }
            if (modelCell.isBlank() && color.isBlank() && quantitiesAreBlank(row, header, formatter, evaluator)) {
                continue;
            }
            if (!modelCell.isBlank()) {
                currentModel = modelCell;
            }
            if (currentModel == null || currentModel.isBlank()) {
                throw rowError(rowIndex, "Model is required");
            }
            if (color.isBlank()) {
                throw rowError(rowIndex, "Color is required");
            }

            int total = 0;
            List<CompanyStockRow> rowItems = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : header.sizeColumns().entrySet()) {
                String size = entry.getKey();
                int quantity = parseQuantity(
                        cellText(row, entry.getValue(), formatter, evaluator),
                        rowIndex,
                        size,
                        true
                );
                total += quantity;
                rowItems.add(new CompanyStockRow(rowIndex + 1, currentModel, color, size, quantity));
            }

            String totalText = cellText(row, header.totalColumn(), formatter, evaluator);
            if (totalText.isBlank()) {
                throw rowError(rowIndex, "Total quantity is required");
            }
            int declaredTotal = parseQuantity(totalText, rowIndex, "合计", false);
            if (declaredTotal != total) {
                throw BusinessException.badRequest(
                        "第 " + (rowIndex + 1) + " 行总数不一致：尺码数量合计为 " + total
                                + "，但总计列为 " + declaredTotal + "，请检查该行各尺码数量或总计列。"
                );
            }
            result.addAll(rowItems);
        }

        if (result.isEmpty()) {
            throw BusinessException.badRequest("Company stock Excel file contains no inventory rows");
        }
        return result;
    }

    private List<CompanyStockRow> parseSchoolUniformRows(
            HeaderLocation header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        List<CompanyStockRow> result = new ArrayList<>();

        for (int rowIndex = header.headerRowIndex() + 1;
             rowIndex <= header.sheet().getLastRowNum();
             rowIndex++) {
            Row row = header.sheet().getRow(rowIndex);
            if (row == null) {
                continue;
            }

            String name = cellText(row, 0, formatter, evaluator);
            if (isSummary(name)) {
                continue;
            }
            if (name.isBlank() && quantitiesAreBlank(row, header, formatter, evaluator)) {
                continue;
            }
            if (name.isBlank()) {
                throw rowError(rowIndex, "Name is required");
            }

            for (Map.Entry<String, Integer> entry : header.sizeColumns().entrySet()) {
                int quantity = parseQuantity(
                        cellText(row, entry.getValue(), formatter, evaluator),
                        rowIndex,
                        entry.getKey(),
                        true
                );
                result.add(new CompanyStockRow(
                        rowIndex + 1,
                        name,
                        header.defaultColor(),
                        entry.getKey(),
                        quantity
                ));
            }
        }

        if (result.isEmpty()) {
            throw BusinessException.badRequest("Company stock Excel file contains no inventory rows");
        }
        return result;
    }

    private String findSchoolUniformName(
            Sheet sheet,
            int headerRowIndex,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex < headerRowIndex; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                String value = cellText(row, columnIndex, formatter, evaluator);
                if (!value.isBlank() && !value.startsWith("更新日期")) {
                    return value;
                }
            }
        }
        return sheet.getSheetName();
    }

    private boolean quantitiesAreBlank(
            Row row,
            HeaderLocation header,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        return header.sizeColumns().values().stream()
                .allMatch(column -> cellText(row, column, formatter, evaluator).isBlank());
    }

    private int parseQuantity(String text, int zeroBasedRowIndex, String size, boolean blankAsZero) {
        if (text.isBlank()) {
            if (blankAsZero) {
                return 0;
            }
            throw rowError(zeroBasedRowIndex, "Quantity is required for " + size);
        }

        try {
            String normalized = text.replace(",", "").trim();
            BigDecimal value = new BigDecimal(normalized).stripTrailingZeros();
            if (value.scale() > 0 || value.signum() < 0 || value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new NumberFormatException();
            }
            return value.intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw BusinessException.badRequest(
                    "第 " + (zeroBasedRowIndex + 1) + " 行尺码 " + size + " 数量格式错误，请填写数字。"
            );
        }
    }

    private String cellText(
            Row row,
            int columnIndex,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        if (row == null || row.getCell(columnIndex) == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(columnIndex), evaluator)
                .replace('\u00A0', ' ')
                .trim();
    }

    private String normalizeSize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isSizeHeader(String value) {
        if (value == null || value.isBlank() || isTotalHeader(value)) {
            return false;
        }
        String normalized = normalizeSize(value);
        return normalized.matches("XS|S|M|L|XL")
                || normalized.matches("(?:[2-9]|10)XL")
                || normalized.matches("X{2,10}L")
                || normalized.matches("\\d{2,3}#?");
    }

    private boolean isTotalHeader(String value) {
        String normalized = normalizeSize(value);
        return "合计".equals(normalized)
                || "总计".equals(normalized)
                || "总数".equals(normalized)
                || "TOTAL".equals(normalized);
    }

    private boolean isSummary(String model) {
        return "合计".equals(model) || "合计总量".equals(model) || "总计".equals(model) || "总数".equals(model);
    }

    private BusinessException rowError(int zeroBasedRowIndex, String message) {
        return BusinessException.badRequest(message + " at row " + (zeroBasedRowIndex + 1));
    }

    private record HeaderLocation(
            Sheet sheet,
            int headerRowIndex,
            Map<String, Integer> sizeColumns,
            Integer totalColumn,
            TemplateType type,
            String defaultColor
    ) {
    }

    private enum TemplateType {
        COMPANY_STOCK,
        SCHOOL_UNIFORM_STOCK
    }
}
