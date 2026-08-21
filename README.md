# 智慧记（Master-Goods）项目总览

这个仓库同时包含：

- Spring Boot 后端
- Android 客户端
- 设计稿与技术分析文档
- 数据迁移与运维脚本

当前主目录结构：

```text
master-goods/
├── Code/                  所有前后端和三端代码总目录
│   ├── backend/            后端构建、源码、测试和工具
│   │   ├── src/            Java 源码、资源和后端测试
│   │   └── tools/          后端联调、迁移和 Agent 工具
│   └── frontend/           前端代码总目录
│       ├── android/        Android 多模块工程
│       ├── ios/            iOS 工程
│       ├── web/            Vue/Vite 管理端
│       └── agent-observability/ Agent 观测页面与数据
├── data/                  数据库、媒体和研究资料
│   ├── database/          迁移源库与迁移输出库
│   ├── media/             本地媒体资料
│   └── research/          研究数据资料
├── docs/                  需求、规范、设计、验收和目录说明
├── testing/               测试计划、台账、脚本和运行证据
├── 审查/                  文件盘点、目录规划和代码审计台账
├── deploy/                部署目录边界；当前没有在用的 154 资料
└── tmp/                   构建缓存、构建产物、浏览器输出和临时工作区
```

目录查看原则：所有代码先看 `Code/`；后端看 `Code/backend/`，后端 Java 源码和测试看 `Code/backend/src/`，Android、iOS、Web 和 Agent 观测前端看 `Code/frontend/`；项目说明看 `docs/`；测试过程和证据看 `testing/`；本地生成内容看 `tmp/`；目录审查记录看 `审查/`；数据库、媒体和研究资料看 `data/`。

后端构建和工具入口统一位于 `Code/backend/`；缓存和构建输出统一位于 `tmp/`。正式代码入口统一位于 `Code/`：

| 正式入口 | 用途 |
|---|---|
| `Code/backend/Dockerfile`、`Code/backend/build.gradle.kts`、`Code/backend/settings.gradle.kts`、`Code/backend/gradlew*` | 后端构建入口 |
| `Code/frontend/ios`、`Code/frontend/android`、`Code/frontend/web` | 三端正式代码入口 |
| `Code/backend/tools/` | 后端工具脚本入口 |
| `tmp/build/gradle-cache/`、`tmp/build/gradle-output/`、`tmp/build/bin/` | Gradle 缓存、构建输出和脚本产物位置 |

其中 `Code/backend/gradle/wrapper/` 是 Git 管理的 Gradle Wrapper，不能放入 `tmp/`；`.gradle`、Kotlin、Web 依赖和构建产物都集中在 `tmp/build/`。

本次目录整理只改善可见性，不改变业务代码的职责，不批量删除历史测试证据，也不把 `tmp/` 或 `testing/.artifacts/` 当作正式源码。

## 快速入口

- Android 工程说明：
  [Code/frontend/android/README.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/README.md)
- 前端目录索引：
  [Code/frontend/README.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/README.md)
- 后端聚合入口：
  [Code/backend/README.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/README.md)
- 后端工具脚本：
  [Code/backend/tools](/Users/sunyiyang/Desktop/Project/master-goods/Code/backend/tools)
- 文档总索引：
  [docs/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/README.md)
- Android 主开发计划：
  [Code/frontend/android/DEVELOPMENT-PLAN.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/DEVELOPMENT-PLAN.md)
- Android UI 规范：
  [Code/frontend/android/UI-DESIGN-SPEC.md](/Users/sunyiyang/Desktop/Project/master-goods/Code/frontend/android/UI-DESIGN-SPEC.md)
- 文档生命周期总图：
  [docs/00_文档总览/文档生命周期总图.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/00_文档总览/文档生命周期总图.md)
- 目录职责与四级业务地图：
  [docs/00_文档总览/项目目录地图.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/00_文档总览/项目目录地图.md)
- 构建与产物索引：
  [docs/04_详细设计与实现/构建命令与产物.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/04_详细设计与实现/构建命令与产物.md)
- 历史归档（临时规则）：
  [docs/90_历史归档/旧文档/临时.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/90_历史归档/旧文档/临时.md)

## 后端启动

### 部署入口

部署模板统一放在 `deploy/`。当前仓库没有在用的 154 部署文件；154.217.241.207 已完全退役，不能再作为启动或验收目标。8220 的当前部署模板需要基于维护文档和只读检查结果单独建立。

### 本地运行

要求：

- JDK 21
- PostgreSQL 15+

```bash
./Code/backend/gradlew -p Code/backend bootRun
```

测试：

```bash
./Code/backend/gradlew -p Code/backend test
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
- 设计稿和 UI 说明统一查看 `Code/frontend/android/UI-DESIGN-SPEC.md`、各端工程说明和 `Code/frontend/web/public/stitch_exports/`。
- 数据迁移与安全审计等正式文档统一收口在：
  [docs/README.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/README.md)
