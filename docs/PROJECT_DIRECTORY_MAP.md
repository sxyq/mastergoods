# 项目目录分层说明

本仓库是后端、安卓 UI、Web 前端、iOS 混合项目。为了不破坏现有 Gradle、Android Gradle Plugin、Xcode 和 Vite 构建链，目录整理采用“保留可构建路径 + 明确端侧命名边界 + 清理本地产物”的方式。

## 当前正式分层

| 分类 | 正式目录 | 说明 |
|------|----------|------|
| 文档 Docs | `docs/` | 需求、计划、验收证据、目录说明 |
| 后端 Backend | `backend/` | 后端聚合真入口，真实承载构建文件，并统一暴露源码与部署入口 |
| Web 前端 Web | `frontend/web/` | Vue/Vite PC 管理端 |
| 安卓 Android | `frontend/android/` | Android 多模块工程 |
| iOS | `frontend/ios/` | iOS 工程与测试 |
| 测试 Testing | `testing/` | 测试计划、执行台账、证据与脚本 |
| 临时 Tmp | `tmp/` | 本地工作区与生成资源，不作为源码入口 |
| 本地数据库 Database | `data/database/` | 迁移源库、迁移输出库和本地数据库资料 |
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

- 目录：`frontend/android/`
- 构建入口：`frontend/android/settings.gradle.kts`
- 主要内容：
  - `app/`：安卓壳与导航入口
  - `core/`：基础模型、网络、设计系统
  - `data/`：Repository 与数据层
  - `feature/`：各业务 UI 功能模块
  - `benchmark/`：性能/基准测试模块

说明：根目录 `master-goods-android` 是指向 `frontend/android` 的兼容符号链接；正式源码目录为 `frontend/android/`。

## Web 前端 Web

- 目录：`frontend/web/`
- 构建入口：`frontend/web/package.json`
- 主要内容：
  - `frontend/web/src/app/`：布局、路由、会话与 RBAC
  - `frontend/web/src/pages/`：PC 管理端页面
  - `frontend/web/src/shared/`：API 客户端、契约、通用工具
  - `frontend/web/public/stitch_exports/`：Stitch MCP 导出的设计稿参考资源
  - `frontend/web/docs/`：Web 后续开发文档

说明：根目录 `web` 是指向 `frontend/web` 的兼容符号链接；页面内 `/stitch_exports/...` 路径仍由 Vite public 目录提供。

## iOS

- 目录：`frontend/ios/`
- 构建入口：`frontend/ios/ZhihuijiIOS.xcodeproj`
- 主要内容：
  - `frontend/ios/ZhihuijiIOS/`：iOS 应用源码
  - `frontend/ios/ZhihuijiIOSTests/`：iOS 测试

说明：根目录 `ios` 是指向 `frontend/ios` 的兼容符号链接；iOS 工程文件与源码整体保持同组。

## 测试与脚本

- 测试计划、明细 CSV 与真实执行证据：`testing/`
- 后端联调、迁移、审计与部署脚本：`backend/tools/`
- 构建命令和产物入口：`docs/BUILD_INDEX.md`

## 本地生成物集中结果

本地生成物没有放回源码目录，而是统一收口到 `tmp/build/`。为兼容已有命令，原标准路径保留为符号链接：

| 目录 | 状态 | 原因 |
|------|------|------|
| `tmp/build/gradle-output/` | 已集中 | 后端、根入口和 Android 模块构建产物 |
| `tmp/build/gradle-cache/` | 已集中 | Gradle 项目缓存和本地 Gradle 缓存 |
| `tmp/build/kotlin-cache/` | 已集中 | Kotlin 编译会话缓存 |
| `tmp/build/web/node_modules/` | 已集中 | Web 本地依赖 |
| `tmp/build/web/dist/` | 已集中 | Web Vite 输出 |
| `tmp/build/bin/` | 已集中 | 编译/脚本输出 |
| `tmp/build/xcode/` | 已集中 | Xcode 用户数据 |
| `tmp/build/mcp/` | 已集中 | MCP 工具调用日志 |
| `.codegraph/` | 已清理 | 本地代码索引缓存 |
| `.trae/` | 已清理 | 工具本地缓存 |

## 无用/可删目录清单

严格来说，这里分成两类：

1. **当前仓库开发不应纳入版本管理的目录**
2. **本地缓存或导出目录，不一定“无用”，但可以按需清空重建**

| 目录 | 分类 | 当前建议 |
|------|------|----------|
| `tmp/build/web/node_modules/` | 本地依赖缓存 | 按需清理；需要时重新 `npm ci` |
| `tmp/build/gradle-cache/` | Gradle 项目和本地缓存 | 停止 Gradle 后按需清理 |
| `tmp/build/kotlin-cache/` | Kotlin 编译缓存 | 按需清理；下次构建会重建 |
| `tmp/build/gradle-output/` | 各端编译产物 | 按需清理；下次构建会重建 |
| `.codegraph/` | 本地索引缓存 | 本轮已清；后续重新索引 |
| `.trae/` | 工具本地缓存 | 本轮已清；不属于项目源码 |
| `12_workspace/` | 本地工作中间产物 | 保留，确认无用后再清 |
| `data/database/migration_output/` | 本地迁移导出产物 | 保留，确认无用后再清 |
| `data/database/migration_source_zhihuiji/` | 本地迁移源数据 | 保留，确认无用后再清 |
| `research_datasets/` | 本地研究数据目录 | 当前为空，可保留或后续删除 |

说明：`data/database/` 只保存本地数据库资料；它们虽然不属于正式源码，但仍可能用于迁移复核和回滚，不能直接判成垃圾目录。

## 根目录保留文件

根目录当前建议长期保留的文件只有三类：

| 文件 | 当前状态 | 说明 |
|------|----------|------|
| `.gitignore` | 真源保留 | 仓库级忽略规则必须在根目录 |
| `AGENTS.md` | 真源保留 | 仓库级开发约束 |
| `README.md` | 真源保留 | 仓库总入口说明 |

其余后端构建入口文件与工具脚本已迁入 `backend/`，根目录仅保留兼容符号链接；根目录 `临时.md` 与 `docs/archived/临时.md` 内容重复，尚未删除任何一份。

## 清理策略

- 已集中：可重建的构建产物目录、依赖缓存、Xcode 用户数据和 MCP 日志统一位于 `tmp/build/`。
- 保留：真实源码、部署模板、已跟踪验收资料、Stitch 设计稿资源、本地迁移资料和可能仍有价值的工作中间目录。
- 不提交：`tmp/build/`、`tmp/output/`、`.ssh-check/`、`.trae/` 和 `data/database/` 下的本地数据库。
- 正式源码目录为 `frontend/android/`、`frontend/ios/`、`frontend/web/`；根目录兼容链接继续保留，后端真实入口与脚本真源集中在 `backend/`。
