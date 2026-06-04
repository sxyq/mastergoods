# B11 Backend BootJar Evidence

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-03 17:32 |
| 执行人/agent | Codex |
| 代码状态 | 最新补档的本地复验日志对应 [20260603-code-state.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/20260603-code-state.md)，`HEAD=11be421`，工作树为 dirty。 |
| 命令 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./master-goods-android/gradlew -p /Users/sunyiyang/Desktop/Project/master-goods bootJar --console=plain -Dorg.gradle.java.home=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home` |
| 结果 | PASS |
| 摘要 | 最新补档日志输出 `BUILD SUCCESSFUL in 1s`，完成 `resolveMainClassName` 与 `bootJar`，说明后端发布 jar 构建链可用。 |
| 附件 | 原始日志已补档：[20260603-backend-bootjar.log](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/backend/20260603-backend-bootjar.log)；代码状态见 [20260603-code-state.md](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/20260603-code-state.md)。 |
| 备注 | 这是发布产物构建验证，不替代 117 主机上的容器启动、健康检查和数据库迁移实跑。当前保留的是 2026-06-03 的补档日志口径。 |
