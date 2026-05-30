# 环境配置说明

本文档说明后端在企业级升级阶段新增的运行环境、数据库迁移和请求追踪配置。

## Spring Profile

后端支持以下 profile：

| Profile | 用途 | 说明 |
| --- | --- | --- |
| `dev` | 本地开发 | 默认使用本地 MySQL，开启 Flyway，日志级别更详细 |
| `test` | 自动化测试 | 默认关闭 Flyway，避免单元测试依赖真实 MySQL |
| `prod` | 生产环境 | 必须通过环境变量提供数据库和 JWT 配置 |

启动指定环境示例：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

生产环境建议通过启动参数或环境变量指定：

```bash
SPRING_PROFILES_ACTIVE=prod
```

## 数据库迁移

项目已经引入 Flyway，迁移脚本目录：

```text
backend/src/main/resources/db/migration
```

当前基线脚本：

```text
V1__init_schema.sql
```

后续每一次数据库变更都应新增迁移脚本，不直接覆盖历史脚本。例如：

```text
V2__add_rbac_tables.sql
V3__add_stock_movements.sql
V4__add_stock_version.sql
```

`schema.sql` 暂时保留，作为手动初始化或课程阶段回溯参考。企业升级后的正式数据库变更以 Flyway 为准。

## 常用环境变量

| 变量名 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `warehouse_management` | 数据库名称 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥，生产环境必须覆盖 |
| `JWT_EXPIRATION_HOURS` | `24` | Token 过期小时数 |
| `FLYWAY_ENABLED` | `true` | 是否启用 Flyway |

## RequestId

后端会为每个请求写入响应头：

```text
X-Request-Id: <request-id>
```

如果调用方传入 `X-Request-Id`，后端会沿用该值；否则自动生成新的 ID。

日志中会输出 requestId，方便排查前后端请求链路。

## 注意事项

- 生产环境不要使用默认 `JWT_SECRET`。
- 不要修改已经执行过的 Flyway 迁移脚本。
- 已有数据库从 `schema.sql` 升级到 Flyway 时，`baseline-on-migrate` 会记录基线版本，避免重复建表。
- 新增业务表或字段时，只新增新的 `V*__*.sql` 文件。
