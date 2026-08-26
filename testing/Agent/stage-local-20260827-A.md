# Agent 本地测试 Wave 1 阶段报告 A

日期：2026-08-27

## 结果统计

| 范围 | 结果 |
|---|---|
| Agent 相关 Gradle/JUnit 筛选 | 338/338 个测试 Passed；38 个 suite；0 Failed、0 Blocked、0 Deferred |
| 后端全量 Gradle/JUnit | 591 Passed、1 Failed；共 592 个测试 |
| 本阶段执行台账 | 1 Passed、1 Failed、1 Blocked、1 Deferred |

Agent 相关筛选覆盖 40 个 Agent 相关测试文件中可由本地目标筛选命中的组件和集成测试，包括工具、Planner、Registry、上下文、回答、安全、草稿、Repository、迁移 SQL 和控制器 MockMvc。源码盘点确认当前 Agent 有 60 个工具实现，其中 46 个 `READ_ONLY`、14 个 `CREATE_ONLY`；核心链路包含 `ToolPlanner`、`ToolRegistry`、`ToolArgumentsValidator`、`ToolExecutor`、上下文压缩、`AnswerSynthesizer`、`RunAuditService`、草稿确认和门店权限拦截。

## 测试证据

- Agent 目标筛选日志：`testing/.artifacts/2026-08-27-agent-local-A/agent-related-tests.log`
- Agent 目标退出状态：`testing/.artifacts/2026-08-27-agent-local-A/agent-related-tests.status`
- 后端全量日志：`testing/.artifacts/2026-08-27-agent-local-A/backend-all-tests.log`
- 后端全量退出状态：`testing/.artifacts/2026-08-27-agent-local-A/backend-all-tests.status`
- JUnit 汇总：`testing/.artifacts/2026-08-27-agent-local-A/junit-summary.txt`
- JUnit XML：`testing/.artifacts/2026-08-27-agent-local-A/junit/`

全量唯一失败为非 Agent 的 `V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields`：JSON 路径 `data[0].receipt_no` 无值，位置为 `V2BillDomainControllerTest.java:221`。失败 XML 已保留为脱敏测试证据，并同步到问题台账。

## 范围与限制

本阶段只使用本地 Gradle、Java 21、H2 和进程内 mock；未执行服务器 HTTP，未读取凭据，未登录，未请求 `/v2/agent/chat`，未请求 SSE，未创建会话，未写业务数据库。测试实际工具调用、回答、审计和业务数据库变化均为 `none`，因为没有执行 Agent API 链路。

账号/store/permission：`none`。需要真实认证、Provider、账号及 owner/store 作用域的运行 case 记为 `Blocked`；性能、服务器 Agent、SSE 和客户端联调 case 记为 `Deferred`。本阶段未修改 `Code/backend/src/main`、迁移、配置或数据目录。
