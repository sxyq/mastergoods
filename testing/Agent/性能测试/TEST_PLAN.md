# Agent 性能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

把 Agent 性能测试收缩成“后端 + 安卓”执行手册，固定执行分层、prompt 模板、会话复用策略、采集点和证据格式，直接服务后续开测。

## Current Baseline

直接复用的 live data：

- 现有会话、消息、audit、draft

业务主表为空：

- 涉及业务域补充查询的性能场景，先最小化补夹具

本轮只保留：

- `AG-PT-BE-*`
- `AG-PT-AN-*`

## Environment Matrix

- local backend
- deployed 154 backend
- Android physical device

分层：

1. backend-only latency
2. Android first visible delta latency
3. end-to-end latency

## Execution Waves

### Wave 0

1. 确认 154 Agent 数据可复用
2. 确认 Android 真机可连目标后端
3. 确认 timing capture 方式与日志可用
4. 确认 image provider 是否配置

### Wave 1

直接复用现有数据：

- short text lookup
- history reload
- cancel path
- audit write path

### Wave 2

允许增量写入：

- new conversation + send
- multi-tool lookup
- long history extension

### Wave 3

高复杂：

- multimodal
- image generation precheck
- long 30-turn conversation
- repeated conversation switching

## Per-Category Execution Rules

### 后端服务时延

- 分类：`AG-PT-BE-01`
- prompt 模板：短文本问答
- 会话策略：可复用现有会话
- 采集点：request accepted -> final response

### 后端首事件 / 首 token / 流式完成

- 分类：`AG-PT-BE-02`
- prompt 模板：短文本问答 / 多工具问答
- 会话策略：可复用现有会话或新建最小会话
- 采集点：run started、first tool event、first delta、complete、cancel stop

### 后端工具规划与执行耗时

- 分类：`AG-PT-BE-03`
- prompt 模板：multi-tool business lookup
- 会话策略：允许新增 run
- 采集点：plan cost、single tool、multi-tool chain

### 审计落库时延

- 分类：`AG-PT-BE-04`
- prompt 模板：普通问答、cancel、blocked ask
- 采集点：audit write lag、drop count

### 长历史 / provider fallback

- 分类：`AG-PT-BE-05`
- prompt 模板：30 turn conversation、provider error fallback
- 会话策略：在现有会话上追加消息密度
- 采集点：latency drift、fallback tail latency

### 多模态 / 大结果块序列化

- 分类：`AG-PT-BE-06`
- 前置：若无 media fixture，先补最小夹具
- prompt 模板：image question / evidence-heavy answer
- 采集点：upload、serialization、payload size

### Android 首屏 / 首 delta 可见时延

- 分类：`AG-PT-AN-01`
- prompt 模板：短文本问答
- 会话策略：复用现有会话或新建最小会话
- 采集点：open page -> first visible delta

### Android 工作台 / 列表 / 通知稳定性

- 分类：`AG-PT-AN-02`
- prompt 模板：history reload / draft refresh / workbench refresh
- 采集点：render cost、refresh cost、memory growth

### Android 图片上传与本地预处理

- 分类：`AG-PT-AN-03`
- 前置：最小 media fixture
- 采集点：upload latency、decode、preview

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

- `prompt_template`
- `conversation_strategy`
- `capture_points`
- `metric_snapshot`
- `raw_log_path`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- Android 真机无法连后端
- timing capture 不可用
- live data 不可读取
- multimodal 所需最小夹具无法创建
- image provider 未配置

## Exit Criteria

1. 本轮只执行 `AG-PT-BE-*` 与 `AG-PT-AN-*`。
2. 每项场景都写清 prompt 模板、会话复用策略、采集点。
3. backend-only、Android 可见时延、end-to-end 三层基线齐备。
4. image generation 未配置时，按 `Blocked` 收口，不伪造通过。
