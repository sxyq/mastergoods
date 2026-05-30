# Controller 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/api/controller/`

本层为 REST API 入口，负责 HTTP 请求路由、参数校验和响应封装。所有接口统一返回 `ApiResponse<T>` 包装体。

---

## AdminController

- **文件**: `AdminController.java`
- **基路径**: `/v1/admin`
- **Profile**: `local`（仅本地环境生效）
- **作用**: 后台管理接口，提供系统概览、用户管理、演示数据初始化和 Agent 冒烟测试。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `adminService` | `AdminService` | 管理服务，封装用户/统计/演示逻辑 | 无 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `summary()` | GET | `/summary` | 获取系统各实体计数概览 | 可增加缓存减少频繁 count 查询 |
| ✏️ `listUsers(keyword)` | GET | `/users` | ✏️ 按关键词搜索用户列表 | keyword 应增加长度限制防滥用 |
| `createUser(request)` | POST | `/users` | 创建用户 | 缺少 @Valid 注解，请求体未做 Bean Validation |
| `updateUser(userId, request)` | PUT | `/users/{userId}` | 更新用户信息 | 同上，缺少 @Valid |
| `seedDemoData(reset)` | POST | `/demo/seed` | 初始化演示数据 | reset 参数建议用 @RequestParam(defaultValue) 替代裸 boolean |
| `runAgentSmoke()` | POST | `/agent/smoke` | 执行 Agent 冒烟测试 | 无超时保护，LLM 调用可能长时间阻塞 |

---

## AgentController

- **文件**: `AgentController.java`
- **基路径**: `/v1/agent`
- **作用**: AI Agent 工作台接口，提供工作台数据、问答、草稿生成与提交。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `agentService` | `LlmDrivenAgentService` | LLM 驱动的 Agent 服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `QueryRequest(query)` | 问答请求体 | query 字段应加 @NotBlank 校验 |
| `DraftRequest(instruction)` | 草稿指令请求体 | instruction 应加 @NotBlank 校验 |
| `SubmitRequest(draft)` | 草稿提交请求体 | draft 应加 @NotNull 校验 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `workbench(windowDays, limit, agingDays)` | GET | `/workbench` | 获取 Agent 工作台数据 | 参数缺少上界校验，可传入极大值 |
| `reconciliationFollowup(limit, agingDays)` | GET | `/reconciliation-followup` | 获取对账催办数据 | 同上 |
| `reportInsight(windowDays)` | GET | `/report-insight` | 获取报表洞察 | 同上 |
| `alerts(limit, agingDays)` | GET | `/alerts` | 获取预警仪表盘 | 同上 |
| `query(request)` | POST | `/query` | Agent 问答 | 无 |
| `operationDraft(request)` | POST | `/operation-draft` | 生成操作草稿 | 无 |
| `operationSubmit(request)` | POST | `/operation-submit` | 提交操作草稿 | 无 |

---

## AgentTaskController

- **文件**: `AgentTaskController.java`
- **基路径**: `/v1/agent`
- **作用**: Agent 任务与通知管理接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `agentTaskService` | `AgentTaskService` | Agent 任务服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `CreateTaskRequest(taskType, title, input)` | 创建任务请求体 | 字段应加校验注解 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `createTask(request)` | POST | `/tasks` | 提交 Agent 任务 | 无 |
| `listTasks()` | GET | `/tasks` | 列出最近 20 条任务 | 无分页参数 |
| `getTask(taskId)` | GET | `/tasks/{taskId}` | 获取任务详情 | 无 |
| `notifications(unreadOnly, undeliveredOnly)` | GET | `/notifications` | 查询通知列表 | 无 |
| `markRead(notificationId)` | POST | `/notifications/{notificationId}/read` | 标记通知已读 | 无 |
| `markDelivered(notificationId)` | POST | `/notifications/{notificationId}/delivered` | 标记通知已送达 | 无 |
| `notificationStream()` | GET | `/notifications/stream` | SSE 通知推送流 | ✏️ 超时 30 分钟硬编码在 AgentTaskService 中（非 Controller），建议可配置化 |

---

## AuthController

