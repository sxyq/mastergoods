# Server entity 模块分析

- 对应源码目录：`src/main/java/com/zhihuiji/backend/domain/entity`
- 当前实体数：34
- 当前实体：
  - `UserEntity`
  - `SessionEntity`
  - `ProductEntity`
  - `ProductCategoryEntity`
  - `ProductUnitEntity`
  - `ProductPriceLevelEntity`
  - `ProductSupplierRelationEntity`
  - `CustomerEntity`
  - `SupplierEntity`
  - `PartnerGroupEntity`
  - `PartnerContactEntity`
  - `SaleOrderEntity`
  - `SaleOrderItemEntity`
  - `PurchaseOrderEntity`
  - `PurchaseOrderItemEntity`
  - `PaymentEntity`
  - `PayOrderEntity`
  - `FinanceRecordEntity`
  - `InventoryAdjustmentEntity`
  - `AgentTaskEntity`
  - `AgentNotificationEntity`
  - `SyncCursorEntity`
  - `ImportJobEntity`
  - `AccountEntity`
  - `AccountTransferEntity`
  - `BillFundLinkEntity`
  - `CashChangeRecordEntity`
  - `InventoryLedgerEntity`
  - `InventorySnapshotEntity`
  - `InventoryMonthlyStatsEntity`
  - `SalesReturnEntity`
  - `SalesReturnItemEntity`
  - `PurchaseReceiptEntity`
  - `PurchaseReceiptItemEntity`
- 关键辅助类型：
  - `SyncCursorId`

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
| 首版实体集合 | 新版已做 | 旧版本地库字段更厚 | 支撑当前 `/v1` 后端与 `/v2` 前五阶段扩域 | 33 个实体已存在 | 能支撑当前安卓版本与商品/伙伴/单据/财务/库存扩域起点 |
| `owner_user_id` 统一归属 | 新版已做 | 旧版无统一 owner | 所有核心业务实体统一补 owner 维度 | 首批核心实体已补 owner 字段与迁移 | repository/service 过滤继续推进 |
| 商品分类、单位、价格层级、供应关系、分组、联系人实体 | 新版已做 | 旧版这些表域更厚 | 第二、三阶段逐步补第一批扩域表 | `ProductCategoryEntity`、`ProductUnitEntity`、`ProductPriceLevelEntity`、`ProductSupplierRelationEntity`、`PartnerGroupEntity`、`PartnerContactEntity` 已新增 | 商品媒体、账户、库存账本等仍待补 |
| 财务与库存底座扩域实体 | 新版已做 | 旧版表域更厚 | 第四阶段补账户、转账、单据资金关联、找零、库存账本、快照、月统计 | `AccountEntity`、`AccountTransferEntity`、`BillFundLinkEntity`、`CashChangeRecordEntity`、`InventoryLedgerEntity`、`InventorySnapshotEntity`、`InventoryMonthlyStatsEntity` 已新增 | 媒体附件等仍待补 |
| 单据增强第五阶段实体 | 新版已做 | 旧版有更厚的退货/收货态 | 第五阶段补销售退货与采购收货实体 | `SalesReturnEntity`、`SalesReturnItemEntity`、`PurchaseReceiptEntity`、`PurchaseReceiptItemEntity` 已新增 | 为独立退货和采购收货路由打底 |
| 会员实体 | 新版需要去掉 | 旧版可能存在会员/积分扩展 | 当前阶段不纳入 | 当前 server 也不应新增 member 实体 | 若恢复需单独 spec |

## 现有实体字段清单

