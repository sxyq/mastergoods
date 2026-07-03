# 项目目录分层说明

本仓库是后端、安卓 UI、Web 前端、iOS 混合项目。为了不破坏现有 Gradle、Android Gradle Plugin、Xcode 和 Vite 构建链，目录整理采用“保留可构建路径 + 明确端侧命名边界 + 清理本地产物”的方式。

## 当前正式分层

| 分类 | 正式目录 | 说明 |
|------|----------|------|
| 文档 Docs | `docs/` | 需求、计划、验收证据、目录说明 |
| 后端 Backend | `backend/` | 后端聚合真入口，真实承载构建文件，并统一暴露源码与部署入口 |
| Web 前端 Web | `web/` | Vue/Vite PC 管理端 |
| 安卓 Android | `master-goods-android/` | Android 多模块工程 |
| iOS | `ios/` | iOS 工程与测试 |
| 工具脚本 Tools | `backend/tools/` | 后端侧工具、联调脚本、验收与迁移脚本真源 |

说明：上面这些目录已经是当前仓库的正式落位。后端构建入口文件与工具脚本真源现已迁入 `backend/`；根目录原有同名文件保留为兼容符号链接，避免现有脚本、文档和构建链失效。`backend/src` 与 `backend/deploy` 当前仍是对根目录真实源码/部署目录的兼容链接。

## 后端 Backend

- 真实目录：`backend/`
- 根目录兼容入口：`gradle/`、`build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`、`gradlew`、`gradlew.bat`、`Dockerfile`
- 主要内容：
  - `backend/src/main/java/com/zhihuiji/backend/`：Spring Boot 后端源码入口（当前通过链接映射到根目录 `src/`）
  - `backend/src/main/resources/`：应用配置、SQL/Flyway 迁移入口
  - `backend/src/test/`：后端测试入口
  - `backend/deploy/`：服务器部署模板入口（当前通过链接映射到根目录 `deploy/`）
  - `backend/tools/`：后端联调、验收、迁移与审计脚本真源
  - `backend/gradlew`：后端启动/测试入口
  - `backend/build.gradle.kts` / `backend/settings.gradle.kts`：后端 Gradle 真源

说明：这次已把后端构建入口文件和工具脚本真源迁入 `backend/`，并保留根目录兼容符号链接。这样本地浏览时后端已经真正“放在一起”；`src/` 与 `deploy/` 暂时仍保留原物理位置，但在 `backend/` 下已有统一入口。

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

## iOS

- 目录：`ios/`
- 构建入口：`ios/ZhihuijiIOS.xcodeproj`
- 主要内容：
  - `ios/ZhihuijiIOS/`：iOS 应用源码
  - `ios/ZhihuijiIOSTests/`：iOS 测试

说明：iOS 工程已经独立成组，目录名和 `xcodeproj` 需保持一致，不能为了“更整齐”直接改名或平移。

## 本地清理结果

这次已清理的是“删除后可自动重建、不会影响源码正确性”的本地产物目录：

| 目录 | 状态 | 原因 |
|------|------|------|
| `build/` | 已清理 | 后端 Gradle 构建产物 |
| `bin/` | 已清理 | 编译输出目录 |
| `web/dist/` | 已清理 | Web 打包产物 |
| `master-goods-android/app/build/` | 已清理 | 安卓 App 模块构建产物 |
| `master-goods-android/backdrop/build/` | 已清理 | 第三方 backdrop 模块构建产物 |
| `web/node_modules/` | 已清理 | Web 本地依赖缓存 |
| `.gradle/` | 已清理 | 根目录 Gradle 缓存 |
| `master-goods-android/.gradle/` | 已清理 | 安卓 Gradle 缓存 |
| `master-goods-android/.kotlin/` | 已清理 | Kotlin 编译缓存 |
| `.codegraph/` | 已清理 | 本地代码索引缓存 |
| `.trae/` | 已清理 | 工具本地缓存 |

## 无用/可删目录清单

严格来说，这里分成两类：

1. **当前仓库开发不应纳入版本管理的目录**
2. **本地缓存或导出目录，不一定“无用”，但可以按需清空重建**

| 目录 | 分类 | 当前建议 |
|------|------|----------|
| `web/node_modules/` | 本地依赖缓存 | 本轮已清；需要时重新 `npm install` |
| `.gradle/` | 本地 Gradle 缓存 | 本轮已清；下次构建会重建 |
| `.gradle-local/` | 本地 Gradle/运行缓存 | 谨慎删除，可能包含本地运行状态 |
| `master-goods-android/.gradle/` | 安卓本地缓存 | 本轮已清；下次 Android 构建会重建 |
| `master-goods-android/.kotlin/` | Kotlin 编译缓存 | 本轮已清；下次 Android 构建会重建 |
| `.codegraph/` | 本地索引缓存 | 本轮已清；后续重新索引 |
| `.trae/` | 工具本地缓存 | 本轮已清；不属于项目源码 |
| `12_workspace/` | 本地工作中间产物 | 保留，确认无用后再清 |
| `migration_output/` | 本地迁移导出产物 | 保留，确认无用后再清 |
| `migration_source_zhihuiji/` | 本地迁移源数据 | 保留，确认无用后再清 |
| `research_datasets/` | 本地研究数据目录 | 当前为空，可保留或后续删除 |

说明：`12_workspace/`、`migration_output/`、`migration_source_zhihuiji/` 这类目录虽然不属于正式源码，但它们更像“本地资料/导出物”，不是我可以在不了解你后续用途时直接判成垃圾目录的一类。

## 根目录保留文件

根目录当前建议长期保留的文件只有三类：

| 文件 | 当前状态 | 说明 |
|------|----------|------|
| `.gitignore` | 真源保留 | 仓库级忽略规则必须在根目录 |
| `AGENTS.md` | 真源保留 | 仓库级开发约束 |
| `README.md` | 真源保留 | 仓库总入口说明 |

其余后端构建入口文件与工具脚本已迁入 `backend/`，根目录仅保留兼容符号链接；`临时.md` 已迁入 `docs/archived/临时.md`，根目录保留兼容符号链接。

## 清理策略

- 已清理：可重建的构建产物目录、依赖缓存、代码索引缓存与旧 `web/dist`。
- 保留：真实源码、部署模板、已跟踪验收资料、Stitch 设计稿资源、本地迁移资料和可能仍有价值的工作中间目录。
- 不提交：`web/node_modules/`、`web/dist/`、`.ssh-check/`、`.trae/`、Gradle 缓存和本地迁移输出。
- 不乱动：`docs/`、`web/`、`ios/`、`master-goods-android/` 这些正式源码根目录；后端真实入口与脚本真源集中在 `backend/`，根目录兼容入口继续保留。
