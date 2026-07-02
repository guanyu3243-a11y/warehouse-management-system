package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.util.PaginationSupport;
import com.warehouse.management.entity.Product;
import com.warehouse.management.mapper.CategoryMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockInItemMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTests {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private StockMapper stockMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private StockOutItemMapper stockOutItemMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Product.class);
        productService = new ProductServiceImpl(
                productMapper,
                categoryMapper,
                stockMapper,
                stockInItemMapper,
                stockOutItemMapper
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageFuzzyFiltersByColorAndSize() {
        Page<Product> emptyPage = new Page<>(1, 10);
        when(productMapper.selectPage(any(), any())).thenReturn(emptyPage);

        productService.page(1, 10, null, null, null, null, null, "白", "120");

        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectPage(any(IPage.class), queryCaptor.capture());

        assertThat(queryCaptor.getValue().getExpression().getNormal())
                .filteredOn(SqlKeyword.LIKE::equals)
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageKeywordSearchIncludesColorAndSize() {
        Page<Product> emptyPage = new Page<>(1, 10);
        when(productMapper.selectPage(any(), any())).thenReturn(emptyPage);

        productService.page(1, 10, "7XL", null, null, null, null, null, null);

        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productMapper).selectPage(any(IPage.class), queryCaptor.capture());

        LambdaQueryWrapper<Product> query = queryCaptor.getValue();
        query.getSqlSegment();

        assertThat(query.getParamNameValuePairs()).hasSize(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageCapsNormalRequestsAtOneHundredRows() {
        Page<Product> emptyPage = new Page<>(1, 100);
        when(productMapper.selectPage(any(), any())).thenReturn(emptyPage);

        productService.page(1, 10000, null, null, null, null, null, null, null);

        ArgumentCaptor<IPage<Product>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(productMapper).selectPage(pageCaptor.capture(), any());

        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageAllowsExportRequestsUpToExportLimit() {
        Page<Product> emptyPage = new Page<>(1, 10000);
        when(productMapper.selectPage(any(), any())).thenReturn(emptyPage);

        PaginationSupport.withMaxPageSize(10000, () ->
                productService.page(1, 10000, null, null, null, null, null, null, null)
        );

        ArgumentCaptor<IPage<Product>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(productMapper).selectPage(pageCaptor.capture(), any());

        assertThat(pageCaptor.getValue().getSize()).isEqualTo(10000);
    }
}
