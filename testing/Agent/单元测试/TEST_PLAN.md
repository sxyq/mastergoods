# Agent 单元测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

把 Agent 单元测试收缩成“后端 + 安卓”专项执行手册，按 `AG-UT-BE-*` 与 `AG-UT-AN-*` 分类逐批覆盖规划、工具、流式、审计、状态机和结果块解析。

## Current Baseline

本轮主执行范围只保留：

- 后端：`AG-UT-BE-*`
- 安卓：`AG-UT-AN-*`

本轮不展开：

- `AG-UT-WEB-*`
- `AG-UT-IOS-*`

文末可提示后续补齐，但不进入本轮执行波次。

## Environment Matrix

- backend unit tests
- Android local JVM / Robolectric tests

## Execution Waves

### Wave 0

1. 确认 Agent 分类总账与函数账本一致
2. 确认 Android fake SSE 与 backend provider fixture 可复用
3. 确认 `V2AgentAiService` 与 `AgentChatViewModel` 测试基座可跑

### Wave 1

高优先分类：

- `AG-UT-BE-01` 后端编排与规划
- `AG-UT-BE-02` 后端安全与上下文
- `AG-UT-BE-04` 后端只读工具
- `AG-UT-BE-06` 后端会话/草稿/任务持久化
- `AG-UT-BE-07` 后端流式/回答/审计
- `AG-UT-AN-01` 安卓网络与仓储
- `AG-UT-AN-04` 安卓 ViewModel 状态机

### Wave 2

支撑与协议分类：

- `AG-UT-BE-03` 后端工具契约与 DTO
- `AG-UT-BE-05` 后端写工具与草稿创建
- `AG-UT-BE-08` 后端控制器与媒体入口
- `AG-UT-BE-09` 后端 LLM/图像 provider 配置
- `AG-UT-BE-10` 后端管理/演示支撑
- `AG-UT-AN-02` 安卓本地审计与通知存储
- `AG-UT-AN-03` 安卓入口导航与权限分流
- `AG-UT-AN-05` 安卓界面渲染与结果块

### Wave 3

复杂补漏：

1. create-only tool path
2. empty tool result
3. cancellation path
4. parsing failure
5. evidence / audit emission
6. image upload / generation state

## Per-Category Execution Rules

### 后端分类

#### `AG-UT-BE-01` 编排与规划

- 对象：`V2AgentAiService`、`ToolPlanner`
- 必测：happy path、blocked path、fallback path、empty-result path

#### `AG-UT-BE-02` 安全与上下文

- 对象：`SafetyGuard`、owner/store access
- 必测：越权、blocked、owner 丢失、cross-tenant deny

#### `AG-UT-BE-03` 工具契约与 DTO

- 对象：`ToolRegistry`、`ToolSupport`、DTO、schema
- 必测：param decode、result shape、非法工具、bad payload

#### `AG-UT-BE-04` 后端只读工具

- 对象：所有 readonly tools
- 必测：owner scope、result shape、insufficient result、summary/evidence

#### `AG-UT-BE-05` 后端写工具与草稿创建

- 对象：所有 write tools
- 必测：draft payload、param fallback、不落正式表、错误分支

#### `AG-UT-BE-06` 会话/草稿/任务持久化

- 对象：conversation/message/draft/task
- 必测：create/read/update state、confirm/cancel、reload

#### `AG-UT-BE-07` 流式/回答/审计

- 对象：`AnswerSynthesizer`、`SseStreamEmitter`、`RunAuditService`
- 必测：delta 顺序、cancel、blocked、completed、failed、audit emission

#### `AG-UT-BE-08` 控制器与媒体入口

- 对象：`/v2/agent/*`、`/v2/media/*`
- 必测：contract、permission、invalid input、multimodal/image entry

#### `AG-UT-BE-09` LLM/图像 provider 配置

- 对象：`LongCatAnthropicClient`、`AgentImageService`
- 必测：responses、stream、fallback、provider error、未配置 provider

#### `AG-UT-BE-10` 管理/演示支撑

- 对象：admin/demo/task support
- 必测：admin-only branch、demo seed、notification/task support

### 安卓分类

#### `AG-UT-AN-01` 网络与仓储

- 对象：`AgentSseClient`、`AgentV2Repository`
- fake：fake API / fake SSE
- 必测：stream parse、cancel、request mapping、media upload state

#### `AG-UT-AN-02` 本地审计与通知存储

- 对象：audit cache、notification DAO
- 必测：persist、reload、empty state、bad row handling

#### `AG-UT-AN-03` 入口导航与权限分流

- 对象：startup launch、deeplink、main routing
- 必测：launch extra、deeplink、blocked entry

#### `AG-UT-AN-04` ViewModel 状态机

- 对象：`AgentChatViewModel`、draft/workbench ViewModel
- 必测：state before、trigger、state after、error、duplicate guard

#### `AG-UT-AN-05` 界面渲染与结果块

- 对象：result block、markdown、timeline、workbench UI
- 必测：empty、error、partial block、evidence render

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

单元测试额外要求：

- `covered_class`
- `covered_function`
- `test_case`
- `fixture_name`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- provider fixture 无法稳定复现
- fake SSE 无法保证顺序断言
- Android ViewModel 依赖无法隔离
- 工具注册表与真实代码不一致

## Exit Criteria

1. 本轮只执行 `AG-UT-BE-*` 与 `AG-UT-AN-*`。
2. 每个 Agent 函数族至少覆盖 happy、blocked、fallback、cancel、parse fail、empty-result、audit emission。
3. 每个已注册 Agent tool 都有 request / owner / result shape 覆盖。
4. Web/iOS 相关单元测试不进入本轮执行主流程。
