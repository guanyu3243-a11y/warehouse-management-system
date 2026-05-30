package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockMovementResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.StockMovement;
import com.warehouse.management.entity.User;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMovementMapper;
import com.warehouse.management.mapper.UserMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.StockMovementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockMovementServiceImpl implements StockMovementService {

    private static final DateTimeFormatter MOVEMENT_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final StockMovementMapper stockMovementMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    private final UserMapper userMapper;

    public StockMovementServiceImpl(
            StockMovementMapper stockMovementMapper,
            ProductMapper productMapper,
            WarehouseMapper warehouseMapper,
            UserMapper userMapper
    ) {
        this.stockMovementMapper = stockMovementMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<StockMovementResponse> page(
            long page,
            long size,
            Long productId,
            Long warehouseId,
            String movementType,
            String sourceType,
            String sourceNo,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        IPage<StockMovement> result = stockMovementMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query(productId, warehouseId, movementType, sourceType, sourceNo, startTime, endTime)
        );
        List<StockMovementResponse> records = toResponses(result.getRecords());
        return new PageResponse<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StockMovementResponse getById(Long id) {
        StockMovement movement = stockMovementMapper.selectById(id);
        if (movement == null) {
            throw BusinessException.notFound("Stock movement does not exist");
        }
        return toResponses(List.of(movement)).get(0);
    }

    @Override
    public PageResponse<StockMovementResponse> getByProductId(Long productId, long page, long size) {
        return page(page, size, productId, null, null, null, null, null, null);
    }

    @Override
    public PageResponse<StockMovementResponse> getByWarehouseId(Long warehouseId, long page, long size) {
        return page(page, size, null, warehouseId, null, null, null, null, null);
    }

    @Override
    public void record(StockMovementRecordCommand command) {
        StockMovement movement = new StockMovement();
        movement.setMovementNo(generateMovementNo());
        movement.setProductId(command.productId());
        movement.setWarehouseId(command.warehouseId());
        movement.setMovementType(normalize(command.movementType()));
        movement.setSourceType(normalize(command.sourceType()));
        movement.setSourceId(command.sourceId());
        movement.setSourceNo(command.sourceNo());
        movement.setQuantityBefore(defaultInt(command.quantityBefore()));
        movement.setChangeQuantity(defaultInt(command.changeQuantity()));
        movement.setQuantityAfter(defaultInt(command.quantityAfter()));
        movement.setOperatorId(command.operatorId());
        movement.setRemark(trimToNull(command.remark()));
        stockMovementMapper.insert(movement);
    }

    private LambdaQueryWrapper<StockMovement> query(
            Long productId,
            Long warehouseId,
            String movementType,
            String sourceType,
            String sourceNo,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        LambdaQueryWrapper<StockMovement> query = Wrappers.lambdaQuery();
        query.eq(productId != null, StockMovement::getProductId, productId);
        query.eq(warehouseId != null, StockMovement::getWarehouseId, warehouseId);
        query.eq(hasText(movementType), StockMovement::getMovementType, normalize(movementType));
        query.eq(hasText(sourceType), StockMovement::getSourceType, normalize(sourceType));
        query.like(hasText(sourceNo), StockMovement::getSourceNo, sourceNo == null ? null : sourceNo.trim());
        query.ge(startTime != null, StockMovement::getCreatedAt, startTime);
        query.le(endTime != null, StockMovement::getCreatedAt, endTime);
        query.orderByDesc(StockMovement::getCreatedAt).orderByDesc(StockMovement::getId);
        return query;
    }

    private List<StockMovementResponse> toResponses(List<StockMovement> movements) {
        if (movements.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = selectProductMap(movements);
        Map<Long, Warehouse> warehouseMap = selectWarehouseMap(movements);
        Map<Long, User> userMap = selectUserMap(movements);

        return movements.stream()
                .map(movement -> {
                    Product product = productMap.get(movement.getProductId());
                    Warehouse warehouse = warehouseMap.get(movement.getWarehouseId());
                    User user = userMap.get(movement.getOperatorId());
                    return new StockMovementResponse(
                            movement.getId(),
                            movement.getMovementNo(),
                            movement.getProductId(),
                            product == null ? null : product.getSku(),
                            product == null ? null : product.getName(),
                            movement.getWarehouseId(),
                            warehouse == null ? null : warehouse.getName(),
                            movement.getMovementType(),
                            movement.getSourceType(),
                            movement.getSourceId(),
                            movement.getSourceNo(),
                            movement.getQuantityBefore(),
                            movement.getChangeQuantity(),
                            movement.getQuantityAfter(),
                            movement.getOperatorId(),
                            user == null ? null : user.getUsername(),
                            movement.getRemark(),
                            movement.getCreatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, Product> selectProductMap(List<StockMovement> movements) {
        List<Long> productIds = movements.stream()
                .map(StockMovement::getProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, Warehouse> selectWarehouseMap(List<StockMovement> movements) {
        List<Long> warehouseIds = movements.stream()
                .map(StockMovement::getWarehouseId)
                .distinct()
                .toList();
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private Map<Long, User> selectUserMap(List<StockMovement> movements) {
        List<Long> userIds = movements.stream()
                .map(StockMovement::getOperatorId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String generateMovementNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "SM" + LocalDateTime.now().format(MOVEMENT_NO_TIME_FORMAT) + suffix;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
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
