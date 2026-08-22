# Agent 工具链复核与收束报告（2026-08-22）

## 环境边界

- Revive：当前分支 `codex/publish-local-updates`；本轮开始前工作树无未提交修改；基线提交为 `a80db11a`。
- 当前 8220 运行证据：`sxyq27-zhj-api:20260818`。旧 154 结果、旧部署结果和本地源码结果没有合并。
- 本轮没有读取、打印或保存 Token、Cookie、密码、私钥或完整认证载荷；没有部署、迁移、push 或启动新的长时间测试。

## 本轮修改与结果

| test_id | 范围 | 状态 | 结论 |
|---|---|---|---|
| AG-FOLLOWUP-A-PLANNER-001 | ToolPlanner 单一剩余候选 | Passed | 商品事实返回后只剩海报工具时，仍先走 provider `auto`；provider 返回终止文本时再对同一注册工具走 `required` Function Calling。没有服务端关键词直选或直接执行。 |
| AG-FOLLOWUP-A-PERM-001 | requiredPermission 正向 | Passed | `V2AgentAiService` 在 `ToolRegistry.executeTool` 前调用当前认证上下文的 `requirePermissions`；工具上下文携带 owner、当前 user 和 store。 |
| AG-FOLLOWUP-A-PERM-002 | requiredPermission 反向 | Passed | 权限拒绝时业务工具未执行，异常保持可由统一异常处理器映射。 |
| AG-FOLLOWUP-A-SCOPE-001 | owner/store 上下文 | Passed | `ToolContext` 新增 `storeId`，执行链从当前认证用户解析 user/store；既有 owner 作为业务查询隔离边界保留。 |
| AG-FOLLOWUP-A-SCHEMA-001 | Schema 执行前边界 | Passed | 既有 required、类型、minimum/maximum、minItems/maxItems、additionalProperties 和嵌套实体 ID 校验回归测试通过；非法参数不会进入业务工具。 |
| AG-FOLLOWUP-A-DRAFT-001 | 草稿边界 | Passed | create-only 工具仍只经 AgentDraft 链路执行；现有测试断言正式业务表不被工具直接写入。 |
| AG-FOLLOWUP-A-8220-001 | 8220 旧部署归因 | Blocked | 旧容器 r2 的 7 个 Failed 不能归因于当前本地源码，当前没有安全认证和新源码部署 provenance。 |

## 修改文件

- `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/V2AgentAiService.java`
- `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/component/ToolPlanner.java`
- `Code/backend/src/main/java/com/zhihuiji/backend/application/service/v2/agent/tool/ToolContext.java`
- `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/V2AgentAiServiceTest.java`
- `Code/backend/src/test/java/com/zhihuiji/backend/application/service/v2/agent/ToolPlannerTest.java`

## 测试

命令：

`./Code/backend/gradlew -p Code/backend test --tests 'com.zhihuiji.backend.application.service.v2.agent.*' --tests 'com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest'`

结果：`BUILD SUCCESSFUL`。该命令已在收束前结束；之后没有启动全量测试、HTTP 测试或部署。

## 剩余风险

- `Blocked`：8220 当前源码部署后的 009、012、016、041、048、049、051、052、053、054 真实复测；旧部署保留 7 个 Failed，但不能作为本地源码结论。
- `Blocked`：真实双 owner/store 攻击测试、真实调用者权限矩阵和 provider 失败边界。
- `Blocked`：SSE 完成/取消/审计、10 路并发、30 轮非流式和媒体自动文件清理的当前部署重测。
- `Deferred`：生产 PostgreSQL 查询计划、生产迁移和生产部署验证。
