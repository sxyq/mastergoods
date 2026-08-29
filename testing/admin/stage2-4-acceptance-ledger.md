# 管理员后台第 2-4 阶段测试与验收台账

## 记录信息

| 字段 | 内容 |
|---|---|
| 批次 | 管理员后台阶段 2-4 协同验收 |
| 核对日期 | 2026-08-30 |
| 环境 | 本地 macOS、Java 21、Gradle 8.7、H2 测试上下文；Web 使用项目 Node 运行时 |
| 数据 | 单元测试夹具为脱敏对象；没有使用生产凭据或生产数据 |
| 总结 | 阶段 2 部分完成；阶段 3 源码与定向测试已具备；阶段 4 仍不能发布 |

## 测试证据

| 测试编号 | 覆盖内容 | 证据位置/命令 | 结果 |
|---|---|---|---|
| `TEST-ADM-I03` | page=0、负 page、size>200 的分页边界 | `AdminAgentDetailServiceContractTest.messagesKeepLargeConversationIdsAsStringsAndClampPageSize` | 通过 |
| `TEST-ADM-I04` | 大于 JavaScript 安全整数的 conversation ID 保持字符串、非法 ID 拒绝 | `AdminAgentDetailServiceContractTest` | 通过 |
| `TEST-ADM-S08` | 事件读取先确认可见 run，再执行事件查询；store 范围缺字段时拒绝 | `AdminAgentDetailServiceContractTest` | 通过 |
| `TEST-ADM-S11` | 无正文权限时不查询内容；授权内容中的 token/password 等字段脱敏 | `AdminAgentDetailServiceContractTest` | 通过 |
| `TEST-ADM-R05` | 事件序号缺口与重复序号返回 `eventIntegrity=false` | `AdminAgentDetailServiceContractTest` | 通过 |
| `TEST-ADM-I08` | `Last-Event-ID`、`afterSequence` 优先级、非法游标、终态事件 | `AdminAgentSseContractTest` | 通过 |
| `TEST-ADM-F10` | Token 估算来源、输入/输出/总量和运行耗时 | `AdminAgentDetailServiceContractTest` | 通过 |
| `TEST-ADM-I01` | 无管理员身份 | `AdminPrincipalResolverTest`、`AdminAuthorizationBoundaryTest` | 单元层通过；HTTP 401 尚未证明 |
| `TEST-ADM-S03/S04/S05` | 角色权限、owner/store 收窄、观察员写权限拒绝 | `infrastructure/security/admin/*`、阶段 1 台账 | 通过单元层；真实 HTTP 与写库审计未完成 |

## 阶段状态

| 阶段 | 已完成 | 当前可验收项 | 阻塞/未完成项 | 结论 |
|---|---|---|---|---|
| 阶段 2 核心只读 | 总览、用户、门店、运行列表；消息、事件、usage、上下文、草稿、SSE 查询代码和合同测试 | 读取 DTO、分页上限、字符串 ID、序列完整性、内容脱敏单元合同 | `AgentRunAuditEntity` 没有可靠 `actor_user_id/store_id`；store 授权会被服务层拒绝；没有真实数据库多 owner/store 验证；异常身份仍只有单元证据 | 部分完成 |
| 阶段 3 受控管理 | 配置、审计、系统、导出和保留策略 Controller/Service/Repository 已存在；Vue 页面已接入 | 页面路由、API 客户端、迁移和定向合同测试可检查 | 真实 HTTP、版本冲突/幂等、二次确认、写库审计和跨范围环境仍未执行 | 源码完成；环境待验收 |
| 阶段 4 联调交付 | Web 构建通过；管理员定向后端测试通过；V2 bill Controller 测试通过；全仓回归通过 | 静态编译、单元测试、`git diff --check` | 没有目标 PostgreSQL、SSE 长连接、浏览器和恢复演练证据 | 不可发布 |

## 实际命令

```text
./Code/backend/gradlew -p Code/backend test \
  --tests '*AdminAgentDetailServiceContractTest' \
  --tests '*AdminAgentSseContractTest'
结果：通过，10 tests completed。

./Code/backend/gradlew -p Code/backend test \
  --tests '*api.controller.admin.*' \
  --tests '*application.service.admin.*' \
  --tests '*infrastructure.security.admin.*' \
  --tests '*infrastructure.db.AgentRunAuditSchemaStageOneBaselineTest'
结果：通过。

PATH="/Users/sunyiyang/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH" npm run build
执行目录：Code/frontend/web
结果：通过；Vite 输出提示 JS chunk 约 544 kB，属于性能跟踪项。

./Code/backend/gradlew -p Code/backend test --no-parallel --max-workers=1 --rerun-tasks --no-daemon
结果：通过，703 tests completed，`BUILD SUCCESSFUL`。
`AdminControllerTest` 保留 `local` profile 以加载演示 Bean，并通过测试属性覆盖到独立内存 H2，避免共享文件写入冲突。

git diff --check
结果：通过。
```

## 验收判断

阶段 2 的只读投影已具备可重复的单元合同证据，但数据字段缺口和真实数据库范围尚未解决，不能宣告阶段完成。阶段 3 的正式代码和定向测试已具备，真实写入、幂等和审计环境仍待验收。阶段 4 的 Web 构建、定向测试和 703 项全仓回归均通过；目标环境和浏览器长链路未验证，不能进入发布验收。

## 证据限制

- 当前测试没有启动目标 PostgreSQL，也没有执行 `EXPLAIN ANALYZE`、多租户真实数据查询或迁移恢复演练。
- 当前 SSE 测试验证 Controller 的补读参数、事件序列化和终态关闭；没有验证真实网络断线、重连竞态和长连接资源回收。
- 当前代码已有管理员配置写入、管理员审计、导出任务和保留策略接口；真实环境中的幂等、版本冲突、写库审计次数和下载审计仍待执行。
