# 智慧记（Master-Goods）项目总览

这个仓库同时包含：

- Spring Boot 后端
- Android 客户端
- 设计稿与技术分析文档
- 数据迁移与运维脚本

当前主目录结构：

```text
master-goods/
  backend/                 后端聚合入口
  frontend/android/        Android 工程
  frontend/ios/            iOS 工程
  frontend/web/            Vue/Vite 管理端
  docs/                    设计稿、迁移、安全、技术分析文档
  testing/                 测试计划、台账与执行脚本
  tmp/                     本地工作区与生成资源
  tools/                   指向 backend/tools 的兼容入口
```

`master-goods-android`、`ios`、`web` 仍保留为根目录兼容符号链接，正式源码目录以 `frontend/` 下的分类目录为准。

## 快速入口

- Android 工程说明：
  [frontend/android/README.md](/Users/sunyiyang/Desktop/Project/master-goods/frontend/android/README.md)
- 前端目录索引：
  [frontend/README.md](/Users/sunyiyang/Desktop/Project/master-goods/frontend/README.md)
- 后端聚合入口：
  [backend/README.md](/Users/sunyiyang/Desktop/Project/master-goods/backend/README.md)
- 后端工具脚本：
  [backend/tools](/Users/sunyiyang/Desktop/Project/master-goods/backend/tools)
- 临时规则归档：
  [docs/archived/临时.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/archived/临时.md)
- 文档总索引：
  [docs/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/README.md)
- Android 主开发计划：
  [frontend/android/DEVELOPMENT-PLAN.md](/Users/sunyiyang/Desktop/Project/master-goods/frontend/android/DEVELOPMENT-PLAN.md)
- Android UI 规范：
  [frontend/android/UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/frontend/android/UI-DESIGN-SPEC.md)
- 构建入口索引：
  [docs/BUILD_INDEX.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/BUILD_INDEX.md)

## 后端启动

### Docker Compose

```bash
cp .env.example .env
docker compose up --build -d
```

服务地址：

- `http://localhost:18080`

健康检查：

```bash
curl http://localhost:18080/v1/sync/health
```

### 本地运行

要求：

- JDK 21
- PostgreSQL 15+

```bash
./backend/gradlew bootRun
```

兼容旧入口：

```bash
./gradlew bootRun
```

测试：

```bash
./backend/gradlew test
```

## 当前后端主要能力

- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`
- `POST /v1/auth/verify-code`
- `GET /v1/auth/users/me`
- `GET /v1/sync/health`
- `POST /v1/sync/upload`
- `POST /v1/sync/pull`
- `GET/POST/PUT/DELETE /v1/products`
- `GET/POST/PUT/DELETE /v1/customers`
- `GET/POST/PUT/DELETE /v1/suppliers`
- `POST /v1/sale-orders`
- `GET /v1/sale-orders`
- `GET /v1/sale-orders/{id}`
- `PUT /v1/sale-orders/{id}`
- `POST /v1/sale-orders/{id}/payments`
- `PUT /v1/sale-orders/{id}/cancel`
- `POST /v1/purchase-orders`
- `GET /v1/purchase-orders`
- `GET /v1/purchase-orders/{id}`
- `POST /v1/pay-orders`
- `GET /v1/pay-orders`
- `GET /v1/finance-records`
- `POST /v1/finance-records`
- `GET /v1/reports/sales/summary`
- `GET /v1/reports/sales/by-product`
- `GET /v1/reports/sales/by-customer`
- `GET /v1/reports/sales/receivable`
- `GET /v1/reports/inventory`
- `GET /v1/reports/inventory/flow`
- `GET /v1/agent/workbench`
- `POST /v1/agent/chat`

## 当前已知约束

- 当前服务端虽然已有账号与会话体系，但业务数据表仍未完整做到按 `user_id` 隔离
- 旧版智慧记导入到服务器这一条链路，仍需要先补完多账号数据隔离再适合正式启用
- Android 端 UI 仍在持续向设计稿对齐

## 说明

- 邀请码注册码固定：`021218`
- 设计稿统一存放在：
  [docs/design-mockups](/Users/sunyiyang/Desktop/Project/master-goods/docs/design-mockups)
- 数据迁移与安全审计等正式文档统一收口在：
  [docs/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/README.md)
