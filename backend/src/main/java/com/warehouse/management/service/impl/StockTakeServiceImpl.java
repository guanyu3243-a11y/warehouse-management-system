package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.ExcelImportResultResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockTakeItemRequest;
import com.warehouse.management.dto.StockTakeItemResponse;
import com.warehouse.management.dto.StockTakeRequest;
import com.warehouse.management.dto.StockTakeResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockTake;
import com.warehouse.management.entity.StockTakeItem;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockTakeItemMapper;
import com.warehouse.management.mapper.StockTakeMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.ExcelService;
import com.warehouse.management.service.StockMovementService;
import com.warehouse.management.service.StockTakeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockTakeServiceImpl implements StockTakeService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DRAFT_STATUS = "DRAFT";

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private static final String CANCELLED_STATUS = "CANCELLED";

    private static final DateTimeFormatter DOCUMENT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final List<String> STOCK_TAKE_EXPORT_HEADERS = List.of(
            "productId",
            "sku",
            "productName",
            "bookQuantity",
            "actualQuantity",
            "differenceQuantity",
            "remark"
    );

    private final StockTakeMapper stockTakeMapper;

    private final StockTakeItemMapper stockTakeItemMapper;

    private final StockMapper stockMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final StockMovementService stockMovementService;

    private final ExcelService excelService;

    public StockTakeServiceImpl(
            StockTakeMapper stockTakeMapper,
            StockTakeItemMapper stockTakeItemMapper,
            StockMapper stockMapper,
            ProductMapper productMapper,
            WarehouseMapper warehouseMapper,
            StockMovementService stockMovementService,
            ExcelService excelService
    ) {
        this.stockTakeMapper = stockTakeMapper;
        this.stockTakeItemMapper = stockTakeItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockMovementService = stockMovementService;
        this.excelService = excelService;
    }

    @Override
    public PageResponse<StockTakeResponse> page(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<StockTake> query = Wrappers.lambdaQuery();
        query.eq(hasText(status), StockTake::getStatus, status == null ? null : status.trim());
        query.eq(warehouseId != null, StockTake::getWarehouseId, warehouseId);
        query.orderByDesc(StockTake::getCreatedAt);

        IPage<StockTake> result = stockTakeMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(stockTake -> toResponse(stockTake, false)).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public StockTakeResponse getById(Long id) {
        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockTakeResponse create(StockTakeRequest request) {
        validateRequest(request);

        StockTake stockTake = new StockTake();
        stockTake.setStockTakeNo(generateDocumentNo());
        stockTake.setOperatorId(CurrentUserContext.getRequired().id());
        stockTake.setStatus(DRAFT_STATUS);
        stockTake.setWarehouseId(request.warehouseId());
        stockTake.setTitle(request.title().trim());
        stockTake.setRemark(trimToNull(request.remark()));
        stockTake.setTotalBookQuantity(0);
        stockTake.setTotalActualQuantity(0);
        stockTake.setTotalDifferenceQuantity(0);
        stockTakeMapper.insert(stockTake);

        Totals totals = replaceItems(stockTake.getId(), stockTake.getWarehouseId(), request.items());
        applyTotals(stockTake, totals);
        stockTakeMapper.updateById(stockTake);
        return toResponse(stockTake, true);
    }

    @Override
    @Transactional
    public StockTakeResponse update(Long id, StockTakeRequest request) {
        StockTake stockTake = getExisting(id);
        ensureDraft(stockTake);
        validateRequest(request);

        stockTake.setWarehouseId(request.warehouseId());
        stockTake.setTitle(request.title().trim());
        stockTake.setRemark(trimToNull(request.remark()));
        Totals totals = replaceItems(id, request.warehouseId(), request.items());
        applyTotals(stockTake, totals);
        stockTakeMapper.updateById(stockTake);
        return toResponse(stockTake, true);
    }

    @Override
    @Transactional
    public StockTakeResponse confirm(Long id) {
        StockTake stockTake = getExisting(id);
        ensureDraft(stockTake);

        List<StockTakeItem> items = getItems(id);
        if (items.isEmpty()) {
            throw BusinessException.badRequest("Stock take has no items");
        }

        markConfirmedIfDraft(id);
        for (StockTakeItem item : items) {
            applyStockTake(stockTake, item);
        }

        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockTakeResponse cancel(Long id) {
        StockTake stockTake = getExisting(id);
        ensureDraft(stockTake);

        markCancelledIfDraft(id);
        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public ExcelImportResultResponse importItems(Long id, MultipartFile file) {
        StockTake stockTake = getExisting(id);
        ensureDraft(stockTake);

        List<ExcelService.ExcelRow> rows = excelService.readRows(file);
        if (rows.isEmpty()) {
            return new ExcelImportResultResponse(0, 0, 0, List.of());
        }

        List<StockTakeItemRequest> importedItems = new ArrayList<>();
        List<ExcelImportResultResponse.ExcelImportFailure> failures = new ArrayList<>();

        for (ExcelService.ExcelRow row : rows) {
            try {
                importedItems.add(new StockTakeItemRequest(
                        requiredLong(row, "productId"),
                        requiredInteger(row, "actualQuantity"),
                        optional(row, "remark")
                ));
            } catch (RuntimeException e) {
                failures.add(new ExcelImportResultResponse.ExcelImportFailure(row.rowNumber(), failureMessage(e)));
            }
        }

        if (!failures.isEmpty()) {
            return new ExcelImportResultResponse(rows.size(), 0, failures.size(), failures);
        }

        validateItems(importedItems);
        Totals totals = replaceItems(id, stockTake.getWarehouseId(), importedItems);
        applyTotals(stockTake, totals);
        stockTakeMapper.updateById(stockTake);
        return new ExcelImportResultResponse(rows.size(), rows.size(), 0, List.of());
    }

    @Override
    public byte[] exportItems(Long id) {
        StockTakeResponse stockTake = getById(id);
        var rows = stockTake.items().stream()
                .map(item -> List.of(
                        value(item.productId()),
                        value(item.sku()),
                        value(item.productName()),
                        value(item.bookQuantity()),
                        value(item.actualQuantity()),
                        value(item.differenceQuantity()),
                        value(item.remark())
                ))
                .toList();
        return excelService.writeWorkbook("stock_take", STOCK_TAKE_EXPORT_HEADERS, rows);
    }

    private void applyStockTake(StockTake stockTake, StockTakeItem item) {
        Stock stock = stockMapper.selectByProductAndWarehouseForUpdate(
                item.getProductId(),
                stockTake.getWarehouseId()
        );
        int quantityBefore = stock == null ? 0 : defaultInt(stock.getQuantity());
        int bookQuantity = defaultInt(item.getBookQuantity());
        int actualQuantity = defaultInt(item.getActualQuantity());
        int changeQuantity = actualQuantity - bookQuantity;

        if (quantityBefore != bookQuantity) {
            throw BusinessException.badRequest("Stock changed after stock take was saved for product id " + item.getProductId());
        }

        if (stock == null) {
            if (actualQuantity > 0) {
                stock = new Stock();
                stock.setWarehouseId(stockTake.getWarehouseId());
                stock.setProductId(item.getProductId());
                stock.setQuantity(actualQuantity);
                stock.setLockedQuantity(0);
                stock.setVersion(0);
                stockMapper.insert(stock);
            }
        } else {
            int updated = stockMapper.updateQuantityById(stock.getId(), actualQuantity);
            if (updated != 1) {
                throw BusinessException.badRequest("Stock update failed, please retry");
            }
        }

        if (changeQuantity != 0) {
            stockMovementService.record(new StockMovementRecordCommand(
                    item.getProductId(),
                    stockTake.getWarehouseId(),
                    "STOCK_TAKE",
                    "STOCK_TAKE",
                    stockTake.getId(),
                    stockTake.getStockTakeNo(),
                    quantityBefore,
                    changeQuantity,
                    actualQuantity,
                    stockTake.getOperatorId(),
                    movementRemark(stockTake.getRemark(), item.getRemark())
            ));
        }
    }

    private Totals replaceItems(Long stockTakeId, Long warehouseId, List<StockTakeItemRequest> itemRequests) {
        stockTakeItemMapper.delete(
                Wrappers.<StockTakeItem>lambdaQuery()
                        .eq(StockTakeItem::getStockTakeId, stockTakeId)
        );

        int totalBook = 0;
        int totalActual = 0;
        int totalDifference = 0;

        for (StockTakeItemRequest request : itemRequests) {
            Stock stock = stockMapper.selectByProductAndWarehouseForUpdate(request.productId(), warehouseId);
            int bookQuantity = stock == null ? 0 : defaultInt(stock.getQuantity());
            int actualQuantity = request.actualQuantity();
            int differenceQuantity = actualQuantity - bookQuantity;

            StockTakeItem item = new StockTakeItem();
            item.setStockTakeId(stockTakeId);
            item.setProductId(request.productId());
            item.setBookQuantity(bookQuantity);
            item.setActualQuantity(actualQuantity);
            item.setDifferenceQuantity(differenceQuantity);
            item.setRemark(trimToNull(request.remark()));
            stockTakeItemMapper.insert(item);

            totalBook += bookQuantity;
            totalActual += actualQuantity;
            totalDifference += differenceQuantity;
        }

        return new Totals(totalBook, totalActual, totalDifference);
    }

    private void markConfirmedIfDraft(Long id) {
        int updated = stockTakeMapper.update(
                null,
                Wrappers.<StockTake>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CONFIRMED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock take has already been processed");
        }
    }

    private void markCancelledIfDraft(Long id) {
        int updated = stockTakeMapper.update(
                null,
                Wrappers.<StockTake>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CANCELLED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock take has already been processed");
        }
    }

    private void validateRequest(StockTakeRequest request) {
        Warehouse warehouse = warehouseMapper.selectById(request.warehouseId());
        if (warehouse == null || !ACTIVE_STATUS.equals(warehouse.getStatus())) {
            throw BusinessException.badRequest("Warehouse does not exist or is disabled");
        }

        validateItems(request.items());
    }

    private void validateItems(List<StockTakeItemRequest> items) {
        if (items.isEmpty()) {
            throw BusinessException.badRequest("Stock take items are required");
        }

        Set<Long> productIds = new HashSet<>();
        for (StockTakeItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw BusinessException.badRequest("Duplicate product in stock take: " + item.productId());
            }

            Product product = productMapper.selectById(item.productId());
            if (product == null || !ACTIVE_STATUS.equals(product.getStatus())) {
                throw BusinessException.badRequest("Product does not exist or is disabled");
            }
        }
    }

    private StockTake getExisting(Long id) {
        StockTake stockTake = stockTakeMapper.selectById(id);
        if (stockTake == null) {
            throw BusinessException.notFound("Stock take does not exist");
        }
        return stockTake;
    }

    private void ensureDraft(StockTake stockTake) {
        if (!DRAFT_STATUS.equals(stockTake.getStatus())) {
            throw BusinessException.badRequest("Only draft stock takes can be changed");
        }
    }

    private List<StockTakeItem> getItems(Long stockTakeId) {
        return stockTakeItemMapper.selectList(
                Wrappers.<StockTakeItem>lambdaQuery().eq(StockTakeItem::getStockTakeId, stockTakeId)
        );
    }

    private StockTakeResponse toResponse(StockTake stockTake, boolean includeItems) {
        Warehouse warehouse = warehouseMapper.selectById(stockTake.getWarehouseId());
        List<StockTakeItemResponse> items = includeItems ? toItemResponses(getItems(stockTake.getId())) : List.of();

        return new StockTakeResponse(
                stockTake.getId(),
                stockTake.getStockTakeNo(),
                stockTake.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                stockTake.getOperatorId(),
                stockTake.getTitle(),
                stockTake.getTotalBookQuantity(),
                stockTake.getTotalActualQuantity(),
                stockTake.getTotalDifferenceQuantity(),
                stockTake.getStatus(),
                stockTake.getRemark(),
                items,
                stockTake.getCreatedAt(),
                stockTake.getUpdatedAt()
        );
    }

    private List<StockTakeItemResponse> toItemResponses(List<StockTakeItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = productMapper.selectBatchIds(
                        items.stream().map(StockTakeItem::getProductId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return new StockTakeItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product == null ? null : product.getSku(),
                            product == null ? null : product.getName(),
                            item.getBookQuantity(),
                            item.getActualQuantity(),
                            item.getDifferenceQuantity(),
                            item.getRemark()
                    );
                })
                .toList();
    }

    private void applyTotals(StockTake stockTake, Totals totals) {
        stockTake.setTotalBookQuantity(totals.bookQuantity());
        stockTake.setTotalActualQuantity(totals.actualQuantity());
        stockTake.setTotalDifferenceQuantity(totals.differenceQuantity());
    }

    private Long requiredLong(ExcelService.ExcelRow row, String key) {
        String value = required(row, key);
        try {
            return Long.valueOf(trimDecimalSuffix(value));
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(key + " must be a number");
        }
    }

    private Integer requiredInteger(ExcelService.ExcelRow row, String key) {
        String value = required(row, key);
        try {
            int parsedValue = Integer.parseInt(trimDecimalSuffix(value));
            if (parsedValue < 0) {
                throw BusinessException.badRequest(key + " must be greater than or equal to 0");
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest(key + " must be a number");
        }
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

    private String trimDecimalSuffix(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith(".0") ? trimmed.substring(0, trimmed.length() - 2) : trimmed;
    }

    private String failureMessage(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }

    private String generateDocumentNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ST" + LocalDateTime.now().format(DOCUMENT_NO_TIME_FORMAT) + suffix;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String movementRemark(String documentRemark, String itemRemark) {
        if (hasText(itemRemark)) {
            return itemRemark.trim();
        }
        return trimToNull(documentRemark);
    }

    private long normalizePage(long page) {
        return Math.max(page, 1);
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private record Totals(
            Integer bookQuantity,
            Integer actualQuantity,
            Integer differenceQuantity
    ) {
    }
}
