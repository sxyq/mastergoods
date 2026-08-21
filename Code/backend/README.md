# Backend Aggregation

`Code/backend/` 是仓库中后端入口文件的真实位置。

这里集中放置了后端开发最常用的入口：

- `Code/backend/src/`
- 根目录 `deploy/`（部署模板边界，不属于后端源码）
- `Code/backend/tools/`
- `Code/backend/gradle/`
- `Code/backend/build.gradle.kts`
- `Code/backend/settings.gradle.kts`
- `Code/backend/gradle.properties`
- `Code/backend/gradlew`
- `Code/backend/gradlew.bat`
- `Code/backend/Dockerfile`

说明：

- 后端构建入口文件和源码已经实际集中到 `Code/backend/`。
- 后端工具脚本位于 `Code/backend/tools/`。
- 部署模板保留在根目录 `deploy/`，不在后端源码目录内复制第二份。
- 根目录不再保留后端 Gradle、Docker、`bin` 和 `build` 的兼容符号链接。
- 后端构建、测试、Docker 和工具脚本都从 `Code/backend/` 入口使用。
