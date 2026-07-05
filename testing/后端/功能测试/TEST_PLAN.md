# 后端功能测试执行手册

上级真源：

- `../测试分类说明.md`
- `../测试分类总台账.csv`

## Objective

验证后端真实 HTTP 接口在 live data、最小夹具、owner 隔离和 Agent 联动下的完整闭环，并把所有场景拆成可以直接执行的 Wave。

## Current Baseline

直接复用 154 live baseline：

- `users = 5`
- `sessions = 5`
- `stores = 1`
- `store_memberships = 1`
- `agent_conversations = 18`
- `agent_messages = 29`
- `agent_run_audits = 18`
- `agent_run_audit_events = 41`
- `agent_drafts = 3`

当前为空的业务主表：

- `products`
- `customers`
- `suppliers`
- `sale_orders`
- `purchase_orders`
- `finance_records`
- `inventory_snapshots`
- `inventory_ledger`
- `accounts`
- `payments`
- `media_assets`

规则：

1. Auth、session、store context、Agent conversation/message/audit/draft 场景直接使用现有 live 数据。
2. Product、customer、supplier、sales、purchase、finance、inventory、media 场景按测试步骤创建最小夹具。
3. 不单独维护长期业务种子库。

## Environment Matrix

- `Wave 0`: 本地接口可调用、154 可访问、账号与 store context 可用
- `Wave 1`: 现有 154 数据即可执行的场景
- `Wave 2`: 需要最小夹具补数的场景
- `Wave 3`: 长链路、导入同步、压力与回归补证

环境维度：

- local backend
- deployed 154 backend

推荐命令：

```bash
./gradlew bootRun
curl http://127.0.0.1:18080/...
```

## Execution Waves

### Wave 0

前置确认：

1. 登录拿到可用 token
2. 确认当前 owner/store 可读
3. 确认 Agent conversation / draft / audit 数据可查询
4. 记录业务主表为空的初始状态

通过标准：

- 至少一条 auth 接口通过
- 至少一条 store context 接口通过
- 至少一条 Agent 历史接口通过

### Wave 1

只复用 live 数据执行：

- auth
- session
- store context
- agent conversation/message
- agent draft
- agent audit

### Wave 2

最小夹具执行：

- product/customer/supplier
- sales/purchase
- finance/payment
- inventory
- media

### Wave 3

高复杂链路：

- sync/import jobs
- report/dashboard
- agent stream cancel / retry / multimodal
- image generation precheck

## Per-Category Execution Rules

### Auth and account

- 前置：现有用户可登录
- 正向：login / refresh / me / logout
- 反向：wrong credential / expired token / invalid refresh
- 产出：request/response + session row 变化

### Store and permission context

- 前置：现有 `stores = 1`、`store_memberships = 1`
- 正向：读取当前 store / member 上下文
- 反向：无权限 / owner mismatch / missing token
- 要求：明确验证 owner/store 一致性

### Agent conversation / message / audit / draft

- 前置：复用现有 18 个会话、29 条消息、18 条 run audit、3 条 draft
- 正向：list/detail/reload/history readback
- 反向：invalid conversation id / invalid draft id / cross-owner id
- 状态流转：create conversation、send、cancel、draft confirm/cancel、audit readback
- 允许：在当前库继续增量写入新会话、新 run、新 draft
- 必须记录：本轮新增数据

### Product / customer / supplier

- 前置：创建最小夹具
- 正向：create/list/detail/update
- 反向：duplicate / invalid field / cross-owner read/write
- 清理：删除或标记测试夹具

### Sales / purchase

- 前置：补 product/customer/supplier 最小夹具
- 正向：create/detail/status transition
- 反向：invalid relation / wrong owner / wrong status transition
- 清理：保留或回滚需在证据中注明

### Finance / payment / account transfer

- 前置：补 account / payment / order 相关最小夹具
- 正向：create/link/aggregate
- 反向：invalid link / owner mismatch / empty aggregate

### Inventory

- 前置：补 product 与库存相关最小夹具
- 正向：snapshot / ledger / adjustment / count draft
- 反向：不存在商品 / 跨 owner / 非法数量

### Media

- 前置：如主表为空，先补最小上传夹具
- 正向：upload / bind / readback
- 反向：invalid mime / missing asset / wrong binding target

### Reports and dashboards

- 前置：Wave 2 夹具已存在
- 正向：summary / trend / filter
- 反向：空态 / invalid range / owner mismatch

### Sync / import

- 前置：job 入口可用
- 正向：create job / poll / retry
- 反向：bad payload / duplicate retry / illegal status

### Agent image generation

- 先做 precheck：provider 是否已配置
- 若未配置：直接登记 `Blocked`
- 不允许把“接口存在但 provider 未配”记为通过

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

- request sample
- response sample
- affected rows
- created fixture ids

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- 环境未起
- token 不可用
- owner/store 上下文不可读
- 数据前置缺失且无法补最小夹具
- 目标接口未启用
- image provider 未配置

阻塞后动作：

1. 登记阻塞点
2. 保存当前 pre_state
3. 不继续伪造后续通过结果

## Exit Criteria

1. Wave 1 场景全部可直接执行并留证。
2. Wave 2 所有业务域都明确最小夹具策略。
3. 每个业务域都有正向、owner 隔离反向、状态流转三类记录。
4. Agent conversation/message/draft/audit 全链路完成 live data 验证。
5. image generation 若未配置，已按 `Blocked` 收口。
