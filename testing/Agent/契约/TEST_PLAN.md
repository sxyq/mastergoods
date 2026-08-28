# Agent API、SSE 与序列化契约测试规划（契约）

更新日期：2026-08-28。端点清单以当前源码 [V2AgentController.java](../../../Code/backend/src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java) 为准，事件字段以 [SseStreamEmitter.java](../../../Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/component/SseStreamEmitter.java) 与 DTO 为准。本文件只定义契约检查，不把 HTTP 200、静态代码存在或相邻业务用例当成通过证据。

## 一、范围与记录规则

- 当前 Controller 映射为 24 个：会话、消息、草稿、工作台、任务、通知、聊天、图片、取消和审计。
- 每个 REST 父用例必须展开为独立的 `-B01` 至 `-B09` 记录；非流式与流式接口分别使用 `-REST`、`-SSE`，不能共用结果。
- 每条记录都要登记请求 JSON、查询参数、响应包络、字段路径、HTTP 状态、当前 owner/store 标签、工具调用链、数据库变化和证据路径。
- 路径参数不是数字、缺少必填字段、空白字段、错误类型和未知字段要分别记录，不能合并为“参数错误”。
- 没有真实 PostgreSQL 时可以检查 SQL 结构、分页边界和 H2 逻辑；生产 `EXPLAIN/EXPLAIN ANALYZE` 仍记 `Blocked` 或 `Deferred`，不能用 H2 结果代替。

### 1.1 REST 父用例分支

| 分支编号 | 内容 | 必须观察 |
|---|---|---|
| `B01` | 合法成功 | 请求字段、状态码、成功包络、全部响应字段与类型 |
| `B02` | 合法空数据 | 空数组/null 字段、分页元数据、客户端可解析性 |
| `B03` | 缺失/空白/错误类型 | 字段路径、稳定 4xx、业务 Service 是否未被调用 |
| `B04` | 未认证 | 稳定 401、无工具/Repository/写入副作用 |
| `B05` | 权限不足 | 稳定 403、权限边界、审计记录 |
| `B06` | 跨 owner/store | 拒绝或安全空结果；不得泄露标题、正文、联系人和业务数据 |
| `B07` | 非法 ID/边界参数 | 0、负数、超大 ID、非数字、分页/列表上限、非法枚举 |
| `B08` | 重复请求 | 读请求结果稳定；写请求状态机、幂等和正式表变化符合设计 |
| `B09` | 证据闭环 | 响应、工具、run-trace/audit、before/after、清理结果可以用 ID 对齐 |

## 二、REST 端点清单（AG-C-API-001~024）

每行父用例展开为 `AG-C-API-xxx-B01` 至 `AG-C-API-xxx-B09`。`成功响应`仅描述字段形状，最终结果必须以当前 DTO 和真实响应为准。

