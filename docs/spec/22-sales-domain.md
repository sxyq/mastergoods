# 22 销售域

## 需求表

| 对象 | 状态 | 旧版情况 | 新版目标 | 当前实现 | 备注 |
|---|---|---|---|---|---|
| sale_orders | 需重构 | 旧版交易过程字段更细 | 销售单据主表 | 已有基础结构 | 需增强 |
| sale_order_items | 需重构 | 明细字段更丰富 | 销售明细表 | 已有基础结构 | 需增强 |
| sale_orders owner 归属 | 新版已做 | 旧版无统一 owner | 销售单主表与明细表全部 owner 隔离 | 已补 `owner_user_id`，并通过 service/repository 默认过滤 | 历史数据回填到系统默认归属账号 |
| /v2/sale-orders | 新版已做 | 旧版无 `/v2` 契约层 | 单据域首批新版接口 | 已落地列表、详情、创建、草稿更新、收款、状态更新、取消 | 仍沿用当前单据语义 |
| sales_returns | 新版已做 | 旧版可表达退货 | 独立退货单 | 已落地 `sales_returns` + `sales_return_items`，`/v2/sales-returns` 列表/详情/创建 | 本轮补强：`CreateItemRequest.quantity` 与 service 双重禁止 0/负数量，`customerId` 必须 owner 内存在，`originalOrderId` 与客户一致性已校验；对应 service test 已实跑通过 |
| sales_drafts | 新版已做 | 旧版有订单态/草稿态 | 草稿/预订单链路 | 已增强：`OrderStatus` 新增 CONFIRMED(3)，草稿更新支持明细编辑，新增 confirm 端点 | B05 已补；证据：`V2SaleOrderService` 增强 |
| payments | 需重构 | 旧版收款语义更丰富 | 订单收款独立表 | 已有基础结构 | 需增强；B05 已增强付款账户关联（`PayOrderEntity` 新增 `account_id`，付款时扣减账户余额并创建 `bill_fund_link`） |
