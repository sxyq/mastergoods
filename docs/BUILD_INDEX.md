# 构建入口索引

## 后端

- 工作目录：`backend/`
- 本地启动：`./gradlew bootRun`
- 全量测试：`./gradlew test`
- 根目录兼容命令：`./gradlew test`
- 主要源码：`src/main/java/`、`src/main/resources/`
- 部署模板：`deploy/`

## Android

- 工作目录：`frontend/android/`
- 编译 App：`./gradlew :app:compileDebugKotlin`
- 编译 Agent：`./gradlew :feature:agent:compileDebugKotlin`
- 构建调试包：`./gradlew assembleDebug`
- 真机安装：`./gradlew :app:installDebug`
- Android 测试目录：`frontend/android/**/src/test/`、`frontend/android/benchmark/`

## Web

- 工作目录：`frontend/web/`
- 开发服务：`npm run dev`
- 类型检查与生产构建：`npm run build`
- Web 测试与契约资料：`frontend/web/src/**`、`frontend/web/docs/`

## iOS

- 工作目录：`frontend/ios/`
- 工程：`frontend/ios/ZhihuijiIOS.xcodeproj`
- 构建与测试：使用 Xcode 或 `xcodebuild`，必须以实际命令输出作为结果依据。

## 测试台账与证据

- 测试计划和明细：`testing/安卓/`、`testing/后端/`、`testing/Agent/`
- 运行台账：各测试类别目录下的 `live_execution_ledger.csv`
- 执行证据：`testing/.artifacts/`
- 破坏性逆向安全测试：保持在对应安全/逆向测试目录，不能计入常规通过率。

## 产物与缓存

- 本地生成资源：`tmp/output/`
- 本地工作区：`tmp/workspace/`
- Gradle、Kotlin 和 Web 依赖缓存按 `.gitignore` 忽略，不作为源码入口。
