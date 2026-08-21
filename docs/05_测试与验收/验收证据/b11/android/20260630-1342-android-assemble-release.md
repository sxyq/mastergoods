# B11 2026-06-30 Android assembleRelease 复验

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-30 13:42 CST |
| 执行人/agent | Codex |
| 代码状态 | `HEAD=128a3d56`；工作树为 dirty，含 iOS / backend / web / android 在途改动，详见当次 `git status --short` |
| 命令 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android :core:network:testDebugUnitTest :app:compileReleaseKotlin :app:assembleRelease --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home` |
| 结果 | PASS |
| 摘要 | 当前 Android 网络修复工作树上，`core:network` 定向单测、`compileReleaseKotlin` 与整包 `assembleRelease` 已重新跑通，最终输出 `BUILD SUCCESSFUL in 5m 17s`。这证明 release 构建链未被本轮 baseUrl / trusted-host / HTTPS 收口打坏，但仍不能替代真机安装、运行期截图、证书 pin 现场确认与发布验收。 |
| 关键输出 | `> Task :app:assembleRelease`；`BUILD SUCCESSFUL in 5m 17s`；`1126 actionable tasks: 674 executed, 30 from cache, 422 up-to-date` |
| 仍未证明 | release APK 真机安装后的冷启动、登录、同步、AI、媒体上传、运行时网络行为，以及当前设备侧 crash/logcat 现场。 |
| 附件 | 当前 turn 的 Gradle 命令输出；相关源码见 `master-goods-android/core/datastore/src/main/java/com/zhihuiji/core/datastore/SettingsStore.kt`、`master-goods-android/core/network/src/main/java/com/zhihuiji/core/network/NetworkModule.kt`、`master-goods-android/core/network/build.gradle.kts` |
