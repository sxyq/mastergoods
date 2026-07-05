# 安卓单元测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

把 Android 多模块工程的单元测试从“覆盖方向”升级为按 `AND-UT-*` 分类执行的函数级手册，优先把网络、SSE、repository、viewmodel、导航与 Agent 状态链路做成可回归基线。

## Current Baseline

代码范围：

- `app`
- `core/common`
- `core/database`
- `core/datastore`
- `core/designsystem`
- `core/model`
- `core/network`
- `data/*`
- `feature/*`

本轮优先：

- `Wave 1`: `AND-UT-03/04/05/09/10/12`
- `Wave 2`: formatter / mapper / datastore / room / Compose / security

## Environment Matrix

- local JVM unit tests
- Robolectric where framework behavior is unavoidable

推荐命令：

```bash
cd master-goods-android
./gradlew test
./gradlew :feature:agent:test
./gradlew :core:network:test
```

## Execution Waves

### Wave 0

1. 确认所有台账已同步
2. 确认 coroutine test、fake repository、fake SSE flow 可用
3. 确认 `app`、`core/network`、`feature/agent` 能单独跑测试

### Wave 1

执行高优先分类：

- `AND-UT-03` 网络拦截器
- `AND-UT-04` 安全 API 调用包装
- `AND-UT-05` SSE 解析与取消
- `AND-UT-09` Repository 行为
- `AND-UT-10` ViewModel 状态机
- `AND-UT-12` 启动与导航解析

### Wave 2

执行基础和支撑分类：

- `AND-UT-01` 纯工具函数
- `AND-UT-02` 序列化契约
- `AND-UT-06` DataStore 与本地会话
- `AND-UT-07` 本地加密与安全存储
- `AND-UT-08` Room DAO 与 Mapper
- `AND-UT-11` Compose 渲染契约
- `AND-UT-13` 运行时安全守卫

### Wave 3

补齐复杂补漏：

1. Agent image upload / generation state
2. result block parsing
3. duplicated trigger guard
4. long stream cancel / retry

## Per-Category Execution Rules

### `AND-UT-01` 纯工具函数

- 对象：`MoneyFormatter`、`TimeFormatter`、`StatusLabels`
- 必测：正常值、空值、边界值、格式稳定性
- 允许：表达式体纯 getter 不单列

### `AND-UT-02` 序列化契约

- 对象：DTO、Agent stream/event/result block
- 必测：encode、decode、unknown field、snake_case 对齐

### `AND-UT-03` 网络拦截器

- fake：fake request chain
- 必测：header 注入、refresh、base url 切换、retry 边界
- 命名：`should_<behavior>_<condition>`

### `AND-UT-04` 安全 API 调用包装

- fake：fake API success/error/timeout/cancel
- 必测：success、HTTP error、业务 error、cancel、timeout

### `AND-UT-05` SSE 解析与取消

- fake：fake SSE event 流
- 必测：event order、partial delta、EOF、cancel、非法包体
- 断言模板：`state before -> feed event -> collected state after`

### `AND-UT-06` DataStore 与本地会话

- fake：in-memory DataStore / test dispatcher
- 必测：读写、默认值、迁移、logout 清理

### `AND-UT-07` 本地加密与安全存储

- 必测：加密、解密、坏密钥、坏密文

### `AND-UT-08` Room DAO 与 Mapper

- fake：in-memory Room
- 必测：owner scope、分页、排序、映射一致性

### `AND-UT-09` Repository 行为

- fake：fake API + fake DAO + fake SSE
- 必测：success、API error、empty data、serialization compatibility
- 允许间接覆盖：私有 helper 通过公开 repository method 断言

### `AND-UT-10` ViewModel 状态机

- 必测模板：
  - `state before`
  - `trigger`
  - `state after`
  - `error path`
  - `duplicate guard`
- 高优先：
  - `feature/agent/AgentChatViewModel`
  - 认证
  - dashboard
  - 业务域关键 ViewModel

### `AND-UT-11` Compose 渲染契约

- 必测：空态、错误态、按钮禁用、result block 结构渲染
- 允许通过上层状态驱动间接覆盖纯小型 UI helper

### `AND-UT-12` 启动与导航解析

- 必测：launch extra、deeplink、Agent 启动参数、权限分流

### `AND-UT-13` 运行时安全守卫

- 必测：debug/release 分支、签名校验、阻断分支

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

- `test_file`
- `test_case`
- `covered_function`
- `indirect_coverage_note`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- fake SSE 无法稳定复现
- Robolectric 环境不支持目标行为
- 关键 ViewModel 依赖无法隔离
- 导航启动参数缺少可构造入口

## Exit Criteria

1. `AND-UT-*` 全部进入执行波次。
2. `Wave 1` 六类全部具备 success + negative path。
3. Agent Android 状态链路覆盖本地插入、切换、收流、取消、草稿、图片状态。
4. 每个 handwritten repository 与 ViewModel 函数都有 ledger 状态。
