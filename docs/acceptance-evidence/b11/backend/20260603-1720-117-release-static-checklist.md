# B11 117 / Backend Release Static Checklist

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-03 17:20 |
| 执行人/agent | Codex |
| 代码状态 | 执行当时未单独归档 `git rev-parse --short HEAD` 与 `git status --short`；当前仅保留本 Markdown 摘要，无法事后精确回填。 |
| 结果 | PASS（静态配置检查） |
| 范围 | 117 部署配置、后端生产配置、迁移与健康检查静态入口 |
| 附件 | 无独立截图或运行日志；当前证据由本 Markdown 摘要与下方代码引用组成。 |

## 已核对项

| 项目 | 结论 | 证据 |
|---|---|---|
| 117 compose 存在 | 已做 | [docker-compose.yml](/Users/sunyiyang/Desktop/Project/master-goods/deploy/117/docker-compose.yml) |
| 117 runtime Dockerfile 存在 | 已做 | [Dockerfile.runtime](/Users/sunyiyang/Desktop/Project/master-goods/deploy/117/Dockerfile.runtime) |
| 后端生产 profile 存在 | 已做 | [application-prod.yml](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/application-prod.yml) |
| Flyway 迁移在生产配置启用 | 已做 | [application-prod.yml](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/application-prod.yml) 中 `spring.flyway.enabled: true` |
| PostgreSQL / Redis 运行依赖已声明 | 已做 | [docker-compose.yml](/Users/sunyiyang/Desktop/Project/master-goods/deploy/117/docker-compose.yml) 中 `postgres`、`redis`、`backend` |
| 健康检查入口存在 | 已做 | [README.md](/Users/sunyiyang/Desktop/Project/master-goods/README.md) 中 `curl http://localhost:18080/v1/sync/health` |
| 生产日志级别已收紧 | 已做 | [application-prod.yml](/Users/sunyiyang/Desktop/Project/master-goods/src/main/resources/application-prod.yml) 中 `org.springframework.web: WARN`、`org.hibernate.SQL: WARN` |

## 仍未由本机动态证明的事项

- 未连接 117 主机执行 `docker compose up` / `docker ps` / `curl`
- 未采集 117 环境的真实健康检查输出
- 未验证真实环境变量、数据库迁移回滚、日志落盘和容器重启行为

## 本机已补的发布构建证据

- [20260603-1732-backend-bootjar.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260603-1732-backend-bootjar.md)

## 备注

- 本证据只证明“静态发布入口存在且结构合理”，不证明 117 环境已经实际部署成功。
- B11 的 117 smoke 仍需在目标主机或可访问该主机的环境中执行。
