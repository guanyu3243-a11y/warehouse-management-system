package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.ProductRequest;
import com.warehouse.management.dto.ProductResponse;
import com.warehouse.management.dto.StockInResponse;
import com.warehouse.management.dto.StockMovementResponse;
import com.warehouse.management.dto.StockOutResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.dto.SupplierRequest;
import com.warehouse.management.dto.SupplierResponse;
import com.warehouse.management.dto.WarehouseRequest;
import com.warehouse.management.dto.WarehouseResponse;
import com.warehouse.management.service.BusinessExcelService;
import com.warehouse.management.service.ExcelService;
import com.warehouse.management.service.ProductService;
import com.warehouse.management.service.StockInService;
import com.warehouse.management.service.StockMovementService;
import com.warehouse.management.service.StockOutService;
import com.warehouse.management.service.StockService;
import com.warehouse.management.service.SupplierService;
import com.warehouse.management.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessExcelServiceImpl implements BusinessExcelService {

    private static final long EXPORT_PAGE = 1L;

    private static final long EXPORT_SIZE = 10000L;

    private static final List<String> PRODUCT_HEADERS = List.of(
            "sku",
            "name",
            "categoryId",
            "size",
            "color",
            "brand",
            "season",
            "costPrice",
            "salePrice",
            "lowStockThreshold",
            "status"
    );

    private static final List<String> WAREHOUSE_HEADERS = List.of(
            "code",
            "name",
            "address",
            "contactName",
            "contactPhone",
            "status"
    );

    private static final List<String> SUPPLIER_HEADERS = List.of(
            "code",
            "name",
            "contactName",
            "phone",
            "email",
            "address",
            "status"
    );

    private final ExcelService excelService;

    private final ProductService productService;

    private final WarehouseService warehouseService;

    private final SupplierService supplierService;

    private final StockService stockService;

    private final StockMovementService stockMovementService;

    private final StockInService stockInService;

    private final StockOutService stockOutService;

    public BusinessExcelServiceImpl(
            ExcelService excelService,
            ProductService productService,
            WarehouseService warehouseService,
            SupplierService supplierService,
            StockService stockService,
            StockMovementService stockMovementService,
            StockInService stockInService,
            StockOutService stockOutService
    ) {
        this.excelService = excelService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.supplierService = supplierService;
        this.stockService = stockService;
        this.stockMovementService = stockMovementService;
        this.stockInService = stockInService;
        this.stockOutService = stockOutService;
    }

    @Override
    public byte[] productTemplate() {
        return excelService.writeWorkbook("products", PRODUCT_HEADERS, List.of());
    }

    @Override
    public byte[] exportProducts(String keyword, Long categoryId, String brand, String season, String status) {
        PageResponse<ProductResponse> page = productService.page(
                EXPORT_PAGE,
                EXPORT_SIZE,
                keyword,
                categoryId,
                brand,
                season,
                status
        );
        var rows = page.records().stream()
                .map(product -> List.of(
                        value(product.sku()),
                        value(product.name()),
                        value(product.categoryId()),
                        value(product.size()),
                        value(product.color()),
                        value(product.brand()),
                        value(product.season()),
                        value(product.costPrice()),
                        value(product.salePrice()),
                        value(product.lowStockThreshold()),
                        value(product.status()),
                        value(product.createdAt()),
                        value(product.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("products", withAuditHeaders(PRODUCT_HEADERS), rows);
    }

    @Override
    public ExcelImportResultResponse importProducts(MultipartFile file) {
        return importRows(file, row -> productService.create(new ProductRequest(
                required(row, "sku"),
                required(row, "name"),
                requiredLong(row, "categoryId"),
                optional(row, "size"),
                optional(row, "color"),
                optional(row, "brand"),
                optional(row, "season"),
                optionalBigDecimal(row, "costPrice", BigDecimal.ZERO),
                optionalBigDecimal(row, "salePrice", BigDecimal.ZERO),
                optionalInteger(row, "lowStockThreshold", 0),
                optional(row, "status", "ACTIVE")
        )));
    }

    @Override
    public byte[] warehouseTemplate() {
        return excelService.writeWorkbook("warehouses", WAREHOUSE_HEADERS, List.of());
    }

    @Override
    public byte[] exportWarehouses(String keyword, String status) {
        PageResponse<WarehouseResponse> page = warehouseService.page(EXPORT_PAGE, EXPORT_SIZE, keyword, status);
        var rows = page.records().stream()
                .map(warehouse -> List.of(
                        value(warehouse.code()),
                        value(warehouse.name()),
                        value(warehouse.address()),
                        value(warehouse.contactName()),
                        value(warehouse.contactPhone()),
                        value(warehouse.status()),
                        value(warehouse.createdAt()),
                        value(warehouse.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("warehouses", withAuditHeaders(WAREHOUSE_HEADERS), rows);
    }

    @Override
    public ExcelImportResultResponse importWarehouses(MultipartFile file) {
        return importRows(file, row -> warehouseService.create(new WarehouseRequest(
                required(row, "code"),
                required(row, "name"),
                optional(row, "address"),
                optional(row, "contactName"),
                optional(row, "contactPhone"),
                optional(row, "status", "ACTIVE")
        )));
    }

    @Override
    public byte[] supplierTemplate() {
        return excelService.writeWorkbook("suppliers", SUPPLIER_HEADERS, List.of());
    }

    @Override
    public byte[] exportSuppliers(String keyword, String status) {
        PageResponse<SupplierResponse> page = supplierService.page(EXPORT_PAGE, EXPORT_SIZE, keyword, status);
        var rows = page.records().stream()
                .map(supplier -> List.of(
                        value(supplier.code()),
                        value(supplier.name()),
                        value(supplier.contactName()),
                        value(supplier.phone()),
                        value(supplier.email()),
                        value(supplier.address()),
                        value(supplier.status()),
                        value(supplier.createdAt()),
                        value(supplier.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("suppliers", withAuditHeaders(SUPPLIER_HEADERS), rows);
    }

    @Override
    public ExcelImportResultResponse importSuppliers(MultipartFile file) {
        return importRows(file, row -> supplierService.create(new SupplierRequest(
                required(row, "code"),
                required(row, "name"),
                optional(row, "contactName"),
                optional(row, "phone"),
                optional(row, "email"),
                optional(row, "address"),
                optional(row, "status", "ACTIVE")
        )));
    }

    @Override
    public byte[] exportStock(Long warehouseId, Long categoryId, String keyword, Boolean lowStockOnly) {
        PageResponse<StockResponse> page = stockService.page(
                EXPORT_PAGE,
                EXPORT_SIZE,
                warehouseId,
                categoryId,
                keyword,
                lowStockOnly
        );
        List<String> headers = List.of(
                "id",
                "productId",
                "sku",
                "productName",
                "categoryId",
                "warehouseId",
                "warehouseName",
                "quantity",
                "lockedQuantity",
                "availableQuantity",
                "lowStockThreshold",
                "lowStock",
                "createdAt",
                "updatedAt"
        );
        var rows = page.records().stream()
                .map(stock -> List.of(
                        value(stock.id()),
                        value(stock.productId()),
                        value(stock.sku()),
                        value(stock.productName()),
                        value(stock.categoryId()),
                        value(stock.warehouseId()),
                        value(stock.warehouseName()),
                        value(stock.quantity()),
                        value(stock.lockedQuantity()),
                        value(stock.availableQuantity()),
                        value(stock.lowStockThreshold()),
                        value(stock.lowStock()),
                        value(stock.createdAt()),
                        value(stock.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("stock", headers, rows);
    }

    @Override
    public byte[] exportStockMovements(
            Long productId,
            Long warehouseId,
            String movementType,
            String sourceType,
            String sourceNo,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        PageResponse<StockMovementResponse> page = stockMovementService.page(
                EXPORT_PAGE,
                EXPORT_SIZE,
                productId,
                warehouseId,
                movementType,
                sourceType,
                sourceNo,
                startTime,
                endTime
        );
        List<String> headers = List.of(
                "id",
                "movementNo",
                "productId",
                "sku",
                "productName",
                "warehouseId",
                "warehouseName",
                "movementType",
                "sourceType",
                "sourceId",
                "sourceNo",
                "quantityBefore",
                "changeQuantity",
                "quantityAfter",
                "operatorId",
                "operatorUsername",
                "remark",
                "createdAt"
        );
        var rows = page.records().stream()
                .map(movement -> List.of(
                        value(movement.id()),
                        value(movement.movementNo()),
                        value(movement.productId()),
                        value(movement.productSku()),
                        value(movement.productName()),
                        value(movement.warehouseId()),
                        value(movement.warehouseName()),
                        value(movement.movementType()),
                        value(movement.sourceType()),
                        value(movement.sourceId()),
                        value(movement.sourceNo()),
                        value(movement.quantityBefore()),
                        value(movement.changeQuantity()),
                        value(movement.quantityAfter()),
                        value(movement.operatorId()),
                        value(movement.operatorUsername()),
                        value(movement.remark()),
                        value(movement.createdAt())
                ))
                .toList();
        return excelService.writeWorkbook("stock_movements", headers, rows);
    }

    @Override
    public byte[] exportStockIn(String status, Long warehouseId, Long supplierId) {
        PageResponse<StockInResponse> page = stockInService.page(EXPORT_PAGE, EXPORT_SIZE, status, warehouseId, supplierId);
        List<String> headers = List.of(
                "id",
                "stockInNo",
                "warehouseId",
                "warehouseName",
                "supplierId",
                "supplierName",
                "operatorId",
                "totalQuantity",
                "totalAmount",
                "status",
                "remark",
                "createdAt",
                "updatedAt"
        );
        var rows = page.records().stream()
                .map(stockIn -> List.of(
                        value(stockIn.id()),
                        value(stockIn.stockInNo()),
                        value(stockIn.warehouseId()),
                        value(stockIn.warehouseName()),
                        value(stockIn.supplierId()),
                        value(stockIn.supplierName()),
                        value(stockIn.operatorId()),
                        value(stockIn.totalQuantity()),
                        value(stockIn.totalAmount()),
                        value(stockIn.status()),
                        value(stockIn.remark()),
                        value(stockIn.createdAt()),
                        value(stockIn.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("stock_in", headers, rows);
    }

    @Override
    public byte[] exportStockOut(String status, Long warehouseId) {
        PageResponse<StockOutResponse> page = stockOutService.page(EXPORT_PAGE, EXPORT_SIZE, status, warehouseId);
        List<String> headers = List.of(
                "id",
                "stockOutNo",
                "warehouseId",
                "warehouseName",
                "operatorId",
                "totalQuantity",
                "totalAmount",
                "status",
                "remark",
                "createdAt",
                "updatedAt"
        );
        var rows = page.records().stream()
                .map(stockOut -> List.of(
                        value(stockOut.id()),
                        value(stockOut.stockOutNo()),
                        value(stockOut.warehouseId()),
                        value(stockOut.warehouseName()),
                        value(stockOut.operatorId()),
                        value(stockOut.totalQuantity()),
                        value(stockOut.totalAmount()),
                        value(stockOut.status()),
                        value(stockOut.remark()),
                        value(stockOut.createdAt()),
                        value(stockOut.updatedAt())
                ))
                .toList();
        return excelService.writeWorkbook("stock_out", headers, rows);
    }

    private ExcelImportResultResponse importRows(MultipartFile file, ImportAction action) {
        List<ExcelService.ExcelRow> rows = excelService.readRows(file);
        int successCount = 0;
        List<ExcelImportResultResponse.ExcelImportFailure> failures = new ArrayList<>();
        for (ExcelService.ExcelRow row : rows) {
            try {
                action.importRow(row);
                successCount++;
            } catch (RuntimeException e) {
                failures.add(new ExcelImportResultResponse.ExcelImportFailure(row.rowNumber(), failureMessage(e)));
            }
        }
        return new ExcelImportResultResponse(rows.size(), successCount, failures.size(), failures);
    }

    private List<String> withAuditHeaders(List<String> headers) {
        List<String> values = new ArrayList<>(headers);
        values.add("createdAt");
        values.add("updatedAt");
        return values;
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private String required(ExcelService.ExcelRow row, String key) {
        String value = optional(row, key);
        if (value == null) {
            throw BusinessException.badRequest(key + " is required");
        }
        return value;
    }

    private String optional(ExcelService.ExcelRow row, String key) {
        String value = row.values().get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String optional(ExcelService.ExcelRow row, String key, String defaultValue) {
        String value = optional(row, key);
        return value == null ? defaultValue : value;
    }

    private Long requiredLong(ExcelService.ExcelRow row, String key) {
        String value = required(row, key);
        try {
            return Long.valueOf(trimDecimalSuffix(value));
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(key + " must be a number");
        }
    }

    private Integer optionalInteger(ExcelService.ExcelRow row, String key, Integer defaultValue) {
        String value = optional(row, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(trimDecimalSuffix(value));
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(key + " must be a number");
        }
    }

    private BigDecimal optionalBigDecimal(ExcelService.ExcelRow row, String key, BigDecimal defaultValue) {
        String value = optional(row, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(key + " must be a decimal");
        }
    }

    private String trimDecimalSuffix(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith(".0") ? trimmed.substring(0, trimmed.length() - 2) : trimmed;
    }

    private String failureMessage(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    @FunctionalInterface
    private interface ImportAction {

        void importRow(ExcelService.ExcelRow row);
    }
}
