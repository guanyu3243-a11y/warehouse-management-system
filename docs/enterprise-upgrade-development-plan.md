# 企业级升级开发文档

## 文档目标

本文档用于指导服装仓库管理系统从课程项目 / MVP 升级为可企业落地的系统。

升级原则：

- 不推倒重来，不大规模重构现有可运行功能。
- 不破坏现有 API，优先通过新增表、字段、接口和页面扩展能力。
- 每个阶段都必须能独立运行、独立测试、独立提交 Git。
- 优先补齐企业落地最关键的三条主线：
  - 角色权限细化
  - 库存流水
  - Excel 导入 / 导出

当前系统已完成：

- 登录 / JWT
- 用户管理 / ADMIN 权限
- 商品分类管理
- 商品管理
- 仓库管理
- 供应商管理
- 入库 / 出库
- 库存查询
- 低库存预警
- Dashboard
- 操作日志
- Vue 前端页面
- README 和基础测试

## 企业级升级总览

当前系统已经具备仓库管理系统的主流程，但企业生产可用还需要补齐以下能力：

1. 权限体系从简单角色升级为 RBAC 权限模型。
2. 库存变化从只改当前库存升级为完整库存流水。
3. 出入库确认增加并发控制和幂等保护。
4. 支持 Excel 导入 / 导出，提升真实业务录入效率。
5. 操作日志升级为审计日志，能追踪关键字段变化。
6. 数据库变更从单个 `schema.sql` 升级为可迁移脚本。
7. 增加更完整的自动化测试、部署和运维准备。

## 阶段一：企业升级基础准备

### 目标

- 建立企业升级的基础技术底座。
- 为后续数据库变更、权限升级、库存流水和导入导出做准备。

### 为什么要先做

后续会新增多张表和较多接口，如果继续直接修改 `schema.sql`，后期很难管理数据库版本。企业项目必须能清楚知道每一次数据库变更。

### 后端改动

- 引入数据库迁移工具，推荐 Flyway。
- 新增迁移目录：
  - `backend/src/main/resources/db/migration`
- 将现有建表 SQL 整理为初始版本：
  - `V1__init_schema.sql`
- 增加统一错误码设计文档或枚举。
- 增加请求日志中的 requestId / traceId。
- 增加 `dev`、`test`、`prod` 配置说明。

### 前端改动

- 增加统一 403 / 404 页面。
- 完善 Axios 对 403、500 的提示。
- 保持现有页面不变。

### 数据库改动

- 暂不新增业务表。
- 只建立迁移机制。

### 测试

```bash
cd backend
mvn test

cd frontend
npm run build
```

### 验收标准

- 空数据库可以通过迁移脚本初始化。
- 现有登录、商品、入库、出库、库存查询流程不受影响。
- README 增加数据库迁移说明。

### 建议 Git commit

```text
chore: add database migration baseline
```

## 阶段二：角色权限细化 RBAC

### 目标

- 将当前 `ADMIN / STAFF` 简单角色升级为企业常用 RBAC 权限模型。
- 支持菜单权限、按钮权限、接口权限。
- 为仓库主管、仓库操作员、只读人员等角色提供扩展空间。

### 必须先做的原因

权限是企业系统的入口控制。后续库存流水、导入导出、报表、盘点、调拨都需要明确谁可以看、谁可以操作。

### 后端新增表

#### `roles`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `code` | 角色编码，例如 `ADMIN`、`MANAGER`、`STAFF`、`VIEWER` |
| `name` | 角色名称 |
| `description` | 说明 |
| `status` | 状态 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

#### `permissions`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `code` | 权限编码，例如 `product:create` |
| `name` | 权限名称 |
| `type` | 权限类型：`MENU`、`BUTTON`、`API` |
| `module` | 所属模块 |
| `path` | 前端路由或后端接口路径 |
| `method` | HTTP 方法，可为空 |
| `sort_order` | 排序 |
| `status` | 状态 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

#### `role_permissions`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `role_id` | 角色 ID |
| `permission_id` | 权限 ID |
| `created_at` | 创建时间 |

#### `user_roles`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `role_id` | 角色 ID |
| `created_at` | 创建时间 |

#### `user_warehouse_permissions`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `user_id` | 用户 ID |
| `warehouse_id` | 仓库 ID |
| `created_at` | 创建时间 |

