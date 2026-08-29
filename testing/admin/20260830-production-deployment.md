# 管理员后台生产发布记录（2026-08-30）

## 发布范围

本次发布包含正式工程 `Code/backend/`、`Code/frontend/web/` 和 V33-V40 Flyway 迁移。`Temp/grok-style-admin-preview/` 未参与发布。

| 端 | 目标 | 发布对象 | 当前入口 |
|---|---|---|---|
| 后端 API | `8.220.206.9`，`/opt/sxyq27/master-goods` | `sxyq27-zhj-api:20260830T052508-admin-c2d9040c-dirty-fix2`；JAR SHA-256 `045f177afd0139494f7ec0756d35ec4a35931f2373aca7c6ac6fa45e93db0e2c` | `https://zhj-api.sxyq27.online/` |
| Web 管理端 | `124.222.153.108`，Nginx | `/opt/sxyq27/releases/20260830T052508-admin-c2d9040c-dirty/zhj`；`/opt/sxyq27/staged/zhj` 已切换 | `https://sxyq27.online/zhj/` |

## 发布前核对

- 后端：`./Code/backend/gradlew -p Code/backend test bootJar --no-daemon`，`BUILD SUCCESSFUL`。
- Web：`PATH=/Users/sunyiyang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH VITE_PUBLIC_BASE=/zhj/ VITE_API_BASE_URL=https://zhj-api.sxyq27.online npm run build`，构建成功。
- 上传后 JAR 哈希与本地产物一致；Web 的 `index.html`、JS、CSS 哈希逐项一致。
- 8220 已保存 PostgreSQL custom dump、旧 JAR、旧 Compose 和 Dockerfile；发布专用目录为 `/opt/sxyq27/master-goods/releases/20260830T052508-admin-c2d9040c-dirty/`。
- 8220 发布目录不含完整 Git 源码，本次镜像由部署产物写入运行时镜像后生成；后续若在服务器执行 `docker compose build`，需提供完整源码或单独的运行时 Dockerfile。

## 发布过程与修正

第一次启动因 `V33__agent_memories.sql` 使用 PostgreSQL 不支持的 `AUTO_INCREMENT` 失败，容器已立即恢复到旧镜像，数据库停留在 V32。第一次修正主键后，第二次启动又暴露 `DOUBLE` 类型不受 PostgreSQL 识别，容器再次回滚；随后将该字段改为 `DOUBLE PRECISION`，重新构建并发布。

最终启动日志显示 V33、V34、V35、V36、V37、V38、V39、V40 全部成功应用，Tomcat 已在 18080 端口启动。当前数据库 Flyway 版本为 V40。

## 上线核验

| 检查 | 结果 |
|---|---|
| 8220 后端容器 | `running`，重启次数 0 |
| 124 Nginx 配置 | `nginx -t` 成功，reload 成功 |
| Web 首页与管理员 SPA 路由 | `/zhj/`、`/zhj/admin/overview`、`/zhj/admin/users` 均返回 200 |
| Web 静态资源 | JS、CSS、`stitch_exports` 资源返回 200 |
| API 未鉴权行为 | 根路径和管理员接口返回 403 |
| CORS 预检 | `Origin: https://sxyq27.online` 返回 200，并允许 Authorization/Content-Type |
| 管理员账号 | `admin_accounts=0`；未创建账号或授权范围 |

## 回滚边界与限制

- 后端回滚镜像：`sxyq27-zhj-api:rollback-20260830T052508-admin-c2d9040c-dirty`；旧 Compose 和数据库备份位于同一发布目录。
- 前端回滚目标：`/opt/sxyq27/releases/20260816T181800Z-direct-api/zhj`，记录在 `previous-frontend-target.txt`。
- 数据库只做前进迁移，不执行结构回退；回滚应用时仍需确认 V40 数据结构与旧应用兼容性。
- 当前有 2 个普通用户、0 个管理员账号、无业务夹具。需要由业务负责人指定现有用户及其角色/范围后，才能进行真实管理员登录和功能验收。
- 本记录证明部署和基础可达性，不代表角色隔离、跨范围查询、SSE 重连、性能、恢复和高风险操作验收已经完成。

## 证据位置

- 8220 远端：`/opt/sxyq27/master-goods/releases/20260830T052508-admin-c2d9040c-dirty/release-meta.txt`
- 124 远端：`/opt/sxyq27/releases/20260830T052508-admin-c2d9040c-dirty/frontend-meta.txt`
- 本地构建产物：`tmp/build/gradle-output/backend/libs/zhihuiji-backend-0.1.0.jar`、`tmp/build/web/dist/`
