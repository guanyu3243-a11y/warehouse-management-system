# 数据库设计文档

## 设计目标

数据库用于支撑服装仓库管理系统的核心业务，包括用户认证、商品资料、仓库资料、供应商资料、库存数量、入库单、出库单和操作日志。

本阶段只设计表结构草案，不创建 SQL 脚本。正式 DDL 会在数据库实现阶段补充。

## 通用约定

- 主键统一使用 `id BIGINT`。
- 时间字段统一使用 `created_at` 和 `updated_at`。
- 逻辑状态字段优先使用 `status`，例如 `ACTIVE`、`DISABLED`。
- 金额字段使用 `DECIMAL(10,2)`。
- 库存数量使用 `INT`。
- 表名使用小写复数形式。
- 字段名使用 snake_case。
- MyBatis-Plus 实体类后续会按这些字段映射。

## 表清单

| 表名 | 说明 |
| --- | --- |
| `users` | 用户表 |
| `categories` | 商品分类表 |
| `products` | 服装商品表 |
| `warehouses` | 仓库表 |
| `suppliers` | 供应商表 |
| `stock` | 库存表 |
| `stock_in` | 入库单表 |
| `stock_in_items` | 入库明细表 |
| `stock_out` | 出库单表 |
| `stock_out_items` | 出库明细表 |
| `operation_logs` | 操作日志表 |

## users 用户表

用于保存系统用户信息和登录认证基础数据。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `username` | VARCHAR(50) | 用户名，唯一 |
| `password` | VARCHAR(255) | 加密后的密码 |
| `email` | VARCHAR(100) | 邮箱 |
| `phone` | VARCHAR(30) | 手机号 |
| `role` | VARCHAR(30) | 角色，例如 `ADMIN`、`STAFF` |
| `status` | VARCHAR(20) | 状态，例如 `ACTIVE`、`DISABLED` |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`username`
- 普通索引：`role`、`status`

## categories 商品分类表

用于管理服装商品分类，例如 T-shirt、Hoodie、Jeans、Jacket。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `name` | VARCHAR(100) | 分类名称 |
| `code` | VARCHAR(50) | 分类编码，唯一 |
| `description` | VARCHAR(255) | 分类描述 |
| `sort_order` | INT | 排序值 |
| `status` | VARCHAR(20) | 状态 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`code`
- 普通索引：`status`

## products 服装商品表

用于保存服装商品基础资料。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `sku` | VARCHAR(80) | SKU，唯一 |
| `name` | VARCHAR(150) | 商品名称 |
| `category_id` | BIGINT | 商品分类 ID |
| `size` | VARCHAR(30) | 尺码，例如 S、M、L、XL |
| `color` | VARCHAR(50) | 颜色 |
| `brand` | VARCHAR(100) | 品牌 |
| `season` | VARCHAR(50) | 季节，例如 Spring、Summer、Autumn、Winter |
| `cost_price` | DECIMAL(10,2) | 成本价 |
| `sale_price` | DECIMAL(10,2) | 售价 |
| `low_stock_threshold` | INT | 低库存阈值 |
| `status` | VARCHAR(20) | 状态 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`sku`
- 普通索引：`category_id`、`brand`、`season`、`status`

## warehouses 仓库表

用于维护仓库信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `code` | VARCHAR(50) | 仓库编码，唯一 |
| `name` | VARCHAR(100) | 仓库名称 |
| `address` | VARCHAR(255) | 仓库地址 |
| `contact_name` | VARCHAR(50) | 联系人 |
| `contact_phone` | VARCHAR(30) | 联系电话 |
| `status` | VARCHAR(20) | 状态 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`code`
- 普通索引：`status`

## suppliers 供应商表

用于维护供应商信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `code` | VARCHAR(50) | 供应商编码，唯一 |
| `name` | VARCHAR(150) | 供应商名称 |
| `contact_name` | VARCHAR(50) | 联系人 |
| `phone` | VARCHAR(30) | 联系电话 |
| `email` | VARCHAR(100) | 邮箱 |
| `address` | VARCHAR(255) | 地址 |
| `status` | VARCHAR(20) | 状态 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`code`
- 普通索引：`status`

## stock 库存表

