package com.warehouse.management.service.impl;

import com.warehouse.management.dto.InventoryAdjustmentResponse;
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
class InventoryAdjustmentServiceImplTests {

    @Mock
    private InventoryAdjustmentMapper inventoryAdjustmentMapper;

    @Mock
    private InventoryAdjustmentItemMapper inventoryAdjustmentItemMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private StockMovementService stockMovementService;

    private InventoryAdjustmentServiceImpl inventoryAdjustmentService;

    @BeforeEach
    void setUp() {
        inventoryAdjustmentService = new InventoryAdjustmentServiceImpl(
                inventoryAdjustmentMapper,
                inventoryAdjustmentItemMapper,
                stockMapper,
                productMapper,
                warehouseMapper,
                stockMovementService
        );
    }

    @Test
    void confirmUpdatesStockAndRecordsAdjustmentMovement() {
        InventoryAdjustment draft = adjustment(1L, "IA-1", "DRAFT");
        InventoryAdjustment confirmed = adjustment(1L, "IA-1", "CONFIRMED");
        InventoryAdjustmentItem item = adjustmentItem(100L, 5);
        Stock stock = stock(50L, 100L, 10L, 10);

        when(inventoryAdjustmentMapper.selectById(1L)).thenReturn(draft, confirmed);
        when(inventoryAdjustmentItemMapper.selectList(any())).thenReturn(List.of(item));
        when(inventoryAdjustmentMapper.update(isNull(), any())).thenReturn(1);
        when(stockMapper.selectByProductAndWarehouseForUpdate(100L, 10L)).thenReturn(stock);
        when(stockMapper.updateQuantityById(50L, 15)).thenReturn(1);
        when(warehouseMapper.selectById(10L)).thenReturn(warehouse(10L));
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(product(100L)));

        InventoryAdjustmentResponse response = inventoryAdjustmentService.confirm(1L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantityBefore()).isEqualTo(10);
        assertThat(response.items().get(0).quantityAfter()).isEqualTo(15);
        verify(stockMapper).updateQuantityById(50L, 15);
        verify(inventoryAdjustmentItemMapper).updateById(item);

        ArgumentCaptor<StockMovementRecordCommand> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRecordCommand.class);
        verify(stockMovementService).record(movementCaptor.capture());
        StockMovementRecordCommand movement = movementCaptor.getValue();

        assertThat(movement.movementType()).isEqualTo("ADJUSTMENT");
        assertThat(movement.sourceType()).isEqualTo("INVENTORY_ADJUSTMENT");
        assertThat(movement.sourceNo()).isEqualTo("IA-1");
        assertThat(movement.quantityBefore()).isEqualTo(10);
        assertThat(movement.changeQuantity()).isEqualTo(5);
        assertThat(movement.quantityAfter()).isEqualTo(15);
    }

    private InventoryAdjustment adjustment(Long id, String adjustmentNo, String status) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setId(id);
        adjustment.setAdjustmentNo(adjustmentNo);
        adjustment.setWarehouseId(10L);
        adjustment.setOperatorId(30L);
        adjustment.setReason("Stock take difference");
        adjustment.setTotalAdjustQuantity(5);
        adjustment.setStatus(status);
        return adjustment;
    }

    private InventoryAdjustmentItem adjustmentItem(Long productId, Integer adjustQuantity) {
        InventoryAdjustmentItem item = new InventoryAdjustmentItem();
        item.setId(1L);
        item.setAdjustmentId(1L);
        item.setProductId(productId);
        item.setAdjustQuantity(adjustQuantity);
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
