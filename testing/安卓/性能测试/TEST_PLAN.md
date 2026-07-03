# 安卓性能测试全覆盖方案

## Objective

为 Android 客户端建立启动、渲染、交互、长对话、图片处理和 Agent 页面性能基线。

## Scope

重点模块：

- app startup
- dashboard
- list screens
- agent screen
- media upload
- generated image rendering

## Metrics

- cold start
- warm start
- hot start
- time to first frame
- frame time p95
- jank percent
- memory growth
- CPU usage
- battery-sensitive long task duration

## Test Types

### 1. Macrobenchmark

Existing basis:

- `benchmark/AppMacrobenchmark.kt`

Extend to include:

- app startup
- dashboard open
- product list open
- agent page open
- send message and wait for first visible delta

### 2. Runtime Diagnostics

Use:

- `adb shell dumpsys gfxinfo`
- `dumpsys meminfo`
- `perfetto`
- `simpleperf`

### 3. Soak Tests

Scenarios:

1. 30-turn agent conversation
2. repeated conversation switching
3. repeated image upload and preview

## Target Thresholds

Initial targets:

1. cold start under 2500 ms
2. agent page render under 700 ms after navigation
3. first visible answer delta under 2500 ms with local backend
4. long chat scroll jank under 5 percent

## Deliverables

- macrobenchmark report
- frame stats
- memory snapshots
- perfetto traces

## Exit Criteria

1. Startup and Agent page each have baseline results.
2. Long-chat and image scenarios have memory evidence.
3. Regressions are compared against stored baseline before release.
