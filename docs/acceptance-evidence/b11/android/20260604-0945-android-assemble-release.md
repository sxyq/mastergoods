# B11 Android Assemble Release Evidence

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-04 09:45 |
| 执行人/agent | Codex |
| 代码状态 | 当前工作树为 dirty；本次验证基于在途 UI 收口后的本地状态执行，不代表干净发布候选。 |
| 命令 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./tools/b11_acceptance_check.sh android-assemble-release` |
| 结果 | PASS |
| 摘要 | 输出 `BUILD SUCCESSFUL in 1m 32s`，`1207 actionable tasks: 192 executed, 1015 up-to-date`。本次验证用于确认最新 UI 收口后的 Android release 构建链仍然可用。 |
| 附件 | 原始日志：[20260604-0945-android-assemble-release.log](/Users/sunyiyang/Desktop/Project/master-goods/docs/acceptance-evidence/b11/android/20260604-0945-android-assemble-release.log) |
| 备注 | 该条证据补强的是最新 UI 收口后的 release 构建健康度，不替代 release 包安装、运行期截图、117 现场 smoke、性能记录或发布级安全现场验收。仍存在 AGP 8.5.2 对 compileSdk 35 的非阻塞提示。 |