### 后端新增接口

角色管理：

- `GET /api/roles`
- `GET /api/roles/{id}`
- `POST /api/roles`
- `PUT /api/roles/{id}`
- `PUT /api/roles/{id}/status`
- `DELETE /api/roles/{id}`

权限管理：

- `GET /api/permissions`
- `GET /api/permissions/tree`
- `POST /api/permissions`
- `PUT /api/permissions/{id}`
- `PUT /api/permissions/{id}/status`

授权：

- `GET /api/roles/{id}/permissions`
- `PUT /api/roles/{id}/permissions`
- `GET /api/users/{id}/roles`
- `PUT /api/users/{id}/roles`
- `GET /api/users/{id}/warehouses`
- `PUT /api/users/{id}/warehouses`
- `GET /api/auth/permissions`

### 后端模块

- `RoleController`
- `PermissionController`
- `RoleService`
- `PermissionService`
- `AuthorizationService`
- `PermissionInterceptor` 或权限注解
- DTO:
  - `RoleCreateRequest`
  - `RoleUpdateRequest`
  - `PermissionRequest`
  - `RolePermissionUpdateRequest`
  - `UserRoleUpdateRequest`
  - `UserWarehousePermissionUpdateRequest`

### 前端新增页面

- `frontend/src/views/system/RoleList.vue`
- `frontend/src/views/system/PermissionList.vue`
- `frontend/src/views/system/UserPermissionDialog.vue`

前端需要支持：

- 角色列表
- 新增 / 编辑角色
- 启用 / 禁用角色
- 权限树勾选
- 用户分配角色
- 用户分配仓库数据范围
- 菜单和按钮根据权限动态显示

### 不能破坏的地方

- 保留 `users.role` 字段作为兼容字段，短期内继续返回给前端。
- 保留当前 `ADMIN` 判断逻辑，RBAC 完成后再逐步替换。
- 不改变 `/api/auth/login`、`/api/auth/me` 的现有响应结构，只新增权限字段或新增接口。

### 测试

- ADMIN 可以访问用户、角色、权限管理接口。
- STAFF 访问角色权限接口返回 403。
- 用户没有某权限时，接口返回 403。
- 前端无权限菜单不显示。
- 手动访问无权限路由时跳转 Dashboard 或显示 403。

### 验收标准

- 系统内置角色至少包括：
  - 系统管理员
  - 仓库主管
  - 仓库操作员
  - 只读查看人员
- 每个角色可以配置菜单和按钮权限。
- 后端接口权限不能只依赖前端隐藏按钮。

### 建议 Git commit

```text
feat: add RBAC permissions
```

## 阶段三：库存流水

### 目标

- 所有库存变更必须留下流水记录。
- 入库、出库、后续调整、盘点、调拨都统一写入库存流水。
- 支持按商品、仓库、单据、时间查询库存变化。

### 必须先做的原因

企业仓库系统最重要的是库存可信。当前系统只保存当前库存，无法完整追踪每一次库存变化，排查库存差异会很困难。

### 数据库新增表

#### `stock_movements`

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `movement_no` | 流水号 |
| `product_id` | 商品 ID |
| `warehouse_id` | 仓库 ID |
| `movement_type` | 类型：`STOCK_IN`、`STOCK_OUT`、`ADJUSTMENT`、`TRANSFER_IN`、`TRANSFER_OUT`、`STOCK_TAKE` |
| `source_type` | 来源类型：`STOCK_IN`、`STOCK_OUT` 等 |
| `source_id` | 来源单据 ID |
| `source_no` | 来源单号 |
| `quantity_before` | 变更前库存 |
| `change_quantity` | 变化数量，入库为正，出库为负 |
| `quantity_after` | 变更后库存 |
| `operator_id` | 操作人 |
| `remark` | 备注 |
| `created_at` | 创建时间 |

建议索引：

- `product_id`
- `warehouse_id`
- `movement_type`
- `source_type + source_id`
- `created_at`

### 后端新增接口

- `GET /api/stock-movements`
- `GET /api/stock-movements/{id}`
- `GET /api/stock-movements/product/{productId}`
- `GET /api/stock-movements/warehouse/{warehouseId}`

查询条件：

- `productId`
- `warehouseId`
- `movementType`
- `sourceType`
- `sourceNo`
- `startTime`
- `endTime`
- `page`
- `size`

