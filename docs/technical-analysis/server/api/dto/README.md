# Server api/dto 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/api/dto`
- 当前包含：
  - 销售/采购/付款/资金等 DTO
  - `v2/sales`
  - `v2/purchase`
  - `v2/pay`
  - `v2/product`
  - `v2/partner`
  - `v2/finance`
  - `v2/inventory`
  - `v2/sync`
  - `agent/*`
  - `report/*`

## 状态图例

- `新版已做`
- `新版待做`
- `旧版存在新版未做`
- `新版需要去掉`
- `需重构`
- `待验证`

## 总体状态表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| `/v1` DTO 集合 | 新版已做 | 旧版无当前 server DTO 分层 | 为安卓和管理端提供接口模型 | 当前 DTO 已覆盖首版主要接口 | 可继续服务 `/v1` |
| `/v2` request/response/summary 分层 | 新版已做 | 旧版无 `/v2` | 形成更清晰的读模型、写模型、统计模型 | 已建立 `api/dto/v2/sales|purchase|pay|product|partner|finance|inventory` 首批命名空间；B04 中 `accounts` 已拆分 create/update 请求模型 | 媒体等继续补齐 |
| 商品多价格/库存快照等 DTO | 旧版存在新版未做 | 旧版领域更厚 | 新版 DTO 要覆盖更完整业务域 | 账户/转账/单据资金关联/找零/库存 DTO 已补；商品多价格等仍待补 | 将显著扩容 |

## 现有 DTO 结构

### 基础业务 DTO

| DTO | 现有字段 | 当前状态 | 说明 |
|---|---|---|---|
| `SaleOrderDto` | `id, orderNo, customerId, customerName, items, subtotalAmount, discountAmount, totalAmount, paidAmount, notes, status, createdAt, updatedAt` | 需重构 | 基础销售 DTO 已存在，但字段仍偏薄 |
| `SaleOrderItemDto` | `id, orderId, productId, productCode, productName, customerId, customerName, quantity, unitPrice, amount, createdAt` | 需重构 | 明细冗余多，缺 owner 与更细业务字段 |
| `PurchaseOrderDto` | `id, orderNo, supplierId, supplierName, items, totalAmount, notes, status, createdAt, updatedAt` | 需重构 | 已补 supplierId，仍缺应付/实付等 |
| `PurchaseOrderItemDto` | `id, orderId, productCode, productName, quantity, unitCost, amount, createdAt` | 需重构 | 与后续采购订单态/入库态不匹配 |
| `PayOrderDto` | `id, orderNo, supplierId, supplierName, amount, method, referenceNo, notes, status, createdAt, updatedAt` | 需重构 | `/v1` 仍缺账户、项目、单据联动语义 |
| `FinanceRecordDto` | `id, recordNo, type, category, partnerName, amount, method, notes, createdAt, updatedAt` | 需重构 | 只有轻量流水字段 |
| `SupplierDto` | `id, name, phone, address, notes, balance, status, createdAt, updatedAt` | 需重构 | 供应商画像偏薄 |
| `ProductAdjustStockRequest` | `delta, reason, operator` | 新版已做 | 基础库存调整请求 |
| `SaleOrderStatusRequest` | `status` | 新版已做 | 基础状态更新请求 |

### Agent DTO

| DTO 组 | 当前状态 | 说明 |
|---|---|---|
| `AgentConversationUpdateRequest` | 待验证 | B07 新增会话更新请求 DTO | 承接 `PUT /v2/agent/conversations/{id}` 请求体 | 含 `status` 字段，约束为 `[active, closed, archived]` |
| `AgentMessageListResponse` | 新版需要去掉 | B07 确认为死代码 | 已移除 | 原本用于消息列表响应，实际未被任何接口引用 |
| `AnswerDtos` | 新版已做 | AI 问答结果 | | |
| `OperationDraftDtos` | 新版已做 | 草稿与提交结果 | | |
| `AgentTaskDtos` | 新版已做 | 任务摘要、详情、图表块、通知 | | |
| `WorkbenchDtos` | 新版已做 | AI 工作台聚合读模型 | | |
| `AlertDtos` / `ReconciliationDtos` | 新版已做 | 经营提醒与对账结果 | | |
| `AgentDto` | 新版需要去掉 | 已退化为 legacy marker，应继续避免依赖 | | |

