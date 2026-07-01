package com.warehouse.management.service.impl;

import com.warehouse.management.dto.StockPageResponse;
import com.warehouse.management.dto.StockResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.util.PaginationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

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

        StockPageResponse response = stockService.page(
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

    @Test
    void pageSummaryCountsAllMatchedStocksInsteadOfCurrentPageOnly() {
        mockStocks(
                stock(1L, 101L, 10L, 10, 1),
                stock(2L, 102L, 10L, 20, 2),
                stock(3L, 103L, 10L, 30, 3)
        );
        mockProducts(
                product(101L, "2629-BLACK-150", "2629 POLO", "黑色", "150#"),
                product(102L, "2629-BLACK-160", "2629 POLO", "黑色", "160#"),
                product(103L, "2629-BLACK-170", "2629 POLO", "黑色", "170#")
        );
        mockWarehouses(warehouse(10L));

        StockPageResponse response = stockService.page(
                1,
                2,
                null,
                null,
                "2629",
                false,
                "黑",
                null
        );

        assertThat(response.records()).hasSize(2);
        assertThat(response.total()).isEqualTo(3);
        assertThat(response.summary().skuCount()).isEqualTo(3);
        assertThat(response.summary().totalQuantity()).isEqualTo(60);
        assertThat(response.summary().totalLockedQuantity()).isEqualTo(6);
        assertThat(response.summary().totalAvailableQuantity()).isEqualTo(54);
    }

    @Test
    void pageSummarySumsMatchedModelAndColorAcrossSizes() {
        mockStocks(
                stock(1L, 101L, 10L, 12, 0),
                stock(2L, 102L, 10L, 18, 1),
                stock(3L, 103L, 10L, 15, 0)
        );
        mockProducts(
                product(101L, "2629-BLACK-150", "2629 POLO", "黑色", "150#"),
                product(102L, "2629-BLACK-160", "2629 POLO", "黑色", "160#"),
                product(103L, "2629-RED-160", "2629 POLO", "红色", "160#")
        );
        mockWarehouses(warehouse(10L));

        StockPageResponse response = stockService.page(
                1,
                10,
                null,
                null,
                "2629",
                false,
                "黑色",
                null
        );

        assertThat(response.summary().skuCount()).isEqualTo(2);
        assertThat(response.summary().totalQuantity()).isEqualTo(30);
        assertThat(response.summary().totalLockedQuantity()).isEqualTo(1);
        assertThat(response.summary().totalAvailableQuantity()).isEqualTo(29);
    }

    @Test
    void pageSummaryUsesFuzzyColorFilter() {
        mockStocks(
                stock(1L, 101L, 10L, 12, 0),
                stock(2L, 102L, 10L, 18, 0)
        );
        mockProducts(
                product(101L, "SKU-BLACK", "短裙", "黑色", "160#"),
                product(102L, "SKU-WHITE", "短裙", "白色", "160#")
        );
        mockWarehouses(warehouse(10L));

        StockPageResponse response = stockService.page(
                1,
                10,
                null,
                null,
                "短裙",
                false,
                "黑",
                null
        );

        assertThat(response.summary().skuCount()).isEqualTo(1);
        assertThat(response.summary().totalQuantity()).isEqualTo(12);
        assertThat(response.records()).extracting(StockResponse::sku).containsExactly("SKU-BLACK");
    }

    @Test
    void pageSummaryUsesFuzzyProductSizeFilter() {
        mockStocks(
                stock(1L, 101L, 10L, 12, 0),
                stock(2L, 102L, 10L, 18, 0)
        );
        mockProducts(
                product(101L, "SKU-160", "短袖T恤", "黑色", "160#"),
                product(102L, "SKU-170", "短袖T恤", "黑色", "170#")
        );
        mockWarehouses(warehouse(10L));

        StockPageResponse response = stockService.page(
                1,
                10,
                null,
                null,
                "短袖",
                false,
                null,
                "160"
        );

        assertThat(response.summary().skuCount()).isEqualTo(1);
        assertThat(response.summary().totalQuantity()).isEqualTo(12);
        assertThat(response.records()).extracting(StockResponse::sku).containsExactly("SKU-160");
    }

    @Test
    void pageSummaryReturnsZeroWhenNoStocksMatched() {
        mockStocks(stock(1L, 101L, 10L, 12, 0));
        mockProducts(product(101L, "SKU-160", "短袖T恤", "黑色", "160#"));
        mockWarehouses(warehouse(10L));

        StockPageResponse response = stockService.page(
                1,
                10,
                null,
                null,
                "不存在",
                false,
                null,
                null
        );

        assertThat(response.records()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.summary().skuCount()).isZero();
        assertThat(response.summary().totalQuantity()).isZero();
        assertThat(response.summary().totalLockedQuantity()).isZero();
        assertThat(response.summary().totalAvailableQuantity()).isZero();
    }

    @Test
    void pageCapsNormalRequestsAtOneHundredRows() {
        mockBulkStocks(150);

        StockPageResponse response = stockService.page(
                1,
                10000,
                null,
                null,
                null,
                false,
                null,
                null
        );

        assertThat(response.records()).hasSize(100);
        assertThat(response.total()).isEqualTo(150);
    }

    @Test
    void pageAllowsExportRequestsAboveOneHundredRows() {
        mockBulkStocks(150);

        StockPageResponse response = PaginationSupport.withMaxPageSize(10000, () -> stockService.page(
                1,
                10000,
                null,
                null,
                null,
                false,
                null,
                null
        ));

        assertThat(response.records()).hasSize(150);
        assertThat(response.total()).isEqualTo(150);
    }

    private Stock stock(Long id, Long productId, Long warehouseId) {
        return stock(id, productId, warehouseId, 10, 0);
    }

    private Stock stock(Long id, Long productId, Long warehouseId, Integer quantity, Integer lockedQuantity) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(quantity);
        stock.setLockedQuantity(lockedQuantity);
        return stock;
    }

    private void mockStocks(Stock... stocks) {
        when(stockMapper.selectList(any())).thenReturn(List.of(stocks));
    }

    private void mockProducts(Product... products) {
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(products));
    }

    private void mockWarehouses(Warehouse... warehouses) {
        when(warehouseMapper.selectBatchIds(any())).thenReturn(List.of(warehouses));
    }

    private void mockBulkStocks(int count) {
        List<Stock> stocks = IntStream.rangeClosed(1, count)
                .mapToObj(index -> stock((long) index, 1000L + index, 10L))
                .toList();
        List<Product> products = IntStream.rangeClosed(1, count)
                .mapToObj(index -> product(1000L + index, "SKU-" + index, "商品" + index, "黑色", "160#"))
                .toList();
        when(stockMapper.selectList(any())).thenReturn(stocks);
        when(productMapper.selectBatchIds(any())).thenReturn(products);
        when(warehouseMapper.selectBatchIds(any())).thenReturn(List.of(warehouse(10L)));
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
