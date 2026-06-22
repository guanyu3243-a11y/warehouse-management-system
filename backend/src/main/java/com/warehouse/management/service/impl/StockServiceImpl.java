package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.StockPageResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.dto.StockSummaryResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.StockService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;

    private final ProductMapper productMapper;

    private final WarehouseMapper warehouseMapper;

    public StockServiceImpl(StockMapper stockMapper, ProductMapper productMapper, WarehouseMapper warehouseMapper) {
        this.stockMapper = stockMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public StockPageResponse page(
            long page,
            long pageSize,
            Long warehouseId,
            Long categoryId,
            String keyword,
            Boolean lowStockOnly,
            String color,
            String productSize
    ) {
        List<StockResponse> filtered = loadStockEntries(warehouseId).stream()
                .filter(entry -> categoryId == null || categoryId.equals(entry.response().categoryId()))
                .filter(entry -> !hasText(keyword) || matchesKeyword(entry.response(), keyword.trim()))
                .filter(entry -> !Boolean.TRUE.equals(lowStockOnly) || Boolean.TRUE.equals(entry.response().lowStock()))
                .filter(entry -> !hasText(color) || productMatches(entry.product(), Product::getColor, color.trim()))
                .filter(entry -> !hasText(productSize) || productMatches(entry.product(), Product::getSize, productSize.trim()))
                .map(StockEntry::response)
                .toList();
        StockSummaryResponse summary = summarize(filtered);

        long normalizedPage = normalizePage(page);
        long normalizedSize = normalizeSize(pageSize);
        int fromIndex = (int) Math.min((normalizedPage - 1) * normalizedSize, filtered.size());
        int toIndex = (int) Math.min(fromIndex + normalizedSize, filtered.size());

        return new StockPageResponse(
                filtered.subList(fromIndex, toIndex),
                filtered.size(),
                normalizedPage,
                normalizedSize,
                summary
        );
    }

    @Override
    public List<StockResponse> getByProductId(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BusinessException.notFound("Product does not exist");
        }
        return loadStockResponses(null).stream()
                .filter(stock -> productId.equals(stock.productId()))
                .toList();
    }

    private boolean productMatches(Product product, Function<Product, String> getter, String expected) {
        String actual = product == null ? null : getter.apply(product);
        return actual != null && actual.contains(expected);
    }

    private List<StockResponse> loadStockResponses(Long warehouseId) {
        return loadStockEntries(warehouseId).stream()
                .map(StockEntry::response)
                .toList();
    }

    private List<StockEntry> loadStockEntries(Long warehouseId) {
        List<Stock> stocks = stockMapper.selectList(
                Wrappers.<Stock>lambdaQuery()
                        .eq(warehouseId != null, Stock::getWarehouseId, warehouseId)
                        .orderByDesc(Stock::getUpdatedAt)
        );
        if (stocks.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = selectProductMap(stocks);
        Map<Long, Warehouse> warehouseMap = selectWarehouseMap(stocks);

        return stocks.stream()
                .map(stock -> {
                    Product product = productMap.get(stock.getProductId());
                    return new StockEntry(
                            toResponse(stock, product, warehouseMap.get(stock.getWarehouseId())),
                            product
                    );
                })
                .toList();
    }

    private Map<Long, Product> selectProductMap(List<Stock> stocks) {
        Set<Long> productIds = stocks.stream().map(Stock::getProductId).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<Long, Warehouse> selectWarehouseMap(List<Stock> stocks) {
        Set<Long> warehouseIds = stocks.stream().map(Stock::getWarehouseId).collect(Collectors.toSet());
        if (warehouseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private StockResponse toResponse(Stock stock, Product product, Warehouse warehouse) {
        int quantity = defaultInt(stock.getQuantity());
        int lockedQuantity = defaultInt(stock.getLockedQuantity());
        int availableQuantity = quantity - lockedQuantity;
        int lowStockThreshold = product == null ? 0 : defaultInt(product.getLowStockThreshold());

        return new StockResponse(
                stock.getId(),
                stock.getProductId(),
                product == null ? null : product.getSku(),
                product == null ? null : product.getName(),
                product == null ? null : product.getCategoryId(),
                stock.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                quantity,
                lockedQuantity,
                availableQuantity,
                lowStockThreshold,
                availableQuantity <= lowStockThreshold,
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }

    private boolean matchesKeyword(StockResponse stock, String keyword) {
        return contains(stock.sku(), keyword)
                || contains(stock.productName(), keyword)
                || contains(stock.warehouseName(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private StockSummaryResponse summarize(List<StockResponse> stocks) {
        long totalQuantity = stocks.stream()
                .mapToLong(stock -> defaultInt(stock.quantity()))
                .sum();
        long totalLockedQuantity = stocks.stream()
                .mapToLong(stock -> defaultInt(stock.lockedQuantity()))
                .sum();
        long totalAvailableQuantity = stocks.stream()
                .mapToLong(stock -> defaultInt(stock.availableQuantity()))
                .sum();
        return new StockSummaryResponse(
                stocks.size(),
                totalQuantity,
                totalLockedQuantity,
                totalAvailableQuantity
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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

    private record StockEntry(
            StockResponse response,
            Product product
    ) {
    }
}
