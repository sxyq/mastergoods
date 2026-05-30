# Entity 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/domain/entity/`

本层为 JPA 实体，映射数据库表。所有实体使用 `Long` 类型时间戳（毫秒）而非 `LocalDateTime`。

---

## UserEntity

- **表名**: `users`
- **ID 策略**: `IDENTITY`（自增）

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `phone` | phone | String(32) | NOT NULL, UNIQUE | 手机号 | 无 |
| `passwordHash` | password_hash | String(128) | NOT NULL | 密码哈希 | 无 |
| `nickname` | nickname | String(64) | NOT NULL | 昵称 | 无 |
| `status` | status | Integer | NOT NULL | 状态（0=停用, 1=启用） | 可改用枚举 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## SessionEntity

- **表名**: `sessions`
- **ID 策略**: `IDENTITY`

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `userId` | user_id | Long | NOT NULL | 用户 ID | 缺少外键约束 |
| `token` | token | String(128) | NOT NULL, UNIQUE | 访问令牌 | 无 |
| `refreshToken` | refresh_token | String(128) | NOT NULL, UNIQUE | 刷新令牌 | 无 |
| `expiresAt` | expires_at | Long | NOT NULL | 过期时间戳 | 无 |
| `isActive` | is_active | Boolean | NOT NULL | 是否活跃 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## ProductEntity

- **表名**: `products`
- **ID 策略**: `IDENTITY`
- **校验**: `@NotBlank`(code, name, category, unit), `@NotNull`(salePrice, purchasePrice, stock, safeStock, status)

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `code` | code | String(64) | NOT NULL, UNIQUE | 商品编码 | 无 |
| `name` | name | String(128) | NOT NULL | 商品名称 | 无 |
| `category` | category | String(64) | NOT NULL | 分类 | 无 |
| `unit` | unit | String(32) | NOT NULL | 单位 | 无 |
| `salePrice` | sale_price | Double | NOT NULL | 售价 | 应改用 BigDecimal |
| `purchasePrice` | purchase_price | Double | NOT NULL | 进价 | 应改用 BigDecimal |
| `stock` | stock | Double | NOT NULL | 当前库存 | 应改用 BigDecimal |
| `safeStock` | safe_stock | Double | NOT NULL | 安全库存 | 应改用 BigDecimal |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## CustomerEntity

- **表名**: `customers`
- **ID 策略**: `IDENTITY`

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `name` | name | String(128) | NOT NULL | 名称 | 无 |
| `phone` | phone | String(32) | NOT NULL, UNIQUE | 手机号 | 无 |
| `level` | level_value | Integer | NOT NULL | 等级 | 字段名与列名不一致（level vs level_value），易混淆 |
| `address` | address | String(255) | - | 地址 | 无 |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `balance` | balance | Double | NOT NULL | 余额（应收） | 应改用 BigDecimal |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## SupplierEntity

- **表名**: `suppliers`
- **ID 策略**: `IDENTITY`

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `name` | name | String(128) | NOT NULL | 名称 | 无 |
| `phone` | phone | String(32) | NOT NULL, UNIQUE | 手机号 | 无 |
| `address` | address | String(255) | - | 地址 | 无 |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `balance` | balance | Double | NOT NULL | 余额（应付） | 应改用 BigDecimal |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## SaleOrderEntity

- **表名**: `sale_orders`
- **ID 策略**: 手动分配（无 @GeneratedValue）

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 手动 ID 生成策略，存在碰撞风险 |
| `orderNo` | order_no | String(64) | NOT NULL, UNIQUE | 单号 | 无 |
| `customerId` | customer_id | Long | - | 客户 ID | 缺少外键约束 |
| `customerName` | customer_name | String(128) | - | 客户名称（冗余） | 数据冗余，需保证与 Customer 同步 |
| `subtotalAmount` | subtotal_amount | Double | NOT NULL | 小计 | 应改用 BigDecimal |
| `discountAmount` | discount_amount | Double | NOT NULL | 折扣 | 应改用 BigDecimal |
| `totalAmount` | total_amount | Double | NOT NULL | 总金额 | 应改用 BigDecimal |
| `paidAmount` | paid_amount | Double | NOT NULL | 已付金额 | 应改用 BigDecimal |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## SaleOrderItemEntity

