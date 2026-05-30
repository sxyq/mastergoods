# DTO 层技术分析

> 路径: `src/main/java/com/zhihuiji/backend/api/dto/`

本层为数据传输对象（Data Transfer Object），用于 Controller 与 Service 之间的数据隔离，避免直接暴露领域实体。

---

## AgentDto

- **文件**: `agent/AgentDto.java`
- **命名策略**: `LowerCamelCaseStrategy`（驼峰）
- **作用**: AI Agent 相关的所有 DTO 聚合类，包含工作台、问答、草稿、任务、通知等数据结构。

### 内部 record

| Record | 作用 | 字段概览 | 修改建议 |
|--------|------|----------|----------|
| `AgentWorkbenchDto` | 工作台总览 | reconciliation, reportInsight, alerts, suggestedQuestions, suggestedInstructions, overviewBlocks, instantBlocks, proactiveAnswers, proactiveDrafts | 提供了简化构造器（6参数），其余默认空列表 |
| `ReconciliationFollowupDto` | 对账催办 | totalReceivable, totalPayable, totalReceived, totalPaid, netCashFlow, receivableCustomers, payableSuppliers, agingRisks | 无 |
| `FollowupPartyDto` | 催办对象 | entityId, entityType, name, phone, amount, actionLabel | 无 |
| `AgingRiskDto` | 账龄风险 | entityType, entityId, name, orderNo, createdAt, ageDays, amount, summary, suggestedAction | 无 |
| `ReportInsightDto` | 报表洞察 | periodLabel, currentSales, previousSales, salesChangeRate, narrative, leadingProductName, leadingProductAmount, leadingCustomerName, leadingCustomerAmount, highlights, suggestedActions | 无 |
| `AlertDashboardDto` | 预警仪表盘 | alerts | 无 |
| `AlertDto` | 单条预警 | id, type, severity, title, description, recommendedAction, entityName, entityId, metric | 无 |
| `AgentAnswerDto` | 问答结果 | query, intent, answer, highlights, columns, rows, suggestedActions | 无 |
| `OperationDraftDto` | 操作草稿 | operationType, summary, partnerRole, partnerId, partnerName, items, notes, canSubmit, warnings, suggestedActions | 无 |
| `OperationDraftItemDto` | 草稿明细 | productId, productCode, productName, quantity, unitPrice, amount, currentStock | 无 |
| `OperationSubmitResultDto` | 草稿提交结果 | operationType, orderId, orderNo, message, nextAction | 无 |
| `AgentTaskSummaryDto` | 任务摘要 | id, taskType, title, status, triggerSource, progress, createdAt, updatedAt, completedAt | 无 |
| `AgentTaskMetricDto` | 指标 | label, value, delta, emphasis | 无 |
| `AgentTaskSectionDto` | 段落 | title, narrative, bullets | 无 |
| `AgentTaskTableDto` | 表格 | title, columns, rows | 无 |
| `AgentTaskChartSeriesDto` | 图表系列 | name, values | 无 |
| `AgentTaskChartDto` | 图表 | title, chartType, categories, series | 无 |
| `AgentRenderBlockDto` | 渲染块 | type, title, subtitle, tone, text, bullets, metrics, table, chart, draft | type 仅允许 hero/metric_grid/bullet_list/table/chart/draft |
| `AgentTaskResultDto` | 任务结果 | title, subtitle, summary, metrics, sections, tables, charts, suggestedActions, draft, renderBlocks | 无 |
| `AgentTaskDetailDto` | 任务详情 | task, input, result | 无 |
| `AgentNotificationDto` | 通知 | id, title, body, level, taskId, isRead, isDelivered, createdAt | 无 |

---

## ReportDto

- **文件**: `report/ReportDto.java`
- **命名策略**: `SnakeCaseStrategy`（下划线）
- **作用**: 报表相关 DTO 聚合类。

### 内部 record