### Report DTO

| DTO 组 | 当前状态 | 说明 |
|---|---|---|
| `ReportDto.*` | 需重构 | 已覆盖首版报表，但字段命名与安卓侧已有偏差，也缺更厚的库存/财务统计 DTO |

## 旧版表域到新版 DTO 差异

### 商品与往来单位

| 旧版能力 | 当前 DTO 覆盖 | 状态 | 新版 DTO 目标 | 备注 |
|---|---|---|---|---|
| 商品多价格、多单位、供应关系 | 未覆盖 | 旧版存在新版未做 | `ProductCategoryDto`、`ProductUnitDto`、`ProductPriceLevelDto`、`ProductSupplierRelationDto` | 当前 `ProductDto` 只覆盖基础档案 |
| 客户/供应商联系人、分组、等级、折扣 | 未覆盖 | 旧版存在新版未做 | `PartnerGroupDto`、`PartnerContactDto`、`PartnerPricingPolicyDto` | 当前 `CustomerDto` / `SupplierDto` 还不在 server DTO 层独立成完整结构 |

### 销售与采购

| 旧版能力 | 当前 DTO 覆盖 | 状态 | 新版 DTO 目标 | 备注 |
|---|---|---|---|---|
| 销售订单态/草稿态/退货态 | 未覆盖 | 旧版存在新版未做 | `SalesDraftDto`、`SalesReturnDto`、`SalesOrderSummaryDto`、`SalesOrderDetailDto` | 不能继续只靠一套 `SaleOrderDto` |
| 采购订单态/入库态 | 未覆盖 | 旧版存在新版未做 | `PurchaseReceiptDto`、`PurchaseOrderSummaryDto`、`PurchaseOrderDetailDto` | 当前 `PurchaseOrderDto` 过于单薄 |
| 运费、抹零、来源渠道、操作人 | 未覆盖 | 旧版存在新版未做 | 明确进入 `/v2` 订单 DTO | 有利于报表与审计 |

### 财务与库存

| 旧版能力 | 当前 DTO 覆盖 | 状态 | 新版 DTO 目标 | 备注 |
|---|---|---|---|---|
| 账户主数据 | `AccountDto` 等已覆盖 | 新版已做 | `AccountDto`、`AccountBalanceDto` | 第四阶段已补 `/v2/finance` DTO |
| 单据资金联动、找零 | `BillFundLinkDto`、`CashChangeDto` 等已覆盖 | 新版已做 | `BillFundLinkDto`、`CashChangeDto` | 第四阶段已补 `/v2/finance` DTO |
| 库存流水/快照/月统计 | `InventoryLedgerDto`、`InventorySnapshotDto`、`InventoryMonthlyStatsDto` 等已覆盖 | 新版已做 | `InventoryLedgerDto`、`InventorySnapshotDto`、`InventoryMonthlyStatsDto` | 第四阶段已补 `/v2/inventory` DTO |

## `/v2` DTO 重构原则

| 原则 | 状态 | 说明 |
|---|---|---|
| 读写模型分离 | 新版待做 | `Create*Request`、`Update*Request`、`*SummaryDto`、`*DetailDto` 分开 |
| owner 归属不直接裸露给普通客户端 | 需重构 | 客户端只感知当前账号数据，owner 主要由认证上下文决定 |
| 不再让 Entity 兼任 Request | 新版需要去掉 | `/v2` 必须全部改用专用请求 DTO |
| 统一金额与数量语义 | 需重构 | DTO 先文档化为“高精度金额/数量语义”，后续代码再决定序列化策略 |

## 第一阶段 DTO 落点

1. 保留 `/v1` DTO 不删
2. 新增 `/v2` 命名空间：
   - `api/dto/v2/auth`
   - `api/dto/v2/product`
   - `api/dto/v2/partner`
   - `api/dto/v2/sales`
   - `api/dto/v2/purchase`
   - `api/dto/v2/finance`
   - `api/dto/v2/inventory`
   - `api/dto/v2/media`
   - `api/dto/v2/agent`
   - `api/dto/v2/sync`
