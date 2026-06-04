# B11 Android Assemble Evidence

| 字段 | 内容 |
|---|---|
| 时间 | 2026-06-04 09:30 |
| 执行人/agent | Codex |
| 代码状态 | 当前工作树为 dirty；本次验证基于在途 UI 收口后的本地状态执行，不代表干净发布候选。 |
| 命令 | `JAVA_HOME=/Users/sunyiyang/.local/jdks/temurin-21/Contents/Home ./tools/b11_acceptance_check.sh android-assemble` |
| 结果 | PASS |
| 摘要 | 输出 `BUILD SUCCESSFUL in 10s`，`928 actionable tasks: 69 executed, 859 up-to-date`。本次验证用于确认最新 UI 收口后的整包 Android debug 构建仍然可用。 |
| 附件 | 本轮命令输出来自会话内提权执行，尚未单独补档 `.log` 文件。 |
| 备注 | 该条证据比 2026-06-04 09:15 的定向 Kotlin 编译更接近交付构建口径，但仍只属于 dirty worktree 上的本地构建验证；不替代真机安装、截图、117 环境 smoke、性能记录或发布级安全验收。仍存在 AGP 8.5.2 对 compileSdk 35 的非阻塞提示。 |
