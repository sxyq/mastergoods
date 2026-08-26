# Agent 实测前阶段核对报告（2026-08-27 B）

核对目标：`8.220.206.9` 与 `https://zhj-api.sxyq27.online/`。

本轮结论：服务器服务和 API 入口可达，真实 Agent 实测仍为 `Blocked`。未执行真实测试、未修改业务代码、未创建写入数据。未发起真实 Agent API、SSE、写入或性能请求。

## 1. Revive

- 分支：`codex/publish-local-updates`，相对远端 `ahead 43`。
- 当前 HEAD：`21b23cff54dbe1183a655eac44f1245c9709ba5d`。
- HEAD 提交：`test(agent): record legacy test asset cleanup`，时间 `2026-08-27T00:33:11+08:00`。
- 工作树版本：`21b23cff-dirty`。
- `git diff --stat`：51 个文件，102 行新增，100 行删除。
- 补充：暂存区包含 Agent 测试资产删除，统计为 67 个文件、21953 行删除；本轮没有恢复或执行这些文件。
- Revive 时已存在的 `testing/Agent/` 未跟踪文件和其他用户改动均保留未动。本报告是本轮唯一新增文件。

## 2. 服务器部署证据

### SSH 与服务

- TCP/22 可达；使用既有 root key 完成 BatchMode SSH 认证。
- 系统：Ubuntu 24.04.2 LTS，kernel `6.8.0-63-generic`。
- 主机运行时间：约 11 天 23 小时。
- `nginx=active`，`docker=active`；`nginx -t` 成功。
- PostgreSQL 容器状态为 `running`、health 为 `healthy`。本轮没有执行 `psql` 或读取数据库。

### 运行镜像与 API 端口

| 对象 | 运行证据 |
| --- | --- |
| API 容器 | `sxyq27-zhj-api:20260818`，image ID `sha256:3b526c02ba425908cf4859625568c964c113e53903f8693b1456a573f6fffc48` |
| API 部署产物 | `/app/app.jar` SHA-256 `4289b73346780986647ed1140fa92552656e7cc2ba53a54826e8eaa01832edd2` |
| API 监听 | 容器端口 `18080` 映射为宿主机 `127.0.0.1:18080` |
| PostgreSQL | `m.daocloud.io/docker.io/library/postgres:15`，容器 healthy，未查询表或 session |
| Redis | `m.daocloud.io/docker.io/library/redis:7-alpine`，运行中 |

Nginx 路由摘要：

- `server_name zhj-api.sxyq27.online` 的 HTTPS `location /` 代理到 `http://127.0.0.1:18080`。
- HTTP 入口对该域名执行 HTTPS 301。
- 正式入口 `GET /v2/auth/users/me` 返回 `403`；`GET /v2/auth/login` 返回 `405`。这只证明入口、路由和未认证门禁有响应，没有证明业务认证成功。
- 宿主机 API 端口 `GET /v2/auth/users/me` 返回 `403`。

## 3. 本地源码与服务器版本

### 本地源码证据

- 当前本地 Git HEAD 为 `21b23cff54dbe1183a655eac44f1245c9709ba5d`，工作树 dirty。
- 当前源码存在以下路由和认证实现：
  - `Code/backend/src/main/java/com/zhihuiji/backend/api/controller/v2/V2AuthController.java:18,34-37`：`POST /v2/auth/login`。
  - `Code/backend/src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java:24,186-203`：`POST /v2/agent/chat` 与 `POST /v2/agent/chat/stream`。
  - `Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/config/SecurityConfig.java:47-55`：认证路由放行，其他路由要求认证。
  - `Code/backend/src/main/java/com/zhihuiji/backend/infrastructure/security/TokenAuthenticationFilter.java:46-53`：从 Bearer header 查找有效 session。
- `application-prod.yml` 使用 PostgreSQL 环境变量；当前本地没有可直接用于比较的 `Code/backend/build/libs` 产物。

### 一致性判断

服务器 `/opt/sxyq27/master-goods` 没有可用 Git 工作树，运行镜像没有 `org.opencontainers.image.revision`，JAR 内也没有可用的 Git commit metadata。现有证据只能确认部署镜像标签、镜像 digest 和 JAR digest，无法确认它们对应本地 HEAD，也不能把本地 dirty 源码当作服务器源码。若要求源码级一致性，需要后续提供带 revision 的部署产物或重新记录构建 provenance；本轮不做构建和部署。