| 编号 | 方法/路径 | 请求字段与重点边界 | 成功响应与契约断言 |
|---|---|---|---|
| `AG-C-API-001` | `GET /v2/agent/conversations` | `page`、`limit` 缺省、0、负数、最大、超大 | `ApiResponse`；列表字段类型一致；owner/store 条件、稳定排序和空数组正确 |
| `AG-C-API-002` | `GET /v2/agent/conversations/{id}` | 真实、不存在、0、负数、超大、非数字、跨 owner ID | 会话字段可解析；跨域不回显标题、摘要或消息 |
| `AG-C-API-003` | `POST /v2/agent/conversations` | `title` 缺失、空白、超长、合法状态/非法状态 | 创建响应含 ID 和时间；owner 来自认证；before/after 只增加当前会话 |
| `AG-C-API-004` | `PUT /v2/agent/conversations/{id}` | 标题/状态空白、非法、跨 owner、已删除对象 | 只更新目标会话；字段类型、时间和关联关系不丢失 |
| `AG-C-API-005` | `DELETE /v2/agent/conversations/{id}` | 当前、不存在、已删除、跨 owner、非法 ID、重复删除 | 包络稳定；消息、草稿、检查点和审计关联按设计处理；不删除他人数据 |
| `AG-C-API-006` | `GET /v2/agent/conversations/{conversationId}/messages` | 会话 ID、`page/limit` 边界、空会话、已删除、跨 owner | 消息字段、角色、类型、结构化内容、`run_id` 可解析；排序稳定 |
| `AG-C-API-007` | `GET /v2/agent/conversations/{conversationId}/run-traces` | `limit` 缺省、0、负、最大、跨 owner | trace 字段、工具数、事件数、审计/trace ID 一致；敏感参数脱敏 |
| `AG-C-API-008` | `POST /v2/agent/conversations/{conversationId}/messages` | role/type/content 缺失、空白、非法值、结构化 JSON 错误、跨 owner | 消息与会话关联正确；手工消息不得伪装成 Agent 完成回答 |
| `AG-C-API-009` | `GET /v2/agent/drafts` | `conversation_id`、`page/limit`、空值、负数、跨 owner | 草稿字段可解析；只返回当前作用域；不能把旧缓存当作本次成功 |
| `AG-C-API-010` | `POST /v2/agent/drafts` | `draft_type/title/content_json` 缺失、空白、非法 JSON、非法类型 | 只创建 `active` 草稿；正式业务表不变；内容字段保持 JSON 契约 |
| `AG-C-API-011` | `GET /v2/agent/drafts/pending` | 无 body；有/无 active 草稿；跨 owner | 只返回 active 草稿；字段完整；状态更新后列表同步变化 |
| `AG-C-API-012` | `POST /v2/agent/drafts/{id}/confirm` | active、已确认、已取消、不存在、跨 owner、重复、并发 | 首次确认按路由执行；重复状态稳定；正式业务记录最多一份；500=0 |
| `AG-C-API-013` | `POST /v2/agent/drafts/{id}/cancel` | active、已取消、已确认、不存在、跨 owner、重复 | active→cancelled；已确认不可回滚；正式业务表不因取消变化 |
| `AG-C-API-014` | `PUT /v2/agent/drafts/{id}` | 字段缺失、空白、非法 JSON、非法状态、跨 owner、已确认 | 只有 active 草稿可编辑；确认后不可改写已执行动作 |
| `AG-C-API-015` | `DELETE /v2/agent/drafts/{id}` | active、已处理、不存在、跨 owner、重复删除 | 删除语义稳定；不删除确认后形成的正式业务记录 |
| `AG-C-API-016` | `GET /v2/agent/workbench` | 有数据、空数据、局部依赖异常 | greeting/KPI/quick/recent/pending/risk/warnings/policy 字段可解析；局部错误可解释 |
| `AG-C-API-017` | `GET /v2/agent/tasks` | 有数据、空数据、跨 owner | 列表字段和 `result_json` 可解析；只返回当前 owner |
| `AG-C-API-018` | `GET /v2/agent/notifications` | `unread_only` 缺省、true、false、非法文本 | 布尔过滤准确；当前 owner；查询不产生写入 |
| `AG-C-API-019` | `POST /v2/agent/notifications/{id}/read` | 当前、不存在、已读、跨 owner、非法 ID、重复 | 只改目标通知；重复调用稳定；他人通知不改变 |
| `AG-C-API-020` | `POST /v2/agent/chat` | message、显式 `conversation_id`、`image_asset_ids` 0/1/9/10、非法/跨 owner ID | `run_id`、会话、answer、blocks、draft、mode、tool、audit、trace、终态和错误字段类型一致 |
| `AG-C-API-021` | `POST /v2/agent/images/generate` | prompt 缺失/空白/超长；reference asset 空、非法、跨 owner | 成功时 `image_url/revised_prompt` 可解析；失败时错误包络稳定且无半写入 |
| `AG-C-API-022` | `POST /v2/agent/chat/stream` | 同 chat；空白/超长、跨域会话、9/10 图片、重复提交、客户端断开 | HTTP 200 后仍须有合法 SSE 终态；认证错误应是 HTTP 错误；取消后无新回答增量 |
| `AG-C-API-023` | `POST /v2/agent/runs/{runId}/cancel` | 运行中、完成、已取消、不存在、跨 owner、非法、重复 | `run_id/status/cancelled` 可解析；不影响他人 run；完成后状态不可被错误改写 |
| `AG-C-API-024` | `GET /v2/agent/runs/{runId}/audit` | 运行中、完成、失败、取消、不存在、跨 owner、非法 | audit、事件、seq、event_id、tool_call_id、错误和脱敏字段可解析并互相对齐 |

## 三、SSE 事件契约（AG-C-SSE-001~008）

每个事件契约至少选取成功、创建待确认、失败、取消、断线恢复和未知字段六类流。原始事件保存为脱敏文本；客户端解析结果与服务端原始事件分别记录。

