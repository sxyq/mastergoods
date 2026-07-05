# 安卓性能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

为 Android 客户端建立可以直接执行的性能测试流，覆盖启动、渲染、长对话、图片上传与 Agent 页面关键交互，并固定指标、采集命令、阈值和回归口径。

## Current Baseline

当前性能测试优先关注：

- app startup
- dashboard
- list screens
- agent screen
- media upload
- generated image rendering

现有数据策略：

- Agent 页和历史可复用 154 live 数据
- 业务列表如需非空数据，先在 Wave 2 补最小夹具

## Environment Matrix

- 真实 Android 手机
- API 34 模拟器
- 小屏配置
- local backend
- deployed 154 backend

## Execution Waves

### Wave 0

1. 确认 `benchmark/AppMacrobenchmark.kt` 可运行
2. 确认 `adb shell dumpsys gfxinfo`
3. 确认 `dumpsys meminfo`
4. 确认 `perfetto`
5. 确认 `simpleperf`

### Wave 1

直接复用 Agent live 数据：

- 打开 Agent 页
- 首条 delta 可见时间
- 会话切换

### Wave 2

补最小夹具后执行：

- dashboard open
- product list open
- media upload / preview

### Wave 3

稳定性与 soak：

- 30 turn Agent conversation
- repeated conversation switching
- repeated image upload and preview
- long chat scroll

## Per-Category Execution Rules

### Macrobenchmark

固定项目：

1. app startup
2. dashboard open
3. product list open
4. agent page open
5. send message and wait for first visible delta

每项记录：

- 指标名
- benchmark class / method
- target environment
- threshold
- baseline file path

### Runtime Diagnostics

固定命令：

- `adb shell dumpsys gfxinfo`
- `adb shell dumpsys meminfo`
- `perfetto`
- `simpleperf`

适用场景：

- frame time p95
- jank percent
- memory growth
- CPU usage

### Soak Tests

固定场景：

1. 30-turn agent conversation
2. repeated conversation switching
3. repeated image upload and preview
4. workbench / draft / notification refresh if applicable

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

- `metric_name`
- `threshold`
- `collection_command`
- `baseline_path`
- `trace_path`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- benchmark 模块无法运行
- 设备性能采集命令不可用
- Agent live 数据无法加载
- 最小夹具未准备导致列表场景无法测

## Exit Criteria

1. 启动、Agent 页、首条 delta 都有基线。
2. 长对话、会话切换、图片上传都有 memory / frame 证据。
3. 每项都写清阈值、采集命令和 baseline 存放位置。
