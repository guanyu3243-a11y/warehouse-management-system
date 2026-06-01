# Clothing Warehouse Management System / 服装仓库管理系统

服装仓库管理系统是一个面向小型服装企业的全栈仓库管理项目，覆盖基础资料、入库、出库、库存、盘点、权限、日志、Dashboard 和部署上线准备。项目当前已从课程 MVP 升级到可试运行的企业级 MVP 版本。

## 项目定位

- 适用场景：服装 SKU 管理、仓库日常出入库、库存查询、低库存预警和基础经营看板。
- 目标用户：系统管理员、仓库主管、仓库操作员和只读查看人员。
- 当前状态：核心业务闭环已完成，支持本地开发、Docker Compose 部署、Flyway 自动迁移和 GitHub Actions CI。

## 技术栈

后端：

- Java 17
- Spring Boot 3
- Maven
- MyBatis-Plus
- MySQL
- Flyway
- JWT
- BCrypt

前端：

- Vue 3
- Vite
- Element Plus
- Axios
- Pinia
- Vue Router

部署与工具：

- Docker
- Docker Compose
- Nginx
- GitHub Actions
- Navicat
- IDEA
- VS Code

## 核心功能

账号与权限：

- 用户注册、登录、退出和当前用户查询
- JWT 认证
- BCrypt 密码加密
- 初始化管理员账号
- 用户管理
- 角色管理
- 权限管理
- 用户角色分配
- 用户仓库范围分配
- 前后端 ADMIN 权限控制

基础资料：

- 商品分类管理
- 服装商品管理
- 仓库管理
- 供应商管理
- 商品、仓库、供应商 Excel 导入和导出

库存业务：

- 入库单创建、编辑、确认和取消
- 出库单创建、编辑、确认和取消
- 库存查询
- 低库存预警
- 库存流水
- 库存调整
- 库存盘点
- 库存、库存流水、入库单、出库单导出

运营与审计：

- Dashboard 数据统计
- 操作日志
- 登录日志
- 请求追踪 `X-Request-Id`
- 统一异常响应

前端页面：

- 登录页
- Dashboard
- 用户管理
- 角色管理
- 权限管理
- 商品分类
- 服装商品
- 仓库管理
- 供应商管理
- 入库管理
- 出库管理
- 库存查询
- 低库存预警
- 库存流水
- 库存调整
- 库存盘点
- 操作日志
- 登录日志
- 修改密码
- 403 / 404 页面

## 项目结构

```text
.
+-- backend
|   +-- src/main/java/com/warehouse/management
|   +-- src/main/resources
|   |   +-- application.yml
|   |   +-- application-prod.yml
|   |   +-- db/migration
|   +-- src/test/java/com/warehouse/management
|   +-- Dockerfile
|   +-- pom.xml
+-- frontend
|   +-- public
|   +-- src/api
|   +-- src/constants
|   +-- src/layouts
|   +-- src/router
|   +-- src/stores
|   +-- src/views
|   +-- Dockerfile
|   +-- nginx.conf
|   +-- package.json
|   +-- vite.config.js
+-- docs
+-- .github/workflows
+-- docker-compose.yml
+-- .env.example
+-- README.md
```

## 数据库说明

数据库默认名称：

```text
warehouse_management
```

项目使用 Flyway 管理数据库初始化和迁移。启动后端时会自动执行：

```text
backend/src/main/resources/db/migration
```

当前迁移文件包括：

- `V1__init_schema.sql`
- `V2__add_rbac_tables.sql`
- `V3__add_stock_movements.sql`
- `V4__add_stock_concurrency_control.sql`
- `V5__add_inventory_adjustments.sql`
- `V6__add_stock_takes.sql`
- `V7__enhance_audit_and_login_security.sql`

`schema.sql` 仅作为设计参考保留，日常开发、Docker 部署和生产初始化都应交给 Flyway 自动完成。

## 环境变量

本地开发和 Docker 部署均通过环境变量覆盖敏感配置。不要提交真实 `.env` 文件。

常用环境变量：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=warehouse_management
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=<replace-with-local-dev-secret>
JWT_EXPIRATION_HOURS=24
FLYWAY_ENABLED=true
INIT_ADMIN_ENABLED=false
INIT_ADMIN_USERNAME=
INIT_ADMIN_PASSWORD=
```

环境模板：

```text
.env.example
```

详细说明：

```text
docs/environment-config.md
```

## 本地启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

健康检查：

```text
GET http://localhost:8080/api/health
```

### 前端

首次安装依赖：

```bash
cd frontend
npm install
```

Windows PowerShell 如果拦截 `npm.ps1`，使用：

```bash
npm.cmd install
```

启动开发服务器：

```bash
npm run dev
```

或：

```bash
npm.cmd run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到后端：

```text
http://localhost:8080
```

## 初始化管理员

Docker 部署或生产环境首次启动时，可以在 `.env` 中开启管理员初始化：

```text
INIT_ADMIN_ENABLED=true
INIT_ADMIN_USERNAME=admin
INIT_ADMIN_PASSWORD=<replace-with-strong-password>
```