- **文件**: `AuthController.java`
- **基路径**: `/v1/auth`
- **作用**: 认证鉴权接口，提供注册、登录、刷新、登出和个人信息查询。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `authService` | `AuthService` | 认证服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `RegisterRequest(phone, password, verifyCode)` | 注册请求 | 已有 @NotBlank 校验 |
| `LoginRequest(phone, password)` | 登录请求 | 已有 @NotBlank 校验 |
| `RefreshRequest(refreshToken)` | 刷新令牌请求 | 已有 @NotBlank 校验 |
| `VerifyCodeRequest(phone, type)` | 验证码请求 | 已有 @NotBlank 校验 |
| `VerifyCodeResponse(success, expireSeconds)` | 验证码响应 | 当前硬编码返回失败 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `register(request)` | POST | `/register` | 用户注册 | 无 |
| `login(request)` | POST | `/login` | 用户登录 | 无 |
| `refresh(request)` | POST | `/refresh` | 刷新令牌 | 无 |
| `logout(authorization)` | POST | `/logout` | 登出 | token 提取逻辑可复用 |
| `verifyCode(request)` | POST | `/verify-code` | 获取验证码 | **硬编码返回失败**，需接入真实短信/验证码服务 |
| `me(authorization)` | GET | `/users/me` | 获取当前用户信息 | 无 |
| `extractBearerToken(authorization)` | private | - | 提取 Bearer Token（非空） | 与 extractBearerTokenOrNull 重复，可合并 |
| `extractBearerTokenOrNull(authorization)` | private | - | 提取 Bearer Token（可空） | 同上 |

---

## CustomerController

- **文件**: `CustomerController.java`
- **基路径**: `/v1/customers`
- **作用**: 客户 CRUD 接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `customerService` | `CustomerService` | 客户服务 | 无 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `list(keyword)` | GET | `/` | 搜索客户列表 | 无 |
| `get(id)` | GET | `/{id}` | 获取客户详情 | 无 |
| `create(payload)` | POST | `/` | 创建客户 | **直接接收 Entity 而非 DTO**，暴露了领域模型，应改用 DTO |
| `update(id, payload)` | PUT | `/{id}` | 更新客户 | 同上 |
| `delete(id)` | DELETE | `/{id}` | 删除客户 | 无软删除，物理删除可能导致数据丢失 |

---

## FinanceRecordController

- **文件**: `FinanceRecordController.java`
- **基路径**: `/v1/finance-records`
- **作用**: 资金流水接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `financeRecordService` | `FinanceRecordService` | 资金流水服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `CreateRequest(type, category, partnerName, amount, method, notes)` | 创建资金流水请求 | 字段缺少校验注解 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `list(keyword, type, createdAfter, createdBefore)` | GET | `/` | 查询资金流水列表 | createdAfter/createdBefore 为 String 类型，应改为 Long 或增加格式说明 |
| `create(request)` | POST | `/` | 创建资金流水 | 无 |
| `toDto(entity)` | private | - | Entity 转 DTO | 无 |
| `parseLong(raw)` | private | - | 安全解析 Long | 多个 Controller 重复此方法，应抽取到公共工具类 |

---

## PayOrderController

- **文件**: `PayOrderController.java`
- **基路径**: `/v1/pay-orders`
- **作用**: 付款单接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `payOrderService` | `PayOrderService` | 付款单服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `CreateRequest(supplierId, supplierName, amount, method, referenceNo, notes, status)` | 创建付款单请求 | 字段缺少校验注解 |
| `StatusRequest(status)` | 状态变更请求 | status 缺少校验 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `list(keyword, status, createdAfter, createdBefore)` | GET | `/` | 查询付款单列表 | 同 FinanceRecordController 的参数类型问题 |
| `get(id)` | GET | `/{id}` | 获取付款单详情 | 无 |
| `create(request)` | POST | `/` | 创建付款单 | 无 |
| `updateStatus(id, request)` | PUT | `/{id}/status` | 更新付款单状态 | 无 |
| `toDto(entity)` | private | - | Entity 转 DTO | 无 |
| `parseLong(raw)` | private | - | 安全解析 Long | 重复代码，应抽取 |

