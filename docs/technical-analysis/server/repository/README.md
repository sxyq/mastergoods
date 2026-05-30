# Repository 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/infrastructure/repository/`

本层为 Spring Data JPA Repository 接口，提供数据访问能力。

---

## UserRepository

- **实体**: `UserEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByPhone(phone)` | `Optional<UserEntity>` | 按手机号查询用户 | 无 |

---

## SessionRepository

- **实体**: `SessionEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByTokenAndIsActiveTrue(token)` | `Optional<SessionEntity>` | 按活跃 token 查询 | 无 |
| `findByRefreshTokenAndIsActiveTrue(refreshToken)` | `Optional<SessionEntity>` | 按活跃刷新令牌查询 | 无 |

---

## ProductRepository

- **实体**: `ProductEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByCode(code)` | `Optional<ProductEntity>` | 按编码查询 | 无 |
| `findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(name, code)` | `List<ProductEntity>` | 模糊搜索 | 无 |
| `findByIdForUpdate(id)` | `Optional<ProductEntity>` | 按ID查询并加悲观写锁 | 无 |
| `findByCodeForUpdate(code)` | `Optional<ProductEntity>` | 按编码查询并加悲观写锁 | 无 |

### 修改建议

- `findByIdForUpdate` 和 `findByCodeForUpdate` 使用 `PESSIMISTIC_WRITE` 锁，防止并发修改库存，设计合理。
- 建议增加 `findByStockLessThanEqualSafeStock` 等业务查询方法，减少 Service 层内存过滤。

---

## CustomerRepository

- **实体**: `CustomerEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByPhone(phone)` | `Optional<CustomerEntity>` | 按手机号查询 | 无 |
| `findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(name, phone)` | `List<CustomerEntity>` | 模糊搜索 | 无 |

---

## SupplierRepository

- **实体**: `SupplierEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(name, phone)` | `List<SupplierEntity>` | 模糊搜索 | 无 |
| `existsByPhone(phone)` | `boolean` | 手机号是否存在 | 无 |
| `existsByPhoneAndIdNot(phone, id)` | `boolean` | 手机号是否存在（排除自身） | 无 |

---

## SaleOrderRepository

- **实体**: `SaleOrderEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByCreatedAtBetween(startAt, endAt)` | `List<SaleOrderEntity>` | 按时间范围查询 | 无分页，数据量大时性能堪忧 |

### 修改建议

- 缺少按 status、customerId 等常用条件查询的方法，当前在 Service 层用 `findAll()` + 内存过滤，效率极低。
- 应增加 `findByStatusAndCreatedAtBetween`、`findByCustomerId` 等组合查询。

---

## SaleOrderItemRepository

- **实体**: `SaleOrderItemEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByOrderId(orderId)` | `List<SaleOrderItemEntity>` | 按订单ID查询明细 | 无 |
| `findByCreatedAtBetween(startAt, endAt)` | `List<SaleOrderItemEntity>` | 按时间范围查询 | 无 |

---

## PurchaseOrderRepository

- **实体**: `PurchaseOrderEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByCreatedAtBetween(startAt, endAt)` | `List<PurchaseOrderEntity>` | 按时间范围查询 | 同 SaleOrderRepository |

---

## PurchaseOrderItemRepository

- **实体**: `PurchaseOrderItemEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByOrderId(orderId)` | `List<PurchaseOrderItemEntity>` | 按订单ID查询明细 | 无 |

---

## PaymentRepository

- **实体**: `PaymentEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByOrderId(orderId)` | `List<PaymentEntity>` | 按订单ID查询支付记录 | 无 |
| `findByCreatedAtBetween(startAt, endAt)` | `List<PaymentEntity>` | 按时间范围查询 | 无 |

---

## PayOrderRepository

- **实体**: `PayOrderEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByCreatedAtBetween(startAt, endAt)` | `List<PayOrderEntity>` | 按时间范围查询 | 同 SaleOrderRepository |

---

## FinanceRecordRepository

- **实体**: `FinanceRecordEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| （仅继承 JpaRepository 默认方法） | - | - | **缺少任何自定义查询方法**，所有过滤在 Service 层内存完成 |

### 修改建议

- 应增加 `findByTypeAndCreatedAtBetween`、`findByCategoryContaining` 等数据库级别查询方法。

---

## InventoryAdjustmentRepository

- **实体**: `InventoryAdjustmentEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findByCreatedAtBetween(startAt, endAt)` | `List<InventoryAdjustmentEntity>` | 按时间范围查询 | 无 |

---

## AgentTaskRepository

- **实体**: `AgentTaskEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findTop20ByOrderByCreatedAtDesc()` | `List<AgentTaskEntity>` | 最近20条任务 | 无分页参数 |
| `findFirstByTaskTypeAndStatusInOrderByCreatedAtDesc(taskType, statuses)` | `Optional<AgentTaskEntity>` | 按类型和状态查找最新任务 | 用于防止重复调度 |

---

## AgentNotificationRepository

- **实体**: `AgentNotificationEntity`
- **ID 类型**: `Long`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| `findTop30ByOrderByCreatedAtDesc()` | `List<AgentNotificationEntity>` | 最近30条通知 | 硬编码30条 |
| `findTop30ByIsReadFalseOrderByCreatedAtDesc()` | `List<AgentNotificationEntity>` | 未读通知 | 同上 |
| `findTop30ByIsDeliveredFalseOrderByCreatedAtDesc()` | `List<AgentNotificationEntity>` | 未送达通知 | 同上 |
| `findTop30ByIsReadFalseAndIsDeliveredFalseOrderByCreatedAtDesc()` | `List<AgentNotificationEntity>` | 未读且未送达通知 | 同上 |

---

## SyncCursorRepository

- **实体**: `SyncCursorEntity`
- **ID 类型**: `String`

| 方法 | 返回类型 | 作用 | 修改建议 |
|------|----------|------|----------|
| （仅继承 JpaRepository 默认方法） | - | - | 无 |

---

## 全局问题与修改建议

1. **大量 findAll() + 内存过滤**: `FinanceRecordRepository`、`SaleOrderRepository`、`PayOrderRepository`、`PurchaseOrderRepository` 等缺少条件查询方法，Service 层频繁调用 `findAll()` 后在内存中过滤，数据量大时性能极差。
2. **缺少分页支持**: 所有查询方法返回 `List`，无 `Page` 分页，应引入 `Pageable` 参数。
3. **硬编码 Top N**: `findTop20`、`findTop30` 等硬编码条数，应改为分页参数。
4. **缺少索引利用**: 虽然数据库有索引，但 Repository 方法未充分利用，如缺少 `findByStatus` 等直接查询。
5. **建议引入 Specification/JPA Criteria**: 对于多条件动态查询（如 SaleOrder 的 8 个过滤条件），应使用 JPA Specification 或 QueryDSL。