| 实体 | 现有字段 | 当前状态 | 说明 |
|---|---|---|---|
| `UserEntity` | `id, phone, passwordHash, nickname, status, createdAt, updatedAt` | 新版已做 | 账号基础表可继续沿用 |
| `SessionEntity` | `id, userId, token, refreshToken, expiresAt, isActive, createdAt` | 新版已做 | 会话表可继续沿用 |
| `ProductEntity` | `id, ownerUserId, code, name, category, categoryId, unit, unitId, priceLevelValuesJson, salePrice, purchasePrice, stock, safeStock, status, syncStatus, syncVersion, createdAt, updatedAt` | 需重构 | 已补 owner、分类/单位引用和多价格值快照，仍缺媒体和更完整单位换算 |
| `ProductCategoryEntity` | `id, ownerUserId, name, status, sortOrder, createdAt, updatedAt` | 新版已做 | 第二阶段商品分类主数据 |
| `ProductUnitEntity` | `id, ownerUserId, name, status, sortOrder, createdAt, updatedAt` | 新版已做 | 第二阶段商品单位主数据 |
| `ProductPriceLevelEntity` | `id, ownerUserId, code, name, status, sortOrder, createdAt, updatedAt` | 新版已做 | 第三阶段商品价格层级主数据 |
| `ProductSupplierRelationEntity` | `id, ownerUserId, productId, supplierId, isDefault, purchasePriority, lastPurchasePrice, notes, createdAt, updatedAt` | 新版已做 | 第三阶段商品-供应商关系与采购偏好主数据 |
| `CustomerEntity` | `id, ownerUserId, name, phone, level, groupId, address, notes, contactName, contactPhone, balance, status, syncStatus, syncVersion, createdAt, updatedAt` | 需重构 | 已补 owner 和 `groupId/contact*`，仍缺标签、价格策略等 |
| `SupplierEntity` | `id, ownerUserId, name, phone, groupId, address, notes, contactName, contactPhone, balance, status, syncStatus, syncVersion, createdAt, updatedAt` | 需重构 | 已补 owner 和 `groupId/contact*`，仍缺标签、价格策略等 |
| `PartnerGroupEntity` | `id, ownerUserId, partnerType, name, status, sortOrder, createdAt, updatedAt` | 新版已做 | 第二阶段客户/供应商分组主数据 |
| `PartnerContactEntity` | `id, ownerUserId, partnerType, partnerId, name, phone, title, isPrimary, createdAt, updatedAt` | 新版已做 | 第二阶段客户/供应商联系人主数据 |
| `SaleOrderEntity` | `id, ownerUserId, orderNo, customerId, customerName, subtotalAmount, discountAmount, totalAmount, paidAmount, notes, status, syncStatus, syncVersion, createdAt, updatedAt` | 需重构 | 已补 owner，仍缺运费、抹零、来源、订单态分层 |
| `SaleOrderItemEntity` | `id, ownerUserId, orderId, productId, productCode, productName, customerId, customerName, quantity, unitPrice, amount, createdAt` | 需重构 | 已补 owner，明细仍偏轻 |
| `PurchaseOrderEntity` | `id, ownerUserId, orderNo, supplierId, supplierName, totalAmount, notes, status, syncStatus, syncVersion, createdAt, updatedAt` | 需重构 | 已补 owner 和 `supplierId`，仍缺应付/实付、订单态 |
| `PurchaseOrderItemEntity` | `id, ownerUserId, orderId, productId, productCode, productName, quantity, unitCost, amount, createdAt` | 需重构 | 已补 owner，仍缺更多采购字段 |
| `PaymentEntity` | `id, ownerUserId, orderId, amount, method, referenceNo, type, createdAt` | 需重构 | 已补 owner，仍缺账户、单据资金关联细节 |
| `PayOrderEntity` | `id, ownerUserId, orderNo, supplierId, supplierName, amount, method, referenceNo, notes, accountId, status, createdAt, updatedAt, syncStatus, syncVersion` | 需重构 | 已补 owner 与 `accountId`，本轮配合 service 实现 `PAID` 幂等侧效保护，整体财务模型仍待继续扩厚 |
| `SalesReturnEntity` | `id, ownerUserId, returnNo, originalOrderId, customerId, customerName, totalAmount, refundAmount, notes, status, createdAt, updatedAt` | 新版已做 | 第五阶段独立销售退货单主表；customer 现已要求 owner 内可信存在 |
| `SalesReturnItemEntity` | `id, ownerUserId, returnId, productId, productCode, productName, quantity, unitPrice, amount, createdAt` | 新版已做 | 第五阶段销售退货明细表 |
| `PurchaseReceiptEntity` | `id, ownerUserId, receiptNo, purchaseOrderId, supplierId, supplierName, totalAmount, notes, status, createdAt, updatedAt` | 新版已做 | 第五阶段采购收货主表；supplier 现已要求 owner 内可信存在 |
| `PurchaseReceiptItemEntity` | `id, ownerUserId, receiptId, productId, productCode, productName, quantity, unitCost, amount, createdAt` | 新版已做 | 第五阶段采购收货明细表 |
| `FinanceRecordEntity` | `id, ownerUserId, recordNo, type, category, partnerName, amount, method, notes, createdAt, updatedAt, syncStatus, syncVersion` | 需重构 | 已补 owner，仍是轻量流水模型 |
| `InventoryAdjustmentEntity` | `id, ownerUserId, productId, productCode, productName, quantity, flowType, reason, operatorName, createdAt` | 需重构 | 已补 owner，仍缺账本/快照/月统计 |
| `AgentTaskEntity` | `id, ownerUserId, taskType, title, triggerSource, status, progress, inputText, resultJson, createdAt, updatedAt, completedAt` | 需重构 | 已补 owner，查询与上下文仍需收口 |
| `AgentNotificationEntity` | `id, ownerUserId, taskId, title, body, level, isRead, isDelivered, createdAt` | 需重构 | 已补 owner，查询与状态更新仍需收口 |
| `SyncCursorEntity` | `ownerUserId, clientId, lastCursor, updatedAt` | 新版已做 | 已升级为 `owner_user_id + client_id` 复合语义 |
| `SyncCursorId` | `ownerUserId, clientId` | 新版已做 | 作为 `SyncCursorEntity` 的复合键类型，确保 owner 分桶后的游标主键语义明确 |
| `ImportJobEntity` | `id, ownerUserId, requestedByUserId, clientId, sourceType, sourceUri, sourceChecksum, idempotencyKey, status, stage, retryCount, replayCursor, summaryJson, optionsJson, failureCode, failureMessage, createdAt, updatedAt, startedAt, finishedAt, lastHeartbeatAt` | 新版已做 | B06 首轮导入任务审计与重试模型 |
| `AccountEntity` | `id, ownerUserId, code, name, type, balance, isDefault, status, sortOrder, notes, createdAt, updatedAt` | 新版已做 | 第四阶段财务主数据，取代轻量方法枚举；owner+code 在 entity/migration 两层对齐 |
| `AccountTransferEntity` | `id, ownerUserId, transferNo, fromAccountId, toAccountId, amount, fee, status, notes, createdAt, updatedAt` | 新版已做 | 第四阶段账户间转账能力；owner+transferNo 在 entity/migration 两层对齐 |
| `BillFundLinkEntity` | `id, ownerUserId, billType, billId, accountId, amount, linkType, notes, createdAt, updatedAt` | 新版已做 | 第四阶段单据与资金关联 |
| `CashChangeRecordEntity` | `id, ownerUserId, orderType, orderId, receivable, received, changeAmount, accountId, status, notes, createdAt, updatedAt` | 新版已做 | 第四阶段找零记录 |
| `InventoryLedgerEntity` | `id, ownerUserId, productId, productCode, productName, warehouseId, quantityBefore, quantityChange, quantityAfter, unitCost, sourceType, sourceId, sourceNo, notes, createdAt` | 新版已做 | 第四阶段可追溯库存流水 |
| `InventorySnapshotEntity` | `id, ownerUserId, productId, productCode, productName, warehouseId, quantity, unitCost, totalValue, snapshotDate, createdAt` | 新版已做 | 第四阶段库存快照；owner+product+snapshotDate 已补唯一约束 |
| `InventoryMonthlyStatsEntity` | `id, ownerUserId, productId, productCode, productName, warehouseId, month, year, quantityIn, quantityOut, quantityAdjust, quantityBegin, quantityEnd, totalCostIn, totalCostOut, createdAt, updatedAt` | 新版已做 | 第四阶段库存月统计；owner+product+year+month 已补唯一约束 |

