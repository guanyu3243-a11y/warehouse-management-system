package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockInItemRequest;
import com.warehouse.management.dto.StockInItemResponse;
import com.warehouse.management.dto.StockInRequest;
import com.warehouse.management.dto.StockInResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockIn;
import com.warehouse.management.entity.StockInItem;
import com.warehouse.management.entity.Supplier;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockInItemMapper;
import com.warehouse.management.mapper.StockInMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.SupplierMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.StockInService;
import com.warehouse.management.service.StockMovementService;
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
public class StockInServiceImpl implements StockInService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DRAFT_STATUS = "DRAFT";

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private static final String CANCELLED_STATUS = "CANCELLED";

    private static final DateTimeFormatter DOCUMENT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final StockInMapper stockInMapper;

    private final StockInItemMapper stockInItemMapper;

    private final StockMapper stockMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final SupplierMapper supplierMapper;

    private final StockMovementService stockMovementService;

    public StockInServiceImpl(
            StockInMapper stockInMapper,
            StockInItemMapper stockInItemMapper,
            StockMapper stockMapper,
            ProductMapper productMapper,
            WarehouseMapper warehouseMapper,
            SupplierMapper supplierMapper,
            StockMovementService stockMovementService
    ) {
        this.stockInMapper = stockInMapper;
        this.stockInItemMapper = stockInItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.supplierMapper = supplierMapper;
        this.stockMovementService = stockMovementService;
    }

    @Override
    public PageResponse<StockInResponse> page(long page, long size, String status, Long warehouseId, Long supplierId) {
        LambdaQueryWrapper<StockIn> query = Wrappers.lambdaQuery();
        query.eq(hasText(status), StockIn::getStatus, status == null ? null : status.trim());
        query.eq(warehouseId != null, StockIn::getWarehouseId, warehouseId);
        query.eq(supplierId != null, StockIn::getSupplierId, supplierId);
        query.orderByDesc(StockIn::getCreatedAt);

        IPage<StockIn> result = stockInMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(stockIn -> toResponse(stockIn, false)).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public StockInResponse getById(Long id) {
        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockInResponse create(StockInRequest request) {
        validateRequest(request);

        StockIn stockIn = new StockIn();
        stockIn.setStockInNo(generateDocumentNo());
        stockIn.setOperatorId(CurrentUserContext.getRequired().id());
        stockIn.setStatus(DRAFT_STATUS);
        applyRequest(stockIn, request);
        stockInMapper.insert(stockIn);

        saveItems(stockIn.getId(), request.items());
        return toResponse(stockIn, true);
    }

    @Override
    @Transactional
    public StockInResponse update(Long id, StockInRequest request) {
        StockIn stockIn = getExisting(id);
        ensureDraft(stockIn);
        validateRequest(request);

        applyRequest(stockIn, request);
        stockInMapper.updateById(stockIn);
        stockInItemMapper.delete(Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getStockInId, id));
        saveItems(id, request.items());
        return toResponse(stockIn, true);
    }

    @Override
    @Transactional
    public StockInResponse confirm(Long id) {
        StockIn stockIn = getExisting(id);
        ensureDraft(stockIn);

        List<StockInItem> items = getItems(id);
        if (items.isEmpty()) {
            throw BusinessException.badRequest("Stock-in document has no items");
        }

        markConfirmedIfDraft(id);
        for (StockInItem item : items) {
            increaseStock(stockIn, item);
        }

        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public StockInResponse cancel(Long id) {
        StockIn stockIn = getExisting(id);
        ensureDraft(stockIn);

        markCancelledIfDraft(id);
        return toResponse(getExisting(id), true);
    }

    private void increaseStock(StockIn stockIn, StockInItem item) {
        Stock stock = stockMapper.selectByProductAndWarehouseForUpdate(item.getProductId(), stockIn.getWarehouseId());
        int quantityBefore = stock == null ? 0 : defaultInt(stock.getQuantity());
        int changeQuantity = item.getQuantity();
        int quantityAfter = quantityBefore + changeQuantity;

        if (stock == null) {
            stock = new Stock();
            stock.setWarehouseId(stockIn.getWarehouseId());
            stock.setProductId(item.getProductId());
            stock.setQuantity(quantityAfter);
            stock.setLockedQuantity(0);
            stock.setVersion(0);
            stockMapper.insert(stock);
        } else {
            int updated = stockMapper.updateQuantityById(stock.getId(), quantityAfter);
            if (updated != 1) {
                throw BusinessException.badRequest("Stock update failed, please retry");
            }
        }

        stockMovementService.record(new StockMovementRecordCommand(
                item.getProductId(),
                stockIn.getWarehouseId(),
                "STOCK_IN",
                "STOCK_IN",
                stockIn.getId(),
                stockIn.getStockInNo(),
                quantityBefore,
                changeQuantity,
                quantityAfter,
                stockIn.getOperatorId(),
                movementRemark(stockIn.getRemark(), item.getRemark())
        ));
    }

    private void markConfirmedIfDraft(Long id) {
        int updated = stockInMapper.update(
                null,
                Wrappers.<StockIn>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CONFIRMED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock-in document has already been processed");
        }
    }

    private void markCancelledIfDraft(Long id) {
        int updated = stockInMapper.update(
                null,
                Wrappers.<StockIn>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CANCELLED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Stock-in document has already been processed");
        }
    }

    private void validateRequest(StockInRequest request) {
        Warehouse warehouse = warehouseMapper.selectById(request.warehouseId());
        if (warehouse == null || !ACTIVE_STATUS.equals(warehouse.getStatus())) {
            throw BusinessException.badRequest("Warehouse does not exist or is disabled");
        }

        Supplier supplier = supplierMapper.selectById(request.supplierId());
        if (supplier == null || !ACTIVE_STATUS.equals(supplier.getStatus())) {
            throw BusinessException.badRequest("Supplier does not exist or is disabled");
        }

        for (StockInItemRequest item : request.items()) {
            Product product = productMapper.selectById(item.productId());
            if (product == null || !ACTIVE_STATUS.equals(product.getStatus())) {
                throw BusinessException.badRequest("Product does not exist or is disabled");
            }
        }
    }

    private void applyRequest(StockIn stockIn, StockInRequest request) {
        stockIn.setWarehouseId(request.warehouseId());
        stockIn.setSupplierId(request.supplierId());
        stockIn.setRemark(trimToNull(request.remark()));
        stockIn.setTotalQuantity(totalQuantity(request.items()));
        stockIn.setTotalAmount(totalAmount(request.items()));
    }

    private void saveItems(Long stockInId, List<StockInItemRequest> itemRequests) {
        for (StockInItemRequest request : itemRequests) {
            StockInItem item = new StockInItem();
            item.setStockInId(stockInId);
            item.setProductId(request.productId());
            item.setQuantity(request.quantity());
            item.setUnitCost(defaultMoney(request.unitCost()));
            item.setAmount(defaultMoney(request.unitCost()).multiply(BigDecimal.valueOf(request.quantity())));
            item.setRemark(trimToNull(request.remark()));
            stockInItemMapper.insert(item);
        }
    }

    private StockIn getExisting(Long id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) {
            throw BusinessException.notFound("Stock-in document does not exist");
        }
        return stockIn;
    }

    private void ensureDraft(StockIn stockIn) {
        if (!DRAFT_STATUS.equals(stockIn.getStatus())) {
            throw BusinessException.badRequest("Only draft stock-in documents can be changed");
        }
    }

    private List<StockInItem> getItems(Long stockInId) {
        return stockInItemMapper.selectList(
                Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getStockInId, stockInId)
        );
    }

    private StockInResponse toResponse(StockIn stockIn, boolean includeItems) {
        Warehouse warehouse = warehouseMapper.selectById(stockIn.getWarehouseId());
        Supplier supplier = supplierMapper.selectById(stockIn.getSupplierId());
        List<StockInItemResponse> items = includeItems ? toItemResponses(getItems(stockIn.getId())) : List.of();

        return new StockInResponse(
                stockIn.getId(),
                stockIn.getStockInNo(),
                stockIn.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                stockIn.getSupplierId(),
                supplier == null ? null : supplier.getName(),
                stockIn.getOperatorId(),
                stockIn.getTotalQuantity(),
                stockIn.getTotalAmount(),
                stockIn.getStatus(),
                stockIn.getRemark(),
                items,
                stockIn.getCreatedAt(),
                stockIn.getUpdatedAt()
        );
    }

    private List<StockInItemResponse> toItemResponses(List<StockInItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = productMapper.selectBatchIds(
                        items.stream().map(StockInItem::getProductId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return new StockInItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product == null ? null : product.getSku(),
                            product == null ? null : product.getName(),
                            item.getQuantity(),
                            item.getUnitCost(),
                            item.getAmount(),
                            item.getRemark()
                    );
                })
                .toList();
    }

    private String generateDocumentNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "SI" + LocalDateTime.now().format(DOCUMENT_NO_TIME_FORMAT) + suffix;
    }

    private Integer totalQuantity(List<StockInItemRequest> items) {
        return items.stream().mapToInt(StockInItemRequest::quantity).sum();
    }

    private BigDecimal totalAmount(List<StockInItemRequest> items) {
        return items.stream()
                .map(item -> defaultMoney(item.unitCost()).multiply(BigDecimal.valueOf(item.quantity())))
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