### 后端改动

- 新增 `StockMovement` 实体和 Mapper。
- 新增 `StockMovementService`。
- 改造 `StockInServiceImpl.confirm`：
  - 增加库存时同时写入库存流水。
- 改造 `StockOutServiceImpl.confirm`：
  - 扣减库存时同时写入库存流水。
- 库存流水写入必须与库存变更处于同一个事务。

### 前端新增页面

- `frontend/src/views/stock/StockMovementList.vue`

页面功能：

- 库存流水列表
- 按商品、仓库、流水类型、来源单号、时间范围查询
- 查看变更前、变更数量、变更后
- 从库存页、入库单、出库单跳转查看相关流水

### 不能破坏的地方

- 不改变现有 `stock` 表作为当前库存表的定位。
- 不改变入库、出库确认接口路径。
- 不改变现有库存查询接口响应，库存流水作为新增能力。

### 测试

- 确认入库后库存增加，同时生成正向流水。
- 确认出库后库存减少，同时生成负向流水。
- 库存不足时出库失败，不生成流水。
- 入库或出库事务失败时，库存和流水同时回滚。

### 验收标准

- 任意一条库存记录都能追溯其变更来源。
- 入库 / 出库确认后的库存数量与流水累计结果一致。

### 建议 Git commit

```text
feat: add stock movement ledger
```

## 阶段四：库存并发控制和幂等保护

### 目标

- 防止并发出库导致库存被扣成负数。
- 防止重复点击确认按钮导致重复入库或重复出库。

### 后端改动

- `stock` 表新增字段：
  - `version INT NOT NULL DEFAULT 0`
- 出库扣减时使用以下任一方案：
  - 数据库行锁：`SELECT ... FOR UPDATE`
  - 乐观锁：`WHERE id = ? AND version = ? AND quantity >= ?`
- 入库 / 出库单确认前重新检查状态。
- 确认成功后状态必须从 `DRAFT` 原子变更为 `CONFIRMED`。

### 新增测试

- 并发出库同一商品同一仓库时，不能出现负库存。
- 同一入库单重复确认，只能增加一次库存。
- 同一出库单重复确认，只能扣减一次库存。

### 前端改动

- 确认按钮增加 loading 状态。
- 确认成功后立即刷新单据状态。
- 重复点击时前端阻止二次提交，但不能只依赖前端。

### 验收标准

- 并发测试通过。
- 单据状态与库存变更一致。

### 建议 Git commit

```text
fix: protect stock confirmation concurrency
```

## 阶段五：Excel 导入 / 导出

### 目标

- 支持企业日常批量维护数据。
- 优先支持商品、供应商、仓库、库存流水、库存报表的导入导出。

### 必须包含

第一批必须做：

- 商品导入
- 商品导出
- 库存导出
- 入库单导出
- 出库单导出
- 供应商导入 / 导出
- 仓库导入 / 导出

第二批再做：

- 入库单批量导入
- 出库单批量导入
- 库存盘点导入

### 后端依赖

推荐二选一：

- EasyExcel
- Apache POI

优先建议 EasyExcel，代码量更小，适合业务导入导出。

### 后端新增接口

商品：

- `GET /api/products/export`
- `GET /api/products/import-template`
- `POST /api/products/import`

仓库：

- `GET /api/warehouses/export`
- `GET /api/warehouses/import-template`
- `POST /api/warehouses/import`

供应商：

- `GET /api/suppliers/export`
- `GET /api/suppliers/import-template`
- `POST /api/suppliers/import`

库存：

- `GET /api/stock/export`
- `GET /api/stock-movements/export`

单据：

- `GET /api/stock-in/export`
- `GET /api/stock-out/export`

### 后端新增模块

- `ExcelService`
- `ProductExcelService`
- `WarehouseExcelService`
- `SupplierExcelService`
- `StockExcelService`
- DTO:
  - `ExcelImportResultResponse`
  - `ProductImportRow`
  - `WarehouseImportRow`
  - `SupplierImportRow`

### 数据库可选新增表

#### `import_tasks`

用于记录导入任务，方便企业追踪导入结果。

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `task_no` | 导入任务号 |
| `module` | 模块 |
| `file_name` | 文件名 |
| `total_count` | 总行数 |
| `success_count` | 成功数量 |
| `fail_count` | 失败数量 |
| `status` | 状态 |
| `operator_id` | 操作人 |
| `error_file_url` | 错误文件地址 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |

