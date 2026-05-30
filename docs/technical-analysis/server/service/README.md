# Service 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/application/service/`

本层为核心业务逻辑层，采用事务性服务模式，每个 Service 对应一个业务领域。

---

## AuthService

- **文件**: `AuthService.java`
- **作用**: 用户认证与令牌管理。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `userRepository` | `UserRepository` | 用户数据访问 | 无 |
| `sessionRepository` | `SessionRepository` | 会话数据访问 | 无 |
| `tokenService` | `TokenService` | 令牌生成与验证 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `register(phone, password, verifyCode)` | 用户注册 | **verifyCode 未实际校验**，任何验证码均可注册；密码直接 BCrypt 哈希，合理 |
| `login(phone, password)` | 用户登录 | 返回 token + refreshToken；登录成功后未限制旧会话数量 |
| `refreshToken(refreshToken)` | 刷新令牌 | 旧 token 未失效，可能存在并发会话问题 |
| `logout(token)` | 登出 | 仅将 session 标记为 inactive，token 本身仍有效直到过期 |
| `validateToken(token)` | 验证令牌 | 返回 UserEntity；每次请求都查库，建议引入缓存 |
| ~~`generateOrderNo(prefix)`~~ | ~~生成单号~~ | ❌ **AuthService 中不存在此方法**。单号生成实际位于 SaleOrderService(第58行)、PurchaseOrderService(第42行)、PayOrderService(第154-158行)，使用 `UUID.randomUUID()` 是线程安全的，但 `substring(0,4~6)` 降低了唯一性 |

---

## AdminService

- **文件**: `AdminService.java`
- **Profile**: `local`
- **作用**: 后台管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `userRepository` | `UserRepository` | 用户数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `purchaseOrderRepository` | `PurchaseOrderRepository` | 采购单数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `demoDataService` | `DemoDataService` | 演示数据服务 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `summary()` ✏️ ~~`getSummary()`~~ | 获取各实体计数 | 6 次 count 查询，可合并为单条 SQL |
| `searchUsers(keyword)` | 搜索用户 | 使用内存过滤，应改用数据库查询 |
| `createUser(phone, password, nickname)` | 创建用户 | 无 |
| `updateUser(userId, phone, nickname, status)` | 更新用户 | 无 |

---

## CustomerService

- **文件**: `CustomerService.java`
- **作用**: 客户管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `list(keyword)` ✏️ ~~`search(keyword)`~~ | 搜索客户 | 无 |
| `getById(id)` | 获取客户详情 | 无 |
| `create(entity)` | 创建客户 | **直接接收 Entity**，应在 Service 层做 DTO 转换 |
| `update(id, entity)` | 更新客户 | 同上 |
| `delete(id)` | 删除客户 | 物理删除，无关联检查（如有未结清订单会出问题） |

---

## SupplierService

- **文件**: `SupplierService.java`
- **作用**: 供应商管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `list(keyword, status)` ✏️ ~~`search(keyword, status)`~~ | 搜索供应商 | status 过滤在内存中完成，应下推到数据库 |
| `getById(id)` | 获取供应商详情 | 无 |
| `create(entity)` | 创建供应商 | 手机号唯一性检查后保存，合理 |
| `update(id, entity)` | 更新供应商 | 同上 |
| `delete(id)` | 删除供应商 | 物理删除，无关联检查 |

---

## ProductService

- **文件**: `ProductService.java`
- **作用**: 商品管理与库存调整服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `inventoryAdjustmentRepository` | `InventoryAdjustmentRepository` | 库存调整记录 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `list(keyword)` ✏️ ~~`search(keyword)`~~ | 搜索商品 | 无 |
| `getById(id)` | 获取商品详情 | 无 |
| `getByCode(code)` | 按编码查询 | 无 |
| `create(entity)` | 创建商品 | 编码唯一性检查后保存 |
| `update(id, entity)` | 更新商品 | 同上 |
| `adjustStock(id, delta, reason, operator)` | 调整库存 | 使用悲观锁（findByIdForUpdate），事务保证一致性，设计合理 |
| `delete(id)` | 删除商品 | 物理删除，无关联检查 |

---

## SaleOrderService