---

## ProductController

- **文件**: `ProductController.java`
- **基路径**: `/v1/products`
- **作用**: 商品 CRUD 与库存调整接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `productService` | `ProductService` | 商品服务 | 无 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `list(keyword)` | GET | `/` | 搜索商品列表 | 无 |
| `get(id)` | GET | `/{id}` | 获取商品详情 | 无 |
| `getByCode(code)` | GET | `/by-code` | 按编码查询商品 | 无 |
| `create(payload)` | POST | `/` | 创建商品 | **直接接收 Entity**，应改用 DTO |
| `update(id, payload)` | PUT | `/{id}` | 更新商品 | 同上 |
| `adjustStock(id, request)` | POST | `/{id}/adjust-stock` | 调整库存 | 无 |
| `delete(id)` | DELETE | `/{id}` | 删除商品 | 物理删除，无安全检查 |

---

## PurchaseOrderController

- **文件**: `PurchaseOrderController.java`
- **基路径**: `/v1/purchase-orders`
- **作用**: 采购单接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `purchaseOrderService` | `PurchaseOrderService` | 采购单服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `CreateRequest(supplierName, items, notes, status)` | 创建采购单请求 | items 应加 @NotEmpty 校验 |
| `ItemRequest(productId, productCode, productName, quantity, unitCost)` | 采购明细请求 | 字段缺少校验注解 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `create(request)` | POST | `/` | 创建采购单 | 无 |
| `list(keyword, status)` | GET | `/` | 查询采购单列表 | 无 |
| `get(id)` | GET | `/{id}` | 获取采购单详情 | 无 |
| `toDto(detail)` | private | - | PurchaseDetail 转 DTO | 无 |
| `toDto(order, items)` | private | - | Order+Items 转 DTO | 无 |

---

## ReportController

- **文件**: `ReportController.java`
- **基路径**: `/v1/reports`
- **作用**: 报表数据接口，提供销售、利润、退款、库存、对账等多种报表。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `reportService` | `ReportService` | 报表服务 | 无 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `salesSummary(startAt, endAt)` | GET | `/sales-summary` | 销售汇总 | startAt/endAt 为必填 Long，缺少格式说明 |
| `profitSummary(startAt, endAt)` | GET | `/profit-summary` | 利润汇总 | 同上 |
| `refundRecords(startAt, endAt, limit)` | GET | `/refund-records` | 退款记录 | 同上 |
| `stockOutRecords(startAt, endAt, limit)` | GET | `/stock-out-records` | 出库记录 | 同上 |
| `topProducts(startAt, endAt, limit)` | GET | `/top-products` | 热销商品 | 同上 |
| `profitByProducts(startAt, endAt, limit)` | GET | `/profit-by-products` | 商品利润 | 同上 |
| `profitByCustomers(startAt, endAt, limit)` | GET | `/profit-by-customers` | 客户利润 | 同上 |
| `inventoryFlow(startAt, endAt, limit)` | GET | `/inventory-flow` | 库存流水 | 同上 |
| `customerSales(startAt, endAt, limit)` | GET | `/customer-sales` | 客户销售 | 同上 |
| `topReceivableCustomers(limit)` | GET | `/top-receivable-customers` | 应收客户 | 无 |
| `lowStockProducts(limit)` | GET | `/low-stock-products` | 低库存商品 | 无 |
| `reconciliationSummary(startAt, endAt)` | GET | `/reconciliation-summary` | 对账汇总 | 同上 |

---

## SaleOrderController

