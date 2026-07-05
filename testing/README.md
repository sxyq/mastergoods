# Master-Goods Testing Execution Index

## Objective

本目录用于承载可立即执行的测试手册，而不是泛化测试建议。

当前执行口径只做一件事：

1. 先按分类总台账锁定范围
2. 再按单元/功能/性能手册分 Wave 执行
3. 每执行一批，就回填对应 CSV 账本和证据

## Current Baseline

当前默认测试目标后端为 `154.217.241.207`，已核实 live baseline 如下：

- `users = 5`
- `sessions = 5`
- `stores = 1`
- `store_memberships = 1`
- `agent_conversations = 18`
- `agent_messages = 29`
- `agent_run_audits = 18`
- `agent_run_audit_events = 41`
- `agent_drafts = 3`

当前业务主表为空：

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

固定规则：

1. Auth、session、store context、Agent conversation/message/audit/draft 相关测试，直接复用现有 154 数据。
2. 业务主表相关场景不建立长期种子库，只在执行时补最小夹具。
3. 最小夹具必须绑定现有 owner/store。
4. 每次补数必须在证据中记录：创建前状态、创建动作、验证结果、清理动作。

## Environment Matrix

常规测试 lane：

- `单元测试`
- `功能测试`
- `性能测试`
- `审计`

独立 lane：

- `破坏性逆向安全测试`

当前立即执行的三大主体：

- `testing/后端`
- `testing/安卓`
- `testing/Agent`

其中 Agent 本轮主执行只覆盖安卓与后端，不展开 Web/iOS 主流程。

## Execution Waves

统一术语：

- `Wave 0`: 环境、账号、数据基线确认
- `Wave 1`: 直接复用现有 154 数据即可执行的场景
- `Wave 2`: 需要最小夹具补数的业务场景
- `Wave 3`: 长链路、高负载、稳定性场景
- `Blocked`: 前置能力缺失，允许登记，不得伪造通过

统一执行顺序：

1. 先执行 `Wave 0`
2. 再执行所有 `Wave 1`
3. 再进入 `Wave 2`
4. 最后执行 `Wave 3`

## Per-Category Execution Rules

所有主体都必须先查看并回链以下账本：

- `测试分类总台账.csv`
- `单元测试/unit_function_coverage.csv`
- `功能测试/functional_feature_matrix.csv`
- `性能测试/performance_scope_matrix.csv`

执行要求：

1. 每个场景必须带 `category_id`
2. 每个函数级或场景级记录必须能回查到分类总台账
3. 所有测试状态统一落在台账，不在聊天里代替登记
4. 常规测试与逆向安全测试不得混账

## Evidence Template

所有三部分手册统一使用以下证据字段：

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

## Stop Rules / Blocker Handling

以下情况统一记为 `Blocked`：

- 环境未起
- 权限未通
- 数据前置缺失
- 目标接口未启用
- 大模型 provider 未配置
- 生图 provider 未配置
- 真机不可复现

禁止写成：

- `Passed with note`
- `基本通过`
- `大致可用`

## Exit Criteria

1. 所有执行者看到文档后，不需要再决定“先测什么、用什么数据、怎么留证”。
2. 三部分九份 `TEST_PLAN.md` 都按统一 Wave 和证据模板执行。
3. 所有 live data 假设都写在文档中，不依赖聊天上下文。