- **表名**: `sale_order_items`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同 SaleOrderEntity |
| `orderId` | order_id | Long | NOT NULL | 销售单 ID | 缺少外键约束 |
| `productId` | product_id | Long | NOT NULL | 商品 ID | 缺少外键约束 |
| `productCode` | product_code | String(64) | NOT NULL | 商品编码（冗余） | 冗余字段 |
| `productName` | product_name | String(128) | NOT NULL | 商品名称（冗余） | 冗余字段 |
| `customerId` | customer_id | Long | - | 客户 ID | 冗余字段 |
| `customerName` | customer_name | String(128) | - | 客户名称（冗余） | 冗余字段 |
| `quantity` | quantity | Double | NOT NULL | 数量 | 应改用 BigDecimal |
| `unitPrice` | unit_price | Double | NOT NULL | 单价 | 应改用 BigDecimal |
| `amount` | amount | Double | NOT NULL | 金额 | 应改用 BigDecimal |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## PurchaseOrderEntity

- **表名**: `purchase_orders`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `orderNo` | order_no | String(64) | NOT NULL, UNIQUE | 单号 | 无 |
| `supplierName` | supplier_name | String(128) | NOT NULL | 供应商名称 | 缺少 supplierId 关联 |
| `totalAmount` | total_amount | Double | NOT NULL | 总金额 | 应改用 BigDecimal |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## PurchaseOrderItemEntity

- **表名**: `purchase_order_items`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `orderId` | order_id | Long | NOT NULL | 采购单 ID | 缺少外键约束 |
| `productId` | product_id | Long | NOT NULL | 商品 ID | 缺少外键约束 |
| `productCode` | product_code | String(64) | NOT NULL | 商品编码（冗余） | 冗余字段 |
| `productName` | product_name | String(128) | NOT NULL | 商品名称（冗余） | 冗余字段 |
| `quantity` | quantity | Double | NOT NULL | 数量 | 应改用 BigDecimal |
| `unitCost` | unit_cost | Double | NOT NULL | 单价 | 应改用 BigDecimal |
| `amount` | amount | Double | NOT NULL | 金额 | 应改用 BigDecimal |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## PaymentEntity

- **表名**: `payments`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `orderId` | order_id | Long | NOT NULL | 销售单 ID | 缺少外键约束 |
| `amount` | amount | Double | NOT NULL | 金额 | 应改用 BigDecimal |
| `method` | method | Integer | NOT NULL | 支付方式 | 可改用枚举 |
| `referenceNo` | reference_no | String(128) | - | 参考号 | 无 |
| `type` | type | Integer | NOT NULL | 类型（1=收款, 2=退款） | 可改用枚举 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## PayOrderEntity

- **表名**: `pay_orders`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `orderNo` | order_no | String(64) | NOT NULL, UNIQUE | 单号 | 无 |
| `supplierId` | supplier_id | Long | - | 供应商 ID | 缺少外键约束 |
| `supplierName` | supplier_name | String(128) | NOT NULL | 供应商名称 | 冗余字段 |
| `amount` | amount | Double | NOT NULL | 金额 | 应改用 BigDecimal |
| `method` | method | Integer | NOT NULL | 付款方式 | 可改用枚举 |
| `referenceNo` | reference_no | String(128) | - | 参考号 | 无 |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `status` | status | Integer | NOT NULL | 状态 | 可改用枚举 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## FinanceRecordEntity

- **表名**: `finance_records`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `recordNo` | record_no | String(64) | NOT NULL, UNIQUE | 流水号 | 无 |
| `type` | type | Integer | NOT NULL | 类型（1=收入, 2=支出） | 可改用枚举 |
| `category` | category | String(64) | NOT NULL | 分类 | 无 |
| `partnerName` | partner_name | String(128) | - | 往来对象名称 | 无 |
| `amount` | amount | Double | NOT NULL | 金额 | 应改用 BigDecimal |
| `method` | method | Integer | NOT NULL | 收支方式 | 可改用枚举 |
| `notes` | notes | String(255) | - | 备注 | 无 |
| `syncStatus` | sync_status | Integer | NOT NULL | 同步状态 | 无 |
| `syncVersion` | sync_version | Long | NOT NULL | 同步版本号 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## InventoryAdjustmentEntity

