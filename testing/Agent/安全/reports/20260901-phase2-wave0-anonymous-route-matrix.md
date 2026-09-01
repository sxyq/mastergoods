# 阶段二 Wave 0 匿名认证矩阵

| 字段 | 内容 |
|---|---|
| test_id | `AG-W0-ANON-ROUTE-002` |
| observed_at | 2026-09-01 10:20–10:25（Asia/Shanghai） |
| target | `8.220.206.9` / `https://zhj-api.sxyq27.online/` |
| api_image | `sxyq27-zhj-api:20260901T014000-agent-owner-bill-c012290b` |
| flyway | V42 |
| provider_runtime | `gpt-5.6-luna` / `chat_completions` |
| provider_target | `glm-5.3-flash` |
| status | `Passed`（仅匿名认证拒绝边界） |

## 执行范围

- Agent 路由：24 条，覆盖会话、消息、草稿、工作台、任务、通知、聊天、图片、流式聊天、取消和审计。
- 管理员路由：22 条，覆盖 session、总览、用户、门店、Agent 观测、配置、审计、系统状态、导出列表/详情/下载和保留策略。
- Agent 的 POST/PUT 请求使用空 JSON 做认证层探针；管理员本批使用只读 GET，未发送管理员写请求。
- 请求未携带任何认证材料，未创建账号、会话、Agent 运行或业务数据。

## 结果

| 路由组 | 请求数 | 预期 | 实际 | 结果 |
|---|---:|---|---|---|
| `/v2/agent/*` | 24 | `401 application/json` | 24 条均为 `401 application/json;charset=UTF-8` | `Passed` |
| `/v2/admin/*` | 22 | `401 application/json` | 22 条均为 `401 application/json;charset=UTF-8` | `Passed` |
| `/`、`/healthz` | 2 | 受保护的 JSON 响应 | 均为 `401 application/json;charset=UTF-8` | `Passed` |

未发现匿名放行，也未观察到 5xx。当前结果只覆盖认证边界，不代表带权限接口、业务查询或 Agent 运行成功。

## 数据库前后计数

查询方式：通过 8220 直接进入 PostgreSQL 容器，使用 `psql` 只读执行聚合计数；未读取密码哈希、Session 字段或业务明细。

| 表 | before | after | delta |
|---|---:|---:|---:|
| `users` | 3 | 3 | 0 |
| `stores` | 2 | 2 | 0 |
| `store_memberships` | 2 | 2 | 0 |
| `sessions` | 6 | 6 | 0 |
| `agent_conversations` | 0 | 0 | 0 |
| `agent_messages` | 0 | 0 | 0 |
| `agent_drafts` | 0 | 0 | 0 |
| `agent_run_audits` | 0 | 0 | 0 |
| `agent_memories` | 0 | 0 | 0 |
| `agent_tasks` | 0 | 0 | 0 |
| `agent_notifications` | 0 | 0 | 0 |
| `media_assets` | 0 | 0 | 0 |

数据库前后计数无变化，认证探针未产生业务副作用。

## 当前阶段边界

- Android `emulator-5554` 上的 `com.zhihuiji.app` 1.0.0 仍停留在登录页，未建立可验证 session。
- 目标数据库现有用户的脱敏后缀不包含约定的 `8111`、`8112`、`8113`、`8114` 测试账号；本批不猜测密码、不创建账号、不重置密码。
- 运行模型与目标模型不一致，Provider 真实上下文窗口尚未确认。
- Wave 1–4 的登录后功能、SSE、工具、草稿、审计、跨 owner/store、性能和客户端展示仍为 `Blocked`；iOS 为 `Deferred`。

## 证据与复现边界

- 服务器路由来源：`Code/backend/src/main/java/com/zhihuiji/backend/api/controller/v2/V2AgentController.java` 与 `Code/backend/src/main/java/com/zhihuiji/backend/api/controller/admin/`。
- 设备前置证据：`testing/Agent/客户端/artifacts/20260901-phase2-wave0-AG-CLI-AND-PRELOGIN-001/`。
- 历史真实登录失败证据：`testing/Agent/客户端/artifacts/20260901-agent-phase2-wave0-AG-CLI-AND-P2-LOGIN-001/`。
- 本批没有保存认证头、Cookie、Token、密码、完整请求载荷或未脱敏业务数据。