- **文件**: `SaleOrderService.java`
- **作用**: 销售单管理服务，包含创建、收款、状态流转、取消和 PDF 导出。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `saleOrderItemRepository` | `SaleOrderItemRepository` | 销售明细数据访问 | 无 |
| `paymentRepository` | `PaymentRepository` | 支付记录数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `authService` | `AuthService` | 单号生成 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `create(customerId, customerName, items, notes, discountAmount)` | 创建销售单 | 扣减库存使用悲观锁，合理；~~**未更新客户余额**~~ ❌ 实际代码(第110-118行)已正确更新客户余额 |
| `list(keyword, status, customerId, createdAfter, createdBefore, paidStatus, limit, offset)` ✏️ ~~`search(...)`~~ | 多条件搜索 | **全量查询后内存过滤**，性能极差 |
| `getById(id)` | 获取订单详情 | 无 |
| `updateDraft(id, discountAmount, notes)` | 更新草稿 | 仅草稿状态可修改 |
| `addPayment(orderId, amount, method, referenceNo)` | 添加收款 | 更新已付金额和客户余额，合理 |
| `listPayments(orderId)` | 查询收款记录 | 无 |
| `updateStatus(orderId, status)` | 更新状态 | 状态机校验不完整 |
| `cancel(orderId)` | 取消订单 | ~~**取消时未恢复库存**~~ ❌ 实际代码(第292-301行)已正确恢复库存：遍历订单项、使用 findByIdForUpdate 悲观锁、加回库存，并正确扣减客户余额和创建退款记录 |
| `exportPdf(orderId)` | 导出 PDF | 极简实现，仅输出文本行 |

### 严重问题

1. ~~**取消订单未恢复库存**~~ ❌ 实际代码已正确恢复库存（见上方 cancel 方法说明），此为误报
2. **list 性能极差**: 全量 `findAll()` 后在内存中过滤 8 个条件，数据量大时 OOM 风险

---

## PurchaseOrderService

- **文件**: `PurchaseOrderService.java`
- **作用**: 采购单管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `purchaseOrderRepository` | `PurchaseOrderRepository` | 采购单数据访问 | 无 |
| `purchaseOrderItemRepository` | `PurchaseOrderItemRepository` | 采购明细数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `authService` | `AuthService` | 单号生成 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `create(supplierName, items, notes, status)` | 创建采购单 | 入库时使用悲观锁增加库存，合理 |
| `list(keyword, status)` ✏️ ~~`search(keyword, status)`~~ | 搜索采购单 | **全量查询后内存过滤** |
| `getById(id)` | 获取采购单详情 | 无 |

---

## PayOrderService

- **文件**: `PayOrderService.java`
- **作用**: 付款单管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `payOrderRepository` | `PayOrderRepository` | 付款单数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `authService` | `AuthService` | 单号生成 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `create(supplierId, supplierName, amount, method, referenceNo, notes, status)` | 创建付款单 | 已付状态时更新供应商余额 |
| `list(keyword, status, createdAfter, createdBefore)` ✏️ ~~`search(keyword, status, createdAfter, createdBefore)`~~ | 搜索付款单 | **全量查询后内存过滤** |
| `getById(id)` | 获取付款单详情 | 无 |
| `updateStatus(id, status)` | 更新状态 | 状态变更时更新供应商余额 |

---

## FinanceRecordService

- **文件**: `FinanceRecordService.java`
- **作用**: 资金流水管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `financeRecordRepository` | `FinanceRecordRepository` | 资金流水数据访问 | 无 |
| `authService` | `AuthService` | 单号生成 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `create(type, category, partnerName, amount, method, notes)` | 创建资金流水 | 无 |
| `list(keyword, type, createdAfter, createdBefore)` ✏️ ~~`search(keyword, type, createdAfter, createdBefore)`~~ | 搜索资金流水 | **全量查询后内存过滤** |

---

## ReportService