| Record | 作用 | 字段概览 | 修改建议 |
|--------|------|----------|----------|
| `SalesSummaryReportDto` | 销售汇总 | startAt, endAt, totalSalesAmount, totalPaidAmount, totalRefundAmount, totalUnpaidAmount, totalOrderCount | 无 |
| `ProfitSummaryReportDto` | 利润汇总 | startAt, endAt, estimatedCostAmount, estimatedProfitAmount, estimatedProfitRate | 无 |
| `RefundRecordReportDto` | 退款记录 | paymentId, orderId, orderNo, customerName, refundAmount, method, referenceNo, createdAt | 无 |
| `StockOutRecordReportDto` | 出库记录 | orderId, orderNo, customerId, customerName, productId, productCode, productName, quantity, unitPrice, amount, itemCreatedAt, orderCreatedAt | 无 |
| `TopSellingProductReportDto` | 热销商品 | productId, productCode, productName, totalQuantity, totalAmount | 无 |
| `ProfitByProductReportDto` | 商品利润 | productId, productCode, productName, totalSalesAmount, totalCostAmount, totalProfitAmount, profitRate | 无 |
| `ProfitByCustomerReportDto` | 客户利润 | customerId, customerName, totalSalesAmount, totalCostAmount, totalProfitAmount, profitRate | 无 |
| `InventoryFlowRecordDto` | 库存流水 | orderId, orderNo, productId, productCode, productName, quantity, flowType, flowTime, customerName, sourceType, sourceLabel, adjustReason, operatorName | 无 |
| `CustomerSalesReportDto` | 客户销售 | customerId, customerName, totalOrders, totalAmount | 无 |
| `CustomerReceivableReportDto` | 应收客户 | customerId, customerName, phone, balance | 无 |
| `LowStockProductReportDto` | 低库存商品 | productId, productCode, productName, stock, safeStock | 无 |
| `ReconciliationSummaryReportDto` | 对账汇总 | startAt, endAt, totalReceivableAmount, totalPayableAmount, totalReceivedAmount, totalPaidAmount, netCashFlow | 无 |

---

## FinanceRecordDto

- **文件**: `FinanceRecordDto.java`
- **命名策略**: `SnakeCaseStrategy`
- **作用**: 资金流水 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | 记录 ID | 无 |
| `recordNo` | String | 流水号 | 无 |
| `type` | Integer | 类型（1=收入, 2=支出） | 可改用枚举提升可读性 |
| `category` | String | 分类 | 无 |
| `partnerName` | String | 往来对象名称 | 无 |
| `amount` | Double | 金额 | 应改用 BigDecimal 避免浮点精度问题 |
| `method` | Integer | 收支方式 | 可改用枚举 |
| `notes` | String | 备注 | 无 |
| `createdAt` | Long | 创建时间戳 | 无 |
| `updatedAt` | Long | 更新时间戳 | 无 |

---

## PayOrderDto

- **文件**: `PayOrderDto.java`
- **命名策略**: `SnakeCaseStrategy`
- **作用**: 付款单 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `orderNo` | String | 单号 | 无 |
| `supplierId` | Long | 供应商 ID | 无 |
| `supplierName` | String | 供应商名称 | 无 |
| `amount` | Double | 金额 | 应改用 BigDecimal |
| `method` | Integer | 付款方式 | 可改用枚举 |
| `referenceNo` | String | 参考号 | 无 |
| `notes` | String | 备注 | 无 |
| `status` | Integer | 状态（0=草稿, 1=已付, 2=已取消） | 可改用枚举 |
| `createdAt` | Long | 创建时间戳 | 无 |
| `updatedAt` | Long | 更新时间戳 | 无 |

---

## ProductAdjustStockRequest

- **文件**: `ProductAdjustStockRequest.java`
- **作用**: 库存调整请求 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `delta` | Double | 库存变化量（正=入库，负=出库），@NotNull | 应改用 BigDecimal |
| `reason` | String | 调整原因 | 应加 @NotBlank |
| `operator` | String | 操作人 | 应加 @NotBlank |

---

## PurchaseOrderDto

- **文件**: `PurchaseOrderDto.java`
- **作用**: 采购单 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `orderNo` | String | 单号 | 无 |
| `supplierName` | String | 供应商名称 | 无 |
| `items` | List\<PurchaseOrderItemDto\> | 采购明细列表 | 无 |
| `totalAmount` | Double | 总金额 | 应改用 BigDecimal |
| `notes` | String | 备注 | 无 |
| `status` | Integer | 状态 | 可改用枚举 |
| `createdAt` | Long | 创建时间戳 | 无 |
| `updatedAt` | Long | 更新时间戳 | 无 |

