# 当前后端 Agent 真实重测报告（B，2026-08-24）

## 结论

本轮完成了当前服务入口和安全前置核验，未开始业务 Agent 重测。当前工作树源码 commit 与指定 baseline 一致：`3ced2d07acac6505e3fcb557f994b09f7db64c31`。本机唯一相关监听为 `127.0.0.1:8080`，五个健康/Agent 入口均返回 `401`；监听进程工作目录为 `/Users/sunyiyang/.micloud_webdav`，无法证明它是本项目当前部署。

没有安全认证、当前 Provider provenance、PostgreSQL、第二 owner/store 或可确认的当前部署产物，因此停止所有真实业务 HTTP/SSE、Provider、数据库、草稿、媒体和性能请求。历史 154 环境、旧 Provider、旧容器/数据库和历史证据没有混入本轮结果。

## 结果分布

| 范围 | 总数 | Passed | Failed | Blocked | Deferred |
|---|---:|---:|---:|---:|---:|
| `AG-FT-BE-ALL-001..063` | 63 | 0 | 0 | 62 | 1 |
| `LOOP-001..010` | 10 | 0 | 0 | 4 | 6 |
| `DRAFT-001..003` | 3 | 0 | 0 | 3 | 0 |
| `CTX-001..014` | 14 | 0 | 0 | 12 | 2 |
| `REC-001..002` | 2 | 0 | 0 | 0 | 2 |
| `SEARCH-001..002` | 2 | 0 | 0 | 2 | 0 |
| **合计** | **94** | **0** | **0** | **83** | **11** |

`Blocked` 用于认证、当前服务身份、Provider/数据库访问等执行门禁未满足的用例。`Deferred` 用于当前没有安全可复现的受控失败、事件重放、媒体存储或检查点失效入口的用例。两者都没有发送业务请求。

Deferred 用例：`AG-FT-BE-ALL-057`、`LOOP-003`、`LOOP-004`、`LOOP-006`、`LOOP-008`、`LOOP-009`、`LOOP-010`、`CTX-005`、`CTX-010`、`REC-001`、`REC-002`。

## Revive 与环境证据

- `git status --short --branch`：工作树包含用户 docs 修改和既有 B baseline 报告；没有把这些文件纳入本轮修改。
- `git diff --stat`：本轮开始前只有用户 docs 差异；`functional_feature_matrix.csv` 未被本轮修改。
- `git rev-parse HEAD`：`3ced2d07acac6505e3fcb557f994b09f7db64c31`。
- 进程归属：`127.0.0.1:8080` 的监听进程工作目录为 `/Users/sunyiyang/.micloud_webdav`，与当前仓库不一致。
- `docker` 命令不可用；本机没有观察到 PostgreSQL 监听。
- 安全环境变量中没有可用认证材料；没有读取、打印或保存 Token、Cookie、密码、私钥或完整认证载荷。
- Provider 模型、wire API、部署镜像/JAR 和运行源码 provenance 未确认，不能把本地源码 commit 当成部署版本。

入口探针结果：

| 入口 | 方法 | 状态 | 处理 |
|---|---|---:|---|
| `/actuator/health` | GET | 401 | 仅记录状态，不读取响应体 |
| `/actuator/info` | GET | 401 | 无法获取部署版本 |
| `/v2/agent/conversations` | GET | 401 | 未读取会话 |
| `/v2/agent/chat` | GET 探针 | 401 | 未发送 POST 业务请求 |
| `/v2/agent/chat/stream` | GET 探针 | 401 | 未发送 POST SSE 请求 |

## 实际执行命令

本轮只执行了以下只读/门禁操作：

```text
git status --short --branch
git diff --stat
git rev-parse HEAD
git show -s --format=... HEAD
env 变量名筛选（只输出变量名）
ps 进程名/时间和 lsof 监听端口/工作目录
docker ps（结果为 docker command not found）
curl --max-time 5 的健康和 Agent 入口元数据探针
```

未执行 `POST /v2/agent/chat`、`POST /v2/agent/chat/stream`、cancel、会话创建、草稿确认/取消、媒体操作、Provider 请求、PostgreSQL 查询、并发、30 轮非流式、soak 或设备操作。

## 脚本映射审查

- `testing/scripts/run_server_agent_all_tools.py`：可映射非流式工具 case，但需要当前 base URL、受控认证、Provider 和数据库 before/after；本轮未运行。
- `testing/Agent/功能测试/scripts/run_agent_followup_8220.py`：可映射会话、SSE、cancel、audit、并发和清理；当前认证和服务 provenance 不满足，本轮未运行。
- `testing/scripts/run_server_agent_stream_concurrency.py`：可映射 SSE 并发；没有执行。
- `testing/scripts/run_server_agent_stream_long.py`：可映射长会话和 transcript，但默认参数不能作为当前部署证明；没有执行。
- `Code/backend/tools/ai_agent_performance_evidence.py`：可记录时延和 audit 时间线；没有执行性能请求。

## 证据与台账

- 独立结果台账：`testing/Agent/功能测试/agent-rerun-20260824-B.csv`
- 独立报告：`testing/Agent/功能测试/阶段报告-20260824-Agent-Rerun-B.md`
- Revive 证据：`testing/.artifacts/2026-08-24-agent-rerun-B/preflight/git-revive.txt`
- 入口探针：`testing/.artifacts/2026-08-24-agent-rerun-B/preflight/entry-probe.txt`
- 环境门禁：`testing/.artifacts/2026-08-24-agent-rerun-B/preflight/environment-gate.md`

由于业务请求数为 0，本轮没有原始业务 HTTP/SSE、工具轨迹、正式回答、audit/run-trace、DB/draft before-after 或 cleanup 文件；台账逐项引用门禁证据并明确写出未执行原因，避免用 HTTP 401 或历史证据冒充 Agent 结果。

## 修改与提交边界

- 本轮只新增本报告、独立结果台账和 B 专属 preflight 证据。
- 未修改 `functional_feature_matrix.csv`、业务源码、数据库迁移、配置、用户 docs、C 文件、`data/server-backups` 或 `data/server-exports`。
- 提交前只允许路径级暂存本报告和独立结果台账；B baseline 报告、用户 docs 和其他已有修改保持未暂存。
