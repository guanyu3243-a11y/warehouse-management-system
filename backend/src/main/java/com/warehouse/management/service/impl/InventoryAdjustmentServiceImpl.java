package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.InventoryAdjustmentItemRequest;
import com.warehouse.management.dto.InventoryAdjustmentItemResponse;
import com.warehouse.management.dto.InventoryAdjustmentRequest;
import com.warehouse.management.dto.InventoryAdjustmentResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.entity.InventoryAdjustment;
import com.warehouse.management.entity.InventoryAdjustmentItem;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.InventoryAdjustmentItemMapper;
import com.warehouse.management.mapper.InventoryAdjustmentMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.InventoryAdjustmentService;
import com.warehouse.management.service.StockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final String DRAFT_STATUS = "DRAFT";

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private static final String CANCELLED_STATUS = "CANCELLED";

    private static final DateTimeFormatter DOCUMENT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final InventoryAdjustmentMapper inventoryAdjustmentMapper;

    private final InventoryAdjustmentItemMapper inventoryAdjustmentItemMapper;

    private final StockMapper stockMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final StockMovementService stockMovementService;

    public InventoryAdjustmentServiceImpl(
            InventoryAdjustmentMapper inventoryAdjustmentMapper,
            InventoryAdjustmentItemMapper inventoryAdjustmentItemMapper,
            StockMapper stockMapper,
            ProductMapper productMapper,
            WarehouseMapper warehouseMapper,
            StockMovementService stockMovementService
    ) {
        this.inventoryAdjustmentMapper = inventoryAdjustmentMapper;
        this.inventoryAdjustmentItemMapper = inventoryAdjustmentItemMapper;
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.stockMovementService = stockMovementService;
    }

    @Override
    public PageResponse<InventoryAdjustmentResponse> page(long page, long size, String status, Long warehouseId) {
        LambdaQueryWrapper<InventoryAdjustment> query = Wrappers.lambdaQuery();
        query.eq(hasText(status), InventoryAdjustment::getStatus, status == null ? null : status.trim());
        query.eq(warehouseId != null, InventoryAdjustment::getWarehouseId, warehouseId);
        query.orderByDesc(InventoryAdjustment::getCreatedAt);

        IPage<InventoryAdjustment> result = inventoryAdjustmentMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(adjustment -> toResponse(adjustment, false)).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public InventoryAdjustmentResponse getById(Long id) {
        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse create(InventoryAdjustmentRequest request) {
        validateRequest(request);

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setAdjustmentNo(generateDocumentNo());
        adjustment.setOperatorId(CurrentUserContext.getRequired().id());
        adjustment.setStatus(DRAFT_STATUS);
        applyRequest(adjustment, request);
        inventoryAdjustmentMapper.insert(adjustment);

        saveItems(adjustment.getId(), request.items());
        return toResponse(adjustment, true);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse update(Long id, InventoryAdjustmentRequest request) {
        InventoryAdjustment adjustment = getExisting(id);
        ensureDraft(adjustment);
        validateRequest(request);

        applyRequest(adjustment, request);
        inventoryAdjustmentMapper.updateById(adjustment);
        inventoryAdjustmentItemMapper.delete(
                Wrappers.<InventoryAdjustmentItem>lambdaQuery()
                        .eq(InventoryAdjustmentItem::getAdjustmentId, id)
        );
        saveItems(id, request.items());
        return toResponse(adjustment, true);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse confirm(Long id) {
        InventoryAdjustment adjustment = getExisting(id);
        ensureDraft(adjustment);

        List<InventoryAdjustmentItem> items = getItems(id);
        if (items.isEmpty()) {
            throw BusinessException.badRequest("Inventory adjustment has no items");
        }

        markConfirmedIfDraft(id);
        for (InventoryAdjustmentItem item : items) {
            applyStockAdjustment(adjustment, item);
        }

        return toResponse(getExisting(id), true);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse cancel(Long id) {
        InventoryAdjustment adjustment = getExisting(id);
        ensureDraft(adjustment);

        markCancelledIfDraft(id);
        return toResponse(getExisting(id), true);
    }

    private void applyStockAdjustment(InventoryAdjustment adjustment, InventoryAdjustmentItem item) {
        Stock stock = stockMapper.selectByProductAndWarehouseForUpdate(
                item.getProductId(),
                adjustment.getWarehouseId()
        );
        int quantityBefore = stock == null ? 0 : defaultInt(stock.getQuantity());
        int changeQuantity = item.getAdjustQuantity();
        int quantityAfter = quantityBefore + changeQuantity;

        if (quantityAfter < 0) {
            throw BusinessException.badRequest("Adjustment would make stock negative for product id " + item.getProductId());
        }

        if (stock == null) {
            stock = new Stock();
            stock.setWarehouseId(adjustment.getWarehouseId());
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

        item.setQuantityBefore(quantityBefore);
        item.setQuantityAfter(quantityAfter);
        inventoryAdjustmentItemMapper.updateById(item);

        stockMovementService.record(new StockMovementRecordCommand(
                item.getProductId(),
                adjustment.getWarehouseId(),
                "ADJUSTMENT",
                "INVENTORY_ADJUSTMENT",
                adjustment.getId(),
                adjustment.getAdjustmentNo(),
                quantityBefore,
                changeQuantity,
                quantityAfter,
                adjustment.getOperatorId(),
                movementRemark(adjustment.getRemark(), item.getRemark())
        ));
    }

    private void markConfirmedIfDraft(Long id) {
        int updated = inventoryAdjustmentMapper.update(
                null,
                Wrappers.<InventoryAdjustment>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CONFIRMED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Inventory adjustment has already been processed");
        }
    }

    private void markCancelledIfDraft(Long id) {
        int updated = inventoryAdjustmentMapper.update(
                null,
                Wrappers.<InventoryAdjustment>update()
                        .eq("id", id)
                        .eq("status", DRAFT_STATUS)
                        .set("status", CANCELLED_STATUS)
                        .set("updated_at", LocalDateTime.now())
        );
        if (updated != 1) {
            throw BusinessException.badRequest("Inventory adjustment has already been processed");
        }
    }

    private void validateRequest(InventoryAdjustmentRequest request) {
        Warehouse warehouse = warehouseMapper.selectById(request.warehouseId());
        if (warehouse == null || !ACTIVE_STATUS.equals(warehouse.getStatus())) {
            throw BusinessException.badRequest("Warehouse does not exist or is disabled");
        }

        for (InventoryAdjustmentItemRequest item : request.items()) {
            if (item.adjustQuantity() == 0) {
                throw BusinessException.badRequest("Adjust quantity must not be 0");
            }

            Product product = productMapper.selectById(item.productId());
            if (product == null || !ACTIVE_STATUS.equals(product.getStatus())) {
                throw BusinessException.badRequest("Product does not exist or is disabled");
            }
        }
    }

    private void applyRequest(InventoryAdjustment adjustment, InventoryAdjustmentRequest request) {
        adjustment.setWarehouseId(request.warehouseId());
        adjustment.setReason(request.reason().trim());
        adjustment.setRemark(trimToNull(request.remark()));
        adjustment.setTotalAdjustQuantity(totalAdjustQuantity(request.items()));
    }

    private void saveItems(Long adjustmentId, List<InventoryAdjustmentItemRequest> itemRequests) {
        for (InventoryAdjustmentItemRequest request : itemRequests) {
            InventoryAdjustmentItem item = new InventoryAdjustmentItem();
            item.setAdjustmentId(adjustmentId);
            item.setProductId(request.productId());
            item.setAdjustQuantity(request.adjustQuantity());
            item.setRemark(trimToNull(request.remark()));
            inventoryAdjustmentItemMapper.insert(item);
        }
    }

    private InventoryAdjustment getExisting(Long id) {
        InventoryAdjustment adjustment = inventoryAdjustmentMapper.selectById(id);
        if (adjustment == null) {
            throw BusinessException.notFound("Inventory adjustment does not exist");
        }
        return adjustment;
    }

    private void ensureDraft(InventoryAdjustment adjustment) {
        if (!DRAFT_STATUS.equals(adjustment.getStatus())) {
            throw BusinessException.badRequest("Only draft inventory adjustments can be changed");
        }
    }

    private List<InventoryAdjustmentItem> getItems(Long adjustmentId) {
        return inventoryAdjustmentItemMapper.selectList(
                Wrappers.<InventoryAdjustmentItem>lambdaQuery()
                        .eq(InventoryAdjustmentItem::getAdjustmentId, adjustmentId)
        );
    }

    private InventoryAdjustmentResponse toResponse(InventoryAdjustment adjustment, boolean includeItems) {
        Warehouse warehouse = warehouseMapper.selectById(adjustment.getWarehouseId());
        List<InventoryAdjustmentItemResponse> items = includeItems
                ? toItemResponses(getItems(adjustment.getId()))
                : List.of();

        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                adjustment.getAdjustmentNo(),
                adjustment.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                adjustment.getOperatorId(),
                adjustment.getTotalAdjustQuantity(),
                adjustment.getReason(),
                adjustment.getStatus(),
                adjustment.getRemark(),
                items,
                adjustment.getCreatedAt(),
                adjustment.getUpdatedAt()
        );
    }

    private List<InventoryAdjustmentItemResponse> toItemResponses(List<InventoryAdjustmentItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = productMapper.selectBatchIds(
                        items.stream().map(InventoryAdjustmentItem::getProductId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return new InventoryAdjustmentItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product == null ? null : product.getSku(),
                            product == null ? null : product.getName(),
                            item.getQuantityBefore(),
                            item.getAdjustQuantity(),
                            item.getQuantityAfter(),
                            item.getRemark()
                    );
                })
                .toList();
    }

    private String generateDocumentNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "IA" + LocalDateTime.now().format(DOCUMENT_NO_TIME_FORMAT) + suffix;
    }

    private Integer totalAdjustQuantity(List<InventoryAdjustmentItemRequest> items) {
        return items.stream().mapToInt(InventoryAdjustmentItemRequest::adjustQuantity).sum();
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
}