后端启动时会检查 `users` 表是否为空。只有在表为空且 `INIT_ADMIN_ENABLED=true` 时才会自动创建管理员账号，密码会使用 BCrypt 加密，并绑定 ADMIN 角色。已有用户数据时不会重复创建。

开发环境也可以使用注册接口创建测试账号：

```bash
curl.exe -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\",\"role\":\"ADMIN\"}"
```

## 常用接口

认证：

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
GET  /api/auth/permissions
PUT  /api/auth/password
```

系统管理：

```text
GET    /api/users
POST   /api/users
GET    /api/users/{id}
PUT    /api/users/{id}
PUT    /api/users/{id}/password
PUT    /api/users/{id}/status
DELETE /api/users/{id}
GET    /api/users/{id}/roles
PUT    /api/users/{id}/roles
GET    /api/users/{id}/warehouses
PUT    /api/users/{id}/warehouses

GET    /api/roles
POST   /api/roles
GET    /api/roles/{id}
PUT    /api/roles/{id}
PUT    /api/roles/{id}/status
DELETE /api/roles/{id}
GET    /api/roles/{id}/permissions
PUT    /api/roles/{id}/permissions

GET    /api/permissions
GET    /api/permissions/tree
POST   /api/permissions
PUT    /api/permissions/{id}
PUT    /api/permissions/{id}/status
```

业务管理：

```text
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}

GET    /api/products
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
GET    /api/products/export
GET    /api/products/import-template
POST   /api/products/import

GET    /api/warehouses
POST   /api/warehouses
PUT    /api/warehouses/{id}
DELETE /api/warehouses/{id}
GET    /api/warehouses/export
GET    /api/warehouses/import-template
POST   /api/warehouses/import

GET    /api/suppliers
POST   /api/suppliers
PUT    /api/suppliers/{id}
DELETE /api/suppliers/{id}
GET    /api/suppliers/export
GET    /api/suppliers/import-template
POST   /api/suppliers/import
```

库存业务：

```text
GET  /api/stock
GET  /api/stock/low
GET  /api/stock/export
GET  /api/stock/product/{productId}

GET  /api/stock-movements
GET  /api/stock-movements/export
GET  /api/stock-movements/{id}
GET  /api/stock-movements/product/{productId}
GET  /api/stock-movements/warehouse/{warehouseId}

GET  /api/stock-in
POST /api/stock-in
PUT  /api/stock-in/{id}
POST /api/stock-in/{id}/confirm
POST /api/stock-in/{id}/cancel
GET  /api/stock-in/export

GET  /api/stock-out
POST /api/stock-out
PUT  /api/stock-out/{id}
POST /api/stock-out/{id}/confirm
POST /api/stock-out/{id}/cancel
GET  /api/stock-out/export

GET  /api/inventory-adjustments
POST /api/inventory-adjustments
PUT  /api/inventory-adjustments/{id}
POST /api/inventory-adjustments/{id}/confirm
POST /api/inventory-adjustments/{id}/cancel

GET  /api/stock-takes
POST /api/stock-takes
PUT  /api/stock-takes/{id}
POST /api/stock-takes/{id}/confirm
POST /api/stock-takes/{id}/cancel
POST /api/stock-takes/{id}/import
GET  /api/stock-takes/{id}/export
```

统计与日志：

```text
GET /api/dashboard/summary
GET /api/dashboard/stock-trend
GET /api/dashboard/low-stock-top
GET /api/operation-logs
GET /api/login-logs
```

受保护接口需要请求头：

```text
Authorization: Bearer <token>
```

## 验证命令

后端测试：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
npm run build
```

PowerShell：

```bash
npm.cmd run build
```

Docker Compose 配置检查：

```bash
docker compose --env-file .env.example config
```

## Docker 部署

Docker 部署不会影响本地开发方式，仍然可以使用 IDEA 启动后端、`npm run dev` 启动前端。

1. 复制环境模板：

```bash
cp .env.example .env
```

2. 修改 `.env` 中的 MySQL 密码、JWT 密钥和初始化管理员配置。

3. 启动服务：

```bash
docker compose up -d --build
```

4. 访问系统：

```text
http://localhost
```

生产前端统一请求相对路径 `/api`，由 Nginx 反向代理到 Compose 服务 `backend:8080`。MySQL 使用 volume 持久化数据，数据库结构由 Flyway 自动初始化和迁移。

更多部署、备份和回滚说明：

```text
docs/deployment.md
```

## CI

GitHub Actions 当前只做持续集成，不做自动部署：

- 后端：`mvn test`
- 前端：`npm ci` 和 `npm run build`

配置文件：

```text
.github/workflows/ci.yml
```

## 相关文档

- `docs/requirements.md`：功能需求文档
- `docs/api-design.md`：接口设计文档
- `docs/database-design.md`：数据库设计文档
- `docs/development-plan.md`：MVP 开发计划
- `docs/enterprise-upgrade-development-plan.md`：企业级升级计划
- `docs/environment-config.md`：环境变量说明
- `docs/deployment.md`：部署、备份和回滚说明
- `docs/frontend-ui-design-draft.md`：前端 UI 设计稿
- `docs/project-summary-and-next-steps.md`：项目总结、后续优化和风险建议
