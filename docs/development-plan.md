# 开发计划文档

## 开发原则

本项目采用分阶段开发方式。每个阶段只完成一组清晰目标，先能运行，再逐步扩展功能。

每一步都需要说明：

1. 当前要做什么
2. 为什么要这样做
3. 创建或修改了哪些文件
4. 如何运行和测试
5. 建议的 Git commit 信息

## 阶段一：项目规划

当前阶段。

目标：

- 创建项目 README。
- 完成数据库设计文档。
- 完成开发计划文档。
- 完成 API 设计初稿。

产出文件：

- `README.md`
- `docs/database-design.md`
- `docs/development-plan.md`
- `docs/api-design.md`

运行和测试：

- 本阶段不运行后端或前端。
- 检查 Markdown 文件是否存在，内容是否覆盖核心功能和表设计。

建议 Git commit 信息：

```text
docs: add initial project planning documents
```

## 阶段二：创建后端 Spring Boot 基础项目

目标：

- 创建 `backend` Maven 项目。
- 配置 Spring Boot 3、Java 17。
- 添加基础依赖：Spring Web、MyBatis-Plus、MySQL Driver、Validation、JWT 相关依赖。
- 创建基础分层结构：controller、service、mapper、entity、dto、config、common。

为什么：

- 后端是系统的数据和业务核心。
- 先搭好基础结构，后续每个模块都能按统一方式开发。

预计产出：

- `backend/pom.xml`
- `backend/src/main/java/...`
- `backend/src/main/resources/application.yml`

运行和测试：

```bash
cd backend
mvn spring-boot:run
```

建议 Git commit 信息：

```text
chore: initialize Spring Boot backend project
```

## 阶段三：创建数据库和基础表

目标：

- 创建 MySQL 数据库。
- 编写初始化 SQL。
- 创建规划中的核心表。
- 配置后端数据库连接。

为什么：

- 后端接口需要真实数据库支撑。
- 表结构先稳定下来，实体类和 Mapper 才有明确依据。

预计产出：

- `backend/src/main/resources/db/schema.sql`
- `backend/src/main/resources/application.yml`
- 后端实体类和 Mapper 基础文件

运行和测试：

```bash
cd backend
mvn test
```

建议 Git commit 信息：

```text
feat: add initial database schema
```

## 阶段四：用户注册、登录和 JWT 认证

目标：

- 实现用户注册。
- 实现用户登录。
- 登录成功后返回 JWT。
- 添加 JWT 请求拦截和当前用户识别。

为什么：

- 用户认证是后台管理系统的入口。
- 后续的入库、出库和操作日志都需要知道是谁在操作。

预计产出：

- 用户实体、DTO、VO
- Auth Controller
- JWT 工具类
- 认证拦截器或过滤器

运行和测试：

- 使用 Postman、Apifox 或 curl 测试注册和登录。
- 验证无 token 无法访问受保护接口。

建议 Git commit 信息：

```text
feat: implement user auth with JWT
```

## 阶段五：基础资料模块

目标：

- 商品分类管理。
- 服装商品管理。
- 仓库管理。
- 供应商管理。

为什么：

- 入库和出库都依赖这些基础资料。
- 先完成基础 CRUD，业务流程会更容易实现。

预计产出：

- Category 模块
- Product 模块
- Warehouse 模块
- Supplier 模块

运行和测试：

- 测试新增、编辑、删除、分页查询。
- 验证 SKU、分类编码、仓库编码、供应商编码唯一性。

建议 Git commit 信息：

```text
feat: add master data management APIs
```

## 阶段六：库存、入库和出库模块

目标：

- 实现库存查询。
- 实现入库单和入库明细。
- 确认入库后增加库存。
- 实现出库单和出库明细。
- 确认出库前校验库存，确认后减少库存。

为什么：

- 这是仓库系统最核心的业务流程。
- 库存变动必须由单据驱动，避免直接修改库存造成数据混乱。

预计产出：

- Stock 模块
- Stock In 模块
- Stock Out 模块
- 库存变更事务处理

运行和测试：

- 测试入库后库存增加。
- 测试出库后库存减少。
- 测试库存不足时禁止出库。

建议 Git commit 信息：

```text
feat: implement stock in and stock out workflows
```

## 阶段七：Dashboard、低库存预警和操作日志

目标：

- 首页 Dashboard 数据统计。
- 低库存商品列表。
- 操作日志记录和查询。

为什么：

- Dashboard 帮助用户快速了解仓库状态。
- 低库存预警能及时提醒补货。
- 操作日志方便追踪问题和审计。

预计产出：

- Dashboard API
- Low Stock API
- Operation Log API
- 日志记录切面或拦截器

运行和测试：

- 测试统计数据是否正确。
- 测试低库存条件是否正确。
- 测试关键操作是否写入日志。

建议 Git commit 信息：

```text
feat: add dashboard alerts and operation logs
```

## 阶段八：创建前端 Vue 3 基础项目

目标：

- 创建 `frontend` Vite 项目。
- 配置 Vue 3、Element Plus、Axios、Pinia。
- 创建基础路由和布局。
- 实现登录页和主框架。

为什么：

- 前端需要先搭建统一布局、路由和请求封装。
- 后续每个业务模块都可以直接接入。

预计产出：

- `frontend/package.json`
- `frontend/src/router`
- `frontend/src/stores`
- `frontend/src/api`
- `frontend/src/layouts`
- `frontend/src/views`

运行和测试：

```bash
cd frontend
npm install
npm run dev
```

建议 Git commit 信息：

```text
chore: initialize Vue frontend project
```

## 阶段九：前端业务页面

目标：

- 分类管理页面。
- 商品管理页面。
- 仓库管理页面。
- 供应商管理页面。
- 入库管理页面。
- 出库管理页面。
- 库存查询页面。
- 低库存预警页面。
- 操作日志页面。
- Dashboard 页面。

为什么：

- 前端页面是用户实际操作系统的入口。
- 每个页面都应围绕真实仓库工作流设计。

运行和测试：

- 在浏览器中逐页测试。
- 验证表单校验、分页、搜索、错误提示和登录状态。

建议 Git commit 信息：

```text
feat: add warehouse management frontend pages
```

## 阶段十：联调、测试和完善

目标：

- 前后端联调。
- 修复接口字段不一致。
- 完善错误处理。
- 增加必要的后端单元测试和接口测试。
- 完善 README 运行说明。

为什么：

- 项目只有通过完整流程测试，才能算真正可用。
- 文档需要跟着最终实现更新。

运行和测试：

```bash
cd backend
mvn test

cd frontend
npm run build
```

建议 Git commit 信息：

```text
test: verify full warehouse management workflow
```