---

## PurchaseOrderItemDto

- **文件**: `PurchaseOrderItemDto.java`
- **作用**: 采购明细 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `orderId` | Long | 采购单 ID | 无 |
| `productCode` | String | 商品编码 | 无 |
| `productName` | String | 商品名称 | 无 |
| `quantity` | Double | 数量 | 应改用 BigDecimal |
| `unitCost` | Double | 单价 | 应改用 BigDecimal |
| `amount` | Double | 金额 | 应改用 BigDecimal |
| `createdAt` | Long | 创建时间戳 | 无 |

---

## SaleOrderDto

- **文件**: `SaleOrderDto.java`
- **作用**: 销售单 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `orderNo` | String | 单号 | 无 |
| `customerId` | Long | 客户 ID | 无 |
| `customerName` | String | 客户名称 | 无 |
| `items` | List\<SaleOrderItemDto\> | 销售明细列表 | 无 |
| `subtotalAmount` | Double | 小计 | 应改用 BigDecimal |
| `discountAmount` | Double | 折扣 | 应改用 BigDecimal |
| `totalAmount` | Double | 总金额 | 应改用 BigDecimal |
| `paidAmount` | Double | 已付金额 | 应改用 BigDecimal |
| `notes` | String | 备注 | 无 |
| `status` | Integer | 状态 | 可改用枚举 |
| `createdAt` | Long | 创建时间戳 | 无 |
| `updatedAt` | Long | 更新时间戳 | 无 |

---

## SaleOrderItemDto

- **文件**: `SaleOrderItemDto.java`
- **作用**: 销售明细 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `orderId` | Long | 销售单 ID | 无 |
| `productId` | Long | 商品 ID | 无 |
| `productCode` | String | 商品编码 | 无 |
| `productName` | String | 商品名称 | 无 |
| `customerId` | Long | 客户 ID | 无 |
| `customerName` | String | 客户名称 | 无 |
| `quantity` | Double | 数量 | 应改用 BigDecimal |
| `unitPrice` | Double | 单价 | 应改用 BigDecimal |
| `amount` | Double | 金额 | 应改用 BigDecimal |
| `createdAt` | Long | 创建时间戳 | 无 |

---

## SaleOrderStatusRequest

- **文件**: `SaleOrderStatusRequest.java`
- **作用**: 销售单状态变更请求。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `status` | Integer | 目标状态 | 应加 @NotNull 和范围校验 |

---

## SupplierDto

- **文件**: `SupplierDto.java`
- **命名策略**: `SnakeCaseStrategy`
- **作用**: 供应商 DTO。

| 字段 | 类型 | 作用 | 修改建议 |
|------|------|------|----------|
| `id` | Long | ID | 无 |
| `name` | String | 名称 | 无 |
| `phone` | String | 电话 | 无 |
| `address` | String | 地址 | 无 |
| `notes` | String | 备注 | 无 |
| `balance` | Double | 余额 | 应改用 BigDecimal |
| `status` | Integer | 状态 | 可改用枚举 |
| `createdAt` | Long | 创建时间戳 | 无 |
| `updatedAt` | Long | 更新时间戳 | 无 |

---

## 全局问题与修改建议

1. **金额字段使用 Double**: 所有金额相关字段（amount, price, balance 等）均使用 `Double`，存在浮点精度丢失风险，应统一改用 `BigDecimal`。
2. **状态字段使用 Integer**: 多处状态字段使用 `Integer`，缺少类型安全，建议改用枚举。
3. **命名策略不一致**: `AgentDto` 使用 `LowerCamelCaseStrategy`，`ReportDto`/`FinanceRecordDto` 等使用 `SnakeCaseStrategy`，应统一。
4. **缺少校验注解**: `ProductAdjustStockRequest` 的 reason/operator、`SaleOrderStatusRequest` 的 status 缺少校验。
5. **AgentDto 过于庞大**: 单文件包含 20+ 个 record，建议按职责拆分为 `AgentWorkbenchDto`、`AgentTaskDto`、`AgentNotificationDto` 等独立文件。
