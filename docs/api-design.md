# API 接口设计初稿

## 设计目标

本文档用于描述服装仓库管理系统的后端 REST API 初稿。当前只做接口规划，不实现后端代码。

## 通用约定

基础路径：

```text
/api
```

认证方式：

```text
Authorization: Bearer <token>
```

通用响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

常见状态码：

| code | 说明 |
| --- | --- |
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或 token 无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

## Auth 用户认证

| 方法 | 路径 | 说明 | 是否需要登录 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 用户注册 | 否 |
| POST | `/api/auth/login` | 用户登录并返回 JWT | 否 |
| GET | `/api/auth/me` | 获取当前登录用户信息 | 是 |
| POST | `/api/auth/logout` | 用户退出登录 | 是 |

注册请求字段：

| 字段 | 说明 |
| --- | --- |
| `username` | 用户名 |
| `password` | 密码 |
| `email` | 邮箱 |
| `phone` | 手机号 |

登录请求字段：

| 字段 | 说明 |
| --- | --- |
| `username` | 用户名 |
| `password` | 密码 |

## Categories 商品分类

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/categories` | 分页查询分类 |
| GET | `/api/categories/{id}` | 查询分类详情 |
| POST | `/api/categories` | 新增分类 |
| PUT | `/api/categories/{id}` | 修改分类 |
| DELETE | `/api/categories/{id}` | 删除分类 |

主要字段：

- `name`
- `code`
- `description`
- `sortOrder`
- `status`

## Products 服装商品

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/products` | 分页查询商品 |
| GET | `/api/products/{id}` | 查询商品详情 |
| POST | `/api/products` | 新增商品 |
| PUT | `/api/products/{id}` | 修改商品 |
| DELETE | `/api/products/{id}` | 删除商品 |

主要查询条件：

- `sku`
- `name`
- `categoryId`
- `brand`
- `season`
- `status`

主要字段：

- `sku`
- `name`
- `categoryId`
- `size`
- `color`
- `brand`
- `season`
- `costPrice`
- `salePrice`
- `lowStockThreshold`
- `status`

## Warehouses 仓库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/warehouses` | 分页查询仓库 |
| GET | `/api/warehouses/{id}` | 查询仓库详情 |
| POST | `/api/warehouses` | 新增仓库 |
| PUT | `/api/warehouses/{id}` | 修改仓库 |
| DELETE | `/api/warehouses/{id}` | 删除仓库 |

主要字段：

- `code`
- `name`
- `address`
- `contactName`
- `contactPhone`
- `status`

## Suppliers 供应商

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/suppliers` | 分页查询供应商 |
| GET | `/api/suppliers/{id}` | 查询供应商详情 |
| POST | `/api/suppliers` | 新增供应商 |
| PUT | `/api/suppliers/{id}` | 修改供应商 |
| DELETE | `/api/suppliers/{id}` | 删除供应商 |

主要字段：

- `code`
- `name`
- `contactName`
- `phone`
- `email`
- `address`
- `status`

## Stock 库存

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/stock` | 分页查询库存 |
| GET | `/api/stock/low` | 查询低库存商品 |
| GET | `/api/stock/product/{productId}` | 查询某商品在各仓库的库存 |

主要查询条件：

- `warehouseId`
- `productName`
- `sku`
- `categoryId`
- `lowStockOnly`

库存返回字段：

- `productId`
- `sku`
- `productName`
- `categoryName`
- `warehouseId`
- `warehouseName`
- `quantity`
- `lockedQuantity`
- `lowStockThreshold`
- `lowStock`

## Stock In 入库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/stock-in` | 分页查询入库单 |
| GET | `/api/stock-in/{id}` | 查询入库单详情 |
| POST | `/api/stock-in` | 创建入库单 |
| PUT | `/api/stock-in/{id}` | 修改草稿入库单 |
| POST | `/api/stock-in/{id}/confirm` | 确认入库并增加库存 |
| POST | `/api/stock-in/{id}/cancel` | 取消入库单 |

创建入库单主要字段：

- `warehouseId`
- `supplierId`
- `remark`
- `items`

入库明细字段：

- `productId`
- `quantity`
- `unitCost`
- `remark`

## Stock Out 出库

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/stock-out` | 分页查询出库单 |
| GET | `/api/stock-out/{id}` | 查询出库单详情 |
| POST | `/api/stock-out` | 创建出库单 |
| PUT | `/api/stock-out/{id}` | 修改草稿出库单 |
| POST | `/api/stock-out/{id}/confirm` | 确认出库并扣减库存 |
| POST | `/api/stock-out/{id}/cancel` | 取消出库单 |

创建出库单主要字段：

- `warehouseId`
- `remark`
- `items`

出库明细字段：

- `productId`
- `quantity`
- `unitSalePrice`
- `remark`

出库业务规则：

- 确认出库前必须校验库存是否充足。
- 库存不足时返回业务错误。
- 已确认或已取消的出库单不可再次修改。

## Dashboard 首页统计

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/dashboard/summary` | 获取首页汇总数据 |
| GET | `/api/dashboard/stock-trend` | 获取库存趋势数据 |
| GET | `/api/dashboard/low-stock-top` | 获取低库存商品排行 |

汇总数据初稿：

- 商品总数
- 分类总数
- 仓库总数
- 当前库存总量
- 低库存商品数量
- 今日入库数量
- 今日出库数量

## Operation Logs 操作日志

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/operation-logs` | 分页查询操作日志 |
| GET | `/api/operation-logs/{id}` | 查询日志详情 |

主要查询条件：

- `userId`
- `module`
- `action`
- `startTime`
- `endTime`

## 后续待细化

- 请求和响应 DTO 的完整字段。
- 统一错误码体系。
- 参数校验规则。
- 权限控制规则。
- 是否需要导入、导出 Excel。
- 是否需要库存流水接口。
