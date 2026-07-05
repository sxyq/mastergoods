# Agent 功能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

把 Agent 功能测试收缩成“后端链 + 安卓链”两条主链的立即执行手册，明确哪些场景直接复用 154 live data，哪些场景允许增量写入，哪些场景必须记录本轮新增数据。

## Current Baseline

直接复用：

- `agent_conversations = 18`
- `agent_messages = 29`
- `agent_run_audits = 18`
- `agent_run_audit_events = 41`
- `agent_drafts = 3`
- 配套账号、session、store context 已存在

规则：

1. conversation lifecycle、messaging、tool audit、draft flow 优先直接复用现有数据
2. 需要新增会话、新增 run、新增 draft 的场景，允许直接增量写入当前库
3. 如需业务域数据配合而主表为空，则先补最小夹具，并在 evidence 中标明“本轮新增数据”

## Environment Matrix

- backend API level
- Android client
- local backend
- deployed 154 backend

本轮不展开 Web/iOS 主流程，只保留后续批次提示。

## Execution Waves

### Wave 0

1. 确认已有会话、消息、run audit、draft 可读取
2. 确认安卓 Agent 页可访问
3. 确认 backend chat / stream / draft / audit 接口可调

### Wave 1

直接复用 live data：

- conversation list/detail/reload
- message history reload
- draft list/detail
- run audit readback

### Wave 2

允许增量写入：

- new conversation
- send text question
- cancel run
- draft create / confirm / cancel
- stream retry

### Wave 3

高复杂：

- multimodal
- image upload
- text-to-image precheck
- image-to-image precheck
- long conversation continuity

## Per-Category Execution Rules

### 后端链

#### conversation

- 前置：现有会话可读
- 场景：list、detail、reload、delete if supported
- live data：直接复用

#### chat

- 前置：现有会话或新会话
- 场景：plain text ask、follow-up ask
- 允许增量写入：是

#### stream

- 前置：chat 可用
- 场景：ordered event、first delta、complete、retry
- 必须记录：event 序列

#### cancel

- 前置：stream 正在进行
- 场景：user cancel、final run status
- 必须记录：cancel 前后 audit 变化

#### audit

- 前置：已有 run audit
- 场景：readback、event sequence、tool timeline
- live data：直接复用

#### draft

- 前置：已有 draft 或可创建 draft
- 场景：create、list、confirm、cancel
- 允许增量写入：是

#### multimodal

- 前置：如 media_assets 为空，先补最小 media fixture
- 场景：upload image、ask with image、missing asset
- 必须记录：本轮新增 asset

### 安卓链

#### Agent 页入口

- 前置：已登录、store context 可用
- 场景：open page、load conversation list
- live data：直接复用

#### 列表与历史

- 场景：切换会话、重载历史、删除会话 if supported
- live data：直接复用

#### 收流与取消

- 场景：send、receive delta、cancel、retry
- 允许增量写入：是

#### 草稿 / 工作台

- 场景：draft list、confirm、cancel、workbench refresh
- live data：优先复用，必要时增量写入

#### 图片上传与多模态

- 前置：如无 media fixture，先补最小夹具
- 场景：upload、preview、send multimodal、missing image
- 必须记录：本轮新增 media 数据

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

功能测试额外要求：

- `new_data_created`
- `conversation_id`
- `draft_id`
- `run_id`
- `media_asset_id`

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- backend chat / stream 接口不可用
- 安卓 Agent 页不可进入
- live data 不可读取
- multimodal 所需最小夹具无法创建
- image provider 未配置

## Exit Criteria

1. 后端链与安卓链都按 Wave 完成执行定义。
2. Wave 1 场景全部基于 live data 可直接执行。
3. Wave 2/3 所有增量写入都要求记录本轮新增数据。
4. Web/iOS 不进入本轮主执行口径。