### 前端新增交互

- 列表页增加：
  - 导入按钮
  - 导出按钮
  - 下载模板按钮
- 导入弹窗：
  - 上传 Excel
  - 展示导入结果
  - 展示失败行和失败原因
- 导出：
  - 按当前筛选条件导出

### 权限要求

- 导入通常只允许 `ADMIN`、`MANAGER`。
- 导出可以按模块权限开放。
- 前端按钮和后端接口都必须校验权限。

### 测试

- 模板下载成功。
- 合法 Excel 导入成功。
- 错误 Excel 返回失败行和原因。
- 导入重复 SKU 时提示错误，不覆盖现有数据，除非明确支持覆盖模式。
- 导出文件可以被 Excel 正常打开。

### 验收标准

- 导入失败不会产生半成品脏数据。
- 导入结果可追踪。
- 导出字段和页面筛选条件一致。

### 建议 Git commit

```text
feat: add Excel import and export
```

## 阶段六：库存调整

### 目标

- 支持管理员或仓库主管通过正式单据调整库存。
- 调整必须写库存流水。

### 数据库新增表

- `inventory_adjustments`
- `inventory_adjustment_items`

主表字段：

- `adjustment_no`
- `warehouse_id`
- `operator_id`
- `reason`
- `status`
- `remark`
- `created_at`
- `updated_at`

明细字段：

- `adjustment_id`
- `product_id`
- `quantity_before`
- `adjust_quantity`
- `quantity_after`
- `remark`

### 后端接口

- `GET /api/inventory-adjustments`
- `GET /api/inventory-adjustments/{id}`
- `POST /api/inventory-adjustments`
- `PUT /api/inventory-adjustments/{id}`
- `POST /api/inventory-adjustments/{id}/confirm`
- `POST /api/inventory-adjustments/{id}/cancel`

### 前端页面

- 库存调整列表
- 新建调整单
- 编辑草稿
- 确认调整
- 查看详情

### 验收标准

- 调整确认后库存变化正确。
- 调整流水写入 `stock_movements`。
- 已确认调整单不能再次修改。

### 建议 Git commit

```text
feat: add inventory adjustment workflow
```

## 阶段七：库存盘点

### 目标

- 支持企业定期盘点。
- 盘点差异通过确认盘点单调整库存，并写入库存流水。

### 数据库新增表

- `stock_takes`
- `stock_take_items`

### 后端接口

- `GET /api/stock-takes`
- `GET /api/stock-takes/{id}`
- `POST /api/stock-takes`
- `PUT /api/stock-takes/{id}`
- `POST /api/stock-takes/{id}/confirm`
- `POST /api/stock-takes/{id}/cancel`
- `POST /api/stock-takes/{id}/import`
- `GET /api/stock-takes/{id}/export`

### 前端页面

- 盘点单列表
- 新建盘点单
- 盘点明细录入
- Excel 导入盘点结果
- 差异确认

### 验收标准

- 账面数量、实盘数量、差异数量清晰展示。
- 盘点确认后库存和流水一致。

### 建议 Git commit

```text
feat: add stock take workflow
```

## 阶段八：仓库调拨

### 目标

- 支持商品在仓库之间调拨。
- 调出仓扣减库存，调入仓增加库存。
- 两边都要写库存流水。

### 数据库新增表

- `stock_transfers`
- `stock_transfer_items`

### 后端接口

- `GET /api/stock-transfers`
- `GET /api/stock-transfers/{id}`
- `POST /api/stock-transfers`
- `PUT /api/stock-transfers/{id}`
- `POST /api/stock-transfers/{id}/confirm`
- `POST /api/stock-transfers/{id}/cancel`

### 前端页面

- 调拨单列表
- 新建调拨单
- 编辑草稿
- 确认调拨
- 查看详情

### 验收标准

- 调出仓库存不能小于零。
- 调入仓库存正确增加。
- 同一调拨单确认只生效一次。

### 建议 Git commit

```text
feat: add warehouse transfer workflow
```

## 阶段九：审计和安全增强

### 目标

- 操作日志从基础记录升级为审计记录。
- 加强登录安全和 Token 生命周期。

### 数据库新增表或字段

