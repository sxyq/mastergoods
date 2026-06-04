# 2026-06-04 finance/app 定向编译复验

- 时间：2026-06-04 02:03（Asia/Shanghai）
- 目的：验证 `FinanceRecordListScreen` 补齐搜索入口与账户/转账分段切换后，没有打坏 `feature:finance` 与 `app` 的 Kotlin 编译。
- 命令：

```bash
JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home \
./master-goods-android/gradlew \
  -p /Users/sunyiyang/Desktop/Project/master-goods/master-goods-android \
  :feature:finance:compileDebugKotlin \
  :app:compileDebugKotlin \
  --console=plain \
  -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home
```

- 结果：`BUILD SUCCESSFUL in 10s`
- 备注：
  - 本次通过发生在 dirty worktree 上，只能证明当前在途 UI 改动未打坏本地 Kotlin 编译。
  - 输出仍包含 AGP `8.5.2` 对 `compileSdk = 35` 的非阻塞提示，不属于本次 UI 改动引入的失败。
