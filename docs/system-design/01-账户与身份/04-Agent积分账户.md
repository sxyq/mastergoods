# 01.4 — Agent 积分账户详细设计

> Agent 积分属于账户与身份模块。  
> Agent 模块负责模型调用与 Token 美元成本；本模块负责 Merchant 积分账户、换算、余额、流水和 Task 准入。

# 1. 账户归属

当前冻结：

```text
一个 Merchant
→ 一个 Agent Point Account
```

OWNER / STAFF 使用 Agent 时，都消耗所属 Merchant 的积分账户。

# 2. 与 Agent 模块的边界

```mermaid
sequenceDiagram
    participant TASK as Agent Task
    participant MODEL as Agent Model Billing
    participant POINT as Agent Point Account
    participant DB as PostgreSQL

    TASK->>MODEL: 完成所有 Model Call
    MODEL-->>TASK: Task Total USD + Pricing Version

    TASK->>POINT: SettleTask(total_usd)
    POINT->>POINT: 使用 Points Conversion Version 换算
    POINT->>DB: 写 CONSUME Ledger
    POINT->>DB: 更新 Balance
    POINT-->>TASK: Settlement Result
```

Agent 模块不修改积分余额。

# 3. Task 准入

```mermaid
sequenceDiagram
    actor U as OWNER / STAFF
    participant TASK as Agent Service
    participant POINT as Agent Point Account
    participant DB as PostgreSQL

    U->>TASK: 发起新 Task
    TASK->>POINT: Admission Check
    POINT->>DB: 查询 Balance / Account Status
    DB-->>POINT: Current State

    alt Balance > 0 且账户可用
        POINT-->>TASK: Admit
        TASK-->>U: 开始执行
    else Balance <= 0 / Account Disabled
        POINT-->>TASK: Reject
        TASK-->>U: 积分不足 / 账户不可用
    end
```

# 4. 已准入 Task 可把余额扣成负数

```mermaid
sequenceDiagram
    participant TASK as Agent Task
    participant POINT as Point Account
    participant DB as PostgreSQL

    TASK->>POINT: Final Settlement = 180 Points
    POINT->>DB: Load Balance
    DB-->>POINT: 100

    POINT->>DB: Write CONSUME -180
    POINT->>DB: New Balance = -80
    POINT-->>TASK: Settlement Complete

    Note over TASK,POINT: 已完成 Task 不回滚
```

Task 一旦获得准入，就必须允许执行到本次任务完成。

# 5. 后续下发积分自动抵扣负数

```mermaid
sequenceDiagram
    actor SA as SYSTEM_ADMIN
    participant POINT as Agent Point Account
    participant DB as PostgreSQL
    participant AUDIT as Audit

    SA->>POINT: Grant 300 Points
    POINT->>DB: Current Balance = -80
    POINT->>DB: Write GRANT +300
    POINT->>DB: New Balance = 220
    POINT->>AUDIT: 记录管理员、Merchant、数量、原因
    POINT-->>SA: Balance = 220
```

```text
new_balance = old_balance + ledger_amount
```

负数无需单独“还款”。

# 6. 积分流水

至少支持：

```text
GRANT
CONSUME
ADJUST
REFUND
```

建议流水包含：

```text
merchant
task_id（如适用）
type
amount
balance_before
balance_after
reason
operator
conversion_version
created_at
```

# 7. USD → Points 换算

模型的 Input / Output / Cache USD 定价属于 Agent 模块。

账户模块只接收：

```text
Task Total USD
```

再按 SYSTEM_ADMIN 配置的 `USD → Points Conversion Version` 完成积分结算。

历史 Task 保存其使用的 conversion version，不被后续比例修改重算。

# 8. SYSTEM_ADMIN 下发积分

```mermaid
sequenceDiagram
    actor SA as SYSTEM_ADMIN
    participant WEB as Admin Web
    participant POINT as Agent Point Account
    participant DB as PostgreSQL
    participant AUDIT as Audit

    SA->>WEB: 选择 Merchant
    WEB->>POINT: 查询积分账户
    POINT->>DB: Load Balance / Ledger
    DB-->>POINT: Account State
    POINT-->>WEB: 当前余额

    SA->>WEB: 下发积分 + 原因
    WEB->>POINT: Grant
    POINT->>DB: BEGIN
    POINT->>DB: 写 GRANT Ledger
    POINT->>DB: 更新 Balance
    POINT->>AUDIT: 记录操作
    POINT->>DB: COMMIT
    POINT-->>WEB: 完成
```

# 9. 并发 Task

多个 Task 可以在余额 > 0 时近乎同时通过准入，最终导致更大的负余额。

当前原则仍是：**已准入 Task 必须完成。**

后续实现阶段可讨论并发任务数量限制、最低启动积分阈值或预留额度，但不改变负余额 + 后续充值抵扣规则。