| 编号 | 契约项 | 检查内容 | 验收 |
|---|---|---|---|
| `AG-C-SSE-001` | 公共字段 | `event_type/run_id/timestamp`；活跃 run 的 `conversation_id/seq/event_id/audit_id/trace_id` | 必填字段不缺失，类型稳定，时间可解析 |
| `AG-C-SSE-002` | 事件顺序 | `run_started`、压缩、plan、工具、回答、结果块、终态 | 顺序符合当前基线；允许重复事件仅限文档明确场景 |
| `AG-C-SSE-003` | 工具调用配对 | `tool_started/completed/failed` 的 `tool_call_id`、序号、工具名 | 每个 call 有且仅有一个结束结果；参数摘要可追踪 |
| `AG-C-SSE-004` | 重复与断线 | 重复 `event_id`、异常 EOF、重连和 `Last-Event-ID` | 客户端不重复展示；可恢复时只补缺失事件；不可恢复有终态 |
| `AG-C-SSE-005` | 终态事件 | completed/failed/blocked/exhausted/cancelled 与 `terminal_status` | 每个 run 仅一个终态；SSE、REST audit、消息状态一致 |
| `AG-C-SSE-006` | 敏感字段 | 联系方式、认证载荷、Token、密钥、其他 owner 数据 | 脱敏扫描命中为 0；安全失败记录位置 |
| `AG-C-SSE-007` | 未知字段/事件 | 客户端收到新增字段、未知事件类型和空 data | 不崩溃、不改变业务状态、不吞掉终态 |
| `AG-C-SSE-008` | JSON 序列化 | 单行 JSON、中文、引号、换行、转义、null、数字精度 | Android/iOS/Web 能解析；大 ID 不转为 JS 不安全数字 |

## 四、错误与跨端序列化契约（AG-C-SCHEMA-001~007）

| 编号 | 契约项 | 检查输入 | 验收 |
|---|---|---|---|
| `AG-C-SCHEMA-001` | `ApiResponse` 包络 | success/data、failure/code、`data=null` | 三端可区分成功、业务失败和空数据；不因 null 合法响应崩溃 |
| `AG-C-SCHEMA-002` | 请求校验 | `@Valid`、空白、超长、非法 JSON、非法 ID、未知字段 | 状态码、错误码、字段路径稳定；业务写入前拒绝 |
| `AG-C-SCHEMA-003` | Agent 工具错误 | `TOOL_*`、依赖缺失、权限拒绝、SafetyGuard 拦截 | 客户端保留错误码和安全消息；不把工具错误显示成成功回答 |
| `AG-C-SCHEMA-004` | 运行错误 | Provider 空响应、超时、429、流错误、取消 | `terminal_status`、错误码、audit 与消息一致；失败不伪装成功 |
| `AG-C-SCHEMA-005` | ID 精度 | 大于 JavaScript 安全整数的会话、草稿、业务 ID | Java/Kotlin 使用 64 位整数；Web 使用 string/BigInt；禁止 `Number()` |
| `AG-C-SCHEMA-006` | snake_case | 请求和响应中的 `conversation_id`、`run_id`、`tool_call_id`、时间字段 | 序列化名称与后端 DTO 一致；未知字段策略明确 |
| `AG-C-SCHEMA-007` | 分页 | `page/limit` 或工具 `page/size/limit` 的 0、负、最大和超大 | 无无界查询；空页可解析；总数和返回条数关系正确 |

## 五、执行顺序与证据

1. 从当前 Controller 和 DTO 生成端点快照，确认 24 个路径没有漏项或旧路径残留。
2. 先执行 `B03/B04/B05/B06/B07`，确认校验、认证和权限不会进入敏感业务逻辑。
3. 执行 `B01/B02/B08`，记录成功、空数据和重复请求的响应差异。
4. 对 `/chat/stream`执行 SSE 六类流，逐条核对 `event_id/seq/call_id/run_id`。
5. 将同一响应交给 Android、iOS、Web 模型解析，单独记录客户端结果；设备缺失时只做静态/纯单元契约检查并标 `Blocked`。
6. 所有写接口记录 before/after、清理动作和确认者标签；不得保存认证凭据。

单条证据目录为：

```text
契约/artifacts/<日期>-<波次>-<用例>-<REST或SSE>/
├── 00-environment.md
├── 01-input-redacted.json
├── 02-http-response.json
├── 03-raw-sse.log
├── 04-tool-trace.jsonl
├── 05-run-audit.json
├── 06-database-before.json
├── 07-database-after.json
├── 08-client-parse.md
├── 09-cleanup.json
└── 10-conclusion.md
```

脚本放在 `../脚本/契约/`，日志放在 `契约/logs/`，阶段报告放在 `契约/reports/`。每条记录必须回写 `result`，只允许 `Passed`、`Failed`、`Blocked`、`Deferred`。
