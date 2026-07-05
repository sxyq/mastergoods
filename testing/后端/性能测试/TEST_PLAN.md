# 后端性能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

为后端接口、查询、Agent SSE、audit 写入和 media 上传建立可以直接执行的性能基线，并明确每批压测的环境、并发档位、指标和阻塞条件。

## Current Baseline

live data 直接复用：

- account/store/agent 会话类数据可用于 smoke 与 history 场景

业务主表为空：

- Wave 2 及更高复杂度性能场景需要先补最小夹具

image generation 现状：

- 必须先做 provider precheck
- 若未配置，直接登记 `Blocked`

## Environment Matrix

- local backend
- deployed 154 backend

并发档位：

- 1 用户 smoke
- 10 并发
- 50 并发
- 15 分钟 soak

指标：

- p50
- p95
- p99
- error rate
- throughput
- first-byte latency
- first-event latency
- first-token latency
- cancellation latency
- heap growth
- GC pressure

## Execution Waves

### Wave 0

1. 校验本地与 154 环境都可访问
2. 校验压测工具、SSE harness、GC 日志、DB plan 能工作
3. 校验 Agent live 数据可复用
4. 校验 image provider 是否配置

### Wave 1

直接复用现有 live 数据：

- auth/session smoke
- current store/context
- agent non-stream chat
- agent stream
- agent audit write path
- cancel path

### Wave 2

补最小夹具后执行：

- CRUD read/write
- inventory/report
- media upload

### Wave 3

长链路与稳定性：

- 50 并发
- 15 分钟 soak
- 长历史 conversation
- retry / fallback / stream interruption

## Per-Category Execution Rules

### CRUD Read / Write

- 前置：最小业务夹具已存在
- 采集：p50/p95/p99、error rate、throughput
- 通过阈值：
  - 标准读 p95 < 300 ms
  - 标准写 p95 < 500 ms

### Report API

- 前置：Wave 2 已补报表所需夹具
- 采集：p95、rows scanned、query plan
- 要求：若慢查询出现，必须附 explain 证据

### Agent Non-Streaming

- 直接复用现有会话或新增最小会话
- 采集：
  - full response latency
  - tool duration
  - model duration
  - audit row write time

### Agent SSE

- 直接复用现有会话或新增最小会话
- 采集：
  - run started latency
  - first tool event latency
  - first answer delta latency
  - final completion latency
  - cancellation response latency
  - dropped audit event count
- 通过阈值：
  - first event p95 < 1500 ms
  - cancel p95 < 800 ms
  - dropped audit event = 0

### Audit Write

- 场景：stream、cancel、blocked、completed
- 采集：write lag、row count 对齐、drop count

### Media Upload

- 前置：最小 media fixture
- 采集：upload latency、bind latency、readback latency

### Image Generation

- Wave 0 precheck 未通过则 `Blocked`
- 不进入通过率统计

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

性能测试额外要求：

- load profile
- concurrency
- duration
- metric snapshot
- raw log path

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- 压测工具未就绪
- 154 环境不可达
- 必要夹具缺失
- provider 未配置
- SSE harness 无法稳定采集

## Exit Criteria

1. local 与 154 两套环境都有 Wave 1 基线。
2. Agent non-stream、Agent SSE、audit 写入都有 timing evidence。
3. CRUD、report、media 在补夹具后都有基线。
4. image generation 已完成 precheck 并正确归类为通过或 `Blocked`。
