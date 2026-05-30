# Resources 层技术分析

> 路径: `src/main/resources/`

本层包含应用配置文件和数据库迁移脚本。

---

## application.yml

- **文件**: `application.yml`
- **作用**: 主配置文件，定义通用配置和默认 Profile。

### 配置项

| 配置项 | 值 | 作用 | 修改建议 |
|--------|------|------|----------|
| `spring.application.name` | zhihuiji-backend | 应用名称 | 无 |
| `spring.profiles.active` | local | 默认激活本地环境 | 生产环境应改为 prod |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/master_goods | 数据库连接 | 无 |
| `spring.datasource.username` | postgres | 数据库用户名 | **不应硬编码**，应使用环境变量 |
| `spring.datasource.password` | ✏️ `${DB_PASSWORD:zhihuiji}` | 数据库密码 | ✏️ 已使用环境变量注入，但默认值 `zhihuiji` 过弱，生产环境应移除默认值 |
| `spring.datasource.driver-class-name` | org.postgresql.Driver | 驱动类 | 无 |
| `spring.jpa.hibernate.ddl-auto` | validate | DDL 策略 | 合理，使用 Flyway 管理 |
| `spring.jpa.show-sql` | false | 不显示 SQL | 无 |
| `spring.jpa.properties.hibernate.dialect` | org.hibernate.dialect.PostgreSQLDialect | 方言 | 无 |
| `spring.flyway.enabled` | true | 启用 Flyway | 无 |
| `spring.flyway.locations` | classpath:db/migration | 迁移脚本位置 | 无 |
| `agent.llm.enabled` | false | LLM 默认禁用 | 无 |
| `agent.llm.model` | claude-sonnet-4-20250514 | 模型名称 | 无 |
| `agent.llm.max-tokens` | 4096 | 最大输出 token | 无 |
| `agent.llm.temperature` | 0.3 | 温度 | 无 |

---

## application-local.yml

- **文件**: `application-local.yml`
- **作用**: 本地环境配置覆盖。

### 配置项

| 配置项 | 值 | 作用 | 修改建议 |
|--------|------|------|----------|
| `agent.llm.enabled` | true | 本地启用 LLM | 无 |
| `agent.llm.api-key` | ✏️ `${AGENT_LLM_API_KEY:}`（主配置已外部化） | Anthropic API Key | ✏️ 主配置已使用环境变量，仅 application-local.yml 仍有明文 key |
| `agent.llm.base-url` | https://api.anthropic.com | API 地址 | 无 |

### 严重问题

1. ✏️ **API Key 明文仅存在于 application-local.yml**: 主配置 `application.yml` 已使用 `${AGENT_LLM_API_KEY:}` 环境变量外部化，但 `application-local.yml` 仍包含明文 key。应统一使用环境变量，将明文 key 从版本控制中移除。
2. ✏️ **数据库密码弱默认值**: 使用 `${DB_PASSWORD:zhihuiji}` 格式，已支持环境变量注入，但默认值 `zhihuiji` 过弱且不应出现在配置文件中，生产环境应移除默认值。

---

## V1__init.sql

- **文件**: `db/migration/V1__init.sql`
- **作用**: 初始数据库 Schema，创建核心业务表。

### 表结构

| 表名 | 作用 | 主键策略 | 索引 | 修改建议 |
|------|------|----------|------|----------|
| `users` | 用户表 | BIGSERIAL 自增 | phone UNIQUE | 无 |
| `sessions` | 会话表 | BIGSERIAL 自增 | token UNIQUE, refresh_token UNIQUE | 缺少 user_id 索引 |
| `products` | 商品表 | BIGSERIAL 自增 | code UNIQUE | 缺少 name 索引 |
| `customers` | 客户表 | BIGSERIAL 自增 | phone UNIQUE | 无 |
| `sale_orders` | 销售单表 | BIGINT 手动 | created_at, customer_id | 主键非自增，需应用层保证唯一 |
| `sale_order_items` | 销售明细表 | BIGINT 手动 | order_id, product_id, created_at | 同上 |
| `payments` | 支付记录表 | BIGINT 手动 | order_id, created_at | 同上 |
| `purchase_orders` | 采购单表 | BIGINT 手动 | created_at | 同上 |
| `purchase_order_items` | 采购明细表 | BIGINT 手动 | order_id, product_id | 同上 |
| `inventory_adjustments` | 库存调整表 | BIGINT 手动 | product_id, created_at | 同上 |
| `sync_cursors` | 同步游标表 | VARCHAR(128) 手动 | - | 无 |

### 修改建议

1. **sessions 表缺少 user_id 索引**: 查询用户的所有会话需要全表扫描。
2. **products 表缺少 name 索引**: 按名称搜索需要全表扫描。
3. **手动 ID 策略**: sale_orders 等表使用 BIGINT 手动分配主键，应用层使用时间戳+随机数生成，存在碰撞风险。
4. **缺少外键约束**: 所有关联字段（user_id, order_id, product_id 等）均无外键约束，数据一致性靠应用层保证。
5. **时间戳使用 BIGINT**: 所有时间字段使用毫秒时间戳，缺少时区信息。

---

## V2__suppliers_and_pay_orders.sql

- **文件**: `db/migration/V2__suppliers_and_pay_orders.sql`
- **作用**: 新增供应商和付款单表。

### 表结构

| 表名 | 作用 | 主键策略 | 索引 | 修改建议 |
|------|------|----------|------|----------|
| `suppliers` | 供应商表 | BIGSERIAL 自增 | name, phone, updated_at | 无 |
| `pay_orders` | 付款单表 | BIGINT 手动 | created_at, supplier_id, status | 无 |

---

## V3__finance_records.sql

- **文件**: `db/migration/V3__finance_records.sql`
- **作用**: 新增资金流水表。

### 表结构

| 表名 | 作用 | 主键策略 | 索引 | 修改建议 |
|------|------|----------|------|----------|
| `finance_records` | 资金流水表 | BIGINT 手动 | created_at, type, partner_name | 无 |

---

## V4__agent_tasks_and_notifications.sql

- **文件**: `db/migration/V4__agent_tasks_and_notifications.sql`
- **作用**: 新增 Agent 任务和通知表。

### 表结构

| 表名 | 作用 | 主键策略 | 索引 | 修改建议 |
|------|------|----------|------|----------|
| `agent_tasks` | Agent 任务表 | BIGSERIAL 自增 | created_at, (task_type, status) | 无 |
| `agent_notifications` | Agent 通知表 | BIGSERIAL 自增 | created_at, (is_read, is_delivered) | 缺少 task_id 索引 |

### 修改建议

1. **agent_notifications 缺少 task_id 索引**: 按任务查询通知需要全表扫描。

---

## 全局问题与修改建议

1. ✏️ **敏感信息明文**: 主配置已使用环境变量注入（`${AGENT_LLM_API_KEY:}`、`${DB_PASSWORD:zhihuiji}`），但 application-local.yml 仍有明文 API Key，且数据库密码默认值过弱。
2. **缺少生产环境配置**: 无 `application-prod.yml`，生产环境配置缺失。
3. **手动 ID 碰撞风险**: 多个表使用时间戳+随机数生成主键，高并发下可能重复，建议改用 UUID 或雪花算法。
4. **缺少外键约束**: 数据库层面无外键，数据完整性完全依赖应用层。
5. **缺少数据库索引**: sessions.user_id、products.name、agent_notifications.task_id 等缺少索引。
6. **Flyway 迁移脚本不可变**: 已执行的迁移脚本不可修改，新增索引需创建新的迁移脚本。
