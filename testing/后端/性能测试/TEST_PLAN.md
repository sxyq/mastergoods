# 后端性能测试全覆盖方案

## Objective

建立后端接口、数据库查询、SSE 流、图片处理和 Agent 审计写入的性能基线，并将其纳入发布门槛。

## Scope

重点对象：

- CRUD read endpoints
- report endpoints
- sync/import endpoints
- media upload endpoints
- agent chat endpoints
- agent stream endpoints
- audit event persistence

## Primary Metrics

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

## Scenario Groups

### 1. CRUD API

Measure:

- list page latency
- filtered query latency
- create and update latency

### 2. Report API

Measure:

- sales summary
- sales trend
- profit summary
- refund record ranges

### 3. Agent Non-Streaming

Measure:

- full response latency
- tool duration
- model duration
- audit row write time

### 4. Agent SSE

Measure:

- run started latency
- first tool event latency
- first answer delta latency
- final completion latency
- cancellation response latency
- dropped audit event count

### 5. Media and Image

Measure:

- asset upload latency
- image generation request latency
- reference-image read latency

## Tools

- `k6` or `JMeter` for API load
- database query plans for slow endpoints
- JVM monitoring and GC logs
- custom SSE harness for event timing

## Baseline Targets

Initial release targets:

1. Standard read endpoint p95 under 300 ms
2. Standard write endpoint p95 under 500 ms
3. SSE first event p95 under 1500 ms
4. SSE cancel p95 under 800 ms
5. audit dropped count equals 0

## Run Profiles

- single user smoke
- 10 concurrent users
- 50 concurrent users
- long-running 15 minute agent stream soak

## Deliverables

- raw benchmark logs
- aggregated metric tables
- threshold comparison sheet
- bottleneck notes

## Exit Criteria

1. Every performance-critical backend capability has a baseline.
2. Agent stream and audit write path both have timing evidence.
3. Any regression above threshold is either fixed or explicitly waived.
