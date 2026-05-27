# Clothing Warehouse Management System / 服装仓库管理系统

## 项目介绍

本项目是一个用于服装仓库日常管理的全栈系统，目标是帮助仓库人员完成服装商品资料维护、入库、出库、库存查询、低库存预警和 Dashboard 数据统计。

第一阶段只完成项目规划文档，不生成 Spring Boot 或 Vue 业务代码。

## 技术栈

### Backend

- Spring Boot 3
- Java 17
- Maven
- MySQL
- MyBatis-Plus
- JWT

### Frontend

- Vue 3
- Vite
- Element Plus
- Axios
- Pinia

### Database

- MySQL

## 计划中的仓库结构

```text
.
+-- backend
+-- frontend
+-- docs
|   +-- api-design.md
|   +-- database-design.md
|   +-- development-plan.md
+-- README.md
```

## 核心功能

- 用户注册和登录
- JWT 登录认证
- 商品分类管理，例如 T-shirt、Hoodie、Jeans、Jacket
- 服装商品管理：SKU、名称、分类、尺码、颜色、品牌、季节、成本价、售价、低库存阈值
- 仓库管理
- 供应商管理
- 入库管理
- 出库管理
- 库存查询
- 低库存预警
- 首页 Dashboard 数据统计
- 操作日志

## 运行说明占位

后续阶段会分别补充后端、前端和数据库的运行方式。

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Database

```bash
# 后续会补充 MySQL 建库和初始化脚本
```

## 当前阶段

阶段一：项目规划。

本阶段产出：

- 项目 README
- 数据库设计文档
- 开发计划文档
- API 接口设计初稿

建议 Git commit 信息：

```text
docs: add initial project planning documents
```
