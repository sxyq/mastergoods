# Agent 测试资产清理阶段报告 A

日期：2026-08-27

## 结果

| 检查项 | 状态 |
|---|---|
| 目录地图坏路径修复 | `Passed` |
| 新综合方案作为当前执行入口 | `Passed` |
| 真实 Agent 测试 | `Deferred` |

## 当前入口核对

当前唯一综合执行入口为 `testing/Agent/Agent综合功能与性能测试方案.md`；执行记录使用 `testing/Agent/Agent执行台账.csv`，问题记录使用 `testing/Agent/Agent问题台账.csv`。旧的 `testing/Agent/功能测试/`、`单元测试/`、`性能测试/`、`审计/` 和 `破坏性逆向安全测试/` 入口已删除，不再作为当前入口。

## 清理范围

本阶段清理的旧 Agent 专用测试资产范围如下：

- `Code/backend/tools/` 下 6 个 Agent 证据采集、性能证据和禁止项扫描脚本；
- `testing/Agent/` 下旧 `功能测试/`、`单元测试/`、`性能测试/`、`审计/`、`破坏性逆向安全测试/` 目录，以及其中的测试计划、功能矩阵、性能矩阵、审计台账、分类说明、阶段报告和辅助脚本；
- `testing/scripts/` 下旧 Agent 工具链，包括 `analyze_agent_*`、`append_agent_*`、`reconcile_agent_ledger.py`、`run_server_agent_*` 和 `test_run_server_agent_all_tools.py`；
- `docs/05_测试与验收/` 下旧 Agent 测试说明、历史会话验证、图表结果验证和流式对话验证文档。

上述旧资产未恢复。历史报告和总台账中的旧路径引用保留用于历史追溯，不作为当前入口；`testing/.artifacts/` 未删除。业务源码、数据库、配置、`data/server-backups/` 和 `data/server-exports/` 未触碰。

## 未确认事项

- 当前部署产物与本地源码版本对应关系未确认，记为 `Blocked`。
- 测试账户、owner/store/permission 作用域及 Provider 可用性未确认，记为 `Blocked`。
- 本阶段未登录、未请求 Agent API、未请求 SSE、未创建会话、未写数据库；真实功能、性能和设备测试尚未执行，记为 `Deferred`。

Revive 时已存在的用户文档修改、未跟踪材料及已有暂存内容均保留。本报告只记录本阶段核对结果，不覆盖或回退用户改动。
