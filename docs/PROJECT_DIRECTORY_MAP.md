# 项目目录分层说明

本仓库是后端、安卓 UI、Web 前端混合项目。为了不破坏现有 Gradle、Android Gradle Plugin 和 Vite 构建链，目录整理采用“保留可构建路径 + 明确三端命名边界”的方式。

## 后端 Backend

- 目录：`src/`
- 构建入口：`build.gradle.kts`、`settings.gradle.kts`
- 主要内容：
  - `src/main/java/com/zhihuiji/backend/`：Spring Boot 后端源码
  - `src/main/resources/`：应用配置、SQL/Flyway 迁移
  - `src/test/`：后端测试
  - `deploy/`：服务器部署模板与运行配置

说明：后端保持 Java/Gradle 默认源码目录 `src/`，避免重命名后导致构建、Dockerfile、IDE 索引和 CI 配置失效。

## 安卓 UI Android UI

- 目录：`master-goods-android/`
- 构建入口：`master-goods-android/settings.gradle.kts`
- 主要内容：
  - `app/`：安卓壳与导航入口
  - `core/`：基础模型、网络、设计系统
  - `data/`：Repository 与数据层
  - `feature/`：各业务 UI 功能模块
  - `benchmark/`：性能/基准测试模块

说明：安卓端目录名称暂保留 `master-goods-android/`，它已经被 Android Gradle、模块路径和本地开发文档引用。

## 前端 Frontend

- 目录：`web/`
- 构建入口：`web/package.json`
- 主要内容：
  - `web/src/app/`：布局、路由、会话与 RBAC
  - `web/src/pages/`：PC 管理端页面
  - `web/src/shared/`：API 客户端、契约、通用工具
  - `web/public/stitch_exports/`：Stitch MCP 导出的设计稿参考资源
  - `web/docs/`：Web 后续开发文档

说明：前端保持独立 Vite 项目结构，设计稿资源放入 `web/public/` 后，页面内 `/stitch_exports/...` 路径可以直接被浏览器访问。

## 清理策略

- 已清理：本地 SSH 检查密钥目录、构建产物、临时 AI/性能证据目录、旧 `web/dist`。
- 保留：真实源码、部署模板、已跟踪验收资料、Stitch 设计稿资源、可复用开发缓存。
- 不提交：`web/node_modules/`、`web/dist/`、`.ssh-check/`、`.trae/`、Gradle 缓存和本地迁移输出。
