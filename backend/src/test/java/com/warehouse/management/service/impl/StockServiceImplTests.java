package com.warehouse.management.service.impl;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTests {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    private StockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockServiceImpl(stockMapper, productMapper, warehouseMapper);
    }

    @Test
    void pageFuzzyFiltersByProductColorAndSize() {
        when(stockMapper.selectList(any())).thenReturn(List.of(
                stock(1L, 101L, 10L),
                stock(2L, 102L, 10L)
        ));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(
                product(101L, "SKU-101", "短袖T恤", "白色", "120#"),
                product(102L, "SKU-102", "短袖T恤", "藏青", "130#")
        ));
        when(warehouseMapper.selectBatchIds(any())).thenReturn(List.of(warehouse(10L)));

        PageResponse<StockResponse> response = stockService.page(
                1,
                10,
                null,
                null,
                null,
                false,
                "白",
                "120"
        );

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.records()).extracting(StockResponse::sku).containsExactly("SKU-101");
    }

    private Stock stock(Long id, Long productId, Long warehouseId) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(10);
        stock.setLockedQuantity(0);
        return stock;
    }

    private Product product(Long id, String sku, String name, String color, String size) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setCategoryId(20L);
        product.setColor(color);
        product.setSize(size);
        product.setLowStockThreshold(0);
        return product;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("主仓");
        return warehouse;
    }
}
