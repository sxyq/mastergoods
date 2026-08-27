# Agent API、SSE 与序列化契约测试规划（contract）

更新日期：2026-08-28。以 [V2AgentController.java](../../../Code/backend/src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java) 实际路由与 [SseStreamEmitter.java](../../../Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/component/SseStreamEmitter.java) 事件契约为准。每个端点至少执行：成功、空数据、缺字段/空白、错误类型、未登录、无权限、跨 owner/store、非法 ID、重复请求；写接口核对 before/after 与重复提交。SSE 接口检查原始事件，HTTP 200 不等于完成。

## 一、REST 端点契约（AG-C-API-001~024，初始 `Deferred`）

| 编号 | 方法/路径 | 请求字段与边界 | 成功响应 | 关键验收 |
|---|---|---|---|---|
| AG-C-API-001 | `GET /v2/agent/conversations` | page/limit 缺省、0、负、最大、超大 | `200 data[]`（ID/title/status/latest/时间） | 只返回当前 owner/store；稳定排序；空数组可处理；只读 |
| AG-C-API-002 | `GET /v2/agent/conversations/{id}` | 真实/不存在/0/负/非数字/跨 owner ID | `200 AgentConversationResponse`；跨域安全 4xx 或空 | 不回显跨 owner title/summary |
| AG-C-API-003 | `POST /v2/agent/conversations` | `{title}` 缺失/空白/超长；status 合法/非法 | `200` 带会话 ID/时间 | before/after 会话 +1；owner 来自认证 |
| AG-C-API-004 | `PUT /v2/agent/conversations/{id}` | title/status 空/空白/非法/跨 owner | `200` 更新会话；非法 4xx | 只有目标会话变化；关联不丢失 |
| AG-C-API-005 | `DELETE /v2/agent/conversations/{id}` | 当前/不存在/已删/跨 owner/非法/重复删除 | 成功 envelope；重复删除记录幂等语义 | 会话、消息、草稿、检查点、运行关联按设计清理；不得删他人 |
| AG-C-API-006 | `GET /v2/agent/conversations/{conversationId}/messages` | 会话 ID、分页边界、空/已删/跨 owner | `200 AgentMessageResponse[]`（run_id/role/message_type/content/structured） | 稳定时间/ID 序；不返回别的会话 |
| AG-C-API-007 | `GET /v2/agent/conversations/{conversationId}/run-traces` | limit 边界、跨 owner | `200 AgentRunTraceResponse[]`（状态/工具数/事件数/审计与 trace ID） | trace 与会话 owner 一致；无敏感 payload |
| AG-C-API-008 | `POST /v2/agent/conversations/{conversationId}/messages` | role/message_type/content required；空白/超长/非法 role/type/structured JSON 非法 | `200` 带会话/run 关联 | 手工消息不误当 Agent 完成回答；不跨域写 |
| AG-C-API-009 | `GET /v2/agent/drafts` | conversation_id 筛选、分页、空值、负、跨 owner | `200 AgentDraftResponse[]`（draft_type/title/content_json/status/时间） | 只当前 owner；不把旧缓存当新成功 |
| AG-C-API-010 | `POST /v2/agent/drafts` | draft_type/title/content_json required；空白/非法 JSON/status 非法/跨 owner | `200` 创建 active 草稿 | 只增加 agent_drafts；不进正式表 |
| AG-C-API-011 | `GET /v2/agent/drafts/pending` | 无 body；有/无 active 草稿 | `200` 只含 active、字段完整 | 不返回其他 owner 草稿 |
| AG-C-API-012 | `POST /v2/agent/drafts/{id}/confirm` | active/已确认/已取消/跨 owner/不存在/非法；重复/并发 | 首次确认成功或稳定业务失败；重复按幂等/409 | 确认成功才增正式表；同草稿最多一笔；无 500 |
| AG-C-API-013 | `POST /v2/agent/drafts/{id}/cancel` | active/已取消/已确认/跨 owner/不存在/重复取消 | active→cancelled；重复稳定；已确认不可回滚 | 正式表不因取消变化 |
| AG-C-API-014 | `PUT /v2/agent/drafts/{id}` | draft_type/title/content_json、空白/非法 JSON、status、跨 owner、已确认 | 更新 active 草稿；错误结构稳定 | 已确认草稿不可改写 |
| AG-C-API-015 | `DELETE /v2/agent/drafts/{id}` | active/已处理/不存在/跨 owner/重复删除 | 成功或稳定 4xx | 不删除确认形成的正式业务记录 |
| AG-C-API-016 | `GET /v2/agent/workbench` | 有数据/空数据/部分服务异常 | `AgentWorkbenchResponse`（greeting/KPI/quick/recent/pending/risk/warnings/policy） | 限定当前 owner；部分组件失败有 warnings；客户端能渲染 null/空列表 |
| AG-C-API-017 | `GET /v2/agent/tasks` | 有/空/跨 owner | `200 AgentTaskResponse[]` | 不泄露其他 owner；result_json 可解析 |
| AG-C-API-018 | `GET /v2/agent/notifications` | unread_only 缺省/true/false/非法布尔文本 | `200 AgentNotificationResponse[]` | 过滤当前 owner；数据库只读 |
| AG-C-API-019 | `POST /v2/agent/notifications/{id}/read` | 当前/不存在/已读/跨 owner/非法/重复 | 成功；重复稳定 | 仅目标通知 is_read 改变 |
| AG-C-API-020 | `POST /v2/agent/chat` | message required；conversation_id 省略/当前/跨域/不存在；image_asset_ids 0/1/9/10、非法 ID | `AgentChatResponse`（run_id/conversation_id/answer/blocks/draft_id/safety/mode/llm_status/plan_source/tool_calls/evidence_refs/result_blocks/performance/audit/trace/terminal_status/error_code/safe_message/completed_tools/missing_target_tools） | 业务成功不能只看 200；工具/回答/终态/审计/DB 一致；客户端正确处理 `data=null`、错误信封、草稿、图表 |
| AG-C-API-021 | `POST /v2/agent/images/generate` | prompt required；空白/超长；reference_asset_ids 空/非法/跨 owner | `image_url,revised_prompt` 或稳定 Provider 错误 | 资产引用当前 owner；临时资产清理 |
| AG-C-API-022 | `POST /v2/agent/chat/stream` | 同 chat；重点空白/超长/跨域会话/9、10 图片/重复提交/客户端断开 | HTTP 200 后仍须合法 SSE 终态；`run_started`→工具事件→answer_delta→`answer_completed`→`run_completed`/错误终态 | 不能用空流判成功；取消后无增量；认证失败为 HTTP 错误而非伪 SSE |
| AG-C-API-023 | `POST /v2/agent/runs/{runId}/cancel` | 运行中/已完成/已取消/不存在/跨 owner/非法；重复取消 | `run_id,status,cancelled`；状态机稳定 | 不影响他人 run；完成后不能改取消 |
| AG-C-API-024 | `GET /v2/agent/runs/{runId}/audit` | 运行中/完成/失败/取消/不存在/跨 owner/非法 | audit 字段（status/mode/llm/plan/tool·event count/lossy/warnings/audit·trace/error/时间/完整 events） | seq 单调、event_id 唯一、事件与 tool_call_id 配对、敏感字段脱敏 |