- **文件**: `ReportService.java`
- **作用**: 报表数据聚合服务，提供销售、利润、退款、库存、对账等多种报表。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `saleOrderItemRepository` | `SaleOrderItemRepository` | 销售明细数据访问 | 无 |
| `purchaseOrderRepository` | `PurchaseOrderRepository` | 采购单数据访问 | 无 |
| `purchaseOrderItemRepository` | `PurchaseOrderItemRepository` | 采购明细数据访问 | 无 |
| `paymentRepository` | `PaymentRepository` | 支付记录数据访问 | 无 |
| `payOrderRepository` | `PayOrderRepository` | 付款单数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `inventoryAdjustmentRepository` | `InventoryAdjustmentRepository` | 库存调整数据访问 | 无 |
| `financeRecordRepository` | `FinanceRecordRepository` | 资金流水数据访问 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `salesSummary(startAt, endAt)` | 销售汇总 | 全量加载后内存聚合，应改用 SQL 聚合 |
| `profitSummary(startAt, endAt)` | 利润汇总 | 同上 |
| `refundRecords(startAt, endAt, limit)` | 退款记录 | 同上 |
| `stockOutRecords(startAt, endAt, limit)` | 出库记录 | 同上 |
| `topProducts(startAt, endAt, limit)` | 热销商品 | 同上 |
| `profitByProducts(startAt, endAt, limit)` | 商品利润 | 同上 |
| `profitByCustomers(startAt, endAt, limit)` | 客户利润 | 同上 |
| `inventoryFlow(startAt, endAt, limit)` | 库存流水 | 同上 |
| `customerSales(startAt, endAt, limit)` | 客户销售 | 同上 |
| `topReceivableCustomers(limit)` | 应收客户 | 同上 |
| `lowStockProducts(limit)` | 低库存商品 | 同上 |
| `reconciliationSummary(startAt, endAt)` | 对账汇总 | 同上 |

### 严重问题

1. **全量内存聚合**: 所有报表方法均先全量加载再内存计算，数据量大时性能极差且可能 OOM。
2. **应改用 SQL 聚合**: `SUM`、`GROUP BY`、`ORDER BY` 等操作应下推到数据库层。
3. **注入了 11 个 Repository**: 违反单一职责原则，建议拆分为 `SalesReportService`、`InventoryReportService`、`FinanceReportService` 等。

---

## AgentService

- **文件**: `AgentService.java`
- **作用**: Agent 工作台数据聚合服务（非 LLM 驱动）。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `reportService` | `ReportService` | 报表服务 | 无 |
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `workbench(windowDays, limit, agingDays)` | 工作台总览 | 聚合多个数据源，逻辑合理 |
| `reconciliationFollowup(limit, agingDays)` | 对账催办 | 同上 |
| `reportInsight(windowDays)` | 报表洞察 | 同上 |
| `alerts(limit, agingDays)` | 预警仪表盘 | 同上 |

---

## AgentLlmService

- **文件**: `AgentLlmService.java`
- **作用**: LLM API 封装服务，负责与 Anthropic Claude 交互。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `anthropicClient` | `LongCatAnthropicClient` | Anthropic API 客户端 | 无 |
| `objectMapper` | `ObjectMapper` | JSON 序列化 | 无 |
| `properties` | `AgentLlmProperties` | LLM 配置属性 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `query(userMessage, systemPrompt)` | 通用 LLM 问答 | 返回原始文本响应 |
| `queryStructured(userMessage, systemPrompt, responseType)` | 结构化 LLM 问答 | 使用 JSON mode 解析为指定类型 |
| `buildSystemPrompt(context)` | 构建系统提示词 | 拼接业务上下文（商品/客户/供应商摘要） |
| ~~`buildBusinessContext()`~~ | ~~构建业务上下文~~ | ❌ **AgentLlmService 中不存在此方法**。全量加载逻辑内联于 LlmDrivenAgentService 的 `getWorkbench()` 和 `draftOperation()` 中 |
| `buildContextSummary(products, customers, suppliers)` | 上下文摘要 | 截取前 50 条，硬编码 |

### 修改建议

1. ~~**buildBusinessContext 全量加载**~~ ❌ 此方法不存在于 AgentLlmService，全量加载逻辑内联于 LlmDrivenAgentService 的 `getWorkbench()` 和 `draftOperation()` 中，应改为按需查询或缓存
2. **Token 消耗无监控**: 建议增加 token 使用量统计和告警。
3. **无重试机制**: LLM 调用失败无重试，建议增加指数退避重试。