- **文件**: `SaleOrderController.java`
- **基路径**: `/v1/sale-orders`
- **作用**: 销售单接口，包含 CRUD、收款、状态变更、PDF 导出和取消。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `saleOrderService` | `SaleOrderService` | 销售单服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `CreateRequest(customerId, customerName, items, notes, discountAmount)` | 创建销售单请求 | items 应加 @NotEmpty |
| `ItemRequest(productId, quantity, unitPrice)` | 销售明细请求 | 字段缺少校验 |
| `UpdateDraftRequest(discountAmount, notes)` | 更新草稿请求 | 无 |
| `PaymentRequest(amount, method, referenceNo)` | 收款请求 | amount 应加 @NotNull @Positive |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `create(request)` | POST | `/` | 创建销售单 | 无 |
| `list(keyword, status, ...)` | GET | `/` | 多条件搜索销售单 | 参数过多(8个)，建议封装为查询对象 |
| `get(id)` | GET | `/{id}` | 获取销售单详情 | 无 |
| `updateDraft(id, request)` | PUT | `/{id}` 或 `/{id}/draft` | 更新草稿 | 两个路径映射同一方法，语义不清 |
| `addPayment(id, request)` | POST | `/{id}/payments` | 添加收款记录 | 无 |
| `listPayments(id)` | GET | `/{id}/payments` | 查询收款记录 | 无 |
| `updateStatus(id, request)` | PUT | `/{id}/status` | 更新订单状态 | 无 |
| `exportPdf(id)` | GET | `/{id}/pdf` | 导出 PDF | PDF 生成极简（仅4行文本），应改用专业 PDF 库 |
| `cancel(id)` | PUT | `/{id}/cancel` | 取消订单 | 无 |
| `toDto(detail)` | private | - | OrderDetail 转 DTO | 无 |
| `toDto(order, items)` | private | - | Order+Items 转 DTO | 无 |
| `parseDouble(raw)` | private | - | 安全解析 Double | 重复代码 |
| `parseLong(raw)` | private | - | 安全解析 Long | 重复代码 |
| `parseInteger(raw)` | private | - | 安全解析 Integer | 重复代码 |

---

## SupplierController

- **文件**: `SupplierController.java`
- **基路径**: `/v1/suppliers`
- **作用**: 供应商 CRUD 接口。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `supplierService` | `SupplierService` | 供应商服务 | 无 |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `list(keyword, status)` | GET | `/` | 搜索供应商列表 | 无 |
| `get(id)` | GET | `/{id}` | 获取供应商详情 | 无 |
| `create(payload)` | POST | `/` | 创建供应商 | **直接接收 Entity**，应改用 DTO |
| `update(id, payload)` | PUT | `/{id}` | 更新供应商 | 同上 |
| `delete(id)` | DELETE | `/{id}` | 删除供应商 | 物理删除 |

---

## SyncController

- **文件**: `SyncController.java`
- **基路径**: `/v1/sync`
- **作用**: 离线数据同步接口，支持上传变更和拉取增量。

### 成员变量

| 变量 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `syncService` | `SyncService` | 同步服务 | 无 |

### 内部 record

| Record | 作用 | 修改建议 |
|--------|------|----------|
| `UploadRequest(clientId, changes, lastSyncCursor)` | 上传变更请求 | changes 应限制最大数量 |
| `SyncChangeDto(entityType, entityId, operation, payload, updatedAt)` | 单条变更 | 无 |
| `PullRequest(sinceCursor, limit)` | 拉取请求 | limit 有边界保护（1~500） |

### 函数

| 函数 | HTTP 方法 | 路径 | 作用 | 修改建议 |
|------|-----------|------|------|----------|
| `upload(request)` | POST | `/upload` | 上传客户端变更 | 当前 upload 仅记录游标，未实际应用变更到服务端 |
| `pull(request)` | POST | `/pull` | 拉取增量变更 | 无 |
| `health()` | GET | `/health` | 同步服务健康检查 | 无 |

---

## 全局问题与修改建议

1. **parseLong/parseDouble/parseInteger 重复**: 多个 Controller 中存在相同的解析方法，应抽取到 `api/common/ParseUtils` 工具类。
2. **Entity 直接作为请求体**: `CustomerController`、`ProductController`、`SupplierController` 直接接收 Entity，违反 DTO 隔离原则，应创建专用请求 DTO。
3. **缺少统一分页**: 列表接口均无分页，数据量大时存在性能风险。
4. **验证码接口硬编码失败**: `AuthController.verifyCode()` 永远返回失败，需接入真实服务。
5. **物理删除**: 多处使用物理删除，建议改为软删除。
6. **参数校验不完整**: 多数请求 record 缺少 `@NotBlank`、`@NotNull`、`@Positive` 等 Bean Validation 注解。
