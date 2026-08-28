# Agent 集成测试规划（集成）

更新日期：2026-08-28。集成测试覆盖“多个真实组件协同”的路径：V2AgentAiService 全链路、Provider 客户端、Repository、事务边界、真实数据库、记忆与媒体落库。需要真实服务、Provider 或数据库的项目，条件缺失时记 `Blocked`/`Deferred`。本类别关注组件之间的真实输入输出，不把各组件单元测试拼接成集成通过。

## 一、范围与前提

- 环境：可用的后端服务 + 真实或隔离数据库（PostgreSQL/H2-PG 模式）+ 已配置或 mock 的 Provider。
- 每条用例核对：实际工具调用链与预期一致、事务提交/回滚正确、无重复写入、run-audit 落库并可读。
- mock Provider 只用于稳定控制工具选择、超时和错误分支；使用 mock 时必须在结果中标记，不能把它作为真实 Provider 性能或可用性结论。
- H2/H2-PG 模式可以验证应用逻辑和部分 SQL 行为；不能代替 PostgreSQL 的锁、索引和 `EXPLAIN/EXPLAIN ANALYZE` 结果。

## 二、专项用例（AG-I-001~012，初始 `Deferred`）

| 编号 | 场景 | 步骤 | 预期 | 边界/前提 |
|---|---|---|---|---|
| AG-I-001 | 非流式全链路 | `POST /v2/agent/chat` 单只读工具 → 读数据库 | 消息、run-audit、事件完整落库；业务表只读 | 需真实 DB |
| AG-I-002 | 流式全链路 | `POST /v2/agent/chat/stream` 多工具 → 读数据库 | 事件与 audit 一致；终态唯一 | 需真实 DB |
| AG-I-003 | 事务回滚 | 确认草稿但支付/扣库存中途失败 | 全部回滚；草稿保持 active；无半写入 | 构建失败夹具 |
| AG-I-004 | Provider 客户端 | `LongCatAnthropicClient`（isConfigured、createMessageWithTools、continueWithToolOutputs、streamTextMessage、createJsonMessage、cancelStream、supportsToolResultContinuation） | 各方法行为与配置/响应一致；取消能中止流 | Provider 真实或可控 mock |
| AG-I-005 | 检查点与内存集成 | 30 轮会话触发压缩 → 读 `agent_context_checkpoints` | 检查点唯一且 owner 隔离；边界后消息正确 | 需真实 DB |
| AG-I-006 | 记忆集成 | 回答后读 `agent_memories` | 按 sourceMessageId 去重；脱敏字段无敏感原文 | `agent.memory.enabled=true` |
| AG-I-007 | 媒体落库 | `/v2/agent/images/generate` 与 media 附件 | 资产当前 owner；临时文件清理 | 图片 Provider 可用 |
| AG-I-008 | 草稿确认集成 | 14 类草稿确认 → 对应业务 Service 写入正式表 | 路由、事务、关联审计正确；`create_inventory_count_draft` 走 inventory_adjustment 路由实测 | 真实 DB + 完整夹具 |
| AG-I-009 | 任务/通知联动 | Agent 运行产生任务/通知 → 落库可读 | task/notification 与 run 关联一致 | 现有任务生成入口按需准备 |
| AG-I-010 | Web 搜索集成 | `WebSearchProvider` → 结果解析 → 安全 URL 过滤 | 来源、摘要、引用一致；危险来源不进结果 | Provider 已配置 |
| AG-I-011 | 真实分页查询 | Repository 层 page/size/count 与大表 | SQL 正确、有界 | 真实 PostgreSQL（否则 Blocked/Deferred） |
| AG-I-012 | 数据库迁移数据 | 现有迁移后的 Agent 表结构与索引（V13/14/15/17/32/33） | 表字段、CASCADE、唯一约束符合基线第十二节 | 需重建或既有库 |

## 三、组件边界与记录要求

每条集成用例至少记录以下边界：Controller 输入、认证主体和 owner/store、Provider 请求/响应摘要、ToolPlanner 计划、ToolExecutor 门结果、业务 Service/Repository 调用、事务结果、SSE/REST 输出、audit/run-trace 和数据库 before/after。任何一个环节缺少证据时，结果保持 `Deferred` 或按缺失条件记 `Blocked`。

特别核对两个草稿路由：`create_inventory_count_draft` 工具生成的草稿类型是 `create_inventory_adjustment`，确认时进入库存调整 Service；`media_upload_tool`工具生成的草稿类型是`media_upload`，确认后后端不直接落媒体资产，由客户端继续上传。路由名称、工具名称和草稿类型必须分开记录。

## 四、证据存放

`集成/artifacts/<日期>-<波次>-<用例>/`；服务与 SQL 观察 → `集成/logs/`；脚本 → `../脚本/集成/`（含 refresh 表脚本按类别目录存放）。文件按 README 第六节命名。