## 旧版表组到新版实体差异

### 商品域

| 旧版表/字段组 | 当前实体覆盖 | 状态 | 新版应补内容 | 备注 |
|---|---|---|---|---|
| `products.code/name/category(unit)` | `ProductEntity` 已覆盖基础字段 | 新版已做 | 保持 | 基础商品档案已存在 |
| `unit2/ratio` | 未覆盖 | 旧版存在新版未做 | `product_units`、多单位换算 | 支撑多包装/换算 |
| `pur_prc/sale_prc/trade_prc/prc4~prc10` | `salePrice/purchasePrice` + `priceLevelValuesJson` + `ProductPriceLevelEntity` 起步覆盖 | 需重构 | owner 私有价格层级定义 + 商品多价格值 | 第三阶段先用 `product_price_levels + products.price_level_values_json` 起步 |
| `min_stock/max_stock/last_prc/init_*` | 仅 `safeStock` 与 `stock` | 旧版存在新版未做 | 库存阈值与初始化维度扩充 | 当前库存维度偏薄 |
| `ptype_id` | `category:String` + `categoryId` 扩域位 | 需重构 | `product_categories` 独立表 | 第二阶段已补实体与迁移，`/v2/product-categories` 已落地 |
| `单位体系` | `unit:String` + `unitId` 扩域位 | 需重构 | `product_units` 独立表 | 第二阶段已补实体与迁移，`/v2/product-units` 已落地 |
| `product_suppliers` | `ProductSupplierRelationEntity` 起步覆盖 | 需重构 | 商品-供应商关系表与采购偏好 | 第三阶段已补 `/v2/product-supplier-relations` 起点、默认供应商和采购优先级 |

