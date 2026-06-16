package com.warehouse.management.service.impl;

import com.warehouse.management.common.BusinessException;
import com.warehouse.management.common.CurrentUser;
import com.warehouse.management.common.CurrentUserContext;
import com.warehouse.management.dto.CompanyProductImportResponse;
import com.warehouse.management.dto.StockMovementRecordCommand;
import com.warehouse.management.entity.Category;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.Warehouse;
import com.warehouse.management.mapper.CategoryMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.WarehouseMapper;
import com.warehouse.management.service.CompanyStockExcelParser;
import com.warehouse.management.service.StockMovementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProductImportServiceImplTests {

    @Mock
    private CompanyStockExcelParser parser;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockMovementService stockMovementService;

    private CompanyProductImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        importService = new CompanyProductImportServiceImpl(
                parser,
                categoryMapper,
                warehouseMapper,
                productMapper,
                stockMapper,
                stockMovementService
        );
        CurrentUserContext.set(new CurrentUser(7L, "manager", "MANAGER"));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void createsMissingProductAndZeroStockWithConfirmedDefaults() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "XS", 0)));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(100L);
            return 1;
        });
        when(stockMapper.insert(any(Stock.class))).thenReturn(1);

        CompanyProductImportResponse response = importService.importProducts(20L, file());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product product = productCaptor.getValue();
        assertThat(product.getSku()).isEqualTo("V3-白色-XS");
        assertThat(product.getName()).isEqualTo("V3");
        assertThat(product.getCategoryId()).isEqualTo(20L);
        assertThat(product.getColor()).isEqualTo("白色");
        assertThat(product.getSize()).isEqualTo("XS");
        assertThat(product.getBrand()).isNull();
        assertThat(product.getSeason()).isNull();
        assertThat(product.getCostPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(product.getSalePrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(product.getLowStockThreshold()).isZero();
        assertThat(product.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockMapper).insert(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getProductId()).isEqualTo(100L);
        assertThat(stockCaptor.getValue().getWarehouseId()).isEqualTo(10L);
        assertThat(stockCaptor.getValue().getQuantity()).isZero();

        assertThat(response.specificationCount()).isEqualTo(1);
        assertThat(response.createdProductCount()).isEqualTo(1);
        assertThat(response.reusedProductCount()).isZero();
        assertThat(response.createdStockCount()).isEqualTo(1);
        assertThat(response.updatedStockCount()).isZero();
        assertThat(response.unchangedStockCount()).isEqualTo(1);
        assertThat(response.zeroStockCount()).isEqualTo(1);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    void createsPositiveStockAndRecordsProductImportMovement() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "S", 42)));
        when(productMapper.selectList(any())).thenReturn(List.of());
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(101L);
            return 1;
        });
        when(stockMapper.insert(any(Stock.class))).thenReturn(1);

        CompanyProductImportResponse response = importService.importProducts(20L, file());

        ArgumentCaptor<StockMovementRecordCommand> movementCaptor =
                ArgumentCaptor.forClass(StockMovementRecordCommand.class);
        verify(stockMovementService).record(movementCaptor.capture());
        StockMovementRecordCommand movement = movementCaptor.getValue();
        assertThat(movement.productId()).isEqualTo(101L);
        assertThat(movement.movementType()).isEqualTo("IMPORT");
        assertThat(movement.sourceType()).isEqualTo("COMPANY_PRODUCT_IMPORT");
        assertThat(movement.sourceNo()).isEqualTo(response.batchNo());
        assertThat(movement.quantityBefore()).isZero();
        assertThat(movement.changeQuantity()).isEqualTo(42);
        assertThat(movement.quantityAfter()).isEqualTo(42);
        assertThat(movement.operatorId()).isEqualTo(7L);
        assertThat(response.updatedStockCount()).isEqualTo(1);
    }

    @Test
    void reusesMatchingProductAndOverwritesExistingStock() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "M", 87)));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(102L, "V3-白色-M", "V3", "白色", "M", "ACTIVE")
        ));
        when(stockMapper.selectByProductAndWarehouseForUpdate(102L, 10L))
                .thenReturn(stock(50L, 102L, 10L, 10, 0));
        when(stockMapper.updateQuantityById(50L, 87)).thenReturn(1);

        CompanyProductImportResponse response = importService.importProducts(20L, file());

        verify(productMapper, never()).insert(any(Product.class));
        verify(stockMapper).updateQuantityById(50L, 87);
        assertThat(response.createdProductCount()).isZero();
        assertThat(response.reusedProductCount()).isEqualTo(1);
        assertThat(response.updatedStockCount()).isEqualTo(1);
    }

    @Test
    void existingProductKeepsItsOriginalCategoryAndCommercialFields() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "L", 8)));
        Product existing = product(103L, "V3-白色-L", "V3", "白色", "L", "ACTIVE");
        existing.setCategoryId(99L);
        existing.setBrand("Existing brand");
        existing.setSalePrice(BigDecimal.TEN);
        when(productMapper.selectList(any())).thenReturn(List.of(existing));
        when(stockMapper.selectByProductAndWarehouseForUpdate(103L, 10L))
                .thenReturn(stock(51L, 103L, 10L, 8, 0));

        CompanyProductImportResponse response = importService.importProducts(20L, file());

        verify(productMapper, never()).updateById(any(Product.class));
        verify(stockMapper, never()).updateQuantityById(any(), any());
        assertThat(existing.getCategoryId()).isEqualTo(99L);
        assertThat(existing.getBrand()).isEqualTo("Existing brand");
        assertThat(existing.getSalePrice()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(response.unchangedStockCount()).isEqualTo(1);
    }

    @Test
    void conflictingExistingSkuPreventsEveryWrite() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "S", 42)));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(100L, "V3-白色-S", "Wrong model", "白色", "S", "ACTIVE")
        ));

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Existing product conflicts with Excel")
                .hasMessageContaining("V3-白色-S");

        verify(productMapper, never()).insert(any(Product.class));
        verifyNoInteractions(stockMapper, stockMovementService);
    }

    @Test
    void disabledExistingSkuPreventsEveryWrite() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "S", 42)));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(100L, "V3-白色-S", "V3", "白色", "S", "DISABLED")
        ));

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Existing product is disabled")
                .hasMessageContaining("V3-白色-S");

        verify(productMapper, never()).insert(any(Product.class));
        verifyNoInteractions(stockMapper, stockMovementService);
    }

    @Test
    void inactiveCategoryIsRejectedBeforeParsingOrWriting() {
        Category category = category(20L, "服装", "DISABLED");
        when(categoryMapper.selectById(20L)).thenReturn(category);

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Category does not exist or is disabled");

        verifyNoInteractions(parser, warehouseMapper, productMapper, stockMapper, stockMovementService);
    }

    @Test
    void requiresExactlyOneActiveWarehouse() {
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, "服装", "ACTIVE"));
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "S", 1)));
        when(warehouseMapper.selectList(any())).thenReturn(List.of(
                warehouse(10L, "主仓库"),
                warehouse(11L, "备用仓库")
        ));

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Exactly one active warehouse is required for company product import");

        verifyNoInteractions(productMapper, stockMapper, stockMovementService);
    }

    @Test
    void duplicateExcelSpecificationIsRejected() {
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, "服装", "ACTIVE"));
        when(parser.parse(any())).thenReturn(List.of(
                row(3, "V3", "白色", "S", 42),
                row(4, "V3", "白色", "S", 51)
        ));

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate specification in Excel")
                .hasMessageContaining("V3-白色-S");

        verifyNoInteractions(warehouseMapper, productMapper, stockMapper, stockMovementService);
    }

    @Test
    void targetBelowLockedQuantityPreventsEveryWrite() {
        mockActiveCategoryAndWarehouse();
        when(parser.parse(any())).thenReturn(List.of(row(3, "V3", "白色", "S", 3)));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(100L, "V3-白色-S", "V3", "白色", "S", "ACTIVE")
        ));
        when(stockMapper.selectByProductAndWarehouseForUpdate(100L, 10L))
                .thenReturn(stock(50L, 100L, 10L, 10, 5));

        assertThatThrownBy(() -> importService.importProducts(20L, file()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be less than locked quantity")
                .hasMessageContaining("V3-白色-S");

        verify(productMapper, never()).insert(any(Product.class));
        verify(stockMapper, never()).updateQuantityById(any(), any());
        verify(stockMapper, never()).insert(any(Stock.class));
        verifyNoInteractions(stockMovementService);
    }

    private void mockActiveCategoryAndWarehouse() {
        when(categoryMapper.selectById(20L)).thenReturn(category(20L, "服装", "ACTIVE"));
        when(warehouseMapper.selectList(any())).thenReturn(List.of(warehouse(10L, "主仓库")));
    }

    private CompanyStockExcelParser.CompanyStockRow row(
            int rowNumber,
            String model,
            String color,
            String size,
            int quantity
    ) {
        return new CompanyStockExcelParser.CompanyStockRow(rowNumber, model, color, size, quantity);
    }

    private Category category(Long id, String name, String status) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }

    private Product product(
            Long id,
            String sku,
            String name,
            String color,
            String size,
            String status
    ) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        product.setColor(color);
        product.setSize(size);
        product.setStatus(status);
        return product;
    }

    private Warehouse warehouse(Long id, String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setStatus("ACTIVE");
        return warehouse;
    }

    private Stock stock(
            Long id,
            Long productId,
            Long warehouseId,
            Integer quantity,
            Integer lockedQuantity
    ) {
        Stock stock = new Stock();
        stock.setId(id);
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(quantity);
        stock.setLockedQuantity(lockedQuantity);
        stock.setVersion(0);
        return stock;
    }

    private MockMultipartFile file() {
        return new MockMultipartFile(
                "file",
                "2026年6月5日更新数量统计表(1).xls",
                "application/vnd.ms-excel",
                new byte[]{1}
        );
    }
}
