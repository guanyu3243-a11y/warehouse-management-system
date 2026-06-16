# Company Product Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import the company clothing matrix from the product page, create every model-color-size product, and initialize the only active warehouse stock in one transaction.

**Architecture:** Reuse the Apache POI company matrix parser. Replace the previous stock-only import service with a product-oriented transactional service keyed by generated SKU, expose it under `/api/products`, and add a product-page-specific dialog while preserving the existing standard Excel import.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Apache POI, JUnit 5, Mockito, Vue 3, Element Plus, Axios.

---

### Task 1: Replace stock-only service tests with product import tests

**Files:**
- Delete: `backend/src/test/java/com/warehouse/management/service/impl/CompanyStockImportServiceImplTests.java`
- Create: `backend/src/test/java/com/warehouse/management/service/impl/CompanyProductImportServiceImplTests.java`

- [x] Write tests for product creation, generated SKU, selected category, defaults, zero stock, existing product reuse, conflicts, disabled products, stock overwrite, movement creation, warehouse validation and locked stock.
- [x] Run `mvn "-Dtest=CompanyProductImportServiceImplTests" test`.
- [x] Verify compilation fails because the product import service and response do not exist.

### Task 2: Implement transactional product and inventory import

**Files:**
- Delete: `backend/src/main/java/com/warehouse/management/dto/CompanyStockImportResponse.java`
- Delete: `backend/src/main/java/com/warehouse/management/service/CompanyStockImportService.java`
- Delete: `backend/src/main/java/com/warehouse/management/service/impl/CompanyStockImportServiceImpl.java`
- Create: `backend/src/main/java/com/warehouse/management/dto/CompanyProductImportResponse.java`
- Create: `backend/src/main/java/com/warehouse/management/service/CompanyProductImportService.java`
- Create: `backend/src/main/java/com/warehouse/management/service/impl/CompanyProductImportServiceImpl.java`

- [x] Validate an active category and exactly one active warehouse.
- [x] Generate `model-color-size` SKU and reject duplicate or overlength specifications.
- [x] Reuse matching active products; reject disabled or conflicting existing products.
- [x] Create missing products with confirmed defaults.
- [x] Create or overwrite stock without deleting records.
- [x] Record changed quantities as `IMPORT / COMPANY_PRODUCT_IMPORT`.
- [x] Return batch, product and stock statistics.
- [x] Run parser and product import tests until all pass.

### Task 3: Move API to the product module

**Files:**
- Modify: `backend/src/main/java/com/warehouse/management/controller/InventoryAdjustmentController.java`
- Modify: `backend/src/main/java/com/warehouse/management/controller/ProductController.java`

- [x] Remove the previous inventory-adjustment upload endpoint and dependency.
- [x] Add `POST /api/products/import-company-stock` with `categoryId` and `file`.
- [x] Keep the existing `/api/products/import` endpoint unchanged.
- [x] Run controller compilation and backend tests.

### Task 4: Move the frontend workflow to Clothing Products

**Files:**
- Modify: `frontend/src/api/business.js`
- Modify: `frontend/src/views/stock/StockView.vue`
- Modify: `frontend/src/views/master-data/MasterCrudPage.vue`
- Modify: `frontend/src/views/master-data/ProductView.vue`

- [x] Remove the company import API from `stockApi` and all company import UI from `StockView.vue`.
- [x] Add `productApi.importCompanyStock(categoryId, file)`.
- [x] Add a named header action slot and exposed refresh method to `MasterCrudPage.vue`.
- [x] Add “导入公司商品表” to `ProductView.vue`.
- [x] Require an active category and `.xls/.xlsx` file, confirm the transactional import, display statistics, and refresh products.
- [x] Run `npm.cmd run build`.

### Task 5: Correct documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/user-guide.md`

- [x] Replace stock-page instructions with product-page import instructions.
- [x] Document category selection, SKU generation, all-size creation, defaults, product reuse, stock initialization, movements and atomic rollback.
- [x] Document `POST /api/products/import-company-stock`.

### Task 6: Full verification

**Files:**
- Verify all changed files.

- [x] Run `mvn test` and require `BUILD SUCCESS`.
- [x] Run `npm.cmd run build`.
- [x] Run `git diff --check`.
- [x] Confirm login, permission, Docker, stock-in, stock-out and stock-take files are unchanged.
