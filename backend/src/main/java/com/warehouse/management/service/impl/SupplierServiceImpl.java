package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.SupplierRequest;
import com.warehouse.management.dto.SupplierResponse;
import com.warehouse.management.entity.StockIn;
import com.warehouse.management.entity.Supplier;
import com.warehouse.management.mapper.StockInMapper;
import com.warehouse.management.mapper.SupplierMapper;
import com.warehouse.management.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierServiceImpl implements SupplierService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final SupplierMapper supplierMapper;

    private final StockInMapper stockInMapper;

    public SupplierServiceImpl(SupplierMapper supplierMapper, StockInMapper stockInMapper) {
        this.supplierMapper = supplierMapper;
        this.stockInMapper = stockInMapper;
    }

    @Override
    public PageResponse<SupplierResponse> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Supplier> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Supplier::getName, value)
                    .or()
                    .like(Supplier::getCode, value));
        }
        if (hasText(status)) {
            query.eq(Supplier::getStatus, status.trim());
        }
        query.orderByDesc(Supplier::getCreatedAt);

        IPage<Supplier> result = supplierMapper.selectPage(
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
    public SupplierResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        ensureCodeUnique(request.code(), null);

        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        supplierMapper.insert(supplier);
        return toResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = getExisting(id);
        ensureCodeUnique(request.code(), id);

        applyRequest(supplier, request);
        supplierMapper.updateById(supplier);
        return toResponse(supplier);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        Long stockInCount = stockInMapper.selectCount(
                Wrappers.<StockIn>lambdaQuery().eq(StockIn::getSupplierId, id)
        );
        if (stockInCount > 0) {
            throw BusinessException.badRequest("Supplier is already used by stock-in documents");
        }
        supplierMapper.deleteById(id);
    }

    private Supplier getExisting(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw BusinessException.notFound("Supplier does not exist");
        }
        return supplier;
    }

    private void ensureCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<Supplier> query = Wrappers.<Supplier>lambdaQuery()
                .eq(Supplier::getCode, code.trim());
        if (excludedId != null) {
            query.ne(Supplier::getId, excludedId);
        }
        if (supplierMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("Supplier code already exists");
        }
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setCode(request.code().trim());
        supplier.setName(request.name().trim());
        supplier.setContactName(trimToNull(request.contactName()));
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setAddress(trimToNull(request.address()));
        supplier.setStatus(hasText(request.status()) ? request.status().trim() : DEFAULT_STATUS);
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getAddress(),
                supplier.getStatus(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
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
