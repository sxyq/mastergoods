# 阶段报告：20260824 Agent Baseline C

## 当前需求与状态

本阶段只修复 `testing/Agent/功能测试/functional_feature_matrix.csv` 的测试台账基线，不运行后端、Web、Android、iOS 或数据库业务测试。输入严格来自 `git show HEAD:testing/Agent/功能测试/functional_feature_matrix.csv`，没有读取修复前的工作树文件作为输入。

## 本轮实际完成

- 保留 HEAD 的全部 2072 条历史数据物理行和原始字段文本，不重排、不丢弃、不改历史 `scenario_id`，包括重复的 `207`。
- 表头末尾追加 `current_rerun_20260824`。
- 每条历史数据物理行末尾追加 CSV 引号包裹的 Deferred JSON 值。
- 追加 34 条规范 CSV 新场景：3 个 Agent 工具、10 个 LOOP、3 个 DRAFT、14 个 CTX、2 个 REC、2 个 SEARCH。
- 新场景全部使用 `Deferred`，重测尚未开始。

## 历史格式说明

HEAD 原始文本保留了历史格式问题：换行包含 2035 条 CRLF 和 38 条 LF；按单行 CSV 观察，历史列宽为 `{22=>2064, 21=>5, 25=>1, 23=>2}`，异常物理行位于 4, 5, 6, 7, 8, 1807, 1808, 1809。这些行中的未转义逗号和列数异常本轮按要求原样保留。历史 `scenario_id=207` 出现 2 次，也按要求保留。

## 修改或操作对象

- `testing/Agent/功能测试/functional_feature_matrix.csv`：从 HEAD 原始文本重建并追加本轮字段及 34 条新行。
- `testing/Agent/功能测试/阶段报告-20260824-Agent-Baseline-A.md`：本报告。
- `testing/.artifacts/2026-08-24-agent-baseline-C/`：HEAD 摘要、修复清单和 Deferred JSON 证据。
- 未修改 `Code/backend`、数据库、配置、用户文档、`data/server-backups`、`data/server-exports`、`testing/Agent/功能测试/live_execution_ledger.csv` 或 B 报告。

## 验证结果

- 历史物理行前缀逐行一致：`PASS`。
- 历史数据行数未丢失：`2072` 条，追加前后保持：`PASS`。
- 历史行末尾字段均为 `current_rerun_20260824` 对应 Deferred JSON：`PASS`。
- 新 ID 数量：34；新 ID 唯一：`PASS`；与历史 ID 无冲突：`PASS`。
- 业务测试：未运行，符合第一阶段范围。
