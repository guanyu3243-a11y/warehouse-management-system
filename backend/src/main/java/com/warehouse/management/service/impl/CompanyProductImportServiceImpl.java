package com.warehouse.management.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.warehouse.management.service.CompanyProductImportService;
import com.warehouse.management.service.CompanyStockExcelParser;
import com.warehouse.management.service.StockMovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyProductImportServiceImpl implements CompanyProductImportService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final int MAX_SKU_LENGTH = 80;

    private static final DateTimeFormatter BATCH_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final CompanyStockExcelParser parser;

    private final CategoryMapper categoryMapper;

    private final WarehouseMapper warehouseMapper;

    private final ProductMapper productMapper;

    private final StockMapper stockMapper;

    private final StockMovementService stockMovementService;

    public CompanyProductImportServiceImpl(
            CompanyStockExcelParser parser,
            CategoryMapper categoryMapper,
            WarehouseMapper warehouseMapper,
            ProductMapper productMapper,
            StockMapper stockMapper,
            StockMovementService stockMovementService
    ) {
        this.parser = parser;
        this.categoryMapper = categoryMapper;
        this.warehouseMapper = warehouseMapper;
        this.productMapper = productMapper;
        this.stockMapper = stockMapper;
        this.stockMovementService = stockMovementService;
    }

    @Override
    @Transactional
    public CompanyProductImportResponse importProducts(Long categoryId, MultipartFile file) {
        Category category = getActiveCategory(categoryId);
        Map<String, ImportSpecification> specifications = parseSpecifications(file);
        Warehouse warehouse = getSingleActiveWarehouse();
        Map<String, Product> productsBySku = getProductsBySku();
        List<ImportItem> items = validateAndStage(specifications, productsBySku, warehouse.getId());
        CurrentUser currentUser = CurrentUserContext.getRequired();
        String batchNo = generateBatchNo();

        int createdProductCount = 0;
        int reusedProductCount = 0;
        int createdStockCount = 0;
        int updatedStockCount = 0;
        int unchangedStockCount = 0;
        int zeroStockCount = 0;

        for (ImportItem item : items) {
            Product product = item.product();
            if (product == null) {
                product = createProduct(item.specification(), category.getId());
                createdProductCount++;
            } else {
                reusedProductCount++;
            }

            Stock stock = item.stock();
            int quantityBefore = stock == null ? 0 : defaultInt(stock.getQuantity());
            int quantityAfter = item.specification().quantity();
            if (quantityAfter == 0) {
                zeroStockCount++;
            }

            if (stock == null) {
                stock = createStock(product.getId(), warehouse.getId(), quantityAfter);
                createdStockCount++;
            }

            if (quantityBefore == quantityAfter) {
                unchangedStockCount++;
                continue;
            }

            if (item.stock() != null) {
                int updated = stockMapper.updateQuantityById(stock.getId(), quantityAfter);
                if (updated != 1) {
                    throw BusinessException.badRequest("Stock update failed, please retry");
                }
            }

            stockMovementService.record(new StockMovementRecordCommand(
                    product.getId(),
                    warehouse.getId(),
                    "IMPORT",
                    "COMPANY_PRODUCT_IMPORT",
                    0L,
                    batchNo,
                    quantityBefore,
                    quantityAfter - quantityBefore,
                    quantityAfter,
                    currentUser.id(),
                    movementRemark(file)
            ));
            updatedStockCount++;
        }

        return new CompanyProductImportResponse(
                batchNo,
                category.getId(),
                category.getName(),
                warehouse.getId(),
                warehouse.getName(),
                items.size(),
                createdProductCount,
                reusedProductCount,
                createdStockCount,
                updatedStockCount,
                unchangedStockCount,
                zeroStockCount
        );
    }

    private Category getActiveCategory(Long categoryId) {
        if (categoryId == null) {
            throw BusinessException.badRequest("Category id is required");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !ACTIVE_STATUS.equals(category.getStatus())) {
            throw BusinessException.badRequest("Category does not exist or is disabled");
        }
        return category;
    }

    private Map<String, ImportSpecification> parseSpecifications(MultipartFile file) {
        Map<String, ImportSpecification> specifications = new LinkedHashMap<>();
        for (CompanyStockExcelParser.CompanyStockRow row : parser.parse(file)) {
            ImportSpecification specification = ImportSpecification.from(row);
            if (specification.sku().length() > MAX_SKU_LENGTH) {
                throw BusinessException.badRequest(
                        "Generated SKU exceeds 80 characters: " + specification.sku()
                );
            }
            ImportSpecification existing = specifications.putIfAbsent(
                    normalizeSku(specification.sku()),
                    specification
            );
            if (existing != null) {
                throw BusinessException.badRequest(
                        "Duplicate specification in Excel: " + specification.sku()
                                + " at rows " + existing.rowNumber() + " and " + specification.rowNumber()
                );
            }
        }
        return specifications;
    }

    private Warehouse getSingleActiveWarehouse() {
        List<Warehouse> warehouses = warehouseMapper.selectList(
                Wrappers.<Warehouse>lambdaQuery().eq(Warehouse::getStatus, ACTIVE_STATUS)
        );
        if (warehouses.size() != 1) {
            throw BusinessException.badRequest(
                    "Exactly one active warehouse is required for company product import"
            );
        }
        return warehouses.get(0);
    }

    private Map<String, Product> getProductsBySku() {
        Map<String, Product> productsBySku = new LinkedHashMap<>();
        for (Product product : productMapper.selectList(Wrappers.lambdaQuery())) {
            String key = normalizeSku(product.getSku());
            if (productsBySku.putIfAbsent(key, product) != null) {
                throw BusinessException.badRequest("Multiple products use the same SKU: " + product.getSku());
            }
        }
        return productsBySku;
    }

    private List<ImportItem> validateAndStage(
            Map<String, ImportSpecification> specifications,
            Map<String, Product> productsBySku,
            Long warehouseId
    ) {
        List<ImportItem> items = new ArrayList<>();
        for (Map.Entry<String, ImportSpecification> entry : specifications.entrySet()) {
            ImportSpecification specification = entry.getValue();
            Product product = productsBySku.get(entry.getKey());
            Stock stock = null;

            if (product != null) {
                validateExistingProduct(product, specification);
                stock = stockMapper.selectByProductAndWarehouseForUpdate(product.getId(), warehouseId);
                int lockedQuantity = stock == null ? 0 : defaultInt(stock.getLockedQuantity());
                if (specification.quantity() < lockedQuantity) {
                    throw BusinessException.badRequest(
                            "Imported quantity cannot be less than locked quantity for SKU: " + specification.sku()
                    );
                }
            }
            items.add(new ImportItem(specification, product, stock));
        }
        return items;
    }

    private void validateExistingProduct(Product product, ImportSpecification specification) {
        if (!ACTIVE_STATUS.equals(product.getStatus())) {
            throw BusinessException.badRequest("Existing product is disabled for SKU: " + specification.sku());
        }
        if (!sameText(product.getName(), specification.model())
                || !sameText(product.getColor(), specification.color())
                || !sameSize(product.getSize(), specification.size())) {
            throw BusinessException.badRequest(
                    "Existing product conflicts with Excel for SKU: " + specification.sku()
            );
        }
    }

    private Product createProduct(ImportSpecification specification, Long categoryId) {
        Product product = new Product();
        product.setSku(specification.sku());
        product.setName(specification.model());
        product.setCategoryId(categoryId);
        product.setSize(specification.size());
        product.setColor(specification.color());
        product.setBrand(null);
        product.setSeason(null);
        product.setCostPrice(BigDecimal.ZERO);
        product.setSalePrice(BigDecimal.ZERO);
        product.setLowStockThreshold(0);
        product.setStatus(ACTIVE_STATUS);
        int inserted = productMapper.insert(product);
        if (inserted != 1 || product.getId() == null) {
            throw BusinessException.badRequest("Product creation failed for SKU: " + specification.sku());
        }
        return product;
    }

    private Stock createStock(Long productId, Long warehouseId, int quantity) {
        Stock stock = new Stock();
        stock.setProductId(productId);
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(quantity);
        stock.setLockedQuantity(0);
        stock.setVersion(0);
        int inserted = stockMapper.insert(stock);
        if (inserted != 1) {
            throw BusinessException.badRequest("Stock creation failed, please retry");
        }
        return stock;
    }

    private String generateBatchNo() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "CPI" + LocalDateTime.now().format(BATCH_NO_TIME_FORMAT) + suffix;
    }

    private String movementRemark(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String remark = "Company product Excel import";
        if (filename != null && !filename.trim().isEmpty()) {
            remark += ": " + filename.trim();
        }
        return remark.length() <= 255 ? remark : remark.substring(0, 255);
    }

    private String normalizeSku(String sku) {
        return normalizeText(sku).toUpperCase(Locale.ROOT);
    }

    private boolean sameText(String left, String right) {
        return normalizeText(left).equals(normalizeText(right));
    }

    private boolean sameSize(String left, String right) {
        return normalizeText(left).equalsIgnoreCase(normalizeText(right));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record ImportSpecification(
            int rowNumber,
            String sku,
            String model,
            String color,
            String size,
            int quantity
    ) {

        private static ImportSpecification from(CompanyStockExcelParser.CompanyStockRow row) {
            String model = row.model().trim();
            String color = row.color().trim();
            String size = row.size().trim().toUpperCase(Locale.ROOT);
            return new ImportSpecification(
                    row.rowNumber(),
                    model + "-" + color + "-" + size,
                    model,
                    color,
                    size,
                    row.quantity()
            );
        }
    }

    private record ImportItem(
            ImportSpecification specification,
            Product product,
            Stock stock
    ) {
    }
}
