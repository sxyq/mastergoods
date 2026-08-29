# Agent 后端本轮运行时报告

- 日期：2026-08-30
- 分支：`codex/publish-local-updates`
- 本轮目标：local profile、18080、默认模型 `glm-5.3-flash`
- 认证处理：未读取、打印或保存任何 key、Token、Cookie、Authorization、密码或完整认证载荷。

## 配置修改

只修改以下三个文件中的默认模型值：

- `Code/backend/src/main/resources/application.yml`
- `Code/backend/src/main/resources/application-local.yml`
- `Code/backend/src/main/resources/application-prod.yml`

三处均为 `${AGENT_LLM_MODEL:glm-5.3-flash}`。已有 `allowed-models` 修改保持不变；OneAPI 默认地址保持为 `https://oneapi.sxyq27.online/v1`。

## 服务证据

- 启动命令：`./Code/backend/gradlew -p Code/backend bootRun --args='--spring.profiles.active=local --server.port=18080'`
- 结果：`Passed`。Spring Boot `3.2.6`，应用版本 `0.1.0`，Java `21.0.11`；local profile 生效，Tomcat 监听 `18080`，安全过滤链加载。
- 脱敏启动日志：`/tmp/master-goods-agent-foreground.XXXXXX.log`（系统临时目录，未进入 Git）。
- 无认证只读路由：`/v2/agent/conversations`、`/v2/agent/workbench`、`/v2/admin/system/health`、`/v2/admin/agent/usage`、`/actuator/health` 均返回 HTTP `403`。这证明认证边界生效，不能证明 Agent 会话可用。
- 服务已在本轮结束时正常停止，`18080` 监听已清空。

## 测试证据

- `./Code/backend/gradlew -p Code/backend test --tests 'com.zhihuiji.backend.application.service.v2.agent.**'`：`Passed`，构建成功，包含 JaCoCo 报告任务。
- `./Code/backend/gradlew -p Code/backend test`：`Failed`，共 `702` 项，`701` 通过、`1` 失败。失败项为已有 `V2BillDomainControllerTest.purchaseReceiptListReturnsSnakeCaseFields`，位置 `V2BillDomainControllerTest.java:221`，为 `PathNotFoundException`；未修改该业务测试。
- Agent 源码静态盘点：`46` 个只读工具文件 + `15` 个创建工具文件 = `61` 个；此项只代表源码注册规模，不代表真实运行通过。
- 已核对静态边界：ToolPlanner/ToolExecutor 执行门、Loop 终态、上下文压缩、草稿确认、生图确认后 Provider 调用、ToolContext 的 owner/store 作用域与认证边界。

## Agent case 统计

本轮真实 chat/stream case：`0 Passed / 0 Failed / 0 Blocked / 0 Deferred`（未创建会话）。

以下观测项均不能在没有有效开发会话、owner/store 和 Provider 认证条件时执行，因此登记为 `Blocked` 或 `Deferred`，没有用静态代码或历史报告替代真实证据：

- 真实输入提示词、SSE 顺序、正式回答、ToolPlanner/ToolExecutor 实际工具链、run/audit、数据库 before/after、Provider 调用：`Blocked`。
- 61 个工具逐工具真实调用、Loop 终态、上下文压缩流式观测、草稿确认/拒绝、生图 Provider 结果、owner/store 跨租户观测：`Deferred`，待授权会话和可控 Provider/数据库条件。

## 历史边界

`testing/Agent/功能/reports/20260829-agent-live-wave0-start-failed.md` 和 `testing/Agent/安全/reports/20260829-agent-remote-env-probe-01.md` 的历史结果仍按原报告保留；历史启动失败、通过或环境阻塞均未折算为本轮结果。

## 剩余风险

当前 shell 未注入 `AGENT_LLM_API_KEY` 或数据库认证变量，因此不能证明 OneAPI `glm-5.3-flash` 的真实 Provider 响应，也不能完成任何需要登录的 Agent case。全量测试仍有上述既有业务测试失败。