### 往来单位域

| 旧版表/字段组 | 当前实体覆盖 | 状态 | 新版应补内容 | 备注 |
|---|---|---|---|---|
| `companies.name/phone/addr/remark/cur_amt` | `CustomerEntity` / `SupplierEntity` 基础覆盖 | 新版已做 | 保持基础字段 | 现有分表比旧版更清晰 |
| `linkman/tel/mobile/fax/mail/birthday` | `contactName/contactPhone` + `partner_contacts` 起步覆盖 | 需重构 | `partner_contacts` 与扩展字段 | 第二阶段先补联系人主表，其他画像后续扩 |
| `grp_id/company_type_id/prc_level/disc` | `groupId` 起步覆盖 | 需重构 | 分组、类型、价格等级、折扣策略 | 第二阶段先补分组主表 |
| `belong_uid` | 无统一 owner | 需重构 | 所有往来单位补 `owner_user_id` | 与账号隔离一致 |

### 销售域

| 旧版表/字段组 | 当前实体覆盖 | 状态 | 新版应补内容 | 备注 |
|---|---|---|---|---|
| `sales + saleitems` 基础单据与明细 | `SaleOrderEntity + SaleOrderItemEntity` 已覆盖基础链路 | 新版已做 | 保持基础交易闭环 | 当前销售主流程可用 |
| `owe_amt/change_amt/express_amt/deduction_amt/disc` | 仅 `discountAmount/paidAmount` | 旧版存在新版未做 | 运费、抹零、优惠、未收金额细化 | 当前交易过程字段偏少 |
| `trade_type/trade_src/user_id/username` | 未覆盖 | 旧版存在新版未做 | 交易来源、操作人、渠道字段 | 有利于经营分析 |
| `sorders/sorderitems` 订单态 | 部分覆盖 | 旧版存在新版未做 | `sales_drafts`、`sales_orders`、`sales_returns` | 已补 `sales_returns` 与草稿确认，独立 `sales_drafts/sales_orders` 分表仍待后续扩域 |
| `payments` 更细资金联动 | `PaymentEntity` 仅基础表 | 需重构 | 账户、单据资金关系、退款细化 | 要与财务域协同 |

### 采购与财务域

