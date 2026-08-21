# 后端单元测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

为 Spring Boot 后端建立可直接执行的函数级单元测试手册，按 `BE-UT-*` 分类逐批完成 public 方法、owner 隔离、安全分支、Agent 链路与 migration 验证。

## Current Baseline

当前执行依赖：

- 代码真源：`Code/backend/src/main/java/com/zhihuiji/backend/**`
- 测试真源：`Code/backend/src/test/java/com/zhihuiji/backend/**`
- 数据真源：当前 8220 基线与本地测试夹具；154 只保留历史资料

当前优先级：

- P0：安全/owner 上下文、controller/DTO 契约、service 主链路、repository owner isolation、agent/ai/image provider
- P1：基础公共类、配置/profile、domain entity、storage

特殊说明：

- `BE-UT-07 Flyway SQL` 属于脚本级验证，不进入函数账本覆盖率统计，但仍必须单列执行与留证

## Environment Matrix

- `Wave 0`: 本地 `./Code/backend/gradlew -p Code/backend test`、`./Code/backend/gradlew -p Code/backend jacocoTestReport`、测试 profile、fixture 可用性确认
- `Wave 1`: P0 分类执行
- `Wave 2`: P1 分类执行
- `Wave 3`: Agent 高复杂 fallback、stream、audit 异常分支补齐

推荐命令：

```bash
./Code/backend/gradlew -p Code/backend test
./Code/backend/gradlew -p Code/backend jacocoTestReport
./Code/backend/gradlew -p Code/backend test --tests "com.zhihuiji.backend.application.service.v2.V2AgentAiServiceTest"
./Code/backend/gradlew -p Code/backend test --tests "com.zhihuiji.backend.api.controller.v2.V2AgentControllerTest"
```

## Execution Waves

### Wave 0

执行内容：

1. 确认 `测试分类总台账.csv` 与 `unit_function_coverage.csv` 已同步
2. 确认 JaCoCo 可产出 XML/HTML
3. 确认现有测试类命名与包结构可继续扩展
4. 确认 `BE-UT-07` 所需 migration 测试基座存在

通过标准：

- 能跑通至少一条 controller test
- 能跑通至少一条 service test
- JaCoCo 报告可生成

### Wave 1

执行 P0 分类：

- `BE-UT-02` 认证令牌
- `BE-UT-03` 安全链路与 owner 上下文
- `BE-UT-04` Controller / DTO 契约
- `BE-UT-05` 业务 Service 规则
- `BE-UT-06` Repository 查询
- `BE-UT-09` 同步与导入
- `BE-UT-11` 多租户隔离
- `BE-UT-13` AI/图像 provider 适配

### Wave 2

执行 P1 分类：

- `BE-UT-01` 公共基础类
- `BE-UT-08` 媒体与存储
- `BE-UT-10` 配置与启动 profile
- `BE-UT-12` 领域实体与聚合约束

### Wave 3

执行高复杂补漏：

1. `V2AgentAiService` create-only / unsupported / fallback 分支
2. `SseStreamEmitter` cancel / error / partial flush 分支
3. `RunAuditService` running/completed/blocked/cancelled/failed
4. provider error、stream interruption、empty tool result
5. `BE-UT-07` 所有 migration 脚本校验

## Per-Category Execution Rules

### `BE-UT-01` 公共基础类

- 工具：JUnit 5
- 重点：`ApiResponse`、`ParseUtils`、`PaginationUtils`、状态枚举、`IdGenerator`
- 必测分支：正常值、空值、非法值、边界值、兼容约束
- 命名：`<ClassName>Test`
- ledger 更新：每个 public 方法一行
- 可豁免：无业务逻辑 getter/setter

### `BE-UT-02` 认证令牌

- 工具：JUnit 5 + Mockito
- 重点：token 生成、refresh、expire、session access
- 必测分支：success、expired、tampered、missing session、wrong owner
- 命名：`<ServiceName>Test#should_<behavior>_<condition>`

### `BE-UT-03` 安全链路与 owner 上下文

- 工具：Spring test / Mockito
- 重点：`TokenAuthenticationFilter`、`StorePermissionInterceptor`、`CurrentOwnerService`
- 必测分支：无 token、无权限、跨租户、owner 丢失、store mismatch
- 通过标准：owner 正反例齐全

### `BE-UT-04` Controller / DTO 契约

- 工具：`@WebMvcTest` + `MockMvc`
- 必测分支：success、validation fail、forbidden、not found、owner isolation
- 命名：`<ControllerName>Test`
- 要求：字段名、错误码、response envelope 必须断言

### `BE-UT-05` 业务 Service 规则

- 工具：JUnit 5 + Mockito
- 必测分支：success、invalid input、missing dependency、cross-tenant deny、downstream failure、summary update
- 高优先类：
  - `V2AgentAiService`
  - `V2AgentConversationService`
  - `AgentImageService`
  - `AgentDraftConfirmService`
  - `ToolPlanner`
  - `AnswerSynthesizer`
  - `RunAuditService`
  - `SseStreamEmitter`
  - `LongCatAnthropicClient`

### `BE-UT-06` Repository 查询

- 工具：`@DataJpaTest`
- 必测分支：owner filter、pagination、sort、null/empty filter、唯一约束/FK
- 要求：owner 正反例必须成对存在

### `BE-UT-07` Flyway SQL

- 工具：migration test / schema assertions
- 不进入函数账本覆盖率统计
- 必测内容：表、索引、约束、owner 字段、agent/media schema
- 证据：脚本名 + 断言结果 + 失败 SQL 片段

### `BE-UT-08` 媒体与存储

- 工具：JUnit 5 + mocked storage
- 必测分支：metadata、path、binding、invalid mime、missing asset

### `BE-UT-09` 同步与导入

- 工具：JUnit 5 + integration fixture
- 必测分支：cursor、claim、retry、status transition、duplicate execution

### `BE-UT-10` 配置与启动 profile

- 工具：SpringBootTest
- 必测分支：local/prod guard、feature flag、LLM 开关、image 开关

### `BE-UT-11` 多租户隔离

- 工具：integration test
- 必测分支：owner 正例、跨租户反例、默认 owner、缺 owner

### `BE-UT-12` 领域实体与聚合约束

- 工具：JUnit 5
- 必测分支：默认值、状态流转、派生字段、一致性约束

### `BE-UT-13` AI/图像 provider 适配

- 工具：fixture test
- 必测分支：responses、stream、fallback、provider error、未配置 image provider

## Evidence Template

- `test_id`
- `category_id`
- `wave_id`
- `env`
- `account/store`
- `pre_state`
- `actions`
- `expected`
- `actual`
- `artifacts`
- `cleanup`
- `result`

额外单元测试产出：

- `test_class`
- `test_method`
- `coverage_delta`
- `branch_notes`

## Stop Rules / Blocker Handling

以下情况记为 `Blocked`：

- gradle test 基座无法运行
- fixture 无法构造 owner/store 上下文
- agent provider 真实依赖导致 deterministic test 不可重复
- migration 测试基座缺失

禁止口径：

- “逻辑简单先跳过”
- “大概覆盖到了”

## Exit Criteria

1. `BE-UT-01` 到 `BE-UT-13` 全部进入执行波次。
2. 所有 handwritten public 方法都在 ledger 中有状态。
3. `BE-UT-07` 单独完成脚本级验证并留证。
4. P0 分类全部具备 success + negative branch。
5. Agent backend 关键类完成 fallback、cancel、audit 五态覆盖。
