package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.dto.ProductRequest;
import com.warehouse.management.dto.ProductResponse;
import com.warehouse.management.entity.Category;
import com.warehouse.management.entity.Product;
import com.warehouse.management.entity.Stock;
import com.warehouse.management.entity.StockInItem;
import com.warehouse.management.entity.StockOutItem;
import com.warehouse.management.mapper.CategoryMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.mapper.StockInItemMapper;
import com.warehouse.management.mapper.StockMapper;
import com.warehouse.management.mapper.StockOutItemMapper;
import com.warehouse.management.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final StockMapper stockMapper;

    private final StockInItemMapper stockInItemMapper;

    private final StockOutItemMapper stockOutItemMapper;

    public ProductServiceImpl(
            ProductMapper productMapper,
            CategoryMapper categoryMapper,
            StockMapper stockMapper,
            StockInItemMapper stockInItemMapper,
            StockOutItemMapper stockOutItemMapper
    ) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.stockMapper = stockMapper;
        this.stockInItemMapper = stockInItemMapper;
        this.stockOutItemMapper = stockOutItemMapper;
    }

    @Override
    public PageResponse<ProductResponse> page(
            long page,
            long size,
            String keyword,
            Long categoryId,
            String brand,
            String season,
            String status
    ) {
        LambdaQueryWrapper<Product> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Product::getName, value)
                    .or()
                    .like(Product::getSku, value)
                    .or()
                    .like(Product::getBrand, value));
        }
        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }
        if (hasText(brand)) {
            query.eq(Product::getBrand, brand.trim());
        }
        if (hasText(season)) {
            query.eq(Product::getSeason, season.trim());
        }
        if (hasText(status)) {
            query.eq(Product::getStatus, status.trim());
        }
        query.orderByDesc(Product::getCreatedAt);

        IPage<Product> result = productMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                query
        );
        return new PageResponse<>(
                result.getRecords().stream().map(this::toResponse).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public ProductResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        ensureSkuUnique(request.sku(), null);
        ensureCategoryExists(request.categoryId());

        Product product = new Product();
        applyRequest(product, request);
        productMapper.insert(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getExisting(id);
        ensureSkuUnique(request.sku(), id);
        ensureCategoryExists(request.categoryId());

        applyRequest(product, request);
        productMapper.updateById(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        long stockCount = stockMapper.selectCount(
                Wrappers.<Stock>lambdaQuery().eq(Stock::getProductId, id)
        );
        long stockInItemCount = stockInItemMapper.selectCount(
                Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getProductId, id)
        );
        long stockOutItemCount = stockOutItemMapper.selectCount(
                Wrappers.<StockOutItem>lambdaQuery().eq(StockOutItem::getProductId, id)
        );
        if (stockCount + stockInItemCount + stockOutItemCount > 0) {
            throw BusinessException.badRequest("Product is already used by stock records or documents");
        }
        productMapper.deleteById(id);
    }

    private Product getExisting(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw BusinessException.notFound("Product does not exist");
        }
        return product;
    }

    private void ensureSkuUnique(String sku, Long excludedId) {
        LambdaQueryWrapper<Product> query = Wrappers.<Product>lambdaQuery()
                .eq(Product::getSku, sku.trim());
        if (excludedId != null) {
            query.ne(Product::getId, excludedId);
        }
        if (productMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("SKU already exists");
        }
    }

    private void ensureCategoryExists(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.badRequest("Category does not exist");
        }
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setCategoryId(request.categoryId());
        product.setSize(trimToNull(request.size()));
        product.setColor(trimToNull(request.color()));
        product.setBrand(trimToNull(request.brand()));
        product.setSeason(trimToNull(request.season()));
        product.setCostPrice(request.costPrice() == null ? BigDecimal.ZERO : request.costPrice());
        product.setSalePrice(request.salePrice() == null ? BigDecimal.ZERO : request.salePrice());
        product.setLowStockThreshold(request.lowStockThreshold() == null ? 0 : request.lowStockThreshold());
        product.setStatus(hasText(request.status()) ? request.status().trim() : DEFAULT_STATUS);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategoryId(),
                product.getSize(),
                product.getColor(),
                product.getBrand(),
                product.getSeason(),
                product.getCostPrice(),
                product.getSalePrice(),
                product.getLowStockThreshold(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
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
}
