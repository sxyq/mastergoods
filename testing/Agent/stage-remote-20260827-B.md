# Agent Remote Wave 2 阶段报告（2026-08-27 B）

## 结论

本轮未具备外部注入的短期认证值，Wave 2 真实认证 Agent 测试保持 `Blocked`。共 60 个 Agent case 未执行：46 个 `READ_ONLY`、14 个 `CREATE_ONLY`，全部登记为 `Blocked`。APP/设备验证为 `Blocked`，性能采样为 `Deferred`。

未执行真实认证 Agent API、SSE、Provider 模型请求、数据库查询、业务写入或性能请求。只执行了正式入口的无认证 GET 门禁探针。

## Revive

- 分支：`codex/publish-local-updates`，相对远端 `ahead 45`。
- 当前 HEAD：`c96863c8`，最近提交为 `test(agent): publish consolidated Agent test assets`。
- 工作树已有用户/并行改动。本轮不恢复已删除旧脚本，不修改共享主台账，不触碰业务源码、数据库、迁移、生产配置或受限数据目录。
- 新增证据只放在 `testing/.artifacts/2026-08-27-agent-remote-B/`；阶段报告放在本文件。

## 门禁探针

目标入口：`https://zhj-api.sxyq27.online/`。

| 请求 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/v2/auth/users/me` | 无认证 GET | `403` | 认证门禁响应 |
| `/v2/agent/chat` | 无认证 GET | `403` | Agent 路由认证门禁响应，无请求体 |
| `/v2/agent/chat/stream` | 无认证 GET | `403` | 流式路由认证门禁响应，未建立 SSE |

探针没有 Authorization header、请求体或 Cookie。状态证据见 `testing/.artifacts/2026-08-27-agent-remote-B/00-auth-gate-status.md`。

## 认证与执行计数

- `AGENT_ACCESS_TOKEN`：当前进程不存在；只做了存在性检查，没有读取或输出值。
- `AGENT_BASE_URL`：当前进程不存在。
- 未从 PostgreSQL `sessions` 表读取认证值。
- 未读取、打印或保存 Token、Cookie、密码、私钥或完整认证载荷。
- authenticated Agent requests：`0`。
- `POST /v2/agent/chat`：`0`；`POST /v2/agent/chat/stream`：`0`。
- SSE sessions：`0`；Provider model requests：`0`；database queries：`0`；业务写入：`0`。
- 本轮没有生成 `run_id`、`audit_id`、`trace_id`、`tool_call_id`、工具顺序、参数摘要、回答或图表结果；这些字段在 ledger 中标记为未执行，不伪造结果。

## Wave 2 状态

| 范围 | 数量 | 状态 | 原因 |
| --- | ---: | --- | --- |
| READ_ONLY 工具 | 46 | `Blocked` | 缺少外部短期认证值，不能调用真实 Agent |
| CREATE_ONLY 工具 | 14 | `Blocked` | 缺少认证和明确写入/清理授权，禁止创建草稿或业务数据 |
| APP 输入、收流、图表、按钮和设备 | 1 组 | `Blocked` | 本机无 `adb`，无设备证据 |
| 性能请求 | 1 组 | `Deferred` | 本轮未进入性能波次，且认证门禁未解除 |

阶段 ledger：`testing/.artifacts/2026-08-27-agent-remote-B/stage-ledger.csv`。ledger 只使用 `Passed`、`Failed`、`Blocked`、`Deferred` 四种结果值。

## Provider 与数据库

- 既有只读环境报告记录服务器运行 Provider 配置为 `gpt-5.6-luna` 与 `https://oneapi.sxyq27.online/v1`；本轮没有重新访问 Provider，也没有使用服务端 API key。
- Provider key 只能在后续服务端运行环境中由应用使用，不能读取、复制或写入证据。当前模型可用性保持 `Blocked`，没有成功或失败模型样本。
- PostgreSQL 只沿用既有容器健康记录；本轮没有连接、查询表、读取 session 或核对业务数据。生产数据读写范围未获授权，数据库相关真实 Agent 验证保持 `Blocked`。
- SQLite 不属于当前生产 Agent 运行时，相关导入或查询保持 `Deferred`。

## 后续解除条件

1. 由用户或外部 secret 注入器提供短期、限定作用域的认证值；只在进程内使用，不写文件、日志、ledger 或 evidence。
2. 确认目标 owner/store、只读范围和会话策略；读取类测试不得从数据库提取 session token。
3. 确认 Provider key 已存在于服务端运行环境、模型路由可用；测试器不读取或保存 key。
4. 若进入 CREATE_ONLY，另行明确写入、草稿确认/拒绝、清理和并发授权；未获授权前保持 `Blocked`。
5. 若进入 APP 或性能波次，分别提供可验证设备和独立性能范围；服务器 JSON/SSE 证据不能替代 APP 结论。

本轮未创建 runner，因为认证变量缺失；未恢复或执行已删除旧 Agent 脚本；未提交业务代码、数据库、迁移、生产配置或其他共享台账。
