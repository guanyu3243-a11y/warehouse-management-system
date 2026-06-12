# 服装仓库管理系统项目知识大纲

本文档基于当前 `warehouse-management-system` 项目的真实代码整理，目标是帮助你真正理解项目，而不是只会启动和演示。阅读时建议打开 IDE，对照文中提到的文件、类名、接口和数据库表一起看。

## 目录

- [一、项目总体介绍](#一项目总体介绍)
- [二、技术栈总览](#二技术栈总览)
- [三、后端项目结构详解](#三后端项目结构详解)
- [四、前端项目结构详解](#四前端项目结构详解)
- [五、数据库设计详解](#五数据库设计详解)
- [六、核心业务流程详解](#六核心业务流程详解)
- [七、关键代码清单](#七关键代码清单)
- [八、必须掌握的基础知识点](#八必须掌握的基础知识点)
- [九、面试高频问题和回答](#九面试高频问题和回答)
- [十、我应该怎么学习这套代码](#十我应该怎么学习这套代码)
- [十一、未来优化方向](#十一未来优化方向)

## 一、项目总体介绍

### 1. 这个系统是什么

这是一个服装仓库管理系统，全称可以叫 Clothing Warehouse Management System。它是一个前后端分离项目：

- 后端在 `backend` 目录，使用 Java 17 + Spring Boot 3。
- 前端在 `frontend` 目录，使用 Vue 3 + Vite + Element Plus。
- 数据库使用 MySQL，表结构由 Flyway 脚本自动迁移。
- 部署支持 Docker Compose，前端容器使用 Nginx 提供静态资源和 `/api` 反向代理。

从业务上看，它不是单纯的 CRUD 练习，而是围绕“服装仓库”做了一个比较完整的业务闭环：

- 维护商品分类、服装商品、仓库、供应商。
- 创建入库单，确认后增加库存。
- 创建出库单，确认后扣减库存。
- 查询库存和低库存预警。
- 记录库存流水，追踪每一次库存变化。
- 做库存调整和库存盘点。
- 管理用户、角色、权限。
- 记录登录日志和操作日志。
- 支持 Excel 导入导出和 Docker 部署。

### 2. 它解决什么业务问题

服装仓库的核心问题是：商品 SKU 多、尺码颜色复杂、库存变化频繁，如果只靠 Excel 或手工记录，容易出现库存不准、出入库无追踪、低库存发现不及时等问题。

本项目主要解决这些问题：

- 统一维护商品、仓库、供应商等基础资料。
- 通过入库单和出库单规范库存变动。
- 通过 `stock` 表保存当前库存。
- 通过 `stock_movements` 表记录库存流水，方便追溯。
- 通过低库存阈值发现补货风险。
- 通过 RBAC 权限控制不同角色能访问的菜单和接口。
- 通过登录日志、操作日志提高审计能力。

### 3. 为什么是前后端分离项目

本项目把后端 API 和前端页面分开，是典型前后端分离架构。

后端负责：

- 业务规则。
- 数据库读写。
- 登录认证。
- 权限校验。
- 库存事务。
- Excel 生成和解析。
- 返回统一 JSON 数据。

前端负责：

- 页面布局。
- 表格、表单、弹窗。
- 登录状态保存。
- 菜单和路由权限控制。
- 调用后端 API。
- 下载导出文件。

这样做的好处：

- 后端 API 可以被 Web、移动端、第三方系统复用。
- 前端可以独立开发和打包。
- 部署时前端可以由 Nginx 托管，后端只提供 `/api`。
- 职责清晰，适合企业项目分工。

### 4. 当前系统适合什么场景

当前系统适合：

- 小型服装企业内部试用。
- 课程项目答辩。
- Java + Vue 全栈项目展示。
- 面试项目讲解。
- 仓库管理系统 MVP 原型。

当前系统已经具备企业 MVP 的一些特征：

- 不是单表 CRUD，而是有完整库存业务流程。
- 有 JWT 登录和 token 黑名单。
- 有 RBAC 权限。
- 有库存流水。
- 有库存并发保护。
- 有 Flyway 迁移。
- 有 Docker Compose 部署。
- 有 GitHub Actions CI。

### 5. 当前系统还不是正式商业版的原因

它还不是正式商业化系统，主要原因是：

- 监控告警还不完整，例如没有 Prometheus、Grafana、集中日志平台。
- 数据备份和恢复还需要正式演练。
- 数据权限只做了基础能力，仓库范围还需要更深地贯穿所有业务查询。
- 单元测试和集成测试覆盖率还可以提高。
- Excel 导入导出是自实现的轻量方案，不适合特别复杂或超大文件。
- 没有 HTTPS、限流、防暴力破解的完整网关方案。
- 没有多租户、多组织、多仓调拨、审批流等高级企业能力。
- 前端移动端适配不是主要目标。

面试时可以这样说：

> 这个项目我定位为企业级 MVP，不是最终商业版。它已经具备库存业务闭环、权限、日志、部署和迁移能力，但如果要正式商用，还需要补齐监控告警、备份恢复、安全加固、数据权限细化和更完整的测试。

## 二、技术栈总览

### 1. 后端技术栈

| 技术 | 本项目中的作用 |
| --- | --- |
| Java 17 | 后端开发语言，支持 record、现代日期 API、清晰的类型系统。 |
| Spring Boot 3.3.5 | 后端应用框架，负责 Web 接口、依赖注入、配置管理和应用启动。 |
| Spring MVC | Controller、Interceptor、Filter 等 Web 层能力。 |
| Spring Validation | DTO 参数校验，例如 `@Valid` 触发请求校验。 |
| Spring Security Crypto | 只使用其中的 `BCryptPasswordEncoder` 做密码加密，没有引入完整 Spring Security 登录体系。 |
| MyBatis-Plus 3.5.7 | 数据访问层，Mapper 继承 `BaseMapper`，快速完成增删改查和分页。 |
| MySQL | 业务数据库，保存用户、商品、库存、单据、权限和日志。 |
| Flyway | 数据库版本迁移，脚本位于 `backend/src/main/resources/db/migration`。 |
| JWT | 登录后签发 token，后续请求通过 `Authorization: Bearer <token>` 认证。 |
| BCrypt | 密码不可明文保存，注册、创建用户、重置密码和初始化管理员都使用 BCrypt 加密。 |
| Maven | 后端依赖管理、测试和打包。 |
| Docker | 后端容器化部署，见 `backend/Dockerfile`。 |

后端依赖集中在 `backend/pom.xml`。注意：当前没有引入 EasyExcel 或 Apache POI，Excel 是项目自己通过 Zip + XML 读写 `.xlsx`。

### 2. 前端技术栈

| 技术 | 本项目中的作用 |
| --- | --- |
| Vue 3 | 前端框架，页面组件都在 `frontend/src/views`。 |
| Vite | 前端开发服务器和构建工具，配置在 `frontend/vite.config.js`。 |
| Element Plus | UI 组件库，表格、表单、按钮、弹窗、菜单都大量使用。 |
| Axios | HTTP 请求库，统一封装在 `frontend/src/api/http.js`。 |
| Pinia | 状态管理，认证状态保存在 `frontend/src/stores/auth.js`。 |
| Vue Router | 前端路由，页面跳转和登录守卫在 `frontend/src/router/index.js`。 |
| Nginx | 生产环境托管前端静态文件，并把 `/api` 反向代理到后端。 |

### 3. 部署技术

| 文件或技术 | 本项目中的作用 |
| --- | --- |
| `backend/Dockerfile` | 使用 Maven 构建 jar，再用 `eclipse-temurin:17-jre` 运行。 |
| `frontend/Dockerfile` | 使用 Node 22 构建前端，再用 Nginx 托管 `dist`。 |
| `docker-compose.yml` | 编排 MySQL、backend、frontend 三个服务。 |
| `frontend/nginx.conf` | 配置 `/api/` 代理和 Vue history 路由刷新。 |
| `.env.example` | 环境变量模板，不包含真实密码。 |
| MySQL volume | `mysql_data:/var/lib/mysql`，保证容器重启后数据不丢。 |

`docker-compose.yml` 中三个服务的关系：

- `mysql`：数据库服务，服务名也是后端的 `DB_HOST`。
- `backend`：Spring Boot 服务，依赖 MySQL 健康检查。
- `frontend`：Nginx 前端服务，代理 `/api` 到 `backend:8080`。

### 4. 每个技术在本项目里具体负责什么

用一句话串起来：

> Vue 3 + Element Plus 做页面，Axios 调 Spring Boot API，Pinia 保存登录状态，Vue Router 做页面守卫；Spring Boot 接收请求，JWT 拦截器识别用户，权限拦截器检查权限，Service 执行业务逻辑，Mapper 访问 MySQL，Flyway 管理表结构，Docker Compose 把 MySQL、后端和前端一起部署。

## 三、后端项目结构详解

后端主包路径：

```text
backend/src/main/java/com/warehouse/management
```

主要目录：

```text
common
config
controller
dto
entity
mapper
service
service/impl
util
```

### 1. controller

Controller 是后端 HTTP 入口，负责接收请求参数、调用 Service、返回 `ApiResponse`。

位置：

```text
backend/src/main/java/com/warehouse/management/controller
```

核心 Controller：

| Controller | 路径 | 作用 | 调用 Service |
| --- | --- | --- | --- |
| `AuthController` | `/api/auth` | 注册、登录、当前用户、权限、改密码、退出登录。 | `AuthService` |
| `UserController` | `/api/users` | 用户列表、详情、新增、编辑、重置密码、状态、删除、分配角色、分配仓库范围。 | `UserManagementService` |
| `RoleController` | `/api/roles` | 角色管理、角色状态、角色权限分配。 | `RoleService` |
| `PermissionController` | `/api/permissions` | 权限列表、权限树、新增、编辑、状态更新。 | `PermissionService` |
| `ProductController` | `/api/products` | 商品 CRUD、导入模板、导入、导出。 | `ProductService`、`BusinessExcelService` |
| `CategoryController` | `/api/categories` | 商品分类 CRUD。 | `CategoryService` |
| `WarehouseController` | `/api/warehouses` | 仓库 CRUD、导入模板、导入、导出。 | `WarehouseService`、`BusinessExcelService` |
| `SupplierController` | `/api/suppliers` | 供应商 CRUD、导入模板、导入、导出。 | `SupplierService`、`BusinessExcelService` |
| `StockInController` | `/api/stock-in` | 入库单分页、详情、创建、编辑、确认、取消、导出。 | `StockInService`、`BusinessExcelService` |
| `StockOutController` | `/api/stock-out` | 出库单分页、详情、创建、编辑、确认、取消、导出。 | `StockOutService`、`BusinessExcelService` |
| `StockController` | `/api/stock` | 库存查询、低库存、按商品查询、库存导出。 | `StockService`、`BusinessExcelService` |
| `StockMovementController` | `/api/stock-movements` | 库存流水查询、详情、按商品/仓库查询、导出。 | `StockMovementService`、`BusinessExcelService` |
| `InventoryAdjustmentController` | `/api/inventory-adjustments` | 库存调整单分页、详情、创建、编辑、确认、取消。 | `InventoryAdjustmentService` |
| `StockTakeController` | `/api/stock-takes` | 库存盘点分页、详情、创建、编辑、确认、取消、明细导入导出。 | `StockTakeService` |
| `DashboardController` | `/api/dashboard` | 汇总统计、库存趋势、低库存 Top。 | `DashboardService` |
| `OperationLogController` | `/api/operation-logs` | 操作日志查询和详情。 | `OperationLogService` |
| `LoginLogController` | `/api/login-logs` | 登录日志查询。 | `LoginLogService` |
| `HealthController` | `/api/health` | 健康检查。 | 无复杂业务 |

Controller 的特点：

- 不直接写复杂业务逻辑。
- 不直接操作数据库。
- 返回统一 `ApiResponse.success(...)`。
- 上传和下载 Excel 时调用 `ExcelResponseUtil.workbook(...)` 或 `BusinessExcelService`。

### 2. service

Service 接口位于：

```text
backend/src/main/java/com/warehouse/management/service
```

Service 接口的作用：

- 定义业务能力，例如 `StockInService.confirm(Long id)`。
- 隔离 Controller 和实现细节。
- 方便以后替换实现、单元测试和维护。

为什么要有 Service 和 ServiceImpl：

- Controller 只依赖接口，不关心具体实现。
- 业务逻辑放在 `service/impl`，职责清晰。
- 如果未来某个业务改成异步、远程调用、缓存，都可以在实现层调整。

核心 Service 职责：

| Service | 主要职责 |
| --- | --- |
| `AuthService` | 注册、登录、当前用户、权限、改密码、退出登录。 |
| `UserManagementService` | 用户管理、重置密码、分配角色、分配仓库范围。 |
| `AuthorizationService` | 查询当前用户角色、权限、仓库范围，判断是否有权限。 |
| `RoleService` | 角色 CRUD、角色权限维护。 |
| `PermissionService` | 权限 CRUD、权限树。 |
| `ProductService` | 商品 CRUD 和校验。 |
| `WarehouseService` | 仓库 CRUD 和被引用限制。 |
| `SupplierService` | 供应商 CRUD 和被引用限制。 |
| `StockInService` | 入库单业务。 |
| `StockOutService` | 出库单业务。 |
| `StockService` | 当前库存查询、低库存查询、按商品查询。 |
| `StockMovementService` | 库存流水查询和写入。 |
| `InventoryAdjustmentService` | 库存调整单业务。 |
| `StockTakeService` | 库存盘点单业务和盘点明细导入导出。 |
| `BusinessExcelService` | 商品、仓库、供应商、库存、单据、流水导入导出。 |
| `ExcelService` | 底层 xlsx 文件读写。 |
| `InitAdminService` | 空库首次启动时创建管理员。 |
| `LoginLogService` | 登录成功/失败日志和失败次数统计。 |
| `TokenBlacklistService` | 退出登录后 token 黑名单。 |
| `OperationLogService` | 操作日志记录和查询。 |

### 3. service/impl

业务逻辑主要放在：

```text
backend/src/main/java/com/warehouse/management/service/impl
```

重点实现类：

#### `AuthServiceImpl`

负责认证业务。

重要方法：

- `register(AuthRegisterRequest request)`：注册用户，默认角色为 `STAFF`，密码用 `passwordEncoder.encode(...)` 加密。
- `login(AuthLoginRequest request, String requestIp, String userAgent)`：校验用户名、密码、状态，生成 JWT，记录登录日志。
- `getCurrentUser(Long userId)`：根据 token 中的 userId 查询当前用户。
- `getCurrentPermissions(Long userId)`：调用 `AuthorizationService` 获取角色、权限和仓库范围。
- `changePassword(Long userId, AuthChangePasswordRequest request)`：校验旧密码，保存新 BCrypt 密码。
- `logout(String token)`：解析 token 过期时间，写入 token 黑名单。

关键点：

- 使用 `PasswordEncoder.matches(raw, encoded)` 校验密码。
- 登录失败会写 `login_logs`。
- 最近 15 分钟失败 5 次会禁止继续登录。
- JWT payload 只放 `userId`、`username`、`role`、`iat`、`exp`。
- `AuthUserResponse` 不返回密码、email、phone 等敏感信息。

#### `StockInServiceImpl`

负责入库业务。

重要方法：

- `create(StockInRequest request)`：创建入库草稿单。
- `update(Long id, StockInRequest request)`：只允许草稿单编辑。
- `confirm(Long id)`：确认入库。
- `cancel(Long id)`：取消草稿单。
- `increaseStock(StockIn stockIn, StockInItem item)`：增加库存并写库存流水。

确认入库的核心逻辑：

1. 查询入库单。
2. `ensureDraft(stockIn)` 保证状态是 `DRAFT`。
3. `markConfirmedIfDraft(id)` 用条件更新把草稿改为已确认，防止重复确认。
4. 遍历明细。
5. `stockMapper.selectByProductAndWarehouseForUpdate(...)` 锁定库存行。
6. 增加 `stock.quantity`。
7. 调 `stockMovementService.record(...)` 写 `STOCK_IN` 流水。

#### `StockOutServiceImpl`

负责出库业务。

重要方法：

- `create(StockOutRequest request)`：创建出库草稿单。
- `update(Long id, StockOutRequest request)`：只允许草稿单编辑。
- `confirm(Long id)`：确认出库。
- `cancel(Long id)`：取消草稿单。
- `decreaseStock(StockOut stockOut, StockOutItem item)`：扣减库存并写库存流水。

出库的关键保护：

- 先用 `FOR UPDATE` 锁定库存行。
- 计算 `quantityAfter = quantityBefore - 出库数量`。
- 如果库存不存在或扣完小于 0，抛出 `BusinessException.badRequest(...)`。
- 调用 `stockMapper.decreaseQuantityByIdIfEnough(...)`，SQL 中有 `AND quantity >= #{quantity}`，避免并发扣成负数。

#### `StockMovementServiceImpl`

负责库存流水。

重要方法：

- `page(...)`：按商品、仓库、流水类型、来源单号、时间范围分页查询。
- `getById(Long id)`：查询流水详情。
- `getByProductId(...)`：查询某商品流水。
- `getByWarehouseId(...)`：查询某仓库流水。
- `record(StockMovementRecordCommand command)`：写库存流水。

流水字段含义：

- `movementNo`：流水号，`SM` + 时间 + 随机后缀。
- `movementType`：库存变化类型，例如 `STOCK_IN`、`STOCK_OUT`、`ADJUSTMENT`、`STOCK_TAKE`。
- `sourceType`：来源业务类型。
- `sourceId` / `sourceNo`：来源单据。
- `quantityBefore`：变化前数量。
- `changeQuantity`：变化数量，入库为正，出库为负。
- `quantityAfter`：变化后数量。

#### `InventoryAdjustmentServiceImpl`

负责库存调整。

重要方法：

- `create(...)`：创建调整单。
- `update(...)`：编辑草稿调整单。
- `confirm(...)`：确认调整。
- `cancel(...)`：取消调整。
- `applyStockAdjustment(...)`：应用库存调整。

关键规则：

- 调整数量 `adjustQuantity` 不能为 0。
- 调整后库存不能小于 0。
- 确认时写 `movementType = ADJUSTMENT`，`sourceType = INVENTORY_ADJUSTMENT` 的库存流水。

#### `StockTakeServiceImpl`

负责库存盘点。

重要方法：

- `create(...)`：创建盘点单。
- `update(...)`：编辑盘点单。
- `confirm(...)`：确认盘点。
- `cancel(...)`：取消盘点。
- `importItems(Long id, MultipartFile file)`：导入盘点明细。
- `exportItems(Long id)`：导出盘点明细。
- `applyStockTake(...)`：按盘点结果更新库存。
- `replaceItems(...)`：替换盘点明细，并计算账面数、实盘数、差异数。

关键规则：

- `bookQuantity` 是创建或导入盘点明细时系统读取到的账面库存。
- `actualQuantity` 是人工实盘数量。
- `differenceQuantity = actualQuantity - bookQuantity`。
- 确认盘点时会检查当前库存是否仍等于 `bookQuantity`，如果中间库存变了，会提示 `Stock changed after stock take was saved...`，避免用过期盘点结果覆盖库存。
- 只有差异不为 0 时才写 `STOCK_TAKE` 流水。

#### `UserManagementServiceImpl`

负责用户管理。

主要能力：

- 分页查询用户。
- 新增用户。
- 编辑用户。
- 重置密码。
- 启用/禁用用户。
- 删除用户。
- 查询和更新用户角色。
- 查询和更新用户仓库范围。

关键规则：

- 用户名唯一。
- 新增和重置密码必须 BCrypt 加密。
- 不能禁用自己。
- 不能删除自己。
- `UserResponse` 不包含 password。

#### `InitAdminServiceImpl`

负责初始化管理员。

执行入口：

- `InitAdminRunner` 实现 `ApplicationRunner`。
- 后端启动后，如果 `app.init-admin.enabled=true`，调用 `InitAdminService.initializeIfNecessary()`。

核心逻辑：

1. 查询 `users` 表数量。
2. 如果已有用户，直接跳过。
3. 从 `InitAdminProperties` 读取 username、password、email、phone。
4. 查找 `ADMIN` 角色。
5. 创建用户，密码 BCrypt 加密。
6. 写入 `user_roles`，绑定 ADMIN 角色。

为什么不能每次启动都创建：

- 会产生重复管理员。
- 会覆盖或污染生产用户数据。
- 所以代码先判断 `users` 表是否为空。

### 4. mapper

Mapper 位于：

```text
backend/src/main/java/com/warehouse/management/mapper
```

MyBatis-Plus Mapper 是数据访问层，每个 Mapper 通常继承：

```java
BaseMapper<Entity>
```

`BaseMapper` 提供常用方法：

- `insert`
- `deleteById`
- `updateById`
- `selectById`
- `selectList`
- `selectPage`
- `selectCount`

当前项目里，大部分 Mapper 没有自定义 SQL，直接使用 MyBatis-Plus。经过代码搜索，只有 `StockMapper` 使用了 `@Select` 和 `@Update` 自定义 SQL。

#### `StockMapper` 的作用

`StockMapper` 是库存并发控制的关键。

它有三个自定义方法：

- `selectByProductAndWarehouseForUpdate(productId, warehouseId)`  
  使用 `SELECT ... FOR UPDATE` 锁定某商品某仓库的库存行。

- `updateQuantityById(id, quantity)`  
  直接把库存更新为指定数量，并让 `version = version + 1`。

- `decreaseQuantityByIdIfEnough(id, quantity)`  
  扣减库存，同时加条件 `AND quantity >= #{quantity}`，避免库存不足时仍扣减。

这三个方法是入库、出库、调整、盘点库存正确性的核心。

### 5. entity

Entity 位于：

```text
backend/src/main/java/com/warehouse/management/entity
```

Entity 和数据库表的关系：

- 一个 Entity 通常对应一张数据库表。
- 字段使用驼峰命名，数据库用下划线命名。
- MyBatis-Plus 配置了 `map-underscore-to-camel-case: true`，所以 `created_at` 会映射到 `createdAt`。

核心 Entity：

| Entity | 对应表 | 作用 |
| --- | --- | --- |
| `User` | `users` | 用户账号，包含 username、password、role、status、email、phone。 |
| `Role` | `roles` | 角色，例如 ADMIN、MANAGER、STAFF、VIEWER。 |
| `Permission` | `permissions` | 权限点，例如 `product:view`、`stock-in:confirm`。 |
| `UserRole` | `user_roles` | 用户和角色多对多关系。 |
| `RolePermission` | `role_permissions` | 角色和权限多对多关系。 |
| `UserWarehousePermission` | `user_warehouse_permissions` | 用户可访问仓库范围。 |
| `Category` | `categories` | 商品分类。 |
| `Product` | `products` | 服装商品，包含 SKU、尺码、颜色、品牌、季节、价格、预警阈值。 |
| `Warehouse` | `warehouses` | 仓库资料。 |
| `Supplier` | `suppliers` | 供应商资料。 |
| `Stock` | `stock` | 当前库存，一条记录表示某商品在某仓库的当前数量。 |
| `StockIn` | `stock_in` | 入库主表。 |
| `StockInItem` | `stock_in_items` | 入库明细。 |
| `StockOut` | `stock_out` | 出库主表。 |
| `StockOutItem` | `stock_out_items` | 出库明细。 |
| `StockMovement` | `stock_movements` | 库存流水。 |
| `InventoryAdjustment` | `inventory_adjustments` | 库存调整主表。 |
| `InventoryAdjustmentItem` | `inventory_adjustment_items` | 库存调整明细。 |
| `StockTake` | `stock_takes` | 库存盘点主表。 |
| `StockTakeItem` | `stock_take_items` | 库存盘点明细。 |
| `LoginLog` | `login_logs` | 登录成功/失败记录。 |
| `OperationLog` | `operation_logs` | 操作日志。 |
| `TokenBlacklist` | `token_blacklist` | 已退出登录 token 黑名单。 |

### 6. dto

DTO 位于：

```text
backend/src/main/java/com/warehouse/management/dto
```

DTO 是 Data Transfer Object，意思是“数据传输对象”。它用于 Controller 和前端之间传输数据。

Request DTO 和 Response DTO 的区别：

- Request DTO：前端提交给后端，例如创建商品、登录、创建入库单。
- Response DTO：后端返回给前端，例如商品详情、登录结果、库存流水。

为什么不能直接把 Entity 返回给前端：

- Entity 里可能有敏感字段，例如 `User.password`。
- Entity 结构和数据库强绑定，不适合作为公开 API。
- Response DTO 可以组合额外字段，例如 `ProductResponse` 可以带 `categoryName`。
- DTO 可以让接口更稳定，数据库字段变化不一定影响前端。

典型 DTO：

| DTO | 类型 | 作用 |
| --- | --- | --- |
| `AuthLoginRequest` | Request | 登录请求，包含 username 和 password。 |
| `AuthLoginResponse` | Response | 登录返回，包含 token、tokenType、expiresIn、user。 |
| `AuthUserResponse` | Response | 当前用户信息，只包含 id、username、role、status。 |
| `ProductRequest` | Request | 商品新增/编辑请求。当前项目中没有单独的 `ProductCreateRequest`，而是统一用 `ProductRequest`。 |
| `ProductResponse` | Response | 商品返回，包含 SKU、分类、价格、低库存阈值等。 |
| `UserCreateRequest` | Request | 管理员新增用户请求。 |
| `UserResponse` | Response | 用户返回，不包含 password。 |
| `StockInRequest` | Request | 入库单请求，包含 warehouseId、supplierId、remark、items。 |
| `StockInItemRequest` | Request | 入库明细，包含 productId、quantity、unitCost。 |
| `StockMovementResponse` | Response | 库存流水返回，包含商品、仓库、变化前后数量、来源单据。 |
| `ExcelImportResultResponse` | Response | Excel 导入结果，包含总行数、成功数、失败数和失败明细。 |
| `PageResponse<T>` | Response | 分页返回，包含 records、total、page、size。 |

### 7. config

配置类位于：

```text
backend/src/main/java/com/warehouse/management/config
```

重点配置类：

#### `WebConfig`

实现 `WebMvcConfigurer`，注册四个拦截器：

1. `JwtAuthInterceptor`
2. `AdminOnlyInterceptor`
3. `PermissionInterceptor`
4. `OperationLogInterceptor`

关键路径：

- `/api/auth/register`、`/api/auth/login`、`/api/health` 不需要 JWT。
- `/api/users/**`、`/api/roles/**`、`/api/permissions/**`、`/api/login-logs/**` 需要 ADMIN。
- 大部分 `/api/**` 需要权限码校验。

#### `JwtAuthInterceptor`

负责认证：

- 读取 `Authorization` 请求头。
- 检查是否以 `Bearer ` 开头。
- 检查 token 是否在黑名单。
- 用 `JwtUtil.parseToken` 解析 token。
- 从 token 中取 `userId`。
- 查询数据库确认用户存在且状态为 ACTIVE。
- 把当前用户放入 `CurrentUserContext`。
- 请求完成后清理 ThreadLocal。

#### `AdminOnlyInterceptor`

负责管理员专属接口：

- 从 `CurrentUserContext` 拿当前用户。
- 如果角色不是 `ADMIN`，抛出 403。
- 用于用户、角色、权限、登录日志等系统管理接口。

#### `PermissionInterceptor`

负责 RBAC 权限码校验：

- 根据 HTTP method 和 URI 推导权限码。
- ADMIN 直接放行。
- 非 ADMIN 调用 `AuthorizationService.hasPermission(userId, permissionCode)`。
- 没权限抛出 `BusinessException.forbidden(...)`。

例子：

- `GET /api/products` -> `product:view`
- `POST /api/products` -> `product:create`
- `PUT /api/products/{id}` -> `product:update`
- `POST /api/stock-in/{id}/confirm` -> `stock-in:confirm`

#### `MybatisPlusConfig`

配置 MyBatis-Plus 分页插件。没有它，`selectPage` 分页能力会不完整。

#### `InitAdminProperties`

读取配置：

```text
app.init-admin.enabled
app.init-admin.username
app.init-admin.password
app.init-admin.email
app.init-admin.phone
```

这些配置来自 `application.yml`，也可以由环境变量覆盖：

```text
INIT_ADMIN_ENABLED
INIT_ADMIN_USERNAME
INIT_ADMIN_PASSWORD
INIT_ADMIN_EMAIL
INIT_ADMIN_PHONE
```

#### `InitAdminRunner`

实现 `ApplicationRunner`，后端启动后执行。如果 `enabled=true`，调用 `InitAdminService.initializeIfNecessary()`。

#### `RequestIdFilter`

为每次请求生成或透传 `X-Request-Id`，同时放到日志 MDC 里，方便排查问题。

#### `OperationLogInterceptor`

在请求完成后记录操作日志：

- 只记录 POST、PUT、DELETE。
- 自动推断模块和动作。
- 对 password、oldPassword、newPassword 做脱敏。
- 写入 `operation_logs`。

#### `AuditRequestCachingFilter`

配合操作日志读取请求体。因为普通 request body 只能读一次，所以需要缓存包装。

### 8. common

公共类位于：

```text
backend/src/main/java/com/warehouse/management/common
```

| 类 | 作用 |
| --- | --- |
| `ApiResponse<T>` | 统一接口响应格式：`code`、`message`、`data`。 |
| `PageResponse<T>` | 实际在 `dto` 包中，统一分页响应：records、total、page、size。 |
| `BusinessException` | 业务异常，例如参数错误、未登录、无权限、数据不存在。 |
| `ErrorCode` | 错误码枚举，包含 200、400、401、403、404、500。 |
| `CurrentUser` | 当前登录用户的轻量对象，包含 id、username、role。 |
| `CurrentUserContext` | 用 `ThreadLocal` 保存当前请求中的用户信息。 |
| `GlobalExceptionHandler` | 统一异常处理，把异常转成 `ApiResponse`。 |

为什么需要这些公共类：

- 前端可以统一处理响应。
- 业务异常不用每个 Controller 手写 try-catch。
- Service 层可以通过 `CurrentUserContext.getRequired()` 获取当前操作人。

### 9. util

工具类位于：

```text
backend/src/main/java/com/warehouse/management/util
```

#### `JwtUtil`

负责 JWT：

- `generateToken(User user)`：生成 token。
- `parseToken(String token)`：解析 token。
- `getExpirationSeconds()`：返回过期秒数。

JWT payload 当前包含：

- `userId`
- `username`
- `role`
- `iat`
- `exp`

不包含：

- password
- email
- phone
- realName

#### `ExcelResponseUtil`

负责下载响应：

- 把 byte[] 包装成 `ResponseEntity<byte[]>`。
- 设置 Excel 文件名。
- 设置下载响应头。

调用位置：

- `ProductController.export`
- `WarehouseController.export`
- `SupplierController.export`
- `StockController.export`
- `StockMovementController.export`
- `StockInController.export`
- `StockOutController.export`
- `StockTakeController.exportItems`

#### Excel 相关服务

Excel 主要不是 util 类，而是 Service：

- `ExcelService`
- `ExcelServiceImpl`
- `BusinessExcelService`
- `BusinessExcelServiceImpl`

`ExcelServiceImpl` 直接生成 `.xlsx` 所需 XML 并用 Zip 输出，读取时解析 xlsx 内部 XML。当前实现轻量、无额外依赖，但复杂 Excel 能力不如 EasyExcel/POI。

## 四、前端项目结构详解

前端主目录：

```text
frontend/src
```

主要目录：

```text
api
constants
layouts
router
stores
styles
utils
views
```

### 1. views

页面组件位于：

```text
frontend/src/views
```

注意：需求里常说 `Login.vue`、`Dashboard.vue`，但本项目真实文件名是：

- `LoginView.vue`
- `DashboardView.vue`

主要页面：

| 文件 | 页面功能 |
| --- | --- |
| `LoginView.vue` | 登录页，调用 Pinia auth store 登录，成功后跳转 Dashboard。 |
| `DashboardView.vue` | Dashboard 数据统计。 |
| `master-data/CategoryView.vue` | 商品分类页面，基于 `MasterCrudPage.vue`。 |
| `master-data/ProductView.vue` | 服装商品页面，支持导入导出。 |
| `master-data/WarehouseView.vue` | 仓库管理页面，支持导入导出。 |
| `master-data/SupplierView.vue` | 供应商管理页面，支持导入导出。 |
| `documents/StockInView.vue` | 入库管理入口。 |
| `documents/StockOutView.vue` | 出库管理入口。 |
| `documents/StockDocumentView.vue` | 入库/出库共用单据页面组件。 |
| `stock/StockView.vue` | 库存查询和低库存预警共用页面。 |
| `stock/StockMovementList.vue` | 库存流水页面。 |
| `stock/InventoryAdjustmentList.vue` | 库存调整页面。 |
| `stock/StockTakeList.vue` | 库存盘点页面。 |
| `user/UserList.vue` | 用户管理页面。 |
| `system/RoleList.vue` | 角色管理页面。 |
| `system/PermissionList.vue` | 权限管理页面。 |
| `logs/LoginLogsView.vue` | 登录日志页面。 |
| `logs/OperationLogsView.vue` | 操作日志页面。 |
| `account/ChangePasswordView.vue` | 修改密码页面。 |
| `error/ForbiddenView.vue` | 403 无权限页面。 |
| `error/NotFoundView.vue` | 404 页面。 |
| `PlaceholderView.vue` | 占位页面，目前不是核心业务页。 |

页面和 API 的关系：

- 商品、仓库、供应商页面调用 `business.js` 中对应 API。
- 用户管理调用 `users.js`。
- 角色和权限调用 `rbac.js`。
- 登录页调用 `auth.js` 和 `stores/auth.js`。

### 2. api

API 封装位于：

```text
frontend/src/api
```

#### `http.js`

这是最重要的前端请求封装。

它做了几件事：

- 创建 Axios 实例。
- 设置 `baseURL = import.meta.env.VITE_API_BASE_URL || '/api'`。
- 设置超时时间 15000ms。
- 请求拦截器中从本地存储读取 token。
- 如果有 token，自动加请求头：

```text
Authorization: Bearer <token>
```

- 响应拦截器识别后端统一格式 `ApiResponse`。
- 如果 `code === 200`，直接返回 `data`。
- 如果 401，清除登录状态并跳转 `/login`。
- 如果 403，跳转 `/403`。

#### `auth.js`

封装认证接口：

- `loginApi(payload)` -> `POST /auth/login`
- `currentUserApi()` -> `GET /auth/me`
- `currentPermissionsApi()` -> `GET /auth/permissions`
- `logoutApi()` -> `POST /auth/logout`
- `changePasswordApi(payload)` -> `PUT /auth/password`

#### `business.js`

封装主要业务接口：

- `categoryApi`
- `productApi`
- `warehouseApi`
- `supplierApi`
- `dashboardApi`
- `stockApi`
- `stockMovementApi`
- `stockInApi`
- `stockOutApi`
- `inventoryAdjustmentApi`
- `stockTakeApi`
- `operationLogApi`
- `loginLogApi`

它还抽象了：

- `createCrudApi(basePath)`：生成 page/detail/create/update/remove。
- `createExcelApi(basePath)`：生成 export/importTemplate/importFile。
- `createDocumentApi(basePath)`：生成单据的 page/detail/create/update/confirm/cancel/export。

#### `users.js`

封装用户管理：

- `pageUsers`
- `getUser`
- `createUser`
- `updateUser`
- `updateUserPassword`
- `updateUserStatus`
- `deleteUser`
- `getUserRoles`
- `updateUserRoles`
- `getUserWarehouses`
- `updateUserWarehouses`

#### `rbac.js`

封装角色和权限：

- `roleApi`
- `permissionApi`

用于角色列表、权限树、角色权限分配等页面。

### 3. router

路由文件：

```text
frontend/src/router/index.js
```

Vue Router 的作用：

- 把 URL 映射到页面组件。
- 控制登录前后页面跳转。
- 根据权限决定是否允许访问页面。

当前路由结构：

- `/login`：登录页，public。
- `/`：主布局 `MainLayout`。
- `/dashboard`：Dashboard。
- `/categories`、`/products`、`/warehouses`、`/suppliers`：基础资料。
- `/users`、`/roles`、`/permissions`、`/login-logs`：系统管理，需要 ADMIN。
- `/stock-in`、`/stock-out`：出入库。
- `/inventory-adjustments`：库存调整。
- `/stock-takes`：库存盘点。
- `/stock`、`/low-stock`：库存和低库存。
- `/stock-movements`：库存流水。
- `/operation-logs`：操作日志。
- `/403`、404 路由。

登录守卫逻辑：

1. 如果目标路由是 public：
   - 已登录则跳 Dashboard。
   - 未登录允许访问。
2. 非 public 且未登录：
   - 跳 `/login`，并带 `redirect`。
3. 已登录但没有用户信息：
   - 调 `authStore.fetchCurrentUser()`。
4. 没有权限列表：
   - 调 `authStore.fetchPermissions()`。
5. 如果路由 `adminOnly` 且不是管理员：
   - 跳 `/403`。
6. 如果路由有 `permission` 且用户没有该权限：
   - 跳 `/403`。

### 4. stores

Pinia store 位于：

```text
frontend/src/stores/auth.js
```

Pinia 是 Vue 的状态管理库。你可以把它理解成“全局变量 + 业务方法”的统一管理。

`auth` store 保存：

- `token`
- `user`
- `roles`
- `permissions`
- `warehouseIds`
- `loading`

重要 getters：

- `isLoggedIn`：是否已登录。
- `username`：当前用户名。
- `roleLabel`：角色中文显示。
- `isAdmin`：是否管理员。
- `hasPermission(code)`：是否拥有权限。

重要 actions：

- `login(form)`：调用登录接口，保存 token 和 user，再拉取权限。
- `fetchCurrentUser()`：刷新当前用户。
- `fetchPermissions()`：刷新角色、权限、仓库范围。
- `logout()`：调用后端退出登录，再清除本地状态。
- `clearSession()`：清空 token、user、permissions。

本地存储工具：

```text
frontend/src/utils/auth-storage.js
```

它负责读取、写入、清除 token 和 user。

### 5. layouts

主布局文件：

```text
frontend/src/layouts/MainLayout.vue
```

它负责：

- 左侧菜单。
- 顶部栏。
- 菜单收起/展开。
- 当前页面标题。
- 用户头像、用户名、角色。
- 修改密码入口。
- 退出登录。
- `<RouterView />` 渲染当前页面。

菜单权限：

- 菜单项使用 `v-if="can('permission:code')"`。
- `can` 方法调用 `authStore.hasPermission(permission)`。
- 系统管理菜单通过 `showSystemMenu` 控制，只给管理员显示。

注意：前端隐藏菜单只是体验优化，不是安全边界。真正安全必须靠后端 `PermissionInterceptor` 和 `AdminOnlyInterceptor`。

### 6. constants

常量文件：

```text
frontend/src/constants/options.js
```

里面有：

- `masterStatusOptions`：基础资料状态，`ACTIVE` / `DISABLED`。
- `documentStatusOptions`：单据状态，`DRAFT` / `CONFIRMED` / `CANCELLED`。
- `operationModuleOptions`：操作日志模块。
- `operationActionOptions`：操作动作。
- `stockMovementTypeOptions`：库存流水类型。
- `findOption`、`statusLabel`、`statusType`：状态显示辅助函数。

这些常量用于表格标签、筛选下拉、状态展示。

### 7. utils

工具目录：

```text
frontend/src/utils
```

重要工具：

- `download.js`：`downloadBlob(blob, filename)`，用于 Excel 下载。
- `auth-storage.js`：token 和 user 的本地存储。
- `format.js`：格式化工具。

`downloadBlob` 的流程：

1. 用 `URL.createObjectURL(blob)` 创建临时地址。
2. 创建隐藏 `<a>`。
3. 设置 `download` 文件名。
4. 触发点击。
5. 回收临时 URL。

## 五、数据库设计详解

### 1. Flyway 是什么

Flyway 是数据库版本管理工具。你可以把它理解为“数据库结构的 Git”。

本项目的脚本在：

```text
backend/src/main/resources/db/migration
```

命名规则：

```text
V1__init_schema.sql
V2__add_rbac_tables.sql
...
```

后端启动时，Flyway 会检查哪些脚本执行过，没执行过的按版本顺序执行。

### 2. 每个迁移脚本做了什么

| 脚本 | 作用 |
| --- | --- |
| `V1__init_schema.sql` | 创建基础表：users、categories、products、warehouses、suppliers、stock、stock_in、stock_in_items、stock_out、stock_out_items、operation_logs。 |
| `V2__add_rbac_tables.sql` | 创建 RBAC 表：roles、permissions、role_permissions、user_roles、user_warehouse_permissions，并初始化角色和权限。 |
| `V3__add_stock_movements.sql` | 创建库存流水表 stock_movements，并增加 `stock-movement:view` 权限。 |
| `V4__add_stock_concurrency_control.sql` | 给 `stock` 表增加 `version` 字段，用于库存更新版本记录。 |
| `V5__add_inventory_adjustments.sql` | 创建库存调整表 inventory_adjustments 和 inventory_adjustment_items，并增加相关权限。 |
| `V6__add_stock_takes.sql` | 创建库存盘点表 stock_takes 和 stock_take_items，并增加相关权限。 |
| `V7__enhance_audit_and_login_security.sql` | 创建 login_logs、token_blacklist，并增强 operation_logs 字段，增加登录日志权限。 |

### 3. `flyway_schema_history` 表的作用

Flyway 会自动创建 `flyway_schema_history` 表，用来记录：

- 哪个脚本执行过。
- 执行顺序。
- 校验和 checksum。
- 执行时间。
- 是否成功。

有了这张表，Flyway 才知道下一次启动时应该执行哪些新脚本。

不要手动改这张表，除非你非常清楚 Flyway 的修复流程。

### 4. 核心表分类说明

#### 基础资料

| 表 | 作用 |
| --- | --- |
| `users` | 用户账号，包含 username、password、role、status。 |
| `categories` | 商品分类，例如上衣、裤装、外套。 |
| `products` | 服装商品，SKU 唯一，关联分类。 |
| `warehouses` | 仓库资料，code 唯一。 |
| `suppliers` | 供应商资料，code 唯一。 |

#### 权限系统

| 表 | 作用 |
| --- | --- |
| `roles` | 角色表，例如 ADMIN、MANAGER、STAFF、VIEWER。 |
| `permissions` | 权限点表，例如 `product:view`。 |
| `user_roles` | 用户和角色关联表。 |
| `role_permissions` | 角色和权限关联表。 |
| `user_warehouse_permissions` | 用户可访问仓库范围。 |

#### 库存业务

| 表 | 作用 |
| --- | --- |
| `stock` | 当前库存表。 |
| `stock_in` | 入库主表。 |
| `stock_in_items` | 入库明细表。 |
| `stock_out` | 出库主表。 |
| `stock_out_items` | 出库明细表。 |
| `stock_movements` | 库存流水表。 |
| `inventory_adjustments` | 库存调整主表。 |
| `inventory_adjustment_items` | 库存调整明细表。 |
| `stock_takes` | 库存盘点主表。 |
| `stock_take_items` | 库存盘点明细表。 |

#### 安全审计

| 表 | 作用 |
| --- | --- |
| `login_logs` | 登录成功/失败记录。 |
| `operation_logs` | 业务操作日志。 |
| `token_blacklist` | 退出登录后的 token 黑名单。 |

### 5. `stock` 和 `stock_movements` 的区别

`stock` 是当前状态：

- 某商品在某仓库现在有多少库存。
- 一条记录由 `product_id + warehouse_id` 唯一确定。
- 用于库存查询、出库校验、低库存预警。

`stock_movements` 是历史过程：

- 每次库存变化都记录一条流水。
- 记录变化前、变化数量、变化后。
- 用于审计、追踪、排查库存差异。

简单理解：

- `stock` 回答“现在有多少”。
- `stock_movements` 回答“为什么变成现在这么多”。

### 6. 主表和明细表的关系

入库例子：

- `stock_in` 是主表，保存入库单号、仓库、供应商、操作人、总数量、总金额、状态。
- `stock_in_items` 是明细表，保存每个商品、数量、单价、金额。

一张入库单有多条明细：

```text
stock_in.id = stock_in_items.stock_in_id
```

出库、库存调整、库存盘点也是同样结构：

- `stock_out` -> `stock_out_items`
- `inventory_adjustments` -> `inventory_adjustment_items`
- `stock_takes` -> `stock_take_items`

为什么要分主表和明细表：

- 主表保存单据级信息。
- 明细表保存商品级信息。
- 一张单据可以包含多个商品。
- 更符合真实业务单据结构。

### 7. 为什么库存调整和库存盘点要单独建表

库存调整和库存盘点看起来都能改库存，但业务含义不同。

库存调整：

- 用于处理明确的库存修正。
- 例如破损、丢失、手工纠错。
- 调整数量可以是正数或负数。
- 表：`inventory_adjustments`、`inventory_adjustment_items`。

库存盘点：

- 用于周期性核对账面库存和实物库存。
- 核心字段是账面数量、实盘数量、差异数量。
- 确认时根据实盘数更新库存。
- 表：`stock_takes`、`stock_take_items`。

单独建表的好处：

- 业务语义清晰。
- 审计更容易。
- 后续可以分别加审批流程。
- 报表可以区分“调整导致变化”和“盘点导致变化”。

## 六、核心业务流程详解

### 1. 登录流程

前端入口：

- 页面：`frontend/src/views/LoginView.vue`
- 状态：`frontend/src/stores/auth.js`
- API：`frontend/src/api/auth.js`

后端入口：

- Controller：`AuthController.login`
- Service：`AuthServiceImpl.login`
- 工具：`JwtUtil`

流程：

1. 用户在登录页输入用户名和密码。
2. `LoginView.vue` 调用 `authStore.login(loginForm)`。
3. `authStore.login` 调 `loginApi(form)`。
4. `loginApi` 发送 `POST /api/auth/login`。
5. 后端 `AuthController.login` 接收请求。
6. `AuthServiceImpl.login` 根据 username 查询 `users`。
7. 用 `passwordEncoder.matches(request.password(), user.getPassword())` 校验 BCrypt 密码。
8. 判断用户状态是否为 `ACTIVE`。
9. 调 `jwtUtil.generateToken(user)` 生成 JWT。
10. 写入登录成功日志。
11. 返回 `AuthLoginResponse`。
12. 前端保存 token 和 user。
13. 前端调用 `/auth/permissions` 拉取权限。
14. 后续请求由 `http.js` 自动带上 `Authorization: Bearer <token>`。

BCrypt 在哪里用：

- `AuthServiceImpl.register`
- `AuthServiceImpl.login`
- `AuthServiceImpl.changePassword`
- `UserManagementServiceImpl` 新增用户和重置密码
- `InitAdminServiceImpl.initializeIfNecessary`

JWT 如何生成：

- `JwtUtil.generateToken(User user)`
- 使用 `Jwts.builder()`
- 放入 `userId`、`username`、`role`
- 设置 `issuedAt` 和 `expiration`
- 用 HS256 签名

### 2. 权限校验流程

权限模型：

```text
users -> user_roles -> roles -> role_permissions -> permissions
```

前端权限：

- 登录后 `authStore.fetchPermissions()` 调 `/api/auth/permissions`。
- 权限保存到 `authStore.permissions`。
- `MainLayout.vue` 中菜单用 `v-if="can('xxx:view')"` 控制显示。
- `router/index.js` 中路由 meta 有 `permission` 和 `adminOnly`。

后端权限：

- `JwtAuthInterceptor` 先确认用户是谁。
- `AdminOnlyInterceptor` 限制 `/api/users/**`、`/api/roles/**`、`/api/permissions/**`、`/api/login-logs/**`。
- `PermissionInterceptor` 根据 URI 和 HTTP method 推导权限码。
- `AuthorizationService.hasPermission(...)` 检查用户是否有权限。

为什么不能只靠前端隐藏按钮：

- 前端代码在浏览器里，用户可以直接调用接口。
- 用户可以用 Postman/curl 绕过页面按钮。
- 真正安全必须在后端校验。
- 前端权限只是改善体验，后端权限才是安全防线。

### 3. 入库流程

涉及文件：

- `StockInController`
- `StockInService`
- `StockInServiceImpl`
- `StockInMapper`
- `StockInItemMapper`
- `StockMapper`
- `StockMovementServiceImpl`

涉及表：

- `stock_in`
- `stock_in_items`
- `stock`
- `stock_movements`

创建入库单：

1. 前端提交 warehouseId、supplierId、items。
2. `StockInController.create` 调 `stockInService.create(request)`。
3. `validateRequest` 校验仓库、供应商、商品都存在且 ACTIVE。
4. 生成单号 `SI + 时间 + 随机后缀`。
5. 主表 `stock_in` 状态保存为 `DRAFT`。
6. 明细保存到 `stock_in_items`。

确认入库：

1. `StockInController.confirm(id)` 调 `stockInService.confirm(id)`。
2. 查询入库单。
3. `ensureDraft` 检查只能确认草稿单。
4. `markConfirmedIfDraft` 条件更新状态，防止重复确认。
5. 遍历入库明细。
6. `StockMapper.selectByProductAndWarehouseForUpdate` 锁定库存行。
7. 当前库存加上入库数量。
8. 更新或插入 `stock`。
9. 写入 `stock_movements`，类型为 `STOCK_IN`。

防止重复确认：

- 代码不是简单 `stockIn.setStatus(CONFIRMED)`。
- 而是 SQL 条件更新：`WHERE id = ? AND status = 'DRAFT'`。
- 如果更新行数不是 1，说明已经被处理过。

### 4. 出库流程

涉及文件：

- `StockOutController`
- `StockOutServiceImpl`
- `StockOutMapper`
- `StockOutItemMapper`
- `StockMapper`
- `StockMovementServiceImpl`

涉及表：

- `stock_out`
- `stock_out_items`
- `stock`
- `stock_movements`

创建出库单：

1. 前端提交 warehouseId 和 items。
2. 后端校验仓库、商品。
3. 生成单号 `SO + 时间 + 随机后缀`。
4. 保存 `stock_out` 主表，状态为 `DRAFT`。
5. 保存 `stock_out_items` 明细。

确认出库：

1. 查询出库单。
2. 只能确认 `DRAFT`。
3. 条件更新状态为 `CONFIRMED`，防止重复确认。
4. 遍历明细。
5. 用 `FOR UPDATE` 锁库存行。
6. 检查库存是否足够。
7. 调 `decreaseQuantityByIdIfEnough` 扣减库存。
8. 写 `STOCK_OUT` 库存流水。

库存不足怎么处理：

- 如果库存记录不存在，抛出 `Insufficient stock...`。
- 如果扣减后小于 0，抛出异常。
- 如果 SQL 条件扣减更新行数不是 1，也抛出库存不足。

并发出库怎么保护：

- `SELECT ... FOR UPDATE` 加行锁。
- `UPDATE stock SET quantity = quantity - ? WHERE id = ? AND quantity >= ?` 加条件扣减。
- 整个确认方法有 `@Transactional`。

### 5. 库存流水流程

哪些业务会产生库存流水：

- 入库确认：`STOCK_IN`
- 出库确认：`STOCK_OUT`
- 库存调整确认：`ADJUSTMENT`
- 库存盘点确认：`STOCK_TAKE`

含义：

| 类型 | 含义 |
| --- | --- |
| `STOCK_IN` | 入库导致库存增加。 |
| `STOCK_OUT` | 出库导致库存减少。 |
| `ADJUSTMENT` | 手工库存调整导致库存变化。 |
| `STOCK_TAKE` | 盘点差异导致库存变化。 |

字段解释：

- `quantity_before`：变化前库存。
- `change_quantity`：变化数量，正数表示增加，负数表示减少。
- `quantity_after`：变化后库存。

例子：

```text
原库存 10，入库 5：
quantity_before = 10
change_quantity = 5
quantity_after = 15

原库存 10，出库 3：
quantity_before = 10
change_quantity = -3
quantity_after = 7
```

### 6. 库存调整流程

为什么需要库存调整：

- 商品破损。
- 商品丢失。
- 系统录入错误。
- 线下库存和系统库存不一致。

流程：

1. 创建库存调整单。
2. 填写仓库、原因、调整明细。
3. 明细中 `adjustQuantity` 可以为正或负，但不能为 0。
4. 草稿可以编辑或取消。
5. 确认时用 `FOR UPDATE` 锁定库存。
6. 计算调整后库存。
7. 如果调整后小于 0，拒绝。
8. 更新 `stock`。
9. 更新调整明细中的 `quantityBefore`、`quantityAfter`。
10. 写 `ADJUSTMENT` 库存流水。

关键文件：

- `InventoryAdjustmentController`
- `InventoryAdjustmentServiceImpl`
- `inventory_adjustments`
- `inventory_adjustment_items`

### 7. 库存盘点流程

为什么需要盘点：

- 系统库存是账面数。
- 实际仓库可能因为漏扫、损耗、人为错误产生差异。
- 盘点用于用实物库存校正系统库存。

账面数量和实盘数量：

- `bookQuantity`：创建盘点明细时系统里的库存数量。
- `actualQuantity`：人工盘点得到的真实数量。
- `differenceQuantity`：差异，等于 `actualQuantity - bookQuantity`。

流程：

1. 创建盘点单。
2. 选择仓库，录入商品和实盘数量。
3. 系统读取当前库存作为账面数量。
4. 保存盘点明细。
5. 可导入盘点明细。
6. 确认盘点时检查当前库存是否仍等于保存时的账面数量。
7. 如果中途库存变了，拒绝确认，避免覆盖新库存。
8. 如果没变，把库存更新为实盘数量。
9. 差异不为 0 时写 `STOCK_TAKE` 流水。

关键文件：

- `StockTakeController`
- `StockTakeServiceImpl`
- `stock_takes`
- `stock_take_items`

### 8. Excel 导入导出流程

当前项目没有使用 EasyExcel，也没有使用 Apache POI。

当前实现：

- `ExcelServiceImpl.writeWorkbook(...)`：自己拼接 xlsx XML，再用 Zip 输出。
- `ExcelServiceImpl.readRows(MultipartFile file)`：读取 xlsx zip entries，解析 `xl/worksheets/sheet1.xml` 和 `xl/sharedStrings.xml`。
- `BusinessExcelServiceImpl`：把业务对象转换为 Excel 行，或把 Excel 行转换为 Request DTO。
- `ExcelResponseUtil.workbook(...)`：生成下载响应。

模板下载：

例如商品模板：

- 前端调用 `productApi.importTemplate()`。
- 后端 `ProductController.importTemplate()` 调 `businessExcelService.productTemplate()`。
- 返回空数据但带表头的 xlsx。

文件上传：

- 前端用 `FormData` 上传。
- 后端接收 `MultipartFile file`。
- `BusinessExcelServiceImpl.importRows(...)` 读取每行。
- 成功则调用对应 Service 的 create。
- 失败则返回 `ExcelImportResultResponse`。

导出文件：

- 前端请求接口时设置 `responseType: 'blob'`。
- 后端返回 byte[]。
- 前端 `downloadBlob` 触发下载。

后续如何优化：

- 引入 EasyExcel，减少自写 xlsx 解析维护成本。
- 支持大文件分批导入。
- 支持失败明细导出。
- 支持异步导入任务和导入进度。

### 9. 登录安全流程

登录成功日志：

- `AuthServiceImpl.login` 成功后调用 `loginLogService.record(...)`。
- 记录 userId、username、success、requestIp、userAgent。

登录失败日志：

- 用户不存在、密码错误、账号禁用、失败次数过多都会记录。

失败次数限制：

- `MAX_FAILED_LOGIN_ATTEMPTS = 5`
- `LOGIN_LOCK_MINUTES = 15`
- 15 分钟内失败 5 次会禁止继续登录。

token 黑名单：

- 退出登录时，`AuthServiceImpl.logout(token)` 解析 token。
- 调用 `TokenBlacklistService.blacklist(...)`。
- 保存 token 的 hash 到 `token_blacklist`。
- 下次请求时 `JwtAuthInterceptor` 调 `tokenBlacklistService.isBlacklisted(token)`。
- 如果命中黑名单，返回 401。

为什么需要黑名单：

- JWT 默认是无状态的，签发后到过期前都有效。
- 如果用户点击退出登录，理论上旧 token 仍可用。
- 黑名单可以让已退出 token 立即失效。

### 10. 初始化管理员流程

为什么需要初始化管理员：

- 新数据库没有任何用户。
- 如果没有注册入口或生产禁用注册，就无法登录后台。
- 初始化管理员用于首次部署进入系统。

配置：

```text
INIT_ADMIN_ENABLED=true
INIT_ADMIN_USERNAME=admin
INIT_ADMIN_PASSWORD=<strong-password>
```

流程：

1. `InitAdminRunner.run(...)` 在启动后执行。
2. 如果 `InitAdminProperties.enabled=false`，直接返回。
3. 调 `InitAdminServiceImpl.initializeIfNecessary()`。
4. 查询 `users` 表是否为空。
5. 如果不为空，跳过。
6. 查询 `ADMIN` 角色。
7. 创建用户，密码 BCrypt 加密。
8. 写 `user_roles` 绑定 ADMIN 角色。

关键文件：

- `InitAdminRunner.java`
- `InitAdminProperties.java`
- `InitAdminServiceImpl.java`
- `.env.example`
- `docker-compose.yml`

## 七、关键代码清单

### 后端必须重点阅读

| 文件 | 为什么重要 |
| --- | --- |
| `backend/src/main/java/com/warehouse/management/controller/AuthController.java` | 认证接口入口，理解登录、当前用户、退出登录从这里开始。 |
| `backend/src/main/java/com/warehouse/management/service/impl/AuthServiceImpl.java` | 登录、注册、密码校验、JWT、登录日志、token 黑名单核心逻辑。 |
| `backend/src/main/java/com/warehouse/management/util/JwtUtil.java` | JWT 生成和解析。 |
| `backend/src/main/java/com/warehouse/management/config/JwtAuthInterceptor.java` | 每个受保护接口如何识别当前用户。 |
| `backend/src/main/java/com/warehouse/management/config/AdminOnlyInterceptor.java` | 系统管理接口只允许 ADMIN。 |
| `backend/src/main/java/com/warehouse/management/config/PermissionInterceptor.java` | RBAC 权限码如何映射和校验。 |
| `backend/src/main/java/com/warehouse/management/config/WebConfig.java` | 拦截器注册顺序和排除路径。 |
| `backend/src/main/java/com/warehouse/management/service/impl/AuthorizationServiceImpl.java` | 用户角色、权限、仓库范围查询。 |
| `backend/src/main/java/com/warehouse/management/service/impl/UserManagementServiceImpl.java` | 用户管理、密码加密、角色分配、仓库范围。 |
| `backend/src/main/java/com/warehouse/management/service/impl/StockInServiceImpl.java` | 入库确认如何增加库存。 |
| `backend/src/main/java/com/warehouse/management/service/impl/StockOutServiceImpl.java` | 出库确认如何扣库存和防止负库存。 |
| `backend/src/main/java/com/warehouse/management/mapper/StockMapper.java` | 库存并发控制核心 SQL。 |
| `backend/src/main/java/com/warehouse/management/service/impl/StockMovementServiceImpl.java` | 库存流水写入和查询。 |
| `backend/src/main/java/com/warehouse/management/service/impl/InventoryAdjustmentServiceImpl.java` | 库存调整业务。 |
| `backend/src/main/java/com/warehouse/management/service/impl/StockTakeServiceImpl.java` | 库存盘点业务和盘点明细导入导出。 |
| `backend/src/main/java/com/warehouse/management/service/impl/BusinessExcelServiceImpl.java` | 业务 Excel 导入导出。 |
| `backend/src/main/java/com/warehouse/management/service/impl/ExcelServiceImpl.java` | 底层 xlsx 生成和解析。 |
| `backend/src/main/java/com/warehouse/management/config/InitAdminRunner.java` | 启动时初始化管理员入口。 |
| `backend/src/main/java/com/warehouse/management/service/impl/InitAdminServiceImpl.java` | 创建初始 ADMIN 用户并绑定角色。 |
| `backend/src/main/java/com/warehouse/management/common/ApiResponse.java` | 统一响应格式。 |
| `backend/src/main/java/com/warehouse/management/common/BusinessException.java` | 业务异常。 |
| `backend/src/main/java/com/warehouse/management/common/ErrorCode.java` | 错误码。 |
| `backend/src/main/java/com/warehouse/management/common/CurrentUserContext.java` | 当前用户上下文。 |
| `backend/src/main/java/com/warehouse/management/common/GlobalExceptionHandler.java` | 全局异常处理。 |

### 前端必须重点阅读

| 文件 | 为什么重要 |
| --- | --- |
| `frontend/src/api/http.js` | Axios 基础封装、token 请求头、401/403 处理。 |
| `frontend/src/api/auth.js` | 认证接口封装。 |
| `frontend/src/api/business.js` | 业务接口封装，包括 CRUD、单据、Excel、库存。 |
| `frontend/src/api/users.js` | 用户管理接口封装。 |
| `frontend/src/api/rbac.js` | 角色和权限接口封装。 |
| `frontend/src/stores/auth.js` | 登录状态、权限、角色、仓库范围。 |
| `frontend/src/router/index.js` | 页面路由、登录守卫、权限路由。 |
| `frontend/src/layouts/MainLayout.vue` | 左侧菜单、顶部栏、退出登录、权限菜单。 |
| `frontend/src/views/LoginView.vue` | 登录页。需求里叫 Login.vue，但真实文件名是 LoginView.vue。 |
| `frontend/src/views/DashboardView.vue` | Dashboard 页面。需求里叫 Dashboard.vue，但真实文件名是 DashboardView.vue。 |
| `frontend/src/views/stock/StockMovementList.vue` | 库存流水页面。 |
| `frontend/src/views/stock/StockTakeList.vue` | 库存盘点页面。 |
| `frontend/src/views/stock/InventoryAdjustmentList.vue` | 库存调整页面。 |
| `frontend/src/views/user/UserList.vue` | 用户管理页面。 |
| `frontend/src/views/system/RoleList.vue` | 角色管理页面。 |
| `frontend/src/views/system/PermissionList.vue` | 权限管理页面。 |
| `frontend/src/views/documents/StockDocumentView.vue` | 入库/出库共用单据页面。 |
| `frontend/src/constants/options.js` | 状态、流水类型、日志模块等选项。 |
| `frontend/src/utils/download.js` | Excel blob 下载。 |

### 数据库必须重点阅读

| 文件 | 为什么重要 |
| --- | --- |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | 基础表、库存表、出入库表、操作日志。 |
| `backend/src/main/resources/db/migration/V2__add_rbac_tables.sql` | RBAC 角色权限系统。 |
| `backend/src/main/resources/db/migration/V3__add_stock_movements.sql` | 库存流水。 |
| `backend/src/main/resources/db/migration/V4__add_stock_concurrency_control.sql` | stock version 字段。 |
| `backend/src/main/resources/db/migration/V5__add_inventory_adjustments.sql` | 库存调整。 |
| `backend/src/main/resources/db/migration/V6__add_stock_takes.sql` | 库存盘点。 |
| `backend/src/main/resources/db/migration/V7__enhance_audit_and_login_security.sql` | 登录日志、token 黑名单、操作日志增强。 |
| `backend/src/main/resources/application.yml` | 默认配置、数据库连接、Flyway、JWT、初始化管理员。 |
| `backend/src/main/resources/application-prod.yml` | 生产配置，敏感信息来自环境变量。 |

### 部署必须重点阅读

| 文件 | 为什么重要 |
| --- | --- |
| `docker-compose.yml` | MySQL、后端、前端三服务编排。 |
| `backend/Dockerfile` | 后端镜像构建和运行。 |
| `frontend/Dockerfile` | 前端构建和 Nginx 镜像。 |
| `frontend/nginx.conf` | `/api` 反向代理和 Vue history 路由刷新。 |
| `.env.example` | 环境变量模板。 |
| `.github/workflows/ci.yml` | GitHub Actions CI。 |

## 八、必须掌握的基础知识点

### 1. Spring Boot 是什么

Spring Boot 是 Java 后端开发框架。它把 Spring 常用配置整合起来，让你可以快速启动一个 Web 服务。本项目的启动类是：

```text
WarehouseManagementApplication.java
```

### 2. RESTful API 是什么

RESTful API 是一种接口风格，用 HTTP 方法表达动作：

- GET 查询
- POST 新增
- PUT 修改
- DELETE 删除

例如：

- `GET /api/products` 查询商品
- `POST /api/products` 新增商品
- `PUT /api/products/{id}` 修改商品
- `DELETE /api/products/{id}` 删除商品

### 3. Controller / Service / Mapper / Entity / DTO 分层是什么

本项目分层：

- Controller：接 HTTP 请求。
- Service：写业务逻辑。
- Mapper：访问数据库。
- Entity：对应数据库表。
- DTO：前后端传输数据。

一句话：

> Controller 不直接操作数据库，Controller 调 Service，Service 调 Mapper，Mapper 操作 Entity，DTO 负责接口输入输出。

### 4. MyBatis-Plus 是什么

MyBatis-Plus 是 MyBatis 的增强工具。继承 `BaseMapper<Entity>` 后，不用写简单 SQL 就能完成常见 CRUD。

本项目只有 `StockMapper` 因为需要库存并发控制，手写了自定义 SQL。

### 5. JWT 是什么

JWT 是 JSON Web Token。登录成功后后端生成 token，前端保存 token，以后每次请求带上 token。

优点：

- 后端不用保存登录 session。
- 适合前后端分离。

缺点：

- token 一旦签发，到过期前默认有效。
- 所以本项目加了 `token_blacklist` 支持退出登录立即失效。

### 6. BCrypt 是什么

BCrypt 是密码哈希算法。它不是加密后还能解密，而是把密码变成不可逆 hash。

本项目：

- 保存密码用 `passwordEncoder.encode(...)`。
- 校验密码用 `passwordEncoder.matches(...)`。

### 7. RBAC 是什么

RBAC 是 Role-Based Access Control，基于角色的权限控制。

本项目模型：

```text
用户 -> 角色 -> 权限
```

例如：

- ADMIN 拥有全部权限。
- STAFF 只能做日常出入库和查询。
- VIEWER 只能查看。

### 8. 拦截器 Interceptor 是什么

Interceptor 是 Spring MVC 在 Controller 执行前后插入逻辑的机制。

本项目用它做：

- JWT 认证。
- ADMIN 限制。
- 权限校验。
- 操作日志。

### 9. 事务 Transaction 是什么

事务保证一组数据库操作要么都成功，要么都失败。

例如确认出库：

- 更新单据状态。
- 扣减库存。
- 写库存流水。

这些必须在同一个事务里，否则可能出现单据已确认但库存没扣的错误。

本项目在关键方法上使用 `@Transactional`。

### 10. 幂等性是什么

幂等性指同一个操作执行多次，结果不应该重复产生副作用。

本项目例子：

- 入库确认不能重复增加库存。
- 出库确认不能重复扣库存。

实现方式：

- 只能确认 `DRAFT` 状态。
- 确认时用 `WHERE status = 'DRAFT'` 条件更新。

### 11. 并发控制是什么

并发控制是处理多人同时操作同一数据时的数据一致性问题。

本项目最典型是多人同时出库同一个商品：

- 需要防止库存扣成负数。
- 需要防止两个请求读到同一个旧库存。

### 12. 数据库行锁 / 乐观锁是什么

行锁：

- `SELECT ... FOR UPDATE`
- 在事务中锁住某一行。
- 其它事务要等锁释放才能改。

乐观锁：

- 通常用 version 字段。
- 更新时检查 version 是否还是原来的。

本项目：

- `stock.version` 字段会递增。
- 实际关键保护主要靠 `FOR UPDATE` 和条件更新 `quantity >= ?`。

### 13. Flyway 是什么

Flyway 是数据库迁移工具。它会按版本执行 SQL 脚本，并记录到 `flyway_schema_history`。

好处：

- 不用人工执行 SQL。
- 团队数据库结构一致。
- 可以追踪每次表结构变化。

### 14. Docker 是什么

Docker 是容器化工具。它把应用和运行环境打包在一起。

本项目：

- 后端 Dockerfile 打包 jar。
- 前端 Dockerfile 构建 dist 并放入 Nginx。

### 15. Docker Compose 是什么

Docker Compose 用一个 `docker-compose.yml` 管理多个容器。

本项目一次启动：

- MySQL
- backend
- frontend

### 16. Nginx 反向代理是什么

反向代理就是浏览器访问 Nginx，Nginx 再把请求转发给后端。

本项目：

- 浏览器访问 `/api/products`。
- Nginx 转发到 `http://backend:8080/api/products`。

### 17. Vue 组件是什么

Vue 组件是一个页面或页面中的一块 UI。通常一个 `.vue` 文件包含：

- template
- script
- style

例如 `LoginView.vue` 就是登录页组件。

### 18. Vue Router 是什么

Vue Router 是前端路由库。它根据 URL 显示对应组件。

例如：

- `/login` -> `LoginView.vue`
- `/dashboard` -> `DashboardView.vue`

### 19. Pinia 是什么

Pinia 是 Vue 的状态管理工具。本项目用它保存登录状态：

- token
- user
- roles
- permissions
- warehouseIds

### 20. Axios 是什么

Axios 是前端 HTTP 请求库。本项目通过 `http.js` 统一封装，自动带 token，统一处理错误。

### 21. Element Plus 是什么

Element Plus 是 Vue 3 UI 组件库。本项目大量使用：

- 表格
- 表单
- 弹窗
- 菜单
- 按钮
- 标签
- 下拉框

### 22. `.env` 环境变量是什么

`.env` 用来保存不同环境的配置，例如：

- 数据库密码
- JWT_SECRET
- 初始化管理员账号

注意：真实 `.env` 不应该提交到 Git，只提交 `.env.example`。

### 23. 前后端分离是什么

前端只负责页面，后端只负责 API。前端通过 HTTP 调用后端。

本项目开发环境：

- 前端 `localhost:5173`
- 后端 `localhost:8080`
- Vite 把 `/api` 代理到后端。

生产环境：

- Nginx 提供前端页面。
- Nginx 把 `/api` 代理到后端。

### 24. CORS / 代理是什么

CORS 是浏览器跨域限制。如果前端和后端端口不同，就可能跨域。

本项目开发时通过 Vite 代理解决：

- 前端请求 `/api`
- Vite 转发到 `localhost:8080`
- 浏览器看起来还是同源请求

生产时通过 Nginx 代理解决。

## 九、面试高频问题和回答

### 1. 你这个项目是做什么的？

答：这是一个服装仓库管理系统，主要解决服装企业的商品资料、入库、出库、库存查询、低库存预警和库存追踪问题。它不是简单 CRUD，因为入库确认会增加库存，出库确认会扣减库存，库存调整和盘点也会产生库存流水。系统还做了 JWT 登录、RBAC 权限、操作日志、登录日志、Excel 导入导出和 Docker 部署。

### 2. 为什么选择 Spring Boot + Vue？

答：Spring Boot 适合做后端业务接口和数据库事务，生态成熟；Vue 3 适合快速做后台管理页面，配合 Element Plus 可以高效实现表格、表单、弹窗。这个项目是前后端分离，后端提供 REST API，前端独立开发和部署，结构比较清晰。

### 3. 你的项目是怎么分层的？

答：后端主要分 Controller、Service、Mapper、Entity、DTO。Controller 接收请求，Service 写业务逻辑，Mapper 访问数据库，Entity 对应数据库表，DTO 用来接收请求和返回数据。比如入库确认是 `StockInController` 调 `StockInServiceImpl.confirm`，再通过 `StockMapper` 更新库存。

### 4. Controller 和 Service 有什么区别？

答：Controller 是接口入口，负责接参数和返回结果；Service 是业务层，负责真正的业务规则和事务。比如 Controller 不应该直接扣库存，扣库存逻辑放在 `StockOutServiceImpl`，这样代码更清晰，也方便测试和维护。

### 5. DTO 和 Entity 有什么区别？

答：Entity 对应数据库表，DTO 是接口传输对象。不能直接把 Entity 返回给前端，因为 Entity 可能包含敏感字段，比如 `User.password`。本项目的 `UserResponse` 不包含 password，`AuthUserResponse` 也只返回 id、username、role、status。

### 6. 登录是怎么实现的？

答：前端登录页调用 `/api/auth/login`，后端 `AuthServiceImpl` 根据 username 查用户，用 BCrypt 校验密码，校验状态后用 `JwtUtil` 生成 token。前端把 token 保存起来，后续请求在 `http.js` 请求拦截器里自动加 `Authorization: Bearer token`。

### 7. JWT 退出登录后为什么还要 token 黑名单？

答：JWT 是无状态的，签发后到过期前默认都有效。如果用户退出登录但 token 没过期，理论上别人拿到旧 token 还能访问。项目用 `token_blacklist` 保存退出 token 的 hash，`JwtAuthInterceptor` 每次请求都会检查黑名单，这样退出后旧 token 立即失效。

### 8. 权限是怎么做的？

答：项目用了 RBAC。数据库有 `users`、`roles`、`permissions`、`user_roles`、`role_permissions`。前端根据 `/api/auth/permissions` 返回的权限控制菜单和路由，后端用 `PermissionInterceptor` 根据接口路径和 HTTP 方法推导权限码，再判断当前用户是否拥有权限。

### 9. 什么是 RBAC？

答：RBAC 就是基于角色的权限控制。用户不直接绑定一堆权限，而是先绑定角色，角色再绑定权限。这样管理员只要维护角色权限，用户换岗位时改角色就可以了。

### 10. 前端隐藏菜单和后端权限校验有什么区别？

答：前端隐藏菜单只是用户体验，防止用户看到不能点的入口。但前端代码不安全，用户可以直接用 curl 或 Postman 调接口。所以真正的安全必须靠后端拦截器校验权限。本项目后端有 `AdminOnlyInterceptor` 和 `PermissionInterceptor`。

### 11. 入库流程怎么实现？

答：先创建入库草稿单，保存 `stock_in` 和 `stock_in_items`。确认时只能确认 DRAFT 状态的单据，后端先把状态条件更新为 CONFIRMED，然后遍历明细，用 `StockMapper.selectByProductAndWarehouseForUpdate` 锁库存行，增加库存，再写 `STOCK_IN` 库存流水。

### 12. 出库流程怎么实现？

答：出库先创建草稿单，确认时检查单据是 DRAFT，先条件更新状态为 CONFIRMED，防止重复确认。然后遍历明细，锁定库存行，判断库存是否足够，用 `decreaseQuantityByIdIfEnough` 扣减库存，最后写 `STOCK_OUT` 库存流水。

### 13. 如何避免库存扣成负数？

答：项目有两层保护。第一层是在 Java 里计算 `quantityAfter`，小于 0 就拒绝。第二层是 SQL 条件更新：`UPDATE stock SET quantity = quantity - ? WHERE id = ? AND quantity >= ?`。即使并发情况下，也不会扣成负数。

### 14. 如何避免重复点击确认导致重复扣库存？

答：确认单据时不是直接改状态，而是使用条件更新：`WHERE id = ? AND status = 'DRAFT'`。如果第一次确认成功，状态变为 CONFIRMED，第二次再点时更新行数为 0，就会抛出“已处理”的异常，不会重复扣库存或加库存。

### 15. 库存流水有什么作用？

答：`stock` 只能告诉我现在库存是多少，`stock_movements` 能告诉我库存为什么变成这样。每次入库、出库、调整、盘点都会记录变化前数量、变化数量、变化后数量、来源单据和操作人，用来审计和排查库存差异。

### 16. 库存调整和库存盘点有什么区别？

答：库存调整是针对明确原因的手工修正，比如破损、丢失、录入错误，直接填调整数量。库存盘点是周期性核对实物库存，先保存账面数和实盘数，再根据差异更新库存。调整强调原因，盘点强调账实核对。

### 17. Flyway 为什么比手动执行 SQL 好？

答：Flyway 能自动记录每个 SQL 脚本是否执行过，团队成员和服务器启动时都能自动迁移到同一个数据库版本。手动执行 SQL 容易漏执行、重复执行或顺序错。项目中所有结构变化都放在 `db/migration` 下。

### 18. Docker 部署是怎么做的？

答：项目用 `docker-compose.yml` 编排三个服务：MySQL、backend、frontend。MySQL 用 volume 持久化数据，backend 用 Java 17 JRE 运行 jar，frontend 用 Nginx 托管 Vue 构建产物。后端通过环境变量连接 MySQL，前端通过 Nginx 把 `/api` 代理到 backend。

### 19. 前端部署为什么需要 Nginx？

答：Vue 构建后是静态文件，需要 Web 服务器托管。Nginx 可以高效提供静态资源，还能做反向代理，把 `/api` 请求转发给后端，并通过 `try_files` 支持 Vue history 路由刷新。

### 20. Nginx 为什么要配置 `/api` 反向代理？

答：生产环境前端不能写死 `localhost:8080`，因为用户浏览器里的 localhost 是用户自己的电脑。统一请求 `/api`，再由 Nginx 转发到 Docker Compose 内部的 `backend:8080`，这样部署更稳定。

### 21. Vue 页面刷新 404 怎么解决？

答：Vue Router 使用 history 模式时，刷新 `/dashboard` 会让浏览器请求服务器的 `/dashboard` 路径。Nginx 如果找不到真实文件就会 404。项目在 `frontend/nginx.conf` 里配置了 `try_files $uri $uri/ /index.html;`，让所有前端路由回到 Vue 应用处理。

### 22. Excel 导入导出怎么实现？

答：项目没有用 EasyExcel，而是自实现 `ExcelServiceImpl`。导出时拼接 xlsx 内部 XML 并打成 zip，导入时读取 xlsx zip entries，解析 sheet 和 sharedStrings。业务层 `BusinessExcelServiceImpl` 负责把 Excel 行转成 ProductRequest、WarehouseRequest 等。

### 23. 项目如何保证数据安全？

答：密码用 BCrypt 保存，JWT 不放敏感字段，用户响应不返回 password。后端有 JWT 拦截、ADMIN 拦截和权限拦截。退出登录会把 token 加入黑名单。操作日志会对 password 字段脱敏。生产配置通过环境变量传入，不提交真实 `.env`。

### 24. 你项目里有哪些企业级特性？

答：有 RBAC 权限、Flyway 数据库迁移、Docker Compose 部署、Nginx 反向代理、MySQL volume 持久化、登录日志、操作日志、请求追踪 `X-Request-Id`、库存并发控制、库存流水、Excel 导入导出和 GitHub Actions CI。

### 25. 这个项目和普通 CRUD 项目有什么区别？

答：普通 CRUD 主要是增删改查表数据。这个项目有业务状态流转和库存事务，比如入库确认增加库存、出库确认扣库存、库存不足不能出库、确认操作要防重复、每次库存变化要写流水。这些都不是简单 CRUD。

### 26. 如果用户量变大，你会怎么优化？

答：可以从几个方面优化：数据库索引和慢 SQL、分页查询、导出异步化、引入 Redis 缓存权限和热点基础资料、日志异步写入、后端服务水平扩展、Nginx 负载均衡、前端按路由懒加载和表格虚拟滚动。

### 27. 为什么暂时没用 Redis？

答：当前系统定位是小型企业 MVP，数据量和并发不大，直接查 MySQL 可以降低复杂度。Redis 可以后续用于缓存权限、token 黑名单、登录失败次数、热点字典数据，但初期不是必须。

### 28. 这个项目如果商业化还缺什么？

答：还需要 HTTPS、监控告警、日志集中收集、自动备份和恢复演练、更细的数据权限、更多测试覆盖、审批流、扫码、报表中心、操作手册，以及更完整的性能和安全评估。

### 29. 如果空数据库首次启动，如何创建管理员？

答：在 `.env` 设置 `INIT_ADMIN_ENABLED=true`、`INIT_ADMIN_USERNAME`、`INIT_ADMIN_PASSWORD`。后端启动后 `InitAdminRunner` 会调用 `InitAdminServiceImpl`，如果 `users` 表为空就创建 ADMIN 用户，并用 BCrypt 加密密码，再写入 `user_roles` 绑定 ADMIN 角色。

### 30. 如果客户要部署，怎么操作？

答：先准备 Docker 和 Docker Compose，复制 `.env.example` 为 `.env`，修改数据库密码、JWT_SECRET 和初始化管理员密码。然后运行 `docker compose up -d --build`。启动后访问 `http://服务器地址`，首次用初始化管理员登录。数据库结构由 Flyway 自动迁移，不需要手动执行 schema.sql。

### 31. 为什么 JWT payload 里不放 email、phone？

答：JWT 会被前端保存，并且每次请求携带。虽然它有签名，但 payload 可以被解码看到，所以不应该放个人信息和敏感信息。本项目只放 userId、username、role、iat、exp，够认证和鉴权使用。

### 32. 操作日志怎么记录？

答：`OperationLogInterceptor` 在请求完成后记录 POST、PUT、DELETE 操作，自动推断模块和动作，保存请求 URI、方法、状态码、错误信息、请求体等信息，并对密码字段脱敏。

### 33. 请求追踪 `X-Request-Id` 有什么用？

答：排查线上问题时，可以用一个 requestId 把前端请求、后端日志和响应关联起来。本项目 `RequestIdFilter` 会生成或透传 `X-Request-Id`，并放到日志 MDC。

### 34. 为什么库存盘点确认时要检查账面数量是否变化？

答：盘点单保存时的账面数可能已经过期。比如保存盘点后又发生出库，如果直接用实盘数覆盖库存，就会丢掉中间业务变化。所以 `StockTakeServiceImpl.applyStockTake` 会检查当前库存是否仍等于 `bookQuantity`。

### 35. 项目里哪些地方用了事务？

答：创建、编辑、确认、取消单据都用了 `@Transactional`，比如 `StockInServiceImpl.confirm`、`StockOutServiceImpl.confirm`、`InventoryAdjustmentServiceImpl.confirm`、`StockTakeServiceImpl.confirm`。用户管理的新增、修改、分配角色等也用了事务。

## 十、我应该怎么学习这套代码

### 1. 第一遍看哪些文件

第一遍目标是理解项目怎么启动、怎么分层。

建议顺序：

1. `README.md`
2. `backend/pom.xml`
3. `frontend/package.json`
4. `backend/src/main/java/com/warehouse/management/WarehouseManagementApplication.java`
5. `backend/src/main/resources/application.yml`
6. `frontend/src/main.js`
7. `frontend/src/router/index.js`
8. `frontend/src/layouts/MainLayout.vue`

先不要深究每个业务细节，先知道项目从哪里启动、页面怎么跳、接口怎么走。

### 2. 第二遍看哪些业务流程

第二遍按业务链路看：

认证权限：

1. `LoginView.vue`
2. `stores/auth.js`
3. `api/http.js`
4. `AuthController.java`
5. `AuthServiceImpl.java`
6. `JwtUtil.java`
7. `JwtAuthInterceptor.java`
8. `PermissionInterceptor.java`

库存业务：

1. `StockInController.java`
2. `StockInServiceImpl.java`
3. `StockOutController.java`
4. `StockOutServiceImpl.java`
5. `StockMapper.java`
6. `StockMovementServiceImpl.java`

企业升级业务：

1. `UserManagementServiceImpl.java`
2. `RoleServiceImpl.java`
3. `PermissionServiceImpl.java`
4. `InventoryAdjustmentServiceImpl.java`
5. `StockTakeServiceImpl.java`
6. `BusinessExcelServiceImpl.java`

### 3. 第三遍怎么自己调试接口

建议用 Postman、curl 或 IDEA HTTP Client。

调试顺序：

1. 启动 MySQL。
2. 启动后端。
3. 登录拿 token。
4. 带 token 调 `/api/auth/me`。
5. 创建分类。
6. 创建商品。
7. 创建仓库。
8. 创建供应商。
9. 创建入库单。
10. 确认入库。
11. 查库存。
12. 创建出库单。
13. 确认出库。
14. 查库存流水。

每一步都看数据库表怎么变化。

### 4. 怎么通过前端页面找到对应后端接口

方法：

1. 打开页面组件，例如 `ProductView.vue`。
2. 看它 import 了哪个 API，例如 `productApi`。
3. 去 `frontend/src/api/business.js` 找 `productApi`。
4. 看到请求路径，例如 `/products`。
5. 后端找 `ProductController` 的 `/api/products`。
6. Controller 调用 `ProductService`。
7. 找 `ProductServiceImpl`。

这是一条前端到后端的追踪路线。

### 5. 怎么通过后端接口找到对应数据库表

方法：

1. 找 Controller。
2. 找 ServiceImpl。
3. 看 ServiceImpl 注入了哪些 Mapper。
4. Mapper 对应 Entity。
5. Entity 对应数据库表。
6. 去 Flyway 脚本看表结构。

例如出库：

```text
StockOutController
-> StockOutServiceImpl
-> StockOutMapper / StockOutItemMapper / StockMapper
-> stock_out / stock_out_items / stock
```

### 6. 怎么用断点调试登录、入库、出库、库存流水

登录断点：

- `AuthController.login`
- `AuthServiceImpl.login`
- `JwtUtil.generateToken`
- `JwtAuthInterceptor.preHandle`

入库断点：

- `StockInController.confirm`
- `StockInServiceImpl.confirm`
- `StockInServiceImpl.increaseStock`
- `StockMovementServiceImpl.record`

出库断点：

- `StockOutController.confirm`
- `StockOutServiceImpl.confirm`
- `StockOutServiceImpl.decreaseStock`
- `StockMapper.decreaseQuantityByIdIfEnough`
- `StockMovementServiceImpl.record`

库存流水断点：

- `StockMovementController.page`
- `StockMovementServiceImpl.page`
- `StockMovementServiceImpl.record`

调试技巧：

- 先用前端页面操作。
- 在 IDEA 中打断点。
- 观察 request DTO。
- 观察 Entity。
- 观察数据库变化。

### 7. 哪些代码不用一开始深究

初学时可以先不深究：

- `ExcelServiceImpl` 内部 xlsx XML 细节。
- 前端 CSS 细节。
- Dockerfile 每一行镜像构建优化。
- 操作日志里请求体缓存的底层 Servlet 细节。
- 所有 DTO 字段的完整记忆。

先掌握主线：

- 登录认证。
- 权限校验。
- 入库。
- 出库。
- 库存流水。
- 数据库表关系。

## 十一、未来优化方向

### 1. Redis

可以用于：

- 缓存用户权限。
- 保存 token 黑名单。
- 记录登录失败次数。
- 缓存基础资料。

当前没用 Redis 是为了降低 MVP 复杂度。

### 2. HTTPS

正式部署必须使用 HTTPS，避免 token 在网络中明文传输。可以用 Nginx 配置证书，或放到云厂商负载均衡后面。

### 3. 自动备份

建议增加：

- MySQL 定时备份。
- 备份上传到对象存储。
- 备份恢复脚本。
- 定期恢复演练。

### 4. 数据权限

当前有 `user_warehouse_permissions` 和前端 store 中的 `warehouseIds`，但后续应更完整地应用到：

- 库存查询。
- 出入库单据。
- Dashboard。
- 导出接口。
- 操作日志范围。

### 5. 报表增强

可以增加：

- 出入库趋势。
- 商品周转率。
- 分类库存占比。
- 仓库库存金额。
- 滞销商品。
- 低库存补货建议。

### 6. EasyExcel

当前 Excel 是自实现方案。后续可以引入 EasyExcel：

- 代码更少。
- 支持更复杂格式。
- 大文件性能更好。
- 错误处理更成熟。

### 7. 单元测试覆盖率

建议补充：

- `AuthServiceImpl` 登录和失败次数测试。
- `PermissionInterceptor` 权限测试。
- `StockOutServiceImpl` 库存不足测试。
- `StockMapper` 并发扣减测试。
- `StockTakeServiceImpl` 账面数变化测试。
- Excel 导入失败测试。

### 8. 操作手册

给真实用户准备：

- 管理员初始化说明。
- 商品导入模板说明。
- 入库操作步骤。
- 出库操作步骤。
- 盘点操作步骤。
- 常见错误处理。

### 9. 监控告警

建议增加：

- 后端健康检查监控。
- MySQL 连接监控。
- 500 错误告警。
- 登录失败异常告警。
- 磁盘空间告警。

### 10. 移动端适配

当前主要面向桌面后台。移动端可以优先适配：

- 登录页。
- 库存查询。
- 扫码出入库。
- 盘点录入。

### 11. 多租户 SaaS

如果要给多个企业使用，需要增加：

- tenant_id。
- 租户隔离。
- 租户管理员。
- 套餐和权限。
- 数据库隔离或逻辑隔离策略。

### 12. 性能优化

可以从这些方向做：

- 慢 SQL 分析。
- 常用查询加索引。
- 导出改异步任务。
- 大表分页优化。
- 前端路由懒加载已经有，但表格大数据还可优化。
- 日志异步写入。

## 总结

学习这个项目时，不要把它看成一堆页面和接口。它真正值得讲的是几个核心闭环：

1. 登录后 JWT 认证。
2. 用户、角色、权限的 RBAC。
3. 入库、出库、库存、流水的一致性。
4. 调整、盘点对库存可信度的补强。
5. Flyway、Docker、Nginx、环境变量组成的部署能力。

如果你能把这五条链路讲清楚，这个项目在学习、答辩和面试中就会很有说服力。