## 4. 认证前置与敏感信息边界

- 本轮未读取、打印或保存 Token、Cookie、密码、私钥、完整认证载荷。
- 本轮未从 PostgreSQL `sessions` 表读取 session token，也未读取任何数据库业务数据。
- 代码可确认登录 endpoint 为 `POST /v2/auth/login`，登录成功响应由服务返回 access token、refresh token 和过期时间；本轮没有提交登录请求。
- 当前没有可供本轮验证的用户提供短期 token、测试账号授权或门店作用域确认。认证实测为 `Blocked`。
- 推荐后续使用用户显式提供的短期、限定作用域认证输入，或同一进程内完成登录并直接消费 login response。认证值只存在进程内，禁止落盘、写入证据、写入命令日志或从数据库提取。

## 5. Provider、模型与数据库计划

| 项目 | 只读证据 | 状态 |
| --- | --- | --- |
| API 服务 | 正式入口和本机 `18080` 均对未认证 GET 返回预期门禁状态 | Ready for authenticated preflight |
| Provider 基础服务 | 服务器安全配置为 `https://oneapi.sxyq27.online/v1`；未认证 HEAD 返回 `404`，说明 DNS/TLS/HTTP 基础链路有响应 | Partial；模型调用 `Blocked` |
| 模型配置 | `AGENT_LLM_ENABLED=true`，模型名为 `gpt-5.6-luna`，wire API 为 `chat_completions` | Configured; key availability unconfirmed |
| Provider key | 只确认 `AGENT_LLM_API_KEY` 环境变量名称存在，未读取值，无法确认值是否有效 | Blocked |
| PostgreSQL | 容器运行且 healthy；没有执行 schema、owner、store、session 或业务数据查询 | Blocked for data-backed Agent test |
| SQLite | 源码包含旧 SQLite 导入能力，生产运行时使用 PostgreSQL；本轮没有 SQLite 文件、导入授权或导入请求 | Deferred |
| 性能验证 | 按本轮范围不发起性能请求 | Deferred |

## 6. 已删除脚本与新脚本输入建议

当前工作树中旧 Agent 脚本处于删除状态，本轮没有恢复或执行。现存 `testing/scripts/run_server_sync_scope_remote.sh` 含有从 `sessions` 表读取 token、发起写入请求和清理数据的逻辑，不适合作为本轮或下一轮的默认入口，应保持禁止复用。

建立新脚本时只接受以下非敏感运行参数：

- `AGENT_BASE_URL`
- `AGENT_PROVIDER_BASE_URL`
- `AGENT_MODEL`
- `AGENT_READ_ONLY=true`
- `AGENT_ALLOW_WRITES=false`
- `AGENT_ALLOW_SSE=false`
- `AGENT_ALLOW_PERF=false`
- `AGENT_CASE_FILTER`
- `AGENT_REQUEST_TIMEOUT_SECONDS`
- `AGENT_OWNER_SCOPE`
- `AGENT_STORE_SCOPE`
- `AGENT_EVIDENCE_DIR`

认证值不从文件、数据库或历史证据取得。若运行器采用环境变量传递短期认证值，只允许由外部 secret 注入器在进程启动时注入；变量值不得打印或写入文件，脚本结束后应清理进程环境。更稳妥的方式是同一进程接收用户授权的 login response 并以内存对象传递。

## 7. 实测门禁

当前阻塞项：

1. 没有用户明确提供或授权的短期认证输入，且禁止从数据库 session 表提取 token。
2. Provider key 值未验证；只能证明 Provider 基础 HTTP 链路有响应，不能证明模型调用可用。
3. PostgreSQL 只确认容器健康，未确认测试 owner/store/data scope；生产数据读写边界仍未授权。
4. 服务器部署缺少源码 commit provenance，无法完成本地源码与运行产物的一致性确认。

可开始认证前置检查的条件：

- 用户提供或授权短期认证输入，并确认 owner/store 作用域与只读范围。
- Provider key 已由运行环境安全注入，模型配置和服务端允许的模型路由已确认；测试运行器不打印其值。
- 接受当前部署 digest 作为被测运行对象，或先补充带 commit revision 的构建证据。
- 明确 PostgreSQL 只读验证计划；需要 SQLite 时提供隔离文件和导入授权。任何写入、SSE、性能场景单独审批后再执行。

本报告未提交 commit，也未修改服务器、源码、既有文档或旧测试脚本。
