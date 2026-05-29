package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.management.common.BusinessException;
import com.warehouse.management.dto.CategoryRequest;
import com.warehouse.management.dto.CategoryResponse;
import com.warehouse.management.dto.PageResponse;
import com.warehouse.management.entity.Category;
import com.warehouse.management.entity.Product;
import com.warehouse.management.mapper.CategoryMapper;
import com.warehouse.management.mapper.ProductMapper;
import com.warehouse.management.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CategoryMapper categoryMapper;

    private final ProductMapper productMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResponse<CategoryResponse> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Category> query = Wrappers.lambdaQuery();
        if (hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(Category::getName, value)
                    .or()
                    .like(Category::getCode, value));
        }
        if (hasText(status)) {
            query.eq(Category::getStatus, status.trim());
        }
        query.orderByAsc(Category::getSortOrder).orderByDesc(Category::getCreatedAt);

        IPage<Category> result = categoryMapper.selectPage(
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
    public CategoryResponse getById(Long id) {
        return toResponse(getExisting(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        ensureCodeUnique(request.code(), null);

        Category category = new Category();
        applyRequest(category, request);
        categoryMapper.insert(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getExisting(id);
        ensureCodeUnique(request.code(), id);

        applyRequest(category, request);
        categoryMapper.updateById(category);
        return toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getExisting(id);
        Long productCount = productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id)
        );
        if (productCount > 0) {
            throw BusinessException.badRequest("Category is already used by products");
        }
        categoryMapper.deleteById(id);
    }

    private Category getExisting(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.notFound("Category does not exist");
        }
        return category;
    }

    private void ensureCodeUnique(String code, Long excludedId) {
        LambdaQueryWrapper<Category> query = Wrappers.<Category>lambdaQuery()
                .eq(Category::getCode, code.trim());
        if (excludedId != null) {
            query.ne(Category::getId, excludedId);
        }
        if (categoryMapper.selectCount(query) > 0) {
            throw BusinessException.badRequest("Category code already exists");
        }
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setCode(request.code().trim());
        category.setDescription(trimToNull(request.description()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setStatus(hasText(request.status()) ? request.status().trim() : DEFAULT_STATUS);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getDescription(),
                category.getSortOrder(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt()
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
