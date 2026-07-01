package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockOutItemRequest;
import com.warehouse.management.dto.StockOutItemResponse;
import com.warehouse.management.dto.StockOutRequest;
import com.warehouse.management.dto.StockOutResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockOut;
import com.warehouse.management.entity.StockOutItem;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutItemMapper;
import com.warehouse.management.mapper.StockOutMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.StockMovementService;
import com.warehouse.management.service.StockOutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockOutServiceImpl implements StockOutService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DRAFT_STATUS = "DRAFT";

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private static final String CANCELLED_STATUS = "CANCELLED";

    private static final DateTimeFormatter DOCUMENT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final StockOutMapper stockOutMapper;

    private final StockOutItemMapper stockOutItemMapper;

    private final StockMapper stockMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final StockMovementService stockMovementService;

    public StockOutServiceImpl(
            StockOutMapper stockOutMapper,
            StockOutItemMapper stockOutItemMapper,
            StockMapper stockMapper,
            ProductMapper productMapper,
            WarehouseMapper warehouseMapper,
            StockMovementService stockMovementService
    ) {
        this.stockOutMapper = stockOutMapper;
        this.stockOutItemMapper = stockOutItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockMovementService = stockMovementService;
    }

    @Override
    public PageResponse<StockOutResponse> page(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<StockOut> query = Wrappers.lambdaQuery();
        query.eq(hasText(status), StockOut::getStatus, status == null ? null : status.trim());
        query.eq(warehouseId != null, StockOut::getWarehouseId, warehouseId);
        query.orderByDesc(StockOut::getCreatedAt);

        IPage<StockOut> result = stockOutMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(stockOut -> toResponse(stockOut, false)).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public StockOutResponse getById(Long id) {
        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockOutResponse create(StockOutRequest request) {
        validateRequest(request);

        StockOut stockOut = new StockOut();
        stockOut.setStockOutNo(generateDocumentNo());
        stockOut.setOperatorId(CurrentUserContext.getRequired().id());
        stockOut.setStatus(DRAFT_STATUS);
        applyRequest(stockOut, request);
        stockOutMapper.insert(stockOut);

        saveItems(stockOut.getId(), request.items());
        return toResponse(stockOut, true);
    }

    @Override
    @Transactional
    public StockOutResponse update(Long id, StockOutRequest request) {
        StockOut stockOut = getExisting(id);
        ensureDraft(stockOut);
        validateRequest(request);

        applyRequest(stockOut, request);
        stockOutMapper.updateById(stockOut);
        stockOutItemMapper.delete(Wrappers.<StockOutItem>lambdaQuery().eq(StockOutItem::getStockOutId, id));
        saveItems(id, request.items());
        return toResponse(stockOut, true);
    }

    @Override
    @Transactional
    public StockOutResponse confirm(Long id) {
        StockOut stockOut = getExisting(id);
        ensureDraft(stockOut);

        List<StockOutItem> items = getItems(id);
        if (items.isEmpty()) {
            throw BusinessException.badRequest("Stock-out document has no items");
        }

        markConfirmedIfDraft(id);
        for (StockOutItem item : items) {
            decreaseStock(stockOut, item);
        }

        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockOutResponse cancel(Long id) {
        StockOut stockOut = getExisting(id);
        ensureDraft(stockOut);

        markCancelledIfDraft(id);
        return toResponse(getExisting(id), true);
    }

    private void decreaseStock(StockOut stockOut, StockOutItem item) {
        Stock stock = stockMapper.selectByProductAndWarehouseForUpdate(item.getProductId(), stockOut.getWarehouseId());
        int quantityBefore = stock == null ? 0 : defaultInt(stock.getQuantity());
        int changeQuantity = -item.getQuantity();
        int quantityAfter = quantityBefore + changeQuantity;

        if (stock == null || quantityAfter < 0) {
            throw BusinessException.badRequest("Insufficient stock for product id " + item.getProductId());
        }

        int updated = stockMapper.decreaseQuantityByIdIfEnough(stock.getId(), item.getQuantity());
        if (updated != 1) {
            throw BusinessException.badRequest("Insufficient stock for product id " + item.getProductId());
        }

        stockMovementService.record(new StockMovementRecordCommand(
                item.getProductId(),
                stockOut.getWarehouseId(),
                "STOCK_OUT",
                "STOCK_OUT",
                stockOut.getId(),
                stockOut.getStockOutNo(),
                quantityBefore,
                changeQuantity,
                quantityAfter,
                stockOut.getOperatorId(),
                movementRemark(stockOut.getRemark(), item.getRemark())
        ));
    }

    private void markConfirmedIfDraft(Long id) {
        int updated = stockOutMapper.update(
                null,
                Wrappers.<StockOut>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CONFIRMED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock-out document has already been processed");
        }
    }

    private void markCancelledIfDraft(Long id) {
        int updated = stockOutMapper.update(
                null,
                Wrappers.<StockOut>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CANCELLED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock-out document has already been processed");
        }
    }

    private void validateRequest(StockOutRequest request) {
        Warehouse warehouse = warehouseMapper.selectById(request.warehouseId());
        if (warehouse == null || !ACTIVE_STATUS.equals(warehouse.getStatus())) {
            throw BusinessException.badRequest("Warehouse does not exist or is disabled");
        }

        for (StockOutItemRequest item : request.items()) {
            Product product = productMapper.selectById(item.productId());
            if (product == null || !ACTIVE_STATUS.equals(product.getStatus())) {
                throw BusinessException.badRequest("Product does not exist or is disabled");
            }
        }
    }

    private void applyRequest(StockOut stockOut, StockOutRequest request) {
        stockOut.setWarehouseId(request.warehouseId());
        stockOut.setRemark(trimToNull(request.remark()));
        stockOut.setTotalQuantity(totalQuantity(request.items()));
        stockOut.setTotalAmount(totalAmount(request.items()));
    }

    private void saveItems(Long stockOutId, List<StockOutItemRequest> itemRequests) {
        for (StockOutItemRequest request : itemRequests) {
            StockOutItem item = new StockOutItem();
            item.setStockOutId(stockOutId);
            item.setProductId(request.productId());
            item.setQuantity(request.quantity());
            item.setUnitSalePrice(defaultMoney(request.unitSalePrice()));
            item.setAmount(defaultMoney(request.unitSalePrice()).multiply(BigDecimal.valueOf(request.quantity())));
            item.setRemark(trimToNull(request.remark()));
            stockOutItemMapper.insert(item);
        }
    }

    private StockOut getExisting(Long id) {
        StockOut stockOut = stockOutMapper.selectById(id);
        if (stockOut == null) {
            throw BusinessException.notFound("Stock-out document does not exist");
        }
        return stockOut;
    }

    private void ensureDraft(StockOut stockOut) {
        if (!DRAFT_STATUS.equals(stockOut.getStatus())) {
            throw BusinessException.badRequest("Only draft stock-out documents can be changed");
        }
    }

    private List<StockOutItem> getItems(Long stockOutId) {
        return stockOutItemMapper.selectList(
                Wrappers.<StockOutItem>lambdaQuery().eq(StockOutItem::getStockOutId, stockOutId)
        );
    }

    private StockOutResponse toResponse(StockOut stockOut, boolean includeItems) {
        Warehouse warehouse = warehouseMapper.selectById(stockOut.getWarehouseId());
        List<StockOutItemResponse> items = includeItems ? toItemResponses(getItems(stockOut.getId())) : List.of();

        return new StockOutResponse(
                stockOut.getId(),
                stockOut.getStockOutNo(),
                stockOut.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                stockOut.getOperatorId(),
                stockOut.getTotalQuantity(),
                stockOut.getTotalAmount(),
                stockOut.getStatus(),
                stockOut.getRemark(),
                items,
                stockOut.getCreatedAt(),
                stockOut.getUpdatedAt()
        );
    }

    private List<StockOutItemResponse> toItemResponses(List<StockOutItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = productMapper.selectBatchIds(
                        items.stream().map(StockOutItem::getProductId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return new StockOutItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product == null ? null : product.getSku(),
                            product == null ? null : product.getName(),
                            item.getQuantity(),
                            item.getUnitSalePrice(),
                            item.getAmount(),
                            item.getRemark()
                    );
                })
                .toList();
    }

    private String generateDocumentNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "SO" + LocalDateTime.now().format(DOCUMENT_NO_TIME_FORMAT) + suffix;
    }

    private Integer totalQuantity(List<StockOutItemRequest> items) {
        return items.stream().mapToInt(StockOutItemRequest::quantity).sum();
    }

    private BigDecimal totalAmount(List<StockOutItemRequest> items) {
        return items.stream()
                .map(item -> defaultMoney(item.unitSalePrice()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
        return com.warehouse.management.util.PaginationSupport.normalizeSize(size);
    }
}
