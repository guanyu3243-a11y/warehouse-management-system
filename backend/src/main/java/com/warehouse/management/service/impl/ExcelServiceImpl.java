package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.service.ExcelService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ExcelServiceImpl implements ExcelService {

    private static final String CONTENT_TYPE_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
            """;

    private static final String ROOT_RELS_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """;

    private static final String WORKBOOK_RELS_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
            """;

    private static final String STYLES_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
              <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
              <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
              <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
              <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
            </styleSheet>
            """;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] writeWorkbook(String sheetName, List<String> headers, List<? extends List<?>> rows) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            writeEntry(zipOutputStream, "[Content_Types].xml", CONTENT_TYPE_XML);
            writeEntry(zipOutputStream, "_rels/.rels", ROOT_RELS_XML);
            writeEntry(zipOutputStream, "xl/workbook.xml", workbookXml(sheetName));
            writeEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML);
            writeEntry(zipOutputStream, "xl/styles.xml", STYLES_XML);
            writeEntry(zipOutputStream, "xl/worksheets/sheet1.xml", sheetXml(headers, rows));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw BusinessException.badRequest("Failed to create Excel file");
        }
    }

    @Override
    public List<ExcelRow> readRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Excel file is required");
        }

        try {
            Map<String, byte[]> entries = readZipEntries(file.getBytes());
            byte[] sheetBytes = entries.get("xl/worksheets/sheet1.xml");
            if (sheetBytes == null) {
                throw BusinessException.badRequest("Excel sheet is missing");
            }

            List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
            List<ParsedRow> parsedRows = parseSheet(sheetBytes, sharedStrings);
            if (parsedRows.isEmpty()) {
                return List.of();
            }

            List<String> headers = parsedRows.get(0).values().stream()
                    .map(String::trim)
                    .toList();
            List<ExcelRow> rows = new ArrayList<>();
            for (int index = 1; index < parsedRows.size(); index++) {
                ParsedRow parsedRow = parsedRows.get(index);
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                    String header = headers.get(columnIndex);
                    if (header.isEmpty()) {
                        continue;
                    }
                    String value = columnIndex < parsedRow.values().size() ? parsedRow.values().get(columnIndex).trim() : "";
                    if (!value.isEmpty()) {
                        hasValue = true;
                    }
                    values.put(header, value);
                }
                if (hasValue) {
                    rows.add(new ExcelRow(parsedRow.rowNumber(), values));
                }
            }
            return rows;
        } catch (IOException e) {
            throw BusinessException.badRequest("Failed to read Excel file");
        }
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String workbookXml(String sheetName) {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="%s" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """.formatted(escapeAttribute(sheetName));
    }

    private String sheetXml(List<String> headers, List<? extends List<?>> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        writeRow(builder, 1, new ArrayList<>(headers));
        int rowIndex = 2;
        for (List<?> row : rows) {
            writeRow(builder, rowIndex, row);
            rowIndex++;
        }
        builder.append("""
                  </sheetData>
                </worksheet>
                """);
        return builder.toString();
    }

    private void writeRow(StringBuilder builder, int rowIndex, List<?> values) {
        builder.append("<row r=\"").append(rowIndex).append("\">");
        for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
            String cellReference = columnName(columnIndex + 1) + rowIndex;
            builder.append("<c r=\"").append(cellReference).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escapeText(toCellText(values.get(columnIndex))))
                    .append("</t></is></c>");
        }
        builder.append("</row>");
    }

    private String toCellText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return DATE_TIME_FORMATTER.format(localDateTime);
        }
        return String.valueOf(value);
    }

    private String columnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int value = columnIndex;
        while (value > 0) {
            value--;
            builder.insert(0, (char) ('A' + value % 26));
            value /= 26;
        }
        return builder.toString();
    }

    private Map<String, byte[]> readZipEntries(byte[] content) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zipInputStream.readAllBytes());
                }
            }
        }
        return entries;
    }

    private List<String> parseSharedStrings(byte[] bytes) {
        if (bytes == null) {
            return List.of();
        }
        Document document = parseXml(bytes);
        NodeList nodes = document.getElementsByTagNameNS("*", "si");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            values.add(nodes.item(index).getTextContent());
        }
        return values;
    }

    private List<ParsedRow> parseSheet(byte[] bytes, List<String> sharedStrings) {
        Document document = parseXml(bytes);
        NodeList rowNodes = document.getElementsByTagNameNS("*", "row");
        List<ParsedRow> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
            Element rowElement = (Element) rowNodes.item(rowIndex);
            int rowNumber = parseInteger(rowElement.getAttribute("r"), rowIndex + 1);
            NodeList cellNodes = rowElement.getElementsByTagNameNS("*", "c");
            List<String> values = new ArrayList<>();
            int fallbackColumnIndex = 0;
            for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                Element cellElement = (Element) cellNodes.item(cellIndex);
                int columnIndex = columnIndex(cellElement.getAttribute("r"), fallbackColumnIndex);
                while (values.size() <= columnIndex) {
                    values.add("");
                }
                values.set(columnIndex, cellValue(cellElement, sharedStrings));
                fallbackColumnIndex = columnIndex + 1;
            }
            rows.add(new ParsedRow(rowNumber, values));
        }
        return rows;
    }

    private Document parseXml(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw BusinessException.badRequest("Invalid Excel file");
        }
    }

    private String cellValue(Element cellElement, List<String> sharedStrings) {
        String type = cellElement.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return firstText(cellElement, "t");
        }

        String rawValue = firstText(cellElement, "v");
        if ("s".equals(type) && !rawValue.isEmpty()) {
            int sharedStringIndex = parseInteger(rawValue, -1);
            if (sharedStringIndex >= 0 && sharedStringIndex < sharedStrings.size()) {
                return sharedStrings.get(sharedStringIndex);
            }
        }
        return rawValue;
    }

    private String firstText(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent();
    }

    private int columnIndex(String cellReference, int fallback) {
        if (cellReference == null || cellReference.isBlank()) {
            return fallback;
        }
        int value = 0;
        for (int index = 0; index < cellReference.length(); index++) {
            char current = cellReference.charAt(index);
            if (!Character.isLetter(current)) {
                break;
            }
            value = value * 26 + Character.toUpperCase(current) - 'A' + 1;
        }
        return value == 0 ? fallback : value - 1;
    }

    private int parseInteger(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String escapeText(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;");
    }

    private record ParsedRow(Integer rowNumber, List<String> values) {
    }
}
