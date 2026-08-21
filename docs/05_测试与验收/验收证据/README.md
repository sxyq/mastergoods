# 验收证据目录说明

## 文档信息

| 字段 | 内容 |
|---|---|
| 文档类型 | 测试验收 |
| 当前状态 | 已完成（迁移说明） |
| 适用端 | 多端 |
| 依据源码 | 无（证据归档） |
| 依据测试 | `testing/README.md` |
| 依据证据 | 本目录（ai-agent、b11、performance） |
| 最后核对 | 2026-08-20 |

## 一、迁移说明

本目录由原 `docs/acceptance-evidence/` 移动而来（git mv），包含三个子目录：

| 子目录 | 内容 |
|---|---|
| `ai-agent/` | Agent 接口与运行证据（含 154 时期） |
| `b11/` | b11 验收证据（backend/android/web/performance/screenshots，含 154 时期） |
| `performance/` | 性能证据（20260609-090033-ai-agent-performance） |

## 二、路径对应

| 旧路径 | 新路径 |
|---|---|
| `docs/acceptance-evidence/ai-agent/...` | `docs/05_测试与验收/验收证据/ai-agent/...` |
| `docs/acceptance-evidence/b11/...` | `docs/05_测试与验收/验收证据/b11/...` |
| `docs/acceptance-evidence/performance/...` | `docs/05_测试与验收/验收证据/performance/...` |

## 三、历史快照内容说明

本目录内历史快照文档（如 b11 strict-audit-snapshot）中记录的执行命令与引用路径属于**当时快照内容**（如 `docs/acceptance-evidence/...`、`src/main/java/...`、`master-goods-android/...`、`docs/spec/41-b11-acceptance-matrix.md`），是历史执行记录的一部分，未逐条改写；引用这些快照时需按本说明的路径对应关系转换。

- 引用的 `docs/spec/*.md`（40/41 等）已随 docs/spec 清理移除，快照中链接失效属预期，内容仍保留在快照文本中。
- 这些快照属于 `historical-only`（154 时期），不参与当前通过率与发布验收。

## 四、当前证据优先

当前环境结论以 `testing/.artifacts/2026-08-18-8220-current-baseline/current-8220-baseline.md` 为准；本目录证据用于历史追溯。

## 对应实现

- 后端代码：不适用
- Android 代码：不适用
- iOS 代码：不适用
- Web 代码：不适用
- Agent 代码：不适用

## 对应接口

- 接口路径：不适用
- 请求模型：不适用
- 响应模型：不适用
- SSE 事件：不适用

## 对应测试

- 证据：本目录

## 当前限制

- 未完成内容：无
- Blocked 内容：无
- Deferred 内容：无
- historical-only 内容：154 时期快照（b11 等）
