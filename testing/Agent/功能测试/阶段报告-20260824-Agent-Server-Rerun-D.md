# 阶段报告：20260824 Agent Server Rerun D

## 当前需求与状态

本轮目标是对 master-goods 当前服务器 `8.220.206.9` 做真实 Agent 实测。已完成只读 Revive、正式 Nginx 入口确认、非流式和 SSE 入口门禁探针，并为 94 条逻辑基线建立独立证据目录和台账。服务入口可达，但没有可使用的 Bearer session token；Agent 路由在认证前返回 HTTP 403，因此 0 条用例进入 Agent、Provider、数据库或业务工具执行。

结果分布：

| 范围 | 总数 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|
| AG-FT-BE-ALL-001..063 | 63 | 0 | 0 | 62 | 1 |
| LOOP-001..010 | 10 | 0 | 0 | 4 | 6 |
| DRAFT-001..003 | 3 | 0 | 0 | 3 | 0 |
| CTX-001..014 | 14 | 0 | 0 | 12 | 2 |
| REC-001..002 | 2 | 0 | 0 | 0 | 2 |
| SEARCH-001..002 | 2 | 0 | 0 | 2 | 0 |
| **合计** | **94** | **0** | **0** | **83** | **11** |

Deferred 项：`AG-FT-BE-ALL-057, LOOP-003, LOOP-004, LOOP-006, LOOP-008, LOOP-009, LOOP-010, CTX-005, CTX-010, REC-001, REC-002`。Blocked 表示认证门禁阻断；Deferred 表示还需要受控 Provider、故障、事件重放或检查点前置条件，当前也没有进入 Agent。两种状态都没有伪造 Passed。

## 本轮实际完成

- SSH root 登录到 `8.220.206.9` 成功，未启动、重启、部署、迁移或改写服务器对象。
- 确认 `nginx`、`docker` active；`sxyq27-zhj-api:20260818` 暴露为 `127.0.0.1:18080`，Nginx 将 `zhj-api.sxyq27.online` 代理到该端口。
- 当前 Provider 非敏感标识为 `oneapi.sxyq27.online`、模型 `gpt-5.6-luna`、`chat_completions`；Provider key 只记录 `[REDACTED]`。
- 非流式入口探针：`POST /v2/agent/chat` -> 403；流式入口探针：`POST /v2/agent/chat/stream` -> 403，均无 SSE 事件。
- 逐条生成 `AG-FT-BE-ALL-001..063`、`LOOP-001..010`、`DRAFT-001..003`、`CTX-001..014`、`REC-001..002`、`SEARCH-001..002` 共 94 条脱敏记录。

## 修改或操作对象

- 本地源码 commit：`93efd9da077d7ca59a30f47313f21822f9eac687`。服务器部署 commit 无法确认；部署目录没有 Git 元数据，单独记录镜像 tag、镜像 ID、JAR 名称和 JAR SHA-256 前缀。
- 新增本轮 D 台账：`testing/Agent/功能测试/agent-server-rerun-20260824-D.csv`。
- 新增本轮 D 报告：`testing/Agent/功能测试/阶段报告-20260824-Agent-Server-Rerun-D.md`。
- 原始脱敏证据：`testing/.artifacts/2026-08-24-agent-server-rerun-D/`；每个 case 含 input、model-visible 摘要、visible output、request meta、tool trace、SSE、audit、DB/draft before-after 和 cleanup 文件。
- 未修改业务源码、数据库迁移、服务器配置、容器、Nginx、Provider、模型、key、生产数据、`functional_feature_matrix.csv`、已有 B/C 文件或用户 docs。

## 验证结果

| 项目 | 结果 | 证据 |
|---|---|---|
| 本地 Revive | 已完成 | `preflight/git-revive.txt` |
| 服务器身份/版本 | 已确认 | `preflight/server-revive.txt` |
| 正式 Agent URL | 已确认 | `preflight/server-revive.txt`、`preflight/entry-probe.json` |
| 非流式入口 | 403，认证前阻断 | `preflight/entry-probe.json` |
| SSE 入口 | 403，无 SSE 事件 | `preflight/entry-probe.json` |
| 94 条逻辑基线 | 94 条已登记，0 条进入 Agent | 独立 CSV 和 cases 目录 |
| DB/draft before-after | 未读取 | 每 case `db_before_after.json`、`draft_before_after.json` |
| audit/run-trace | 未生成 | 每 case `audit.json`、`tool_trace.jsonl` |
| 隐藏思考 | 未采集 | 每 case `model_visible_prompt.txt`、`request_meta.json` |

测试脚本审查结论：`run_server_agent_all_tools.py` 的认证实现会从 PostgreSQL `sessions` 查询 token，本轮没有运行；历史 remote wrapper 与当前服务器、容器、Provider 或认证规则不匹配，也没有运行。

## 剩余工作与风险

- 需要用户或受控运行环境提供当前有效的 Bearer session token，才能执行真实工具选择、正式回答、audit/run-trace、DB/draft before-after、cleanup、长会话、cancel、Last-Event-ID、并发和 soak。
- 当前不能根据 HTTP 403 判断工具、Provider、上下文压缩或终态逻辑正确性；本轮结果只证明正式入口和认证门禁状态。
- 服务器部署 commit 未确认，不能把本地源码 commit 的结论写成部署版本结论。

## 提交边界

提交时只暂存本轮 D CSV 和阶段报告；原始 artifacts 保留为本地证据，不纳入提交。