- `login_logs`
- `token_blacklist`
- `operation_logs` 增加：
  - `request_body`
  - `response_status`
  - `error_message`
  - `before_data`
  - `after_data`
  - `user_agent`

### 后端改动

- 登录失败记录。
- 登录失败次数限制。
- 用户禁用后旧 token 失效。
- 支持修改密码。
- 可选支持 refresh token。

### 前端改动

- 修改密码页面。
- 登录失败提示优化。
- Token 过期提示优化。
- 登录日志页面。

### 验收标准

- 多次登录失败后账号临时锁定或限制登录。
- 关键业务操作能追踪前后数据。
- 禁用用户后不能继续访问受保护接口。

### 建议 Git commit

```text
feat: strengthen audit and login security
```

## 阶段十：报表和 Dashboard 升级

### 目标

- 从基础 Dashboard 升级为企业经营和仓库运营看板。

### 后端接口

- `GET /api/reports/stock-summary`
- `GET /api/reports/stock-in-out`
- `GET /api/reports/low-stock`
- `GET /api/reports/product-turnover`
- `GET /api/reports/warehouse-summary`

### 前端页面

- 报表中心
- 库存汇总报表
- 出入库报表
- 低库存报表
- 商品周转报表

### 验收标准

- 报表支持按时间、仓库、分类筛选。
- 报表可以导出 Excel。
- Dashboard 数据与报表口径一致。

### 建议 Git commit

```text
feat: add warehouse reports
```

## 阶段十一：部署上线准备

### 目标

- 让系统具备企业部署基础。

### 必做内容

- Backend Dockerfile
- Frontend Dockerfile
- `docker-compose.yml`
- Nginx 配置
- 生产环境配置模板
- GitHub Actions
- MySQL 备份脚本
- 健康检查和日志目录说明

### 验收标准

- 一条命令可以启动完整环境。
- CI 能自动运行：
  - `mvn test`
  - `npm run build`
- README 包含部署说明和回滚说明。

### 建议 Git commit

```text
chore: add deployment configuration
```

## 最终推荐开发顺序

严格建议按以下顺序推进：

1. 数据库迁移和配置分环境。
2. RBAC 角色权限细化。
3. 前端动态菜单、按钮权限和 403 页面。
4. 库存流水表和查询接口。
5. 入库 / 出库确认写入库存流水。
6. 库存并发控制和幂等保护。
7. Excel 导入 / 导出基础能力。
8. 商品、仓库、供应商导入导出。
9. 库存、入库、出库导出。
10. 库存调整。
11. 库存盘点。
12. 仓库调拨。
13. 审计和登录安全增强。
14. 报表中心和 Dashboard 升级。
15. Docker、CI/CD、备份和上线文档。

## 最小企业可用版本范围

如果要尽快形成企业可用版本，至少完成：

1. 阶段一：数据库迁移基础。
2. 阶段二：RBAC 权限。
3. 阶段三：库存流水。
4. 阶段四：库存并发控制和幂等保护。
5. 阶段五：Excel 导入 / 导出。

完成这五个阶段后，系统就能从“可演示的仓库系统”升级为“可在小型企业试运行的仓库系统”。

## 每阶段通用测试要求

每个阶段完成后都必须执行：

```bash
cd backend
mvn test

cd frontend
npm run build
```

同时至少手工验证：

1. 登录正常。
2. 当前用户信息正常。
3. 商品、仓库、供应商列表正常。
4. 入库单创建和确认正常。
5. 出库单创建和确认正常。
6. 库存查询正常。
7. 操作日志正常。
8. STAFF 不能访问管理员功能。

## 不允许破坏的现有能力

升级过程中不得随意破坏：

- `/api/auth/login`
- `/api/auth/me`
- `/api/users`
- `/api/categories`
- `/api/products`
- `/api/warehouses`
- `/api/suppliers`
- `/api/stock`
- `/api/stock-in`
- `/api/stock-out`
- `/api/dashboard`
- `/api/operation-logs`
- 当前 `ApiResponse` 响应结构
- 当前前端 Axios 响应处理方式
- 当前入库确认增加库存流程
- 当前出库确认扣减库存流程
- 当前单据状态：`DRAFT`、`CONFIRMED`、`CANCELLED`

如果必须改变字段或行为，应新增接口或新增字段，待前端适配完成后再考虑废弃旧字段。
