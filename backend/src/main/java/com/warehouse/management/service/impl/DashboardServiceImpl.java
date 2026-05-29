package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.warehouse.management.dto.DashboardSummaryResponse;
import com.warehouse.management.dto.DashboardTrendItemResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockIn;
import com.warehouse.management.entity.StockOut;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.CategoryMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockInMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutMapper;
import com.warehouse.management.mapper.SupplierMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final WarehouseMapper warehouseMapper;

    private final SupplierMapper supplierMapper;

    private final StockMapper stockMapper;

    private final StockInMapper stockInMapper;

    private final StockOutMapper stockOutMapper;

    public DashboardServiceImpl(
            ProductMapper productMapper,
            CategoryMapper categoryMapper,
            WarehouseMapper warehouseMapper,
            SupplierMapper supplierMapper,
            StockMapper stockMapper,
            StockInMapper stockInMapper,
            StockOutMapper stockOutMapper
    ) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.warehouseMapper = warehouseMapper;
        this.supplierMapper = supplierMapper;
        this.stockMapper = stockMapper;
        this.stockInMapper = stockInMapper;
        this.stockOutMapper = stockOutMapper;
    }

    @Override
    public DashboardSummaryResponse summary() {
        List<StockResponse> stockResponses = loadStockResponses();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return new DashboardSummaryResponse(
                productMapper.selectCount(null),
                categoryMapper.selectCount(null),
                warehouseMapper.selectCount(null),
                supplierMapper.selectCount(null),
                stockResponses.stream().mapToInt(StockResponse::quantity).sum(),
                stockResponses.stream().filter(StockResponse::lowStock).count(),
                confirmedStockInQuantity(start, end),
                confirmedStockOutQuantity(start, end)
        );
    }

    @Override
    public List<DashboardTrendItemResponse> stockTrend(int days) {
        int normalizedDays = Math.min(Math.max(days, 1), 30);
        LocalDate firstDate = LocalDate.now().minusDays(normalizedDays - 1L);
        LocalDateTime start = firstDate.atStartOfDay();

        Map<LocalDate, TrendCounter> counters = new LinkedHashMap<>();
        for (int index = 0; index < normalizedDays; index++) {
            counters.put(firstDate.plusDays(index), new TrendCounter());
        }

        List<StockIn> stockInList = stockInMapper.selectList(
                Wrappers.<StockIn>lambdaQuery()
                        .eq(StockIn::getStatus, CONFIRMED_STATUS)
                        .ge(StockIn::getUpdatedAt, start)
        );
        for (StockIn stockIn : stockInList) {
            LocalDate date = toTrendDate(stockIn.getUpdatedAt());
            TrendCounter counter = counters.get(date);
            if (counter != null) {
                counter.stockInQuantity += defaultInt(stockIn.getTotalQuantity());
            }
        }

        List<StockOut> stockOutList = stockOutMapper.selectList(
                Wrappers.<StockOut>lambdaQuery()
                        .eq(StockOut::getStatus, CONFIRMED_STATUS)
                        .ge(StockOut::getUpdatedAt, start)
        );
        for (StockOut stockOut : stockOutList) {
            LocalDate date = toTrendDate(stockOut.getUpdatedAt());
            TrendCounter counter = counters.get(date);
            if (counter != null) {
                counter.stockOutQuantity += defaultInt(stockOut.getTotalQuantity());
            }
        }

        return counters.entrySet().stream()
                .map(entry -> new DashboardTrendItemResponse(
                        entry.getKey().toString(),
                        entry.getValue().stockInQuantity,
                        entry.getValue().stockOutQuantity
                ))
                .toList();
    }

    @Override
    public List<StockResponse> lowStockTop(int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 50);
        return loadStockResponses().stream()
                .filter(StockResponse::lowStock)
                .sorted(Comparator.comparingInt(StockResponse::availableQuantity))
                .limit(normalizedLimit)
                .toList();
    }

    private Integer confirmedStockInQuantity(LocalDateTime start, LocalDateTime end) {
        return stockInMapper.selectList(
                        Wrappers.<StockIn>lambdaQuery()
                                .eq(StockIn::getStatus, CONFIRMED_STATUS)
                                .ge(StockIn::getUpdatedAt, start)
                                .lt(StockIn::getUpdatedAt, end)
                ).stream()
                .mapToInt(stockIn -> defaultInt(stockIn.getTotalQuantity()))
                .sum();
    }

    private Integer confirmedStockOutQuantity(LocalDateTime start, LocalDateTime end) {
        return stockOutMapper.selectList(
                        Wrappers.<StockOut>lambdaQuery()
                                .eq(StockOut::getStatus, CONFIRMED_STATUS)
                                .ge(StockOut::getUpdatedAt, start)
                                .lt(StockOut::getUpdatedAt, end)
                ).stream()
                .mapToInt(stockOut -> defaultInt(stockOut.getTotalQuantity()))
                .sum();
    }

    private List<StockResponse> loadStockResponses() {
        List<Stock> stocks = stockMapper.selectList(Wrappers.<Stock>lambdaQuery().orderByDesc(Stock::getUpdatedAt));
        if (stocks.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productMap = selectProductMap(stocks);
        Map<Long, Warehouse> warehouseMap = selectWarehouseMap(stocks);
        List<StockResponse> responses = new ArrayList<>();
        for (Stock stock : stocks) {
            responses.add(toStockResponse(stock, productMap.get(stock.getProductId()), warehouseMap.get(stock.getWarehouseId())));
        }
        return responses;
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

    private StockResponse toStockResponse(Stock stock, Product product, Warehouse warehouse) {
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

    private LocalDate toTrendDate(LocalDateTime value) {
        return value == null ? LocalDate.now() : value.toLocalDate();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static class TrendCounter {
        private int stockInQuantity;
        private int stockOutQuantity;
    }
}
