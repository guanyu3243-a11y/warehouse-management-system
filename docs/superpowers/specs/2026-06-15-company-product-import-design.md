# 公司服装商品与库存一键导入设计

## 1. 目标

在“服装商品”模块直接导入公司现用的：

`2026年6月5日更新数量统计表(1).xls`

系统应自动将表格中的型号、颜色和 `XS` 至 `6XL` 尺码展开为商品规格，同时初始化唯一启用仓库的库存。用户不需要逐条录入商品。

本功能是现有标准商品 Excel 导入的补充，不替换 `POST /api/products/import`，也不改变登录、权限、Docker、入库、出库、库存调整和盘点流程。

## 2. 已确认业务规则

1. 入口位于“服装商品”页面，不位于“库存查询”页面。
2. 上传时必须选择一个状态为 `ACTIVE` 的商品分类。
3. 企业当前只有一个仓库，后端自动使用唯一状态为 `ACTIVE` 的仓库。
4. 每个“型号 + 颜色”创建 `XS、S、M、L、XL、2XL、3XL、4XL、5XL、6XL` 全部十个商品规格。
5. 数量为空时按 `0` 处理，零库存规格也必须创建商品和库存记录。
6. SKU 自动生成为：

   ```text
   型号-颜色-尺码
   ```

7. 商品名称使用 Excel 型号，颜色和尺码分别写入对应商品字段。
8. 新商品统一使用用户选择的分类。
9. 新商品默认值：
   - `brand = null`
   - `season = null`
   - `cost_price = 0`
   - `sale_price = 0`
   - `low_stock_threshold = 0`
   - `status = ACTIVE`
10. 重复导入时，相同 SKU 不重复创建商品，保留已有商品资料，只覆盖库存。
11. 相同 SKU 的已有商品如果名称、颜色或尺码与 Excel 不一致，整次导入失败。
12. 相同 SKU 的已有商品如果已禁用，整次导入失败，不自动启用。
13. 导入数量是目标库存总量，不是累加数量。
14. 不物理删除任何商品或库存记录。
15. 库存发生变化时必须生成库存流水。
16. 任一解析、商品、分类、仓库、库存或流水错误均使整次导入回滚。

## 3. 模板解析

继续使用 Apache POI 和独立解析器 `CompanyStockExcelParser`，支持 `.xls` 与 `.xlsx`。

解析器查找包含以下表头的第一组数据：

```text
型号 | 颜色 | XS | S | M | L | XL | 2XL | 3XL | 4XL | 5XL | 6XL | 合计
```

解析规则：

- 工作表名称和标题行不写死。
- 型号为空时继承上一行非空型号。
- 空白数量转换为 `0`。
- 数量必须为非负整数。
- 行合计必须等于十个尺码数量之和。
- 忽略空白行、`合计` 和 `合计总量` 汇总行。
- 每个型号、颜色行输出十条规格数据。

## 4. 后端设计

### 4.1 API

新增：

```text
POST /api/products/import-company-stock
Content-Type: multipart/form-data

file: .xls 或 .xlsx
categoryId: 启用分类 ID
```

该路径自动沿用现有 `product:create` 权限，不修改权限拦截器或权限表。

### 4.2 服务

新增或调整：

- `CompanyProductImportService`
- `CompanyProductImportServiceImpl`
- `CompanyProductImportResponse`

服务方法：

```java
CompanyProductImportResponse importProducts(Long categoryId, MultipartFile file);
```

事务流程：

1. 校验所选分类存在且启用。
2. 解析完整 Excel。
3. 校验 Excel 规格无重复，SKU 长度不超过数据库限制。
4. 查找唯一启用仓库。
5. 按 SKU 查询现有商品。
6. 校验已有商品状态及名称、颜色、尺码一致性。
7. 锁定已有商品的对应库存记录。
8. 校验目标数量不小于锁定库存。
9. 创建缺失商品。
10. 为缺失库存创建库存记录，包括目标数量为零的规格。
11. 覆盖已有库存数量。
12. 为每个实际数量变化写入库存流水。
13. 返回导入批次和统计结果。

### 4.3 商品复用与冲突

SKU 使用 `trim(型号) + "-" + trim(颜色) + "-" + upper(trim(尺码))`。

对于已有 SKU：

- 名称、颜色和尺码一致且状态为 `ACTIVE`：复用商品。
- 名称、颜色或尺码不一致：返回明确冲突提示并回滚。
- 状态不是 `ACTIVE`：返回明确禁用提示并回滚。
- 分类、品牌、季节、价格和预警阈值保持原值，不被导入覆盖。

### 4.4 库存与流水

- 只允许一个启用仓库。
- 新商品和已有商品都在该仓库建立或更新库存。
- 零库存也建立 `stock` 记录。
- 已有库存使用行锁读取。
- 目标库存不能低于 `locked_quantity`。
- 数量未变化时不写无意义流水。
- 流水字段：

  ```text
  movement_type = IMPORT
  source_type = COMPANY_PRODUCT_IMPORT
  source_id = 0
  source_no = 导入批次号
  ```

## 5. 响应设计

```json
{
  "batchNo": "CPI20260615183000123ABCDEF",
  "categoryId": 1,
  "categoryName": "服装",
  "warehouseId": 1,
  "warehouseName": "主仓库",
  "specificationCount": 1190,
  "createdProductCount": 1189,
  "reusedProductCount": 1,
  "createdStockCount": 1189,
  "updatedStockCount": 830,
  "unchangedStockCount": 360,
  "zeroStockCount": 245
}
```

## 6. 前端设计

1. 删除“库存查询”页面中上一版“导入库存统计表”入口。
2. “服装商品”页面保留现有标准模板导入，并新增“导入公司商品表”按钮。
3. 新弹窗包含：
   - 启用分类选择框，必填；
   - `.xls/.xlsx` 文件上传；
   - 自动创建全部规格、覆盖库存和整表回滚说明；
   - 导入前二次确认。
4. 导入成功后显示商品创建数、商品复用数、库存创建数、库存更新数、未变化数、零库存数和批次号。
5. 导入成功后刷新商品列表。

## 7. 错误处理

业务错误必须明确，例如：

- `Category does not exist or is disabled`
- `Exactly one active warehouse is required for company product import`
- `Generated SKU exceeds 80 characters: ...`
- `Existing product is disabled for SKU: ...`
- `Existing product conflicts with Excel for SKU: ...`
- `Duplicate specification in Excel: ...`
- `Imported quantity cannot be less than locked quantity for SKU: ...`

所有错误通过现有统一异常响应返回，不显示笼统数据库错误。

## 8. 测试与验收

后端测试覆盖：

- 创建全部十个尺码规格，包括零库存。
- 自动 SKU 和默认商品字段正确。
- 用户选择分类应用于新商品。
- 已有相同 SKU 商品被复用。
- 已有商品资料冲突或已禁用时整表失败。
- 唯一启用仓库校验。
- 新建和覆盖库存。
- 零库存记录保留。
- 库存流水内容正确。
- 锁定库存校验。
- 后续错误触发事务回滚。

验收命令：

```text
cd backend && mvn test
cd frontend && npm run build
```

## 9. 非目标

- 不自动创建商品分类。
- 不修改已有商品的分类、品牌、季节、价格或预警阈值。
- 不支持多仓库选择。
- 不修改公司 Excel 文件。
- 不物理删除商品或库存。
- 不修改登录、权限或 Docker 配置。
