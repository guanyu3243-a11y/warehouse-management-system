package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.WarehouseRequest;
import com.warehouse.management.dto.WarehouseResponse;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockIn;
import com.warehouse.management.entity.StockOut;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.StockInMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final WarehouseMapper warehouseMapper;

    private final StockMapper stockMapper;

    private final StockInMapper stockInMapper;

    private final StockOutMapper stockOutMapper;

    public WarehouseServiceImpl(
            WarehouseMapper warehouseMapper,
            StockMapper stockMapper,
            StockInMapper stockInMapper,
            StockOutMapper stockOutMapper
    ) {
        this.warehouseMapper = warehouseMapper;
        this.stockMapper = stockMapper;
        this.stockInMapper = stockInMapper;
        this.stockOutMapper = stockOutMapper;
    }

    @Override
    public PageResponse<WarehouseResponse> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Warehouse> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Warehouse::getName, value)
                    .or()
                    .like(Warehouse::getCode, value));
        }
        if (hasText(status)) {
            query.eq(Warehouse::getStatus, status.trim());
        }
        query.orderByDesc(Warehouse::getCreatedAt);

        IPage<Warehouse> result = warehouseMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public WarehouseResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        ensureCodeUnique(request.code(), null);

        Warehouse warehouse = new Warehouse();
        applyRequest(warehouse, request);
        warehouseMapper.insert(warehouse);
        return toResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = getExisting(id);
        ensureCodeUnique(request.code(), id);

        applyRequest(warehouse, request);
        warehouseMapper.updateById(warehouse);
        return toResponse(warehouse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        long stockCount = stockMapper.selectCount(
                Wrappers.<Stock>lambdaQuery().eq(Stock::getWarehouseId, id)
        );
        long stockInCount = stockInMapper.selectCount(
                Wrappers.<StockIn>lambdaQuery().eq(StockIn::getWarehouseId, id)
        );
        long stockOutCount = stockOutMapper.selectCount(
                Wrappers.<StockOut>lambdaQuery().eq(StockOut::getWarehouseId, id)
        );
        if (stockCount + stockInCount + stockOutCount > 0) {
            throw BusinessException.badRequest("Warehouse is already used by stock records or documents");
        }
        warehouseMapper.deleteById(id);
    }

    private Warehouse getExisting(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw BusinessException.notFound("Warehouse does not exist");
        }
        return warehouse;
    }

    private void ensureCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<Warehouse> query = Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getCode, code.trim());
        if (excludedId != null) {
            query.ne(Warehouse::getId, excludedId);
        }
        if (warehouseMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("Warehouse code already exists");
        }
    }

    private void applyRequest(Warehouse warehouse, WarehouseRequest request) {
        warehouse.setCode(request.code().trim());
        warehouse.setName(request.name().trim());
        warehouse.setAddress(trimToNull(request.address()));
        warehouse.setContactName(trimToNull(request.contactName()));
        warehouse.setContactPhone(trimToNull(request.contactPhone()));
        warehouse.setStatus(hasText(request.status()) ? request.status().trim() : DEFAULT_STATUS);
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getContactName(),
                warehouse.getContactPhone(),
                warehouse.getStatus(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
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