---

## LlmDrivenAgentService

- **文件**: `LlmDrivenAgentService.java`
- **作用**: LLM 驱动的 Agent 服务，提供问答、草稿生成与提交。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `agentService` | `AgentService` | 工作台数据服务 | 无 |
| `agentLlmService` | `AgentLlmService` | LLM 服务 | 无 |
| `saleOrderService` | `SaleOrderService` | 销售单服务 | 无 |
| `purchaseOrderService` | `PurchaseOrderService` | 采购单服务 | 无 |
| `payOrderService` | `PayOrderService` | 付款单服务 | 无 |
| `financeRecordService` | `FinanceRecordService` | 资金流水服务 | 无 |
| `agentTaskService` | `AgentTaskService` | Agent 任务服务 | 无 |
| `objectMapper` | `ObjectMapper` | JSON 处理 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `workbench(windowDays, limit, agingDays)` | 工作台总览 | 委托给 AgentService |
| `reconciliationFollowup(limit, agingDays)` | 对账催办 | 同上 |
| `reportInsight(windowDays)` | 报表洞察 | 同上 |
| `alerts(limit, agingDays)` | 预警仪表盘 | 同上 |
| `query(query)` | Agent 问答 | 调用 LLM 解析意图并返回结果 |
| `operationDraft(instruction)` | 生成操作草稿 | 调用 LLM 生成草稿 |
| ~~`operationSubmit(draft)`~~ | ~~提交操作草稿~~ | ✏️ 实际方法名为 `AgentService.submitDraft(draft)`，非 LlmDrivenAgentService 的方法；根据 operationType 路由到不同 Service |

### 修改建议

1. ~~**operationSubmit 路由逻辑**~~ ✏️ 实际方法为 `AgentService.submitDraft()`，使用 if-else 链判断 operationType，建议改用策略模式
2. **LLM 幻觉风险**: 草稿提交时未做充分校验，LLM 生成的草稿可能包含非法数据。
3. **无并发控制**: 多个用户同时提交相同草稿可能导致数据冲突。

---

## AgentTaskService

- **文件**: `AgentTaskService.java`
- **作用**: Agent 任务与通知管理服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `agentTaskRepository` | `AgentTaskRepository` | 任务数据访问 | 无 |
| `agentNotificationRepository` | `AgentNotificationRepository` | 通知数据访问 | 无 |
| `agentLlmProperties` | `AgentLlmProperties` | LLM 配置 | 无 |
| `agentLlmService` | `AgentLlmService` | LLM 服务 | 无 |
| `llmDrivenAgentService` | `LlmDrivenAgentService` | Agent 服务 | ~~**循环依赖风险**~~ ❌ 实际为单向依赖：AgentTaskService → LlmDrivenAgentService，LlmDrivenAgentService 不依赖 AgentTaskService |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `createTask(taskType, title, input)` | 创建任务 | 无 |
| `listTasks()` | 列出最近任务 | 无分页 |
| `getTask(taskId)` | 获取任务详情 | 无 |
| `notifications(unreadOnly, undeliveredOnly)` | 查询通知 | 无 |
| `markRead(notificationId)` | 标记已读 | 无 |
| `markDelivered(notificationId)` | 标记已送达 | 无 |
| `runScheduledAnomalyWatch()` ✏️ ~~`runScheduledTasks()`~~ | 定时执行任务 | @Scheduled，检查是否有排队中任务并执行 |
| `executeTask(task)` | 执行单个任务 | 根据 taskType 路由到不同处理逻辑 |
| `createNotification(title, body, level, taskId)` | 创建通知 | 无 |

### 修改建议

1. ~~**循环依赖**~~ ❌ `AgentTaskService` → `LlmDrivenAgentService` 为单向依赖，LlmDrivenAgentService 不反向依赖 AgentTaskService，不存在循环依赖
2. **定时任务无分布式锁**: `runScheduledAnomalyWatch` 在多实例部署时会重复执行。
3. **任务执行无超时**: LLM 调用可能无限阻塞，应设置超时。

---

## SyncService

