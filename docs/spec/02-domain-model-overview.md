# 02 领域模型总览

## 领域划分

| 领域 | 状态 | 说明 |
|---|---|---|
| auth / tenant | 新版已做 | 账号、会话、归属边界底座已开始落地 |
| product | 需重构 | 商品、分类、单位、价格体系 |
| partner | 需重构 | 客户、供应商、联系人、分组 |
| sales | 需重构 | 销售、退货、收款、草稿 |
| purchase | 需重构 | 采购、入库、应付、付款 |
| finance | 需重构 | 账户、流水、转账、找零 |
| inventory | 需重构 | 库存流水、快照、月统计 |
| media | 新版待做 | 图片/附件资源 |
| agent | 新版已做 | AI 助手任务与通知 |
| membership | 新版需要去掉 | 暂不纳入新版范围 |

## 全局原则

| 原则 | 状态 | 说明 |
|---|---|---|
| owner_user_id 归属 | 新版已做 | 首批核心业务表已补 owner 字段与历史回填迁移 |
| /v1 兼容 | 新版已做 | 旧接口先保留 |
| /v2 契约 | 新版已做 | 单据域首批、商品域与伙伴域首批扩域、商品多价格与供应关系扩域均已落地 |

## B01 账号隔离底座

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 系统默认归属账号 | 新版已做 | 旧版无此概念 | 承接历史全局数据回填 | 已通过 `SYSTEM-LEGACY-OWNER` 预留记录设计 | 不向正常登录流暴露 |
| 业务表 owner 回填迁移 | 新版已做 | 旧版业务数据全局共享 | 历史数据统一归档到默认 owner | 已在迁移脚本中定义 | 代码层过滤仍在继续补齐 |
| `sync_cursors` 归属升级 | 新版已做 | 仅按 `client_id` 维度 | 升级为 `owner_user_id + client_id` | 已由 `SyncCursorEntity/SyncCursorId` + `V2SyncService` 落地，cursor 值已允许稳定 token 语义 | `/v1` 兼容层仍待继续收口 |
| owner 账号隔离 | 新版已做 | 旧版无统一账号边界 | 核心业务表补 `owner_user_id` | V7 迁移脚本 + `CurrentOwnerService` + `/v2` 默认 owner 过滤 | `/v1` 兼容层仍在补齐 |

## B02 商品域与伙伴域首批扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 商品域首批扩域表 | 新版已做 | 旧版商品有分类与单位体系 | 先补 `product_categories`、`product_units` | 已新增 `V8__product_and_partner_expansion.sql`、实体、repository、service、controller | 为 `/v2/products` 扩域奠定基础 |
| 伙伴域首批扩域表 | 新版已做 | 旧版往来单位有分组与联系人能力 | 先补 `partner_groups`、`partner_contacts` | 已新增迁移、实体、repository、service、controller | tags 和价格策略后续再补 |
| 伙伴域 `/v2` 接口 | 新版已做 | 旧版往来单位接口字段薄 | 客户/供应商/分组/联系人完整 CRUD | `/v2/customers`、`/v2/suppliers`、`/v2/customer-groups`、`/v2/supplier-groups`、`/v2/customer-contacts`、`/v2/supplier-contacts` 已落地 | 证据：6个 V2 Controller |

## B03 商品多价格与供应关系扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 商品价格层级 `product_price_levels` | 新版已做 | 旧版无多价格层级 | 支持零售/批发/自定义等多级定价 | `/v2/product-price-levels` 已落地：CRUD | 证据：`V2ProductPriceLevelController.java` |
| 商品供应关系 `product_supplier_relations` | 新版已做 | 旧版无商品-供应商关联 | 商品与供应商的采购关系 | `/v2/product-supplier-relations` 已落地：CRUD | 证据：`V2ProductSupplierRelationController.java` |
| `/v2/products` 扩域读模型 | 新版已做 | 旧版商品接口字段薄 | 返回 `price_levels/default_supplier/supplier_relations` | 已在 `V2ProductController` 中实现 | 安卓后续可基于此结构设计新模型 |

## 已落地的单据域 `/v2` 首批契约

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 单据域 `/v2` 首批 | 新版已做 | 旧版无 `/v2` 单据接口 | 销售/采购/付款三条新契约 | `/v2/sale-orders`、`/v2/purchase-orders`、`/v2/pay-orders` 已落地 | 证据：3个 V2 单据 Controller |

## B04 财务与库存底座扩域

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 财务底座 | 新版已做 | 旧版有账户/转账/单据资金联动 | 建立独立账户体系与单据资金关联 | `accounts`、`account_transfers`、`bill_fund_links`、`cash_change_records` 已落地 | 证据：`V2AccountController`、`V2AccountTransferController`、`V2BillFundLinkController` |
| 库存底座 | 新版已做 | 旧版有库存流水/快照/月结 | 建立可追溯库存账本 | `inventory_ledger`、`inventory_snapshots`、`inventory_monthly_stats` 已落地 | 证据：`V2InventoryController` |

## B05 单据域增强

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| 销售草稿确认 | 新版已做 | 旧版有订单态/草稿态 | 草稿确认链路 | `OrderStatus` 新增 CONFIRMED(3)，`/v2/sale-orders/{id}/confirm` 已落地 | 证据：`V2SaleOrderService.confirm()` |
| 销售退货 | 新版已做 | 旧版可表达退货 | 独立退货单 | `sales_returns` + `sales_return_items`，`/v2/sales-returns` 已落地 | 证据：`V2SalesReturnController` |
| 采购收货 | 新版已做 | 旧版有订单/入库流转 | 采购收货与入库分离 | `purchase_receipts` + `purchase_receipt_items`，`/v2/purchase-receipts` 已落地 | 证据：`V2PurchaseReceiptController` |
| 付款账户联动 | 新版已做 | 旧版付款无账户关联 | 付款与账户闭环 | `PayOrderEntity.account_id`，付款时扣减账户余额并创建 `bill_fund_link`，且本轮已补 `PAID` 幂等保护 | 证据：`V2PayOrderService.syncPaidAccountSideEffects()` |