3. 本阶段不新增会员相关 DTO

## 第一阶段已落地

| DTO 组 | 状态 | 当前实现 | 备注 |
|---|---|---|---|
| `api/dto/v2/sales/V2SaleOrderDtos` | 新版已做 | 已覆盖销售单列表/详情/创建/草稿更新/收款/状态更新/取消 | snake_case 输出 |
| `api/dto/v2/purchase/V2PurchaseOrderDtos` | 新版已做 | 已覆盖采购单列表/详情/创建 | snake_case 输出 |
| `api/dto/v2/pay/V2PayOrderDtos` | 新版已做 | 已覆盖付款单列表/详情/创建/状态更新，且已补 `accountId` | `PAID` 语义在 service 侧按幂等处理 |
| `api/dto/v2/sales/V2SalesReturnDtos` | 新版已做 | 已覆盖销售退货列表/详情/创建/确认/退款 | 本轮补了 `CreateItemRequest.quantity @Positive` 与 `items @NotEmpty` |
| `api/dto/v2/purchase/V2PurchaseReceiptDtos` | 新版已做 | 已覆盖采购收货列表/详情/创建/确认/取消 | supplier 的可信性与 owner 一致性在 service 侧防御校验 |
| `api/dto/v2/product/V2ProductDtos` | 新版已做 | 已覆盖商品、分类、单位、价格层级、供应关系的读写模型 | 已包含 `ProductResponse/ProductWriteRequest/CategoryWriteRequest/UnitWriteRequest/PriceLevelWriteRequest/ProductSupplierRelationWriteRequest` |
| `api/dto/v2/partner/V2PartnerDtos` | 新版已做 | 已覆盖客户、供应商、分组、联系人读写模型 | 包含 group/contact 与 customer/supplier 两层 DTO |
| `api/dto/v2/finance/V2FinanceDtos` | 新版已做 | 已覆盖账户、转账、单据资金关联、找零读写模型 | 账户已拆为 `AccountCreateRequest` / `AccountUpdateRequest`，避免更新接口误收 `balance` |
| `api/dto/v2/inventory/V2InventoryDtos` | 新版已做 | 已覆盖库存账本、快照、月统计读写模型 | 包含 inventory_ledger/inventory_snapshot/inventory_monthly_stats DTO |
| `api/dto/v2/sync/V2SyncDtos` | 新版已做 | 已覆盖 `/v2/sync` health/cursor/pull/upload DTO | 首轮 owner+client 同步合同；cursor 字段当前是 opaque token，不保证纯时间戳格式；客户端应在本地应用成功后再携带该 token 调 `ack` |
| `api/dto/v2/sync/V2ImportJobDtos` | 新版已做 | 已覆盖导入任务列表、详情、创建、重试 DTO | 首轮 import job 合同；当前状态流约定为 `failed/cancelled -> retry`、`pending/running -> cancel` |
| `api/dto/agent/V2AgentDtos` | 待验证 | B07 新增 `AgentConversationUpdateRequest`，移除 `AgentMessageListResponse` 死代码 | 会话更新请求 DTO 含 status 约束 `[active, closed, archived]` |
| `api/dto/v2/media/V2MediaDtos` | 待验证 | B07 移除 `MediaBindingListResponse` 死代码 | 清理未被任何接口引用的响应 DTO |

## 商品域第三阶段 DTO 补充

| DTO | 状态 | 当前实现 | 备注 |
|---|---|---|---|
| `PriceLevelResponse` / `PriceLevelWriteRequest` | 新版已做 | 已承接 `/v2/product-price-levels` 读写 | owner 级价格层级主数据 |
| `ProductPriceValueResponse` / `ProductPriceValueWriteRequest` | 新版已做 | 已承接 `/v2/products` 内嵌多价格值 | 通过 `level_id + price` 表达商品价格快照 |
| `ProductSupplierRelationResponse` / `ProductSupplierRelationWriteRequest` | 新版已做 | 已承接 `/v2/product-supplier-relations` 与 `/v2/products` 内嵌供应关系 | 含默认供应商、优先级、最近采购价、备注 |