## 二、SSE 事件契约（AG-C-SSE-001~008）

| 编号 | 契约项 | 边界 | 验收 |
|---|---|---|---|
| AG-C-SSE-001 | 事件字段完整性 | 每个事件含 event_type/run_id/timestamp；活跃 run 含 conversation_id/seq/event_id/audit_id/trace_id | 无缺失必需字段 |
| AG-C-SSE-002 | 事件顺序 | 成功流/创建流/失败流/取消流 | 顺序符合功能文档第六节主线；终态唯一 |
| AG-C-SSE-003 | call_id 配对 | tool_started/completed/failed 的 tool_call_id 一致 | 每个 call_id 一一配对 |
| AG-C-SSE-004 | 重复与断线 | 重复 event_id、异常 EOF、Last-Event-ID 恢复 | 客户端去重；不可重复展示 |
| AG-C-SSE-005 | 终态事件 | `run_completed/run_failed/run_blocked/run_exhausted/run_cancelled` 的 terminal_status | 每个 run 仅一次终态；状态值与 audit 一致 |
| AG-C-SSE-006 | 敏感字段 | 事件 payload 内联系方式/凭据/认证载荷 | 扫描命中 0 |
| AG-C-SSE-007 | 未知/多余字段 | 客户端收到未知事件或字段 | 不崩溃、不重复、不丢终态 |
| AG-C-SSE-008 | 序列化与解析 | SSE data JSON 单行可解析；中文与转义字符 | Android/iOS/Web 解析一致 |

## 三、序列化与错误码契约

| 契约项 | 边界 | 验收 |
|---|---|---|
| REST 响应包络 | `ApiResponse` success/data、failure/code | Android、iOS、Web 可区分成功与失败；`data=null` 可处理 |
| 请求校验错误 | @Valid 失败、空白、超长、非法 ID | 稳定 4xx 与字段路径 |
| ID 精度 | 超过 JS 安全整数的业务 ID | Android/iOS 用 Long（64 位）；Web 必须字符串/BigInt；禁止 `Number()` |
| 错误码 | 401/403/409/422/429/5xx + `TOOL_*`/`SAFETY_BLOCKED`/`AGENT_*`/`STREAM_ERROR` | 客户端可区分登录失效/无权限/冲突/参数/限流/服务异常 |

## 四、证据存放

`contract/artifacts/<日期>-<波次>-<用例>/`（README 第五节文件序列）；脚本 `../scripts/contract/`；HTTP 探针与 SSE 抓包样本存档到 logs。