- **文件**: `SyncService.java`
- **作用**: 离线数据同步服务。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `syncCursorRepository` | `SyncCursorRepository` | 同步游标数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `saleOrderItemRepository` | `SaleOrderItemRepository` | 销售明细数据访问 | 无 |
| `purchaseOrderRepository` | `PurchaseOrderRepository` | 采购单数据访问 | 无 |
| `purchaseOrderItemRepository` | `PurchaseOrderItemRepository` | 采购明细数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `payOrderRepository` | `PayOrderRepository` | 付款单数据访问 | 无 |
| `financeRecordRepository` | `FinanceRecordRepository` | 资金流水数据访问 | 无 |
| `paymentRepository` | `PaymentRepository` | 支付记录数据访问 | 无 |
| `inventoryAdjustmentRepository` | `InventoryAdjustmentRepository` | 库存调整数据访问 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `upload(clientId, changes, lastSyncCursor)` | 上传客户端变更 | **仅更新游标，未实际应用变更**，功能不完整 |
| `pull(sinceCursor, limit)` | 拉取增量变更 | 按游标查询各实体变更，逻辑完整 |
| `pullEntityChanges(entityType, sinceCursor, limit)` | 拉取单实体变更 | 使用 switch-case，应改用策略模式 |
| `generateCursor()` | 生成游标 | 使用时间戳+随机数 |

### 严重问题

1. **upload 未实现**: 客户端上传的变更仅记录游标，未实际应用到数据库，同步功能不完整。
2. **注入了 12 个 Repository**: 违反单一职责原则。

---

## DemoDataService

- **文件**: `DemoDataService.java`
- **Profile**: `local`
- **作用**: 生成演示数据。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `userRepository` | `UserRepository` | 用户数据访问 | 无 |
| `productRepository` | `ProductRepository` | 商品数据访问 | 无 |
| `customerRepository` | `CustomerRepository` | 客户数据访问 | 无 |
| `supplierRepository` | `SupplierRepository` | 供应商数据访问 | 无 |
| `saleOrderRepository` | `SaleOrderRepository` | 销售单数据访问 | 无 |
| `saleOrderItemRepository` | `SaleOrderItemRepository` | 销售明细数据访问 | 无 |
| `purchaseOrderRepository` | `PurchaseOrderRepository` | 采购单数据访问 | 无 |
| `purchaseOrderItemRepository` | `PurchaseOrderItemRepository` | 采购明细数据访问 | 无 |
| `paymentRepository` | `PaymentRepository` | 支付记录数据访问 | 无 |
| `payOrderRepository` | `PayOrderRepository` | 付款单数据访问 | 无 |
| `financeRecordRepository` | `FinanceRecordRepository` | 资金流水数据访问 | 无 |
| `passwordEncoder` | `PasswordEncoder` | 密码编码器 | 无 |

### 函数

| 函数 | 作用 | 修改建议 |
|------|------|----------|
| `seed(reset)` | 初始化演示数据 | reset=true 时清空所有数据后重新生成 |
| `clearAll()` | 清空所有数据 | 使用 JPA deleteAll，大量数据时效率低 |

---

## 全局问题与修改建议

1. **全量查询+内存过滤**: 多个 Service 的 list 方法使用 `findAll()` + 内存过滤，数据量大时性能极差，应改用数据库条件查询。
2. **ReportService 全量内存聚合**: 所有报表方法全量加载后内存计算，应改用 SQL 聚合查询。
3. ~~**取消订单未恢复库存**~~ ❌ `SaleOrderService.cancel()` 实际已正确恢复库存（第292-301行），此为误报
4. **SyncService.upload 未实现**: 仅记录游标，未应用变更。
5. ~~**循环依赖风险**~~ ❌ `AgentTaskService` → `LlmDrivenAgentService` 为单向依赖，不存在循环依赖
6. ~~**单号生成非线程安全**~~ ❌ `AuthService.generateOrderNo()` 不存在；单号生成实际在各 OrderService 中使用 `UUID.randomUUID()`（线程安全），但 `substring(0,4~6)` 降低了唯一性
7. **验证码未校验**: `AuthService.register()` 未实际校验验证码。
8. **Service 注入过多 Repository**: `ReportService`(11个)、`SyncService`(12个)、`DemoDataService`(12个) 违反单一职责原则。