- **表名**: `inventory_adjustments`
- **ID 策略**: 手动分配

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK | 主键 | 同上 |
| `productId` | product_id | Long | NOT NULL | 商品 ID | 缺少外键约束 |
| `productCode` | product_code | String(64) | NOT NULL | 商品编码（冗余） | 冗余字段 |
| `productName` | product_name | String(128) | NOT NULL | 商品名称（冗余） | 冗余字段 |
| `quantity` | quantity | Double | NOT NULL | 调整数量 | 应改用 BigDecimal |
| `flowType` | flow_type | Integer | NOT NULL | 流向（0=出库, 1=入库） | 可改用枚举 |
| `reason` | reason | String(255) | - | 调整原因 | 无 |
| `operatorName` | operator_name | String(128) | - | 操作人 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## AgentTaskEntity

- **表名**: `agent_tasks`
- **ID 策略**: `IDENTITY`

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `taskType` | task_type | String(64) | NOT NULL | 任务类型 | 可改用枚举 |
| `title` | title | String(128) | NOT NULL | 标题 | 无 |
| `triggerSource` | trigger_source | String(32) | NOT NULL | 触发来源 | 可改用枚举 |
| `status` | status | String(32) | NOT NULL | 状态（queued/running/completed/failed） | 可改用枚举 |
| `progress` | progress | Integer | NOT NULL | 进度百分比 | 无 |
| `inputText` | input_text | String(1000) | - | 输入文本 | 无 |
| `resultJson` | result_json | TEXT | - | 结果 JSON | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |
| `completedAt` | completed_at | Long | - | 完成时间戳 | 无 |

---

## AgentNotificationEntity

- **表名**: `agent_notifications`
- **ID 策略**: `IDENTITY`

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `id` | id | Long | PK, 自增 | 主键 | 无 |
| `taskId` | task_id | Long | - | 关联任务 ID | 缺少外键约束 |
| `title` | title | String(128) | NOT NULL | 标题 | 无 |
| `body` | body | String(1000) | NOT NULL | 内容 | 无 |
| `level` | level | String(32) | NOT NULL | 级别（info/warning/error） | 可改用枚举 |
| `isRead` | is_read | Boolean | NOT NULL | 是否已读 | 无 |
| `isDelivered` | is_delivered | Boolean | NOT NULL | 是否已送达 | 无 |
| `createdAt` | created_at | Long | NOT NULL | 创建时间戳 | 无 |

---

## SyncCursorEntity

- **表名**: `sync_cursors`
- **ID 策略**: 以 `clientId` 为主键（非自增）

| 字段 | 列名 | 类型 | 约束 | 作用 | 修改建议 |
|------|------|------|------|------|----------|
| `clientId` | client_id | String(128) | PK | 客户端标识 | 无 |
| `lastCursor` | last_cursor | String(128) | - | 上次同步游标 | 无 |
| `updatedAt` | updated_at | Long | NOT NULL | 更新时间戳 | 无 |

---

## 全局问题与修改建议

1. **金额字段统一使用 Double**: 所有金额/数量字段使用 `Double`，存在精度丢失风险，应统一改用 `BigDecimal`。
2. **缺少外键约束**: 所有关联字段（orderId, productId, customerId, supplierId, taskId, userId）均无 JPA 外键映射，数据一致性靠应用层保证。
3. **ID 生成策略不一致**: 部分实体使用 `IDENTITY` 自增，部分手动分配 UUID，策略不统一。
4. **冗余字段过多**: 商品编码/名称、客户名称等在订单明细中冗余存储，虽有助于快照但增加维护成本。
5. **状态字段缺少枚举**: 所有 status/type 字段使用 Integer/String，缺少类型安全。
6. **时间戳使用 Long**: 统一使用毫秒时间戳，缺少时区信息，建议迁移到 `Instant` 或 `OffsetDateTime`。
7. **缺少审计字段**: 无通用基类（如 `BaseEntity`），createdAt/updatedAt 在每个实体中重复定义。
