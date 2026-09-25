# Repository Guidelines

当前状态：**从零重构中**。本分支不包含生产代码，新业务架构、领域模型与数据库设计尚未开始。

## 1. 查历史实现

旧的完整实现只在 archive 分支，本分支不再保留旧源码。

```bash
git show archive/pre-domain-rewrite:<path>
git grep <symbol> archive/pre-domain-rewrite
git log archive/pre-domain-rewrite --oneline
```

- 分支 `archive/pre-domain-rewrite`，提交 `19283a4d`，标签 `pre-domain-rewrite-2026-09-25`
- 不要因为「旧代码写得不错 / 测试齐全 / 可能复用」就把旧实现搬回本分支
- 不要改写 archive 分支与该标签

## 2. 本分支当前结构

| 路径 | 用途 | 是否可依赖 |
|---|---|---|
| `reference/ui-android/` | 历史 Android UI 视觉语言参考（独立 Compose 工程） | **仅设计参考** |
| `docs/rewrite/` | 本轮重构文档（清场清单、旧数据兼容设计、后续产品需求） | 文档 |
| `docs/agent/` | Agent 设计文档与索引 | 文档 |
| `data/database/` | 本地 Legacy 数据库样本 | 仅本地 |

规则：

- **新生产代码不得依赖 `reference/` 目录**——不得 import、不得作为模块引用、不得复制后不注明来源。
- **新代码后续统一进入新的 `Code/` 结构**；在新结构建立之前不要零散放置源码。
- **本轮不要定义新的业务架构**。领域划分、表设计、API 契约由后续产品需求阶段决定。

## 3. Legacy 数据库

- `data/database/migration_source_zhihuiji/` 与 `data/database/migration_output/` 下的 `.db` 文件是真实历史数据样本，**永远不进 Git**（见 `.gitignore` 的 `/data/database/`）。
- 迁移方向固定为：`Legacy Database → Exporter → Transform / Clean → New Import Package → New System`。
- 兼容目标是 **compatible with legacy data**，不是 compatible with legacy database structure；不要把旧表结构复制进新库。
- 样本的 path / size / sha256 记录在 `docs/rewrite/LEGACY-DATA-COMPATIBILITY.md`。

## 4. 提交

- 每个 commit 只做一件事，消息用短祈使句（`feat:` / `fix:` / `refactor:` / `docs:` / `chore:`）。
- 只用明确路径暂存；不要 `git add .` 或 `git add -A`。
- 不提交：构建产物（`build/`、`dist/`、`node_modules/`）、Gradle 缓存、`local.properties`、`.DS_Store`、数据库文件、日志与运行证据。
- 不提交任何密钥、token、密码、私钥；服务器私钥不得放在仓库目录内。

## 5. 安全

- 仓库为 public，不要把任何凭据写入代码、文档、脚本或提交信息。
- 测试环境账号信息只在受控渠道传递，不落入本仓库。
