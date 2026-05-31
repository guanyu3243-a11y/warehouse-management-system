package com.warehouse.management.service.impl;

import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockTakeResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockTake;
import com.warehouse.management.entity.StockTakeItem;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockTakeItemMapper;
import com.warehouse.management.mapper.StockTakeMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.ExcelService;
import com.warehouse.management.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockTakeServiceImplTests {

    @Mock
    private StockTakeMapper stockTakeMapper;

    @Mock
    private StockTakeItemMapper stockTakeItemMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private ExcelService excelService;

    private StockTakeServiceImpl stockTakeService;

    @BeforeEach
    void setUp() {
        stockTakeService = new StockTakeServiceImpl(
                stockTakeMapper,
                stockTakeItemMapper,
                stockMapper,
                productMapper,
                warehouseMapper,
                stockMovementService,
                excelService
        );
    }

    @Test
    void confirmUpdatesStockToActualQuantityAndRecordsMovement() {
        StockTake draft = stockTake(1L, "ST-1", "DRAFT");
        StockTake confirmed = stockTake(1L, "ST-1", "CONFIRMED");
        StockTakeItem item = stockTakeItem(100L, 10, 7);
        Stock stock = stock(50L, 100L, 10L, 10);

        when(stockTakeMapper.selectById(1L)).thenReturn(draft, confirmed);
        when(stockTakeItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockTakeMapper.update(isNull(), any())).thenReturn(1);
        when(stockMapper.selectByProductAndWarehouseForUpdate(100L, 10L)).thenReturn(stock);
        when(stockMapper.updateQuantityById(50L, 7)).thenReturn(1);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse(10L));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(100L)));

        StockTakeResponse response = stockTakeService.confirm(1L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).bookQuantity()).isEqualTo(10);
        assertThat(response.items().get(0).actualQuantity()).isEqualTo(7);
        assertThat(response.items().get(0).differenceQuantity()).isEqualTo(-3);
        verify(stockMapper).updateQuantityById(50L, 7);

        ArgumentCaptor<StockMovementRecordCommand> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRecordCommand.class);
        verify(stockMovementService).record(movementCaptor.capture());
        StockMovementRecordCommand movement = movementCaptor.getValue();

        assertThat(movement.movementType()).isEqualTo("STOCK_TAKE");
        assertThat(movement.sourceType()).isEqualTo("STOCK_TAKE");
        assertThat(movement.sourceNo()).isEqualTo("ST-1");
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.changeQuantity()).isEqualTo(-3);
        assertThat(movement.quantityAfter()).isEqualTo(7);
    }

    private StockTake stockTake(Long id, String stockTakeNo, String status) {
        StockTake stockTake = new StockTake();
        stockTake.setId(id);
        stockTake.setStockTakeNo(stockTakeNo);
        stockTake.setWarehouseId(10L);
        stockTake.setOperatorId(30L);
        stockTake.setTitle("Monthly stock take");
        stockTake.setTotalBookQuantity(10);
        stockTake.setTotalActualQuantity(7);
        stockTake.setTotalDifferenceQuantity(-3);
        stockTake.setStatus(status);
        return stockTake;
    }

    private StockTakeItem stockTakeItem(Long productId, Integer bookQuantity, Integer actualQuantity) {
        StockTakeItem item = new StockTakeItem();
        item.setId(1L);
        item.setStockTakeId(1L);
        item.setProductId(productId);
        item.setBookQuantity(bookQuantity);
        item.setActualQuantity(actualQuantity);
        item.setDifferenceQuantity(actualQuantity - bookQuantity);
        return item;
    }

    private Stock stock(Long id, Long productId, Long warehouseId, Integer quantity) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(quantity);
        stock.setLockedQuantity(0);
        stock.setVersion(0);
        return stock;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("Main Warehouse");
        return warehouse;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-1");
        product.setName("Jacket");
        return product;
    }
}
