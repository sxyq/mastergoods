# B11 Android Assemble Release Evidence

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-03 17:30 |
| 执行人/agent | Codex |
| 代码状态 | 最新补档的本地复验日志对应 [20260603-code-state.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/20260603-code-state.md)，`HEAD=11be421`，工作树为 dirty。 |
| 命令 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android assembleRelease --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home` |
| 结果 | PASS |
| 摘要 | 最新补档日志输出 `BUILD SUCCESSFUL in 1m 34s`，完成 `minifyReleaseWithR8`、`shrinkReleaseRes`、`lintVitalRelease` 与 `assembleRelease`。 |
| 附件 | 原始日志已补档：[20260603-android-assemble-release.log](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260603-android-assemble-release.log)；代码状态见 [20260603-code-state.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/20260603-code-state.md)。 |
| 备注 | 仍有 AGP 8.5.2 对 compileSdk 35 的非阻塞提示；大量模块打印 `consumer-rules.pro` 缺失提示，但未阻塞 release 构建。当前保留的是 2026-06-03 的补档日志口径。 |
