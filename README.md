# Clothing Warehouse Management System / 服装仓库管理系统

服装仓库管理系统是一个用于仓库日常业务管理的全栈项目，覆盖用户认证、基础资料、入库、出库、库存查询、低库存预警、Dashboard 数据统计和操作日志。

## 技术栈

### Backend

- Java 17
- Spring Boot 3
- Maven
- MyBatis-Plus
- MySQL
- JWT

### Frontend

- Vue 3
- Vite
- Element Plus
- Axios
- Pinia
- Vue Router

## 项目结构

```text
.
+-- backend
|   +-- src/main/java/com/warehouse/management
|   +-- src/main/resources/application.yml
|   +-- src/main/resources/db/schema.sql
|   +-- src/main/resources/db/migration
|   +-- src/test/java/com/warehouse/management
|   +-- pom.xml
+-- frontend
|   +-- src/api
|   +-- src/layouts
|   +-- src/router
|   +-- src/stores
|   +-- src/views
|   +-- package.json
|   +-- vite.config.js
+-- docs
|   +-- api-design.md
|   +-- database-design.md
|   +-- development-plan.md
|   +-- enterprise-upgrade-development-plan.md
|   +-- environment-config.md
|   +-- requirements.md
+-- README.md
```

## 核心功能

- 用户注册、登录、JWT 认证、当前用户查询
- 用户管理和 ADMIN 权限控制
- 商品分类管理
- 服装商品管理
- 仓库管理
- 供应商管理
- 入库单管理和确认入库
- 出库单管理和确认出库
- 库存查询
- 低库存预警
- Dashboard 数据统计
- 操作日志查询

## 数据库初始化

项目已经引入 Flyway 作为数据库迁移工具。正常启动后端时，Flyway 会自动执行：

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
```

如果是已有数据库，`baseline-on-migrate` 会记录当前基线，避免重复建表。

`schema.sql` 暂时保留为设计参考。日常开发、Docker 部署和生产初始化都应交给 Flyway 自动执行，不需要手动执行 `schema.sql`。

默认数据库名：

```text
warehouse_management
```

后端默认连接配置来自 `backend/src/main/resources/application.yml`，也可以通过环境变量覆盖：

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=warehouse_management
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=<replace-with-local-dev-secret>
JWT_EXPIRATION_HOURS=24
FLYWAY_ENABLED=true
```

更多环境说明见：

```text
docs/environment-config.md
```

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：

```text
http://localhost:8080
```

健康检查：

```text
GET http://localhost:8080/api/health
```

## 启动前端

首次运行先安装依赖：

```bash
cd frontend
npm install
```

如果 Windows PowerShell 拦截 `npm.ps1`，可以使用：

```bash
npm.cmd install
```

启动开发服务器：

```bash
npm run dev
```

或在 PowerShell 中使用：

```bash
npm.cmd run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到：

```text
http://localhost:8080
```

## 登录测试流程

1. 启动 MySQL，并确保数据库可连接。
2. 启动后端服务。
3. 启动前端服务。
4. 等待后端启动完成，Flyway 会自动执行数据库初始化和迁移。
5. 调用注册接口创建用户。

```bash
curl.exe -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\",\"role\":\"ADMIN\"}"
```

6. 打开 `http://localhost:5173/login`，使用刚注册的账号登录。

## 常用接口

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
GET  /api/auth/permissions

GET  /api/users
POST /api/users
PUT  /api/users/{id}
PUT  /api/users/{id}/password
PUT  /api/users/{id}/status
DELETE /api/users/{id}
GET  /api/users/{id}/roles
PUT  /api/users/{id}/roles
GET  /api/users/{id}/warehouses
PUT  /api/users/{id}/warehouses

GET  /api/roles
GET  /api/roles/{id}
POST /api/roles
PUT  /api/roles/{id}
PUT  /api/roles/{id}/status
DELETE /api/roles/{id}
GET  /api/roles/{id}/permissions
PUT  /api/roles/{id}/permissions

GET  /api/permissions
GET  /api/permissions/tree
POST /api/permissions
PUT  /api/permissions/{id}
PUT  /api/permissions/{id}/status

GET  /api/categories
GET  /api/products
GET  /api/products/export
GET  /api/products/import-template
POST /api/products/import
GET  /api/warehouses
GET  /api/warehouses/export
GET  /api/warehouses/import-template
POST /api/warehouses/import
GET  /api/suppliers
GET  /api/suppliers/export
GET  /api/suppliers/import-template
POST /api/suppliers/import

GET  /api/stock
GET  /api/stock/low
GET  /api/stock/export
GET  /api/stock-movements
GET  /api/stock-movements/export
GET  /api/stock-movements/{id}
GET  /api/stock-movements/product/{productId}
GET  /api/stock-movements/warehouse/{warehouseId}

GET  /api/stock-in
GET  /api/stock-in/export
POST /api/stock-in
POST /api/stock-in/{id}/confirm
POST /api/stock-in/{id}/cancel

GET  /api/stock-out
GET  /api/stock-out/export
POST /api/stock-out
POST /api/stock-out/{id}/confirm
POST /api/stock-out/{id}/cancel

GET  /api/dashboard/summary
GET  /api/dashboard/stock-trend
GET  /api/dashboard/low-stock-top

GET  /api/operation-logs
```

受保护接口需要请求头：

```text
Authorization: Bearer <token>
```

后端会在每次响应中返回请求追踪头，便于排查日志：

```text
X-Request-Id: <request-id>
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

PowerShell 中也可以使用：

```bash
npm.cmd run build
```

Docker Compose 配置检查：

```bash
docker compose --env-file .env.example config
```

## Docker 部署

Docker 部署是新增能力，不影响本地 IDEA 启动后端和 `npm run dev` 启动前端的方式。

1. 复制环境模板并替换占位值：

```bash
cp .env.example .env
```

2. 启动完整环境：

```bash
docker compose up -d --build
```

3. 访问前端：

```text
http://localhost
```

生产前端统一请求相对路径 `/api`，由 Nginx 反向代理到 Compose 服务 `backend:8080`。MySQL 服务名为 `mysql`，后端 Docker 环境的 `DB_HOST` 也固定为 `mysql`。数据库初始化和后续变更继续由 Flyway 自动完成。

更多部署、备份和回滚说明见：

```text
docs/deployment.md
```

## 当前完成阶段

阶段十已暂时跳过，当前完成企业升级阶段十一：部署上线准备。

已完成 Docker 部署配置、Nginx 反向代理、生产环境变量模板、GitHub Actions CI、MySQL 备份脚本和部署文档。

建议 Git commit 信息：

```text
chore: add deployment configuration
```
