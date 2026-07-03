# Backend Aggregation

`backend/` 现在是仓库中后端入口文件的真实位置。

这里集中放置了后端开发最常用的入口：

- `src/`
- `deploy/`
- `tools/`
- `gradle/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `Dockerfile`

说明：

- 后端构建入口文件已经实际迁入 `backend/`。
- `tools/` 目录现在也已经实际迁入 `backend/tools/`。
- `src/` 和 `deploy/` 当前仍通过兼容链接挂入 `backend/`，还没有继续做更重的物理目录迁移。
- 根目录保留的是兼容符号链接，用来继续兼容旧脚本、旧文档和现有命令。
- 因此，后端后续应优先从 `backend/` 进入查看和维护。
