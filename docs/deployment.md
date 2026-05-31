# 部署上线说明

本文档说明服装仓库管理系统的 Docker 部署、生产配置、备份、日志查看和回滚方式。

## 部署原则

- Docker 部署是新增能力，不影响本地开发方式。
- 本地开发仍然可以使用 IDEA 启动后端，使用 `npm run dev` 启动前端。
- 不提交真实 `.env` 文件，只提交 `.env.example`。
- 不在仓库中保存真实 MySQL 密码或 `JWT_SECRET`。
- 数据库初始化和版本迁移统一由 Flyway 执行，不手动执行 `schema.sql`。
- GitHub Actions 只做 CI，不做自动部署。

## 服务关系

`docker-compose.yml` 中包含三个服务：

| 服务名 | 作用 | 说明 |
| --- | --- | --- |
| `mysql` | 数据库 | 使用 `mysql_data` volume 持久化数据 |
| `backend` | Spring Boot API | 通过 `DB_HOST=mysql` 连接数据库 |
| `frontend` | Vue + Nginx | 对外暴露前端页面，并将 `/api` 反代到 `backend:8080` |

Nginx 配置位于：

```text
frontend/nginx.conf
```

其中：

- `/api/` 反向代理到 `http://backend:8080/api/`
- `/dashboard`、`/users` 等 Vue history 路由刷新时会回退到 `index.html`

## 环境变量

先复制模板：

```bash
cp .env.example .env
```

然后修改 `.env` 中的占位值，尤其是：

```text
DB_PASSWORD
MYSQL_ROOT_PASSWORD
JWT_SECRET
INIT_ADMIN_PASSWORD
```

`.env` 已加入 `.gitignore`，不要提交到 Git。

## 初始化管理员

首次部署可以通过环境变量自动创建管理员账号：

```text
INIT_ADMIN_ENABLED=true
INIT_ADMIN_USERNAME=admin
INIT_ADMIN_PASSWORD=<replace-with-strong-password>
```

后端启动时会先检测 `users` 表：

- 如果 `users` 表为空，并且 `INIT_ADMIN_ENABLED=true`，自动创建管理员。
- 密码使用 BCrypt 加密保存。
- 用户 `role` 写入 `ADMIN`，并同步绑定 `user_roles` 中的 `ADMIN` 角色。
- 如果 `users` 表已有数据，不会重复创建管理员。

部署完成后建议把 `.env` 中的 `INIT_ADMIN_ENABLED` 改为 `false`，再重启后端，减少误操作风险。

## 启动

```bash
docker compose up -d --build
```

首次启动时，后端会在连接 MySQL 后自动运行 Flyway 迁移脚本：

```text
backend/src/main/resources/db/migration
```

## 健康检查

默认前端端口为 `.env` 中的 `FRONTEND_PORT`，示例：

```bash
curl http://localhost/api/health
```

如果修改了端口，例如 `FRONTEND_PORT=8088`：

```bash
curl http://localhost:8088/api/health
```

## 日志查看

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

查看所有服务：

```bash
docker compose logs -f
```

## 数据备份

项目提供 MySQL 备份脚本：

```bash
sh scripts/backup-mysql.sh
```

默认备份到：

```text
backups/mysql
```

该目录已加入 `.gitignore`。

也可以直接执行：

```bash
docker exec warehouse-mysql sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > backup.sql
```

## 停止和回滚

停止服务但保留数据：

```bash
docker compose down
```

重新拉起上一版本镜像或回退代码后执行：

```bash
docker compose up -d --build
```

注意：Flyway 迁移脚本一旦在生产数据库执行，不要修改历史迁移文件。如果需要回滚数据结构，应新增反向修复迁移或从备份恢复。

## CI

GitHub Actions 工作流位于：

```text
.github/workflows/ci.yml
```

当前只执行：

- 后端 `mvn -B test`
- 前端 `npm ci` 和 `npm run build`

不会自动部署。
