package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.dto.StockOutResponse;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockIn;
import com.warehouse.management.entity.StockInItem;
import com.warehouse.management.entity.StockOut;
import com.warehouse.management.entity.StockOutItem;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockInItemMapper;
import com.warehouse.management.mapper.StockInMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutItemMapper;
import com.warehouse.management.mapper.StockOutMapper;
import com.warehouse.management.mapper.SupplierMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockConfirmationConcurrencyTests {

    @Mock
    private StockInMapper stockInMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private StockOutMapper stockOutMapper;

    @Mock
    private StockOutItemMapper stockOutItemMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private StockMovementService stockMovementService;

    private StockInServiceImpl stockInService;

    private StockOutServiceImpl stockOutService;

    @BeforeEach
    void setUp() {
        stockInService = new StockInServiceImpl(
                stockInMapper,
                stockInItemMapper,
                stockMapper,
                productMapper,
                warehouseMapper,
                supplierMapper,
                stockMovementService
        );
        stockOutService = new StockOutServiceImpl(
                stockOutMapper,
                stockOutItemMapper,
                stockMapper,
                productMapper,
                warehouseMapper,
                stockMovementService
        );
    }

    @Test
    void duplicateStockInConfirmDoesNotChangeStock() {
        StockIn stockIn = stockIn(1L, "SI-1", "DRAFT");
        when(stockInMapper.selectById(1L)).thenReturn(stockIn);
        when(stockInItemMapper.selectList(any())).thenReturn(List.of(stockInItem(100L, 5)));
        when(stockInMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> stockInService.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been processed");

        verifyNoInteractions(stockMapper, stockMovementService);
    }

    @Test
    void duplicateStockOutConfirmDoesNotChangeStock() {
        StockOut stockOut = stockOut(2L, "SO-1", "DRAFT");
        when(stockOutMapper.selectById(2L)).thenReturn(stockOut);
        when(stockOutItemMapper.selectList(any())).thenReturn(List.of(stockOutItem(100L, 3)));
        when(stockOutMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> stockOutService.confirm(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been processed");

        verifyNoInteractions(stockMapper, stockMovementService);
    }

    @Test
    void stockOutConfirmUsesLockedStockAndRecordsMovement() {
        StockOut draft = stockOut(2L, "SO-1", "DRAFT");
        StockOut confirmed = stockOut(2L, "SO-1", "CONFIRMED");
        StockOutItem item = stockOutItem(100L, 3);
        Stock stock = stock(50L, 100L, 10L, 10);

        when(stockOutMapper.selectById(2L)).thenReturn(draft, confirmed);
        when(stockOutItemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockOutMapper.update(isNull(), any())).thenReturn(1);
        when(stockMapper.selectByProductAndWarehouseForUpdate(100L, 10L)).thenReturn(stock);
        when(stockMapper.decreaseQuantityByIdIfEnough(50L, 3)).thenReturn(1);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse(10L));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(100L)));

        StockOutResponse response = stockOutService.confirm(2L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(stockMapper).decreaseQuantityByIdIfEnough(50L, 3);

        ArgumentCaptor<StockMovementRecordCommand> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRecordCommand.class);
        verify(stockMovementService).record(movementCaptor.capture());
        StockMovementRecordCommand movement = movementCaptor.getValue();

        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.changeQuantity()).isEqualTo(-3);
        assertThat(movement.quantityAfter()).isEqualTo(7);
        assertThat(movement.sourceType()).isEqualTo("STOCK_OUT");
    }

    private StockIn stockIn(Long id, String stockInNo, String status) {
        StockIn stockIn = new StockIn();
        stockIn.setId(id);
        stockIn.setStockInNo(stockInNo);
        stockIn.setWarehouseId(10L);
        stockIn.setSupplierId(20L);
        stockIn.setOperatorId(30L);
        stockIn.setTotalQuantity(5);
        stockIn.setTotalAmount(BigDecimal.TEN);
        stockIn.setStatus(status);
        return stockIn;
    }

    private StockInItem stockInItem(Long productId, Integer quantity) {
        StockInItem item = new StockInItem();
        item.setId(1L);
        item.setStockInId(1L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitCost(BigDecimal.ONE);
        item.setAmount(BigDecimal.valueOf(quantity));
        return item;
    }

    private StockOut stockOut(Long id, String stockOutNo, String status) {
        StockOut stockOut = new StockOut();
        stockOut.setId(id);
        stockOut.setStockOutNo(stockOutNo);
        stockOut.setWarehouseId(10L);
        stockOut.setOperatorId(30L);
        stockOut.setTotalQuantity(3);
        stockOut.setTotalAmount(BigDecimal.TEN);
        stockOut.setStatus(status);
        return stockOut;
    }

    private StockOutItem stockOutItem(Long productId, Integer quantity) {
        StockOutItem item = new StockOutItem();
        item.setId(1L);
        item.setStockOutId(2L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitSalePrice(BigDecimal.ONE);
        item.setAmount(BigDecimal.valueOf(quantity));
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
