package com.warehouse.management.service.impl;

import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.ProductResponse;
import com.warehouse.management.service.ExcelService;
import com.warehouse.management.service.ProductService;
import com.warehouse.management.service.StockInService;
import com.warehouse.management.service.StockMovementService;
import com.warehouse.management.service.StockOutService;
import com.warehouse.management.service.StockService;
import com.warehouse.management.service.SupplierService;
import com.warehouse.management.service.WarehouseService;
import com.warehouse.management.util.PaginationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessExcelServiceImplTests {

    @Mock
    private ExcelService excelService;

    @Mock
    private ProductService productService;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private StockService stockService;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockInService stockInService;

    @Mock
    private StockOutService stockOutService;

    private BusinessExcelServiceImpl excelExportService;

    @BeforeEach
    void setUp() {
        excelExportService = new BusinessExcelServiceImpl(
                excelService,
                productService,
                warehouseService,
                supplierService,
                stockService,
                stockMovementService,
                stockInService,
                stockOutService
        );
    }

    @Test
    void exportProductsRunsPageQueryWithExportPageSizeLimit() {
        when(productService.page(anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    long requestedSize = invocation.getArgument(1);
                    assertThat(PaginationSupport.normalizeSize(requestedSize)).isEqualTo(10000);
                    return new PageResponse<ProductResponse>(List.of(), 0, 1, requestedSize);
                });
        when(excelService.writeWorkbook(anyString(), anyList(), anyList())).thenReturn(new byte[]{42});

        byte[] bytes = excelExportService.exportProducts(null, null, null, null, null, null, null);

        assertThat(bytes).containsExactly((byte) 42);
    }
}