| 旧版表/字段组 | 当前实体覆盖 | 状态 | 新版应补内容 | 备注 |
|---|---|---|---|---|
| `purs + puritems` 基础采购 | `PurchaseOrderEntity + PurchaseOrderItemEntity` 基础覆盖 | 新版已做 | 保持基础采购闭环 | 当前采购主流程可用 |
| `porders/porderitems` 订单/入库态 | 部分覆盖 | 旧版存在新版未做 | `purchase_receipts`、采购订单态 | 已补 `purchase_receipts`，更完整采购订单态仍待后续扩域 |
| `funds` | `FinanceRecordEntity` 轻量覆盖 | 需重构 | 更厚流水模型 | 当前只有轻量流水 |
| `accts` | `AccountEntity` 已覆盖 | 新版已做 | `accounts` | 第四阶段已补实体与迁移 |
| `billfunds/smallchange/projects` | `BillFundLinkEntity` + `CashChangeRecordEntity` 起步覆盖 | 新版已做 | 单据资金关联、找零、项目维度 | 第四阶段已补单据资金关联与找零，项目维度仍待补 |

### 库存与 AI 域

| 旧版表/字段组 | 当前实体覆盖 | 状态 | 新版应补内容 | 备注 |
|---|---|---|---|---|
| `inventories/inventoryitems/stocks/stocks_month` | `InventoryAdjustmentEntity` + `InventoryLedgerEntity` + `InventorySnapshotEntity` + `InventoryMonthlyStatsEntity` 覆盖 | 新版已做 | `inventory_ledger`、`inventory_snapshots`、`inventory_monthly_stats` | 第四阶段已补库存账本、快照、月统计 |
| AI 任务与通知 | 旧版无对应域 | `AgentTaskEntity` / `AgentNotificationEntity` 已覆盖 | 新版已做 | 后续再补 conversation/message/draft | 这是新版优势域 |
| Agent 会话状态枚举约束 | 待验证 | 旧版无对应域 | 会话状态约束为 `[active, closed, archived]`，草稿状态约束为 `[active, archived]` | B07 已在 service 层强制校验状态枚举，closed/archived 会话拒绝新消息 | 状态枚举约束尚未在 entity 层以 `@Enumerated` + validation 注解固化，当前由 service 层防御 |
| V14 迁移 ON DELETE CASCADE | 待验证 | 旧版外键无级联删除 | 会话/草稿/媒体绑定外键级联删除 | B07 已在 V14 迁移中为 `fk_agent_messages_conversation`、`fk_agent_drafts_conversation`、`fk_media_bindings_asset` 添加 `ON DELETE CASCADE` | 确保删除会话时消息/草稿自动清理，删除资产时媒体绑定自动清理 |

## 第一阶段后端重构落点

1. 给以下实体统一补 `ownerUserId`
   - `ProductEntity`
   - `CustomerEntity`
   - `SupplierEntity`
   - `SaleOrderEntity`
   - `SaleOrderItemEntity`
   - `PurchaseOrderEntity`
   - `PurchaseOrderItemEntity`
   - `PaymentEntity`
   - `PayOrderEntity`
   - `FinanceRecordEntity`
   - `InventoryAdjustmentEntity`
   - `AgentTaskEntity`
   - `AgentNotificationEntity`
   - `SyncCursorEntity`
2. owner 底座已开始落地，但 repository、service、controller 仍需继续改造，才能真正实现查询隔离
3. 第二阶段已先新增不破坏现有 `/v1` 的扩域实体起点
   - `product_categories`
   - `product_units`
   - `partner_contacts`
   - `partner_groups`
4. 第三阶段已继续补
   - `product_price_levels`
   - `product_supplier_relations`
   - `products.price_level_values_json`
5. 第四阶段已继续补
   - `accounts`
   - `account_transfers`
   - `bill_fund_links`
   - `cash_change_records`
   - `inventory_ledger`
   - `inventory_snapshots`
   - `inventory_monthly_stats`
6. 下阶段继续补
   - 媒体附件
   - 项目维度
7. 本阶段不新增会员相关实体