用于记录每个商品在每个仓库中的库存数量。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `product_id` | BIGINT | 商品 ID |
| `warehouse_id` | BIGINT | 仓库 ID |
| `quantity` | INT | 当前可用库存 |
| `locked_quantity` | INT | 锁定库存，预留字段 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`product_id` + `warehouse_id`
- 普通索引：`warehouse_id`

## stock_in 入库单表

用于记录一次入库业务的主信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `stock_in_no` | VARCHAR(80) | 入库单号，唯一 |
| `warehouse_id` | BIGINT | 入库仓库 ID |
| `supplier_id` | BIGINT | 供应商 ID |
| `operator_id` | BIGINT | 操作用户 ID |
| `total_quantity` | INT | 入库总数量 |
| `total_amount` | DECIMAL(10,2) | 入库总金额 |
| `status` | VARCHAR(20) | 单据状态，例如 `DRAFT`、`CONFIRMED`、`CANCELLED` |
| `remark` | VARCHAR(255) | 备注 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`stock_in_no`
- 普通索引：`warehouse_id`、`supplier_id`、`operator_id`、`status`

## stock_in_items 入库明细表

用于记录入库单中的商品明细。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `stock_in_id` | BIGINT | 入库单 ID |
| `product_id` | BIGINT | 商品 ID |
| `quantity` | INT | 入库数量 |
| `unit_cost` | DECIMAL(10,2) | 单件成本 |
| `amount` | DECIMAL(10,2) | 明细金额 |
| `remark` | VARCHAR(255) | 备注 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 普通索引：`stock_in_id`、`product_id`

## stock_out 出库单表

用于记录一次出库业务的主信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `stock_out_no` | VARCHAR(80) | 出库单号，唯一 |
| `warehouse_id` | BIGINT | 出库仓库 ID |
| `operator_id` | BIGINT | 操作用户 ID |
| `total_quantity` | INT | 出库总数量 |
| `total_amount` | DECIMAL(10,2) | 出库总金额 |
| `status` | VARCHAR(20) | 单据状态，例如 `DRAFT`、`CONFIRMED`、`CANCELLED` |
| `remark` | VARCHAR(255) | 备注 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 唯一索引：`stock_out_no`
- 普通索引：`warehouse_id`、`operator_id`、`status`

## stock_out_items 出库明细表

用于记录出库单中的商品明细。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `stock_out_id` | BIGINT | 出库单 ID |
| `product_id` | BIGINT | 商品 ID |
| `quantity` | INT | 出库数量 |
| `unit_sale_price` | DECIMAL(10,2) | 单件售价 |
| `amount` | DECIMAL(10,2) | 明细金额 |
| `remark` | VARCHAR(255) | 备注 |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

建议索引：

- 普通索引：`stock_out_id`、`product_id`

## operation_logs 操作日志表

用于记录用户关键操作，便于审计和排查问题。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `user_id` | BIGINT | 操作用户 ID |
| `module` | VARCHAR(80) | 模块名称 |
| `action` | VARCHAR(80) | 操作类型，例如 `CREATE`、`UPDATE`、`DELETE`、`LOGIN` |
| `method` | VARCHAR(10) | HTTP 方法 |
| `request_uri` | VARCHAR(255) | 请求路径 |
| `request_ip` | VARCHAR(50) | 请求 IP |
| `description` | VARCHAR(500) | 操作描述 |
| `created_at` | DATETIME | 创建时间 |

建议索引：

- 普通索引：`user_id`、`module`、`action`、`created_at`

## 主要关系

- 一个分类可以拥有多个商品：`categories.id` -> `products.category_id`
- 一个商品可以存在于多个仓库：通过 `stock` 关联
- 一个仓库可以拥有多个库存记录：`warehouses.id` -> `stock.warehouse_id`
- 一个供应商可以对应多个入库单：`suppliers.id` -> `stock_in.supplier_id`
- 一个入库单可以拥有多条入库明细：`stock_in.id` -> `stock_in_items.stock_in_id`
- 一个出库单可以拥有多条出库明细：`stock_out.id` -> `stock_out_items.stock_out_id`
- 用户可以创建入库单、出库单和操作日志：`users.id` -> `operator_id` / `user_id`

## 后续待确认

- 是否需要多角色权限，例如管理员、仓库主管、普通仓库员。
- 是否需要库存流水表，用于完整追踪每一次库存变化。
- 是否需要出库客户信息或订单来源。
- 是否需要商品图片、条码、批次号、货架位置等字段。